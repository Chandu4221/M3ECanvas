package dev.chandradsl.m3ecanvas.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.history.HistoryStack

/**
 * Manages the editor's state in response to user actions.
 * Hierarchy-aware: nodes can be top-level or nested inside containers.
 * Supports command undo/redo history and clipboard operations.
 */
class EditorController(
    initialProject: M3EProject
) {

    private val history = HistoryStack<M3EProject>()
    private var preDragProject: M3EProject? = null

    var state: EditorState by mutableStateOf(
        EditorState(
            project = initialProject,
            canUndo = false,
            canRedo = false
        )
    )
        private set

    val canUndo: Boolean
        get() = history.canUndo

    val canRedo: Boolean
        get() = history.canRedo

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
            canRedo = history.canRedo
        )
    }

    //region History & Undo/Redo

    fun undo() {
        val previous = history.undo(state.project) ?: return
        val validSelections = state.selectedNodeIds.filter { previous.findNode(it) != null }.toSet()
        state = state.copy(
            project = previous,
            selectedNodeIds = validSelections,
            canUndo = history.canUndo,
            canRedo = history.canRedo
        )
    }

    fun redo() {
        val next = history.redo(state.project) ?: return
        val validSelections = state.selectedNodeIds.filter { next.findNode(it) != null }.toSet()
        state = state.copy(
            project = next,
            selectedNodeIds = validSelections,
            canUndo = history.canUndo,
            canRedo = history.canRedo
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
        pushState()

        val selected = state.selectedNodes.firstOrNull()
        val updatedProject = when {
            // 1. If currently selected node is a container
            selected != null && selected.isContainer -> {
                if (selected.type == ComponentType.SCAFFOLD) {
                    val slot = clone.type.canonicalSlot()
                    if (slot != SlotRole.CONTENT) {
                        state.project.updateNode(selected.withChildInSlot(clone, slot))
                    } else {
                        val content = selected.children.firstOrNull { it.slot == SlotRole.CONTENT && it.isContainer }
                        if (content != null) {
                            state.project.updateNode(content.withChild(clone))
                        } else {
                            state.project.updateNode(selected.withChild(clone))
                        }
                    }
                } else {
                    state.project.updateNode(selected.withChild(clone))
                }
            }
            // 2. If selected node has a parent container, insert as sibling
            selected != null && findParentInProject(selected.id) != null -> {
                val parent = findParentInProject(selected.id)!!
                val index = parent.children.indexOfFirst { it.id == selected.id }
                val updatedChildren = parent.children.toMutableList().apply {
                    if (index >= 0) add(index + 1, clone) else add(clone)
                }
                state.project.updateNode(parent.copy(children = updatedChildren))
            }
            // 3. If root has a Scaffold with Content container, paste into content
            state.project.nodes.any { it.type == ComponentType.SCAFFOLD } -> {
                val scaffold = state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
                val content = scaffold.children.firstOrNull { it.slot == SlotRole.CONTENT && it.isContainer }
                if (content != null) {
                    state.project.updateNode(content.withChild(clone))
                } else {
                    state.project.updateNode(scaffold.withChild(clone))
                }
            }
            // 4. Fallback: add to root
            else -> {
                state.project.withNode(clone)
            }
        }

        updateProject(updatedProject, selectedNodeIds = setOf(clone.id))
        return clone
    }

    /**
     * Duplicates the currently selected node (Ctrl+D), placing the clone as an immediate sibling.
     */
    fun duplicateSelected(): CanvasNode? {
        val selected = state.selectedNodes.firstOrNull() ?: return null
        if (selected.isLockedInSlot) return null // Locked slots cannot be duplicated
        val clone = selected.deepCloneWithNewIds(offsetPosition = true)
        pushState()

        val parent = findParentInProject(selected.id)
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
        state = state.copy(selectedNodeIds = setOf(nodeId))
    }

    fun clearSelection() {
        state = state.copy(selectedNodeIds = emptySet())
    }

    //endregion

    //region Node editing

    /** Adds a new top-level component of [type] at [position] and selects it. */
    fun addNode(type: ComponentType, position: CanvasPosition) {
        val existingCount = state.project.nodes.count { it.type == type }
        val name = "${type.displayName} ${existingCount + 1}"

        val targetPosition = if (type == ComponentType.SCAFFOLD) CanvasPosition.Zero else position
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
    }

    /** Adds a new child of [type] inside the container with [containerId]. */
    fun addChildToContainer(containerId: String, type: ComponentType) {
        val container = state.project.findNode(nodeId = containerId) ?: return
        if (!container.isContainer) return

        val childCount = container.children.count { it.type == type }
        val name = "${type.displayName} ${childCount + 1}"

        val targetSlot = if (container.type == ComponentType.SCAFFOLD) {
            type.canonicalSlot()
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

        val updatedContainer = if (container.type == ComponentType.SCAFFOLD && targetSlot != null && targetSlot != SlotRole.CONTENT) {
            container.withChildInSlot(child = child, slotRole = targetSlot)
        } else {
            container.withChild(child = child)
        }

        pushState()
        updateProject(
            newProject = state.project.updateNode(node = updatedContainer),
            selectedNodeIds = setOf(child.id)
        )
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
        val parent = findParentInProject(nodeId = nodeId)

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

    private fun findParentInProject(nodeId: String): CanvasNode? {
        for (root in state.project.nodes) {
            val found = findParent(node = root, targetId = nodeId)
            if (found != null) return found
        }
        return null
    }

    private fun findParent(node: CanvasNode, targetId: String): CanvasNode? {
        for (child in node.children) {
            if (child.id == targetId) return node
            val deeper = findParent(node = child, targetId = targetId)
            if (deeper != null) return deeper
        }
        return null
    }

    //endregion

    //region Drag

    /** Begins a drag on the node with [nodeId]. Selection is not changed here. */
    fun startDrag(nodeId: String) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        if (node.isLockedInSlot) return // Locked slot nodes cannot be moved arbitrarily
        preDragProject = state.project
        state = state.copy(
            drag = DragState(
                nodeId = nodeId,
                originalNodePosition = node.position
            )
        )
    }

    fun updateDrag(deltaX: Float, deltaY: Float) {
        val drag = state.drag ?: return
        val node = state.project.findNode(nodeId = drag.nodeId) ?: return
        val movedNode = node.movedBy(dx = deltaX, dy = deltaY)
        state = state.copy(
            project = state.project.updateNode(node = movedNode)
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
            canUndo = history.canUndo,
            canRedo = history.canRedo
        )
    }

    //endregion

    //region Device

    fun setDeviceProfile(profile: DeviceProfile) {
        if (state.project.deviceProfile != profile) {
            pushState()
            updateProject(newProject = state.project.copy(deviceProfile = profile))
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
            canUndo = false,
            canRedo = false
        )
    }

    //endregion

    /**
     * Default sizes per component, calculated relative to the active device screen.
     */
    fun defaultSizeFor(type: ComponentType): CanvasSize {
        return defaultSizeFor(
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
            return when (type) {
                ComponentType.SCAFFOLD -> CanvasSize(width = deviceWidth, height = deviceHeight)
                ComponentType.TOP_APP_BAR -> CanvasSize(width = deviceWidth, height = 64f)
                ComponentType.NAVIGATION_BAR -> CanvasSize(width = deviceWidth, height = 80f)
                ComponentType.NAVIGATION_RAIL -> CanvasSize(width = 80f, height = deviceHeight)
                ComponentType.NAVIGATION_DRAWER -> CanvasSize(width = 300f, height = deviceHeight)
                ComponentType.TABS -> CanvasSize(width = deviceWidth, height = 48f)
                ComponentType.SEARCH -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(200f), height = 56f)

                ComponentType.FAB -> CanvasSize(width = 56f, height = 56f)
                ComponentType.EXTENDED_FAB -> CanvasSize(width = 140f, height = 56f)
                ComponentType.BUTTON -> CanvasSize(width = 120f, height = 40f)
                ComponentType.ICON_BUTTON -> CanvasSize(width = 48f, height = 48f)
                ComponentType.SEGMENTED_BUTTON -> CanvasSize(width = (deviceWidth - 48f).coerceAtLeast(220f), height = 48f)
                ComponentType.SPLIT_BUTTON -> CanvasSize(width = 140f, height = 40f)
                ComponentType.BUTTON_GROUP -> CanvasSize(width = 260f, height = 40f)

                ComponentType.CARD -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(200f), height = 160f)
                ComponentType.LISTS -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(200f), height = 72f)
                ComponentType.SHEETS -> CanvasSize(width = deviceWidth, height = 220f)
                ComponentType.DIALOG -> CanvasSize(width = (deviceWidth - 48f).coerceAtLeast(260f), height = 200f)

                ComponentType.SNACKBAR -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(200f), height = 48f)
                ComponentType.BADGE -> CanvasSize(width = 48f, height = 48f)
                ComponentType.TOOLTIP -> CanvasSize(width = 160f, height = 36f)
                ComponentType.PROGRESS_INDICATOR -> CanvasSize(width = (deviceWidth - 48f).coerceAtLeast(200f), height = 8f)
                ComponentType.LOADING_INDICATOR -> CanvasSize(width = 48f, height = 48f)

                ComponentType.CHECKBOX -> CanvasSize(width = 180f, height = 40f)
                ComponentType.RADIO_BUTTON -> CanvasSize(width = 180f, height = 40f)
                ComponentType.SWITCH -> CanvasSize(width = (deviceWidth - 48f).coerceAtLeast(180f), height = 44f)
                ComponentType.SLIDER -> CanvasSize(width = (deviceWidth - 48f).coerceAtLeast(200f), height = 48f)
                ComponentType.CHIPS -> CanvasSize(width = 100f, height = 36f)
                ComponentType.DATE_PICKER -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(280f), height = 320f)
                ComponentType.TIME_PICKER -> CanvasSize(width = (deviceWidth - 48f).coerceAtLeast(260f), height = 180f)
                ComponentType.MENUS -> CanvasSize(width = 180f, height = 140f)

                ComponentType.TEXT_FIELD -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(200f), height = 56f)
                ComponentType.TEXT -> CanvasSize(width = 120f, height = 24f)
                ComponentType.ICON -> CanvasSize(width = 24f, height = 24f)
                ComponentType.IMAGE -> CanvasSize(width = 120f, height = 120f)
                ComponentType.HORIZONTAL_DIVIDER -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(200f), height = 1f)
                ComponentType.VERTICAL_DIVIDER -> CanvasSize(width = 1f, height = 48f)
                ComponentType.SPACER -> CanvasSize(width = 16f, height = 16f)

                ComponentType.BOTTOM_APP_BAR -> CanvasSize(width = deviceWidth, height = 80f)
                ComponentType.RANGE_SLIDER -> CanvasSize(width = (deviceWidth - 48f).coerceAtLeast(200f), height = 48f)
                ComponentType.SURFACE -> CanvasSize(width = (deviceWidth - 32f).coerceAtLeast(200f), height = 160f)

                ComponentType.COLUMN,
                ComponentType.LAZY_COLUMN,
                ComponentType.LAZY_VERTICAL_GRID,
                ComponentType.FLOW_COLUMN -> CanvasSize(width = deviceWidth, height = (deviceHeight - 160f).coerceAtLeast(200f))
                ComponentType.ROW,
                ComponentType.LAZY_ROW,
                ComponentType.FLOW_ROW -> CanvasSize(width = deviceWidth, height = 120f)
                ComponentType.BOX -> CanvasSize(width = deviceWidth, height = 300f)
            }
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
}