package dev.chandradsl.m3ecanvas.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.canvas.AlignmentSnapper
import dev.chandradsl.m3ecanvas.editor.canvas.SnapResult
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry
import dev.chandradsl.m3ecanvas.editor.history.HistoryStack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the editor's state in response to user actions.
 * Hierarchy-aware: nodes can be top-level or nested inside containers.
 * Supports command undo/redo history and clipboard operations.
 */
class EditorController(
    initialProject: M3EProject = newProject(),
    val componentRegistry: ComponentRegistry = ComponentRegistry.default,
    val alignmentSnapper: AlignmentSnapper = AlignmentSnapper.default
) {

    private val history = HistoryStack<M3EProject>()
    private var preDragProject: M3EProject? = null

    var snappingEnabled: Boolean by mutableStateOf(true)

    private var _state by mutableStateOf(
        EditorState(
            project = initialProject,
            canUndo = false,
            canRedo = false
        )
    )

    private val _stateFlow = MutableStateFlow(_state)

    /** StateFlow stream for non-Compose or headless consumers (e.g. CLI, tests, background workers). */
    val stateFlow: StateFlow<EditorState> = _stateFlow.asStateFlow()

    /** Primary Compose observable state. Automatically triggers recomposition upon mutation. */
    var state: EditorState
        get() = _state
        private set(value) {
            _state = value
            _stateFlow.value = value
        }

    fun toggleSnapping() {
        snappingEnabled = !snappingEnabled
    }

    fun toggleMultiDevicePreview() {
        state = state.copy(multiDevicePreview = !state.multiDevicePreview)
    }

    val canUndo: Boolean
        get() = history.canUndo

    val canRedo: Boolean
        get() = history.canRedo

    val undoCount: Int
        get() = history.undoCount

    val redoCount: Int
        get() = history.redoCount

    private fun pushState() {
        history.push(state.project)
    }

    private fun updateProject(
        newProject: M3EProject,
        selectedNodeIds: Set<String> = state.selectedNodeIds
    ) {
        state = state.copy(
            project = newProject,
            selectedNodeIds = selectedNodeIds,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            undoCount = history.undoCount,
            redoCount = history.redoCount
        )
    }

    //region History & Undo/Redo

    fun undo() {
        val previous = history.undo(state.project) ?: return
        val currentDevice = state.project.deviceProfile
        val restoredProject = adaptProjectToDevice(previous, currentDevice)
        val validSelections = state.selectedNodeIds.filter { restoredProject.findNode(it) != null }.toSet()
        state = state.copy(
            project = restoredProject,
            selectedNodeIds = validSelections,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            undoCount = history.undoCount,
            redoCount = history.redoCount
        )
    }

    fun redo() {
        val next = history.redo(state.project) ?: return
        val currentDevice = state.project.deviceProfile
        val restoredProject = adaptProjectToDevice(next, currentDevice)
        val validSelections = state.selectedNodeIds.filter { restoredProject.findNode(it) != null }.toSet()
        state = state.copy(
            project = restoredProject,
            selectedNodeIds = validSelections,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            undoCount = history.undoCount,
            redoCount = history.redoCount
        )
    }

    fun getUndoSnapshots(): List<M3EProject> = history.getUndoList()
    fun getRedoSnapshots(): List<M3EProject> = history.getRedoList()

    fun undoSteps(steps: Int) {
        val previous = history.undoSteps(steps, state.project) ?: return
        val currentDevice = state.project.deviceProfile
        val restoredProject = adaptProjectToDevice(previous, currentDevice)
        val validSelections = state.selectedNodeIds.filter { restoredProject.findNode(it) != null }.toSet()
        state = state.copy(
            project = restoredProject,
            selectedNodeIds = validSelections,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            undoCount = history.undoCount,
            redoCount = history.redoCount
        )
    }

    fun redoSteps(steps: Int) {
        val next = history.redoSteps(steps, state.project) ?: return
        val currentDevice = state.project.deviceProfile
        val restoredProject = adaptProjectToDevice(next, currentDevice)
        val validSelections = state.selectedNodeIds.filter { restoredProject.findNode(it) != null }.toSet()
        state = state.copy(
            project = restoredProject,
            selectedNodeIds = validSelections,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            undoCount = history.undoCount,
            redoCount = history.redoCount
        )
    }

    fun clearHistory() {
        history.clear()
        state = state.copy(
            canUndo = false,
            canRedo = false,
            undoCount = 0,
            redoCount = 0
        )
    }

    //endregion

    //region Clipboard

    /** Stores the first currently selected node into the clipboard buffer. */
    fun copySelected(): CanvasNode? {
        val selected = state.selectedNodes.firstOrNull() ?: return null
        state = state.copy(clipboard = selected)
        return selected
    }

    /**
     * Pastes the node currently in clipboard (or [sourceNode] if provided).
     * Automatically handles container insertion, slot routing, or canvas root placement.
     */
    fun paste(sourceNode: CanvasNode? = null): CanvasNode? {
        val nodeToPaste = sourceNode ?: state.clipboard ?: return null
        val clone = nodeToPaste.deepCloneWithNewIds(offsetPosition = true)
        val hasScaffold = state.project.nodes.any { it.type == ComponentType.SCAFFOLD }

        // Reject pasting a second Scaffold if one already exists
        if (clone.type == ComponentType.SCAFFOLD && hasScaffold) {
            return null
        }

        val selected = state.selectedNodes.firstOrNull()
        val targetContainer = findValidTargetContainer(forType = clone.type, startingFromNodeId = selected?.id)

        val updatedProject = if (targetContainer != null) {
            val supported = targetContainer.type.supportedSlots()
            val targetSlot = if (supported.isNotEmpty()) {
                val allowed = clone.type.allowedSlotsIn(targetContainer.type)
                val occupiedSlots = targetContainer.children.mapNotNull { it.slot }.toSet()
                allowed.firstOrNull { it.isMultiOccupant || it !in occupiedSlots }
                    ?: clone.type.canonicalSlotFor(targetContainer.type)
            } else {
                null
            }
            val positionedClone = clone.copy(slot = targetSlot)
            if (targetSlot != null && !targetSlot.isMultiOccupant) {
                state.project.updateNode(targetContainer.withChildInSlot(positionedClone, targetSlot))
            } else {
                state.project.updateNode(targetContainer.withChild(positionedClone))
            }
        } else if (clone.type.isAllowedAtRoot(hasScaffold)) {
            state.project.withNode(clone)
        } else {
            return null
        }

        pushState()
        updateProject(updatedProject, selectedNodeIds = setOf(clone.id))
        return clone
    }

    /**
     * Duplicates the currently selected node (Ctrl+D), placing the clone as an immediate sibling.
     */
    fun duplicateSelected(): CanvasNode? {
        val selected = state.selectedNodes.firstOrNull() ?: return null
        if (selected.isLockedInSlot) return null // Locked slots cannot be duplicated
        if (selected.type == ComponentType.SCAFFOLD) return null // Scaffold cannot be duplicated
        val parent = findParent(selected.id)
        if (parent != null) {
            if (!parent.type.canAcceptChild(selected.type)) return null
            val slot = selected.slot
            if (slot != null && !slot.isMultiOccupant) return null
        } else {
            val hasScaffold = state.project.nodes.any { it.type == ComponentType.SCAFFOLD }
            if (!selected.type.isAllowedAtRoot(hasScaffold)) return null
        }

        val clone = selected.deepCloneWithNewIds(offsetPosition = !selected.type.isOverlay())
        pushState()

        val updatedProject = if (parent != null) {
            val index = parent.children.indexOfFirst { it.id == selected.id }
            val updatedChildren = parent.children.toMutableList().apply {
                if (index >= 0) add(index + 1, clone) else add(clone)
            }
            state.project.updateNode(parent.copy(children = updatedChildren))
        } else {
            val index = state.project.nodes.indexOfFirst { it.id == selected.id }
            val updatedNodes = state.project.nodes.toMutableList().apply {
                if (index >= 0) add(index + 1, clone) else add(clone)
            }
            state.project.copy(nodes = updatedNodes)
        }

        updateProject(updatedProject, selectedNodeIds = setOf(clone.id))
        return clone
    }

    //endregion

    //region Selection

    fun selectNode(nodeId: String) {
        if (state.isInteractiveMode) return
        state = state.copy(selectedNodeIds = setOf(nodeId))
    }

    fun toggleSelectNode(nodeId: String) {
        if (state.isInteractiveMode) return
        val current = state.selectedNodeIds
        val updated = if (nodeId in current) current - nodeId else current + nodeId
        state = state.copy(selectedNodeIds = updated)
    }

    fun selectAdditionalNode(nodeId: String) {
        if (state.isInteractiveMode) return
        state = state.copy(selectedNodeIds = state.selectedNodeIds + nodeId)
    }

    fun selectNodes(nodeIds: Collection<String>) {
        if (state.isInteractiveMode) return
        state = state.copy(selectedNodeIds = nodeIds.toSet())
    }

    fun selectAll() {
        if (state.isInteractiveMode) return
        val allTopLevelIds = state.project.nodes.map { it.id }.toSet()
        state = state.copy(selectedNodeIds = allTopLevelIds)
    }

    fun clearSelection() {
        state = state.copy(selectedNodeIds = emptySet())
    }

    //endregion

    //region Editor Mode (Design vs Interactive Preview)

    /** Sets the editor operating mode. Entering INTERACTIVE mode clears selection and drag state. */
    fun setMode(mode: EditorMode) {
        if (state.mode == mode) return
        state = state.copy(
            mode = mode,
            selectedNodeIds = if (mode == EditorMode.INTERACTIVE) emptySet() else state.selectedNodeIds,
            dismissedOverlayIds = if (mode == EditorMode.DESIGN) emptySet() else state.dismissedOverlayIds,
            drag = null,
            alignmentGuides = emptyList()
        )
    }

    /** Toggles between DESIGN mode and INTERACTIVE preview mode. */
    fun toggleInteractiveMode() {
        val next = if (state.mode == EditorMode.INTERACTIVE) EditorMode.DESIGN else EditorMode.INTERACTIVE
        setMode(next)
    }

    /** Dismisses a modal overlay dialog or sheet in interactive mode. */
    fun dismissOverlay(nodeId: String) {
        state = state.copy(dismissedOverlayIds = state.dismissedOverlayIds + nodeId)
    }

    /** Resets all dismissed overlays and transient interactive preview states. */
    fun resetInteractiveState() {
        state = state.copy(dismissedOverlayIds = emptySet())
    }

    //endregion

    //region Node editing

    /** Adds a new top-level component of [type] at [position] and selects it. Returns true if added, false if rejected. */
    fun addNode(type: ComponentType, position: CanvasPosition): Boolean {
        val hasScaffold = state.project.nodes.any { it.type == ComponentType.SCAFFOLD }
        if (!type.isAllowedAtRoot(hasScaffold)) return false

        val existingCount = state.project.nodes.count { it.type == type }
        val name = "${type.displayName} ${existingCount + 1}"

        val targetPosition = if (type == ComponentType.SCAFFOLD || type.isOverlay()) CanvasPosition.Zero else position
        val node = CanvasNode(
            type = type,
            name = name,
            position = targetPosition,
            size = defaultSizeFor(type = type)
        )

        pushState()
        updateProject(
            newProject = state.project.withNode(node = node),
            selectedNodeIds = setOf(node.id)
        )
        return true
    }

    /**
     * Finds the nearest legal container for [forType], starting from [startingFromNodeId]
     * (or the currently selected node) and walking up ancestor containers.
     * If no ancestor accepts [forType], checks the Scaffold content container, or returns null.
     */
    fun findValidTargetContainer(forType: ComponentType, startingFromNodeId: String? = null): CanvasNode? {
        val rootScaffold = state.project.nodes.firstOrNull { it.type == ComponentType.SCAFFOLD }

        // Scaffold structural slot components (TopAppBar, NavigationBar, BottomAppBar, Rail, Drawer, FAB, Snackbar)
        // must bind directly to the root Scaffold if one exists.
        if (rootScaffold != null && forType.canonicalSlot() != SlotRole.CONTENT) {
            return if (rootScaffold.type.canAcceptChild(forType)) rootScaffold else null
        }

        val startId = startingFromNodeId ?: state.selectedNodeIds.firstOrNull()
        var current = if (startId != null) state.project.findNode(startId) else null

        // If the selected node itself is a container and accepts forType, use it
        if (current != null && current.isContainer && current.type.canAcceptChild(forType)) {
            return current
        }

        // Walk up ancestor containers
        var ancestor = if (current != null) findParent(current.id) else null
        while (ancestor != null) {
            if (ancestor.isContainer && ancestor.type.canAcceptChild(forType)) {
                return ancestor
            }
            ancestor = findParent(ancestor.id)
        }

        // If no ancestor accepts forType, check the Scaffold's main content container
        if (rootScaffold != null && forType != ComponentType.SCAFFOLD) {
            val contentContainer = rootScaffold.children.firstOrNull {
                it.slot == SlotRole.CONTENT && it.isContainer && it.type.canAcceptChild(forType)
            }
            if (contentContainer != null) return contentContainer
            if (rootScaffold.type.canAcceptChild(forType)) return rootScaffold
        }

        // Finally check any top-level container
        return state.project.nodes.firstOrNull { it.isContainer && it.type.canAcceptChild(forType) }
    }

    /** Adds a new child of [type] inside the container with [containerId]. Returns true if successfully added, false if rejected. */
    fun addChildToContainer(containerId: String, type: ComponentType): Boolean {
        val container = state.project.findNode(nodeId = containerId) ?: return false
        if (!container.isContainer) return false
        if (!container.type.canAcceptChild(type)) return false

        val childCount = container.children.count { it.type == type }
        val name = "${type.displayName} ${childCount + 1}"

        val supported = container.type.supportedSlots()
        val targetSlot = if (supported.isNotEmpty()) {
            val allowed = type.allowedSlotsIn(container.type)
            val occupiedSlots = container.children.mapNotNull { it.slot }.toSet()
            allowed.firstOrNull { it.isMultiOccupant || it !in occupiedSlots }
                ?: type.canonicalSlotFor(container.type)
        } else {
            null
        }

        val child = CanvasNode(
            type = type,
            name = name,
            position = CanvasPosition.Zero,
            size = defaultSizeFor(type = type),
            slot = targetSlot
        )

        val updatedContainer = if (targetSlot != null && !targetSlot.isMultiOccupant) {
            container.withChildInSlot(child = child, slotRole = targetSlot)
        } else {
            container.withChild(child = child)
        }

        pushState()
        updateProject(
            newProject = state.project.updateNode(node = updatedContainer),
            selectedNodeIds = setOf(child.id)
        )
        return true
    }

    /** Sets the slot role for [nodeId], replacing single-occupant occupants if necessary. */
    fun setNodeSlot(nodeId: String, slotRole: SlotRole?) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        if (node.slot == slotRole) return

        val parent = findParent(nodeId = nodeId)
        if (parent != null && slotRole != null) {
            if (!node.type.allowedSlotsIn(parent.type).contains(slotRole)) return
            val cleanedChildren = if (!slotRole.isMultiOccupant) {
                parent.children.filterNot { it.id != nodeId && it.slot == slotRole }
            } else {
                parent.children
            }
            val updatedChildren = cleanedChildren.map {
                if (it.id == nodeId) it.copy(slot = slotRole) else it
            }
            val updatedParent = parent.copy(children = updatedChildren)
            pushState()
            updateProject(newProject = state.project.updateNode(node = updatedParent))
        } else {
            pushState()
            updateProject(newProject = state.project.updateNode(node = node.copy(slot = slotRole)))
        }
    }

    /** Removes the node with [nodeId] from anywhere in the tree. */
    fun removeNode(nodeId: String) {
        pushState()
        updateProject(
            newProject = state.project.removeNode(nodeId = nodeId),
            selectedNodeIds = state.selectedNodeIds - nodeId
        )
    }

    /** Deletes all currently selected nodes and clears the selection. */
    fun deleteSelected() {
        val ids = state.selectedNodeIds
        if (ids.isEmpty()) return
        pushState()
        var project = state.project
        for (id in ids) {
            project = project.removeNode(nodeId = id)
        }
        updateProject(
            newProject = project,
            selectedNodeIds = emptySet()
        )
    }

    /**
     * Nudges selected top-level nodes by the given delta. Children are skipped
     * because their position is controlled by their container's layout.
     */
    fun nudgeSelected(dx: Float, dy: Float) {
        val ids = state.selectedNodeIds
        if (ids.isEmpty()) return
        var project = state.project
        var movedAny = false
        for (id in ids) {
            val isTopLevel = project.nodes.any { it.id == id }
            if (!isTopLevel) continue
            val node = project.findNode(nodeId = id) ?: continue
            if (node.isLockedInSlot) continue
            project = project.updateNode(node = node.movedBy(dx = dx, dy = dy))
            movedAny = true
        }
        if (movedAny) {
            pushState()
            updateProject(newProject = project)
        }
    }

    /** Applies or replaces a [property] on the node with [nodeId], wherever it is. */
    fun updateNodeProperty(nodeId: String, property: ComponentProperty) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        val updatedNode = node.withProperty(property = property)
        pushState()
        updateProject(newProject = state.project.updateNode(node = updatedNode))
    }

    /** Renames the node with [nodeId]. */
    fun renameNode(nodeId: String, name: String) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        pushState()
        updateProject(newProject = state.project.updateNode(node = node.copy(name = name)))
    }

    /** Replaces the layout config of the container with [nodeId]. */
    fun updateLayoutConfig(nodeId: String, config: LayoutConfig) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        pushState()
        updateProject(newProject = state.project.updateNode(node = node.copy(layoutConfig = config)))
    }

    //endregion

    //region Modifier chain

    /** Appends [spec] to the end of the modifier chain of [nodeId]. */
    fun addModifier(nodeId: String, spec: ModifierSpec) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        pushState()
        updateProject(newProject = state.project.updateNode(node = node.withModifier(spec = spec)))
    }

    /** Removes the modifier matching [specId] from [nodeId]. */
    fun removeModifier(nodeId: String, specId: String) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        pushState()
        updateProject(newProject = state.project.updateNode(node = node.withoutModifier(specId = specId)))
    }

    /** Replaces the modifier matching [spec.id] on [nodeId]. */
    fun updateModifier(nodeId: String, spec: ModifierSpec) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        pushState()
        updateProject(newProject = state.project.updateNode(node = node.updateModifier(spec = spec)))
    }

    /** Moves the modifier matching [specId] one position earlier. */
    fun moveModifierUp(nodeId: String, specId: String) {
        moveModifier(nodeId = nodeId, specId = specId, delta = -1)
    }

    /** Moves the modifier matching [specId] one position later. */
    fun moveModifierDown(nodeId: String, specId: String) {
        moveModifier(nodeId = nodeId, specId = specId, delta = 1)
    }

    private fun moveModifier(nodeId: String, specId: String, delta: Int) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        val updatedNode = node.moveModifier(specId = specId, delta = delta)
        if (updatedNode != node) {
            pushState()
            updateProject(newProject = state.project.updateNode(node = updatedNode))
        }
    }

    //endregion

    //region Reorder

    /** Moves the node one position earlier among its siblings (or root nodes). */
    fun moveNodeUp(nodeId: String) {
        reorderNode(nodeId = nodeId, delta = -1)
    }

    /** Moves the node one position later among its siblings (or root nodes). */
    fun moveNodeDown(nodeId: String) {
        reorderNode(nodeId = nodeId, delta = 1)
    }

    private fun reorderNode(nodeId: String, delta: Int) {
        val parent = findParent(nodeId = nodeId)

        if (parent == null) {
            val nodes = state.project.nodes
            val index = nodes.indexOfFirst { it.id == nodeId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= nodes.size) return
            val reordered = nodes.toMutableList().apply {
                val item = removeAt(index = index)
                add(index = target, element = item)
            }
            pushState()
            updateProject(newProject = state.project.copy(nodes = reordered))
        } else {
            val children = parent.children
            val index = children.indexOfFirst { it.id == nodeId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= children.size) return
            val reordered = children.toMutableList().apply {
                val item = removeAt(index = index)
                add(index = target, element = item)
            }
            val updatedParent = parent.copy(children = reordered)
            pushState()
            updateProject(newProject = state.project.updateNode(node = updatedParent))
        }
    }

    /** Finds the parent container node of [nodeId], or null if it is a root node. */
    fun findParent(nodeId: String): CanvasNode? {
        for (root in state.project.nodes) {
            val found = findParentInternal(node = root, targetId = nodeId)
            if (found != null) return found
        }
        return null
    }

    private fun findParentInternal(node: CanvasNode, targetId: String): CanvasNode? {
        for (child in node.children) {
            if (child.id == targetId) return node
            val deeper = findParentInternal(node = child, targetId = targetId)
            if (deeper != null) return deeper
        }
        return null
    }

    //endregion

    //region Drag

    /** Begins a drag on the node with [nodeId]. If multiple nodes are selected, all non-locked selected nodes move together. */
    fun startDrag(nodeId: String) {
        if (state.isInteractiveMode) return
        val node = state.project.findNode(nodeId = nodeId) ?: return
        if (node.isLockedInSlot || node.type == ComponentType.SCAFFOLD || node.type.isOverlay()) return // Locked slots, Scaffolds, and modal overlays cannot be moved arbitrarily
        preDragProject = state.project

        val targetNodeIds = if (nodeId in state.selectedNodeIds && state.selectedNodeIds.size > 1) {
            state.selectedNodeIds.filter { id ->
                val n = state.project.findNode(id)
                n != null && !n.isLockedInSlot && n.type != ComponentType.SCAFFOLD && !n.type.isOverlay()
            }.toSet()
        } else {
            setOf(nodeId)
        }

        val originalPositions = targetNodeIds.mapNotNull { id ->
            val n = state.project.findNode(id)
            if (n != null) id to n.position else null
        }.toMap()

        state = state.copy(
            drag = DragState(
                nodeId = nodeId,
                originalNodePosition = node.position,
                nodeIds = targetNodeIds,
                originalPositions = originalPositions
            )
        )
    }

    fun updateDrag(deltaX: Float, deltaY: Float) {
        val drag = state.drag ?: return
        val primaryNode = state.project.findNode(drag.nodeId) ?: return

        val candidatePos = primaryNode.position.offset(deltaX, deltaY)
        val otherNodes = state.project.nodes.filter { it.id !in drag.nodeIds && it.type != ComponentType.SCAFFOLD && !it.type.isOverlay() }
        val snapResult = if (snappingEnabled && drag.nodeIds.size == 1) {
            alignmentSnapper.computeSnap(
                node = primaryNode,
                candidatePos = candidatePos,
                otherNodes = otherNodes,
                deviceWidth = state.project.deviceProfile.size.width,
                deviceHeight = state.project.deviceProfile.size.height,
                enabled = true
            )
        } else {
            SnapResult(candidatePos, emptyList())
        }

        val effectiveDeltaX = snapResult.snappedPosition.x - primaryNode.position.x
        val effectiveDeltaY = snapResult.snappedPosition.y - primaryNode.position.y

        var updatedProject = state.project
        for (id in drag.nodeIds) {
            val n = updatedProject.findNode(nodeId = id) ?: continue
            if (!n.isLockedInSlot && n.type != ComponentType.SCAFFOLD && !n.type.isOverlay()) {
                updatedProject = updatedProject.updateNode(node = n.movedBy(dx = effectiveDeltaX, dy = effectiveDeltaY))
            }
        }
        state = state.copy(
            project = updatedProject,
            alignmentGuides = snapResult.guides
        )
    }

    fun endDrag() {
        val prior = preDragProject
        preDragProject = null
        if (prior != null && prior != state.project) {
            history.push(prior)
        }
        state = state.copy(
            drag = null,
            alignmentGuides = emptyList(),
            canUndo = history.canUndo,
            canRedo = history.canRedo
        )
    }

    //endregion

    //region Theme

    fun updateTheme(themeConfig: CanvasThemeConfig) {
        if (state.project.themeConfig != themeConfig) {
            pushState()
            val updated = state.project.withTheme(themeConfig = themeConfig)
            updateProject(newProject = updated)
        }
    }

    fun setThemeSeed(hex: String) {
        updateTheme(state.project.themeConfig.copy(seedColorHex = hex))
    }

    fun toggleThemeDarkMode() {
        updateTheme(state.project.themeConfig.copy(isDark = !state.project.themeConfig.isDark))
    }

    //endregion

    //region Device

    private fun adaptProjectToDevice(project: M3EProject, profile: DeviceProfile): M3EProject {
        val updatedNodes = project.nodes.map { node ->
            if (node.type == ComponentType.SCAFFOLD) {
                val updatedChildren = node.children.map { child ->
                    if (child.slot == SlotRole.CONTENT) {
                        child.copy(size = CanvasSize(width = profile.size.width, height = (profile.size.height - 144f).coerceAtLeast(0f)))
                    } else {
                        child
                    }
                }
                node.copy(size = profile.size, children = updatedChildren)
            } else {
                node
            }
        }
        return project.copy(deviceProfile = profile, nodes = updatedNodes)
    }

    fun setDeviceProfile(profile: DeviceProfile) {
        if (state.project.deviceProfile != profile) {
            // Device profile change is viewport / preview configuration, NOT an undoable edit
            val adaptedProject = adaptProjectToDevice(state.project, profile)
            history.map { snapshot -> adaptProjectToDevice(snapshot, profile) }
            preDragProject = preDragProject?.let { adaptProjectToDevice(it, profile) }
            state = state.copy(project = adaptedProject)
        }
    }

    /** Replaces the entire project and resets transient UI state (used by Load). */
    fun replaceProject(project: M3EProject) {
        history.clear()
        preDragProject = null
        state = state.copy(
            project = project,
            selectedNodeIds = emptySet(),
            drag = null,
            viewport = ViewportState(),
            canUndo = false,
            canRedo = false,
            undoCount = 0,
            redoCount = 0
        )
    }

    //region Viewport Operations

    fun setZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0.25f, 3.0f)
        state = state.copy(viewport = state.viewport.copy(zoom = clamped))
    }

    fun zoomIn(step: Float = 0.15f) {
        setZoom(state.viewport.zoom + step)
    }

    fun zoomOut(step: Float = 0.15f) {
        setZoom(state.viewport.zoom - step)
    }

    fun resetZoom() {
        state = state.copy(viewport = state.viewport.copy(zoom = 1f, panOffset = CanvasPosition.Zero))
    }

    var canvasViewportSize by mutableStateOf(CanvasSize(800f, 600f))
        internal set

    fun updateCanvasViewportSize(width: Float, height: Float) {
        if (canvasViewportSize.width != width || canvasViewportSize.height != height) {
            canvasViewportSize = CanvasSize(width, height)
        }
    }

    fun pan(deltaX: Float, deltaY: Float) {
        if (state.drag != null) return
        val current = state.viewport.panOffset
        val updated = CanvasPosition(x = current.x + deltaX, y = current.y + deltaY)
        state = state.copy(viewport = state.viewport.copy(panOffset = updated))
    }

    fun resetPan() {
        state = state.copy(viewport = state.viewport.copy(panOffset = CanvasPosition.Zero))
    }

    fun fitToScreen(availableWidth: Float = canvasViewportSize.width, availableHeight: Float = canvasViewportSize.height) {
        if (availableWidth <= 0f || availableHeight <= 0f) return
        val deviceWidth = state.project.deviceProfile.size.width + 48f
        val deviceHeight = state.project.deviceProfile.size.height + 96f
        val scaleX = availableWidth / deviceWidth
        val scaleY = availableHeight / deviceHeight
        val optimalScale = minOf(scaleX, scaleY).coerceIn(0.25f, 1.0f)
        state = state.copy(
            viewport = ViewportState(
                zoom = optimalScale,
                panOffset = CanvasPosition.Zero
            )
        )
    }

    //endregion

    //endregion

    /**
     * Default sizes per component, calculated relative to the active device screen.
     */
    fun defaultSizeFor(type: ComponentType): CanvasSize {
        return componentRegistry.defaultSizeFor(
            type = type,
            deviceWidth = state.project.deviceProfile.size.width,
            deviceHeight = state.project.deviceProfile.size.height
        )
    }

    companion object {
        fun defaultSizeFor(
            type: ComponentType,
            deviceWidth: Float = 412f,
            deviceHeight: Float = 915f
        ): CanvasSize {
            return ComponentRegistry.default.defaultSizeFor(type, deviceWidth, deviceHeight)
        }
        fun newProject(name: String = "Untitled", withDefaultScaffold: Boolean = true): M3EProject {
            val device = DeviceProfile.default
            val nodes = if (withDefaultScaffold) {
                val topBar = CanvasNode(
                    type = ComponentType.TOP_APP_BAR,
                    name = "Top App Bar",
                    position = CanvasPosition.Zero,
                    size = CanvasSize(width = device.size.width, height = 64f),
                    slot = SlotRole.TOP_BAR,
                    properties = listOf(
                        ComponentProperty.Text(key = "title", value = "My Screen")
                    )
                )
                val contentColumn = CanvasNode(
                    type = ComponentType.COLUMN,
                    name = "Screen Content",
                    position = CanvasPosition.Zero,
                    size = CanvasSize(width = device.size.width, height = device.size.height - 144f),
                    slot = SlotRole.CONTENT,
                    layoutConfig = LayoutConfig(spacing = 16f, padding = 16f)
                )
                val scaffold = CanvasNode(
                    type = ComponentType.SCAFFOLD,
                    name = "Screen Scaffold",
                    position = CanvasPosition.Zero,
                    size = device.size,
                    children = listOf(topBar, contentColumn)
                )
                listOf(scaffold)
            } else {
                emptyList()
            }

            return M3EProject(
                name = name,
                deviceProfile = device,
                nodes = nodes
            )
        }
    }

    /**
     * Scans the current project node tree and returns any violations of Material 3 hierarchy
     * or Compose layout constraints.
     */
    fun validateHierarchy(): List<HierarchyViolation> {
        val violations = mutableListOf<HierarchyViolation>()

        // 1. Scaffold count check
        if (state.project.nodes.count { it.type == ComponentType.SCAFFOLD } > 1) {
            violations.add(
                HierarchyViolation(
                    nodeId = "",
                    nodeName = "Scaffold",
                    nodeType = ComponentType.SCAFFOLD,
                    parentId = null,
                    parentType = null,
                    description = "Multiple Scaffold instances found at project root"
                )
            )
        }

        // 2. Recursively check nodes
        fun checkNode(node: CanvasNode, parent: CanvasNode?) {
            if (parent != null) {
                if (!parent.type.canAcceptChild(node.type)) {
                    violations.add(
                        HierarchyViolation(
                            nodeId = node.id,
                            nodeName = node.name,
                            nodeType = node.type,
                            parentId = parent.id,
                            parentType = parent.type,
                            description = "${parent.type.displayName} cannot contain ${node.type.displayName}"
                        )
                    )
                }
                if (node.slot != null && !node.type.allowedSlotsIn(parent.type).contains(node.slot)) {
                    violations.add(
                        HierarchyViolation(
                            nodeId = node.id,
                            nodeName = node.name,
                            nodeType = node.type,
                            parentId = parent.id,
                            parentType = parent.type,
                            description = "Slot ${node.slot.displayName} is not allowed for ${node.type.displayName} in ${parent.type.displayName}"
                        )
                    )
                }
            } else {
                val hasScaffold = state.project.nodes.any { it.type == ComponentType.SCAFFOLD }
                if (!node.type.isAllowedAtRoot(hasScaffold = hasScaffold && node.type != ComponentType.SCAFFOLD)) {
                    violations.add(
                        HierarchyViolation(
                            nodeId = node.id,
                            nodeName = node.name,
                            nodeType = node.type,
                            parentId = null,
                            parentType = null,
                            description = "${node.type.displayName} is not allowed as a root component"
                        )
                    )
                }
            }

            // Check duplicate single-occupant slots among direct children
            val seenSingleOccupantSlots = mutableSetOf<SlotRole>()
            for (child in node.children) {
                if (child.slot != null && !child.slot.isMultiOccupant) {
                    if (child.slot in seenSingleOccupantSlots) {
                        violations.add(
                            HierarchyViolation(
                                nodeId = child.id,
                                nodeName = child.name,
                                nodeType = child.type,
                                parentId = node.id,
                                parentType = node.type,
                                description = "Duplicate single-occupant slot ${child.slot.displayName} in ${node.type.displayName}"
                            )
                        )
                    } else {
                        seenSingleOccupantSlots.add(child.slot)
                    }
                }
                checkNode(child, node)
            }
        }

        for (root in state.project.nodes) {
            checkNode(root, null)
        }

        return violations
    }
}

/**
 * Represents a violation of Material 3 hierarchy rules or Compose layout constraints.
 */
data class HierarchyViolation(
    val nodeId: String,
    val nodeName: String,
    val nodeType: ComponentType,
    val parentId: String?,
    val parentType: ComponentType?,
    val description: String
)