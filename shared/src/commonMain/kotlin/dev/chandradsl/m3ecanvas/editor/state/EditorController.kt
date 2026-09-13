package dev.chandradsl.m3ecanvas.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.canvas.AlignmentSnapper
import dev.chandradsl.m3ecanvas.editor.canvas.InMemoryLayoutGeometryStore
import dev.chandradsl.m3ecanvas.editor.canvas.LayoutGeometryStore
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry
import dev.chandradsl.m3ecanvas.editor.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cohesive facade / EditorStore managing editor state in response to user actions.
 * Internally decomposes responsibilities into focused SRP services:
 * - [HistoryService]: undo/redo stack & device adaptation across history
 * - [DocumentEditorService]: pure tree mutations and hierarchy invariants
 * - [SelectionService]: single/multi/toggle/marquee selections
 * - [ClipboardService]: copy, paste, duplicate with stable UUID generation
 * - [DragDropService]: drag state and alignment snapping calculations
 * - [LayoutGeometryStore]: real Compose bounds observation
 */
class EditorController(
    initialProject: M3EProject = newProject(),
    val componentRegistry: ComponentRegistry = ComponentRegistry.default,
    val alignmentSnapper: AlignmentSnapper = AlignmentSnapper.default,
    val geometryStore: LayoutGeometryStore = InMemoryLayoutGeometryStore(),
    val historyService: HistoryService = HistoryService(),
    val documentEditor: DocumentEditorService = DocumentEditorService(),
    val selectionService: SelectionService = SelectionService(),
    val clipboardService: ClipboardService = ClipboardService(),
    val dragDropService: DragDropService = DragDropService()
) {

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
        get() = historyService.canUndo

    val canRedo: Boolean
        get() = historyService.canRedo

    val undoCount: Int
        get() = historyService.undoCount

    val redoCount: Int
        get() = historyService.redoCount

    private fun pushState() {
        historyService.push(state.project)
    }

    private fun updateProject(
        newProject: M3EProject,
        selectedNodeIds: Set<String> = state.selectedNodeIds
    ) {
        state = state.copy(
            project = newProject,
            selectedNodeIds = selectedNodeIds,
            canUndo = historyService.canUndo,
            canRedo = historyService.canRedo,
            undoCount = historyService.undoCount,
            redoCount = historyService.redoCount
        )
    }

    //region History & Undo/Redo

    fun undo() {
        val restored = historyService.undo(state.project, state.project.deviceProfile) ?: return
        val validSelections = selectionService.filterValidSelections(state.selectedNodeIds, restored)
        state = state.copy(
            project = restored,
            selectedNodeIds = validSelections,
            canUndo = historyService.canUndo,
            canRedo = historyService.canRedo,
            undoCount = historyService.undoCount,
            redoCount = historyService.redoCount
        )
    }

    fun redo() {
        val restored = historyService.redo(state.project, state.project.deviceProfile) ?: return
        val validSelections = selectionService.filterValidSelections(state.selectedNodeIds, restored)
        state = state.copy(
            project = restored,
            selectedNodeIds = validSelections,
            canUndo = historyService.canUndo,
            canRedo = historyService.canRedo,
            undoCount = historyService.undoCount,
            redoCount = historyService.redoCount
        )
    }

    fun getUndoSnapshots(): List<M3EProject> = historyService.getUndoSnapshots()
    fun getRedoSnapshots(): List<M3EProject> = historyService.getRedoSnapshots()

    fun undoSteps(steps: Int) {
        val restored = historyService.undoSteps(steps, state.project, state.project.deviceProfile) ?: return
        val validSelections = selectionService.filterValidSelections(state.selectedNodeIds, restored)
        state = state.copy(
            project = restored,
            selectedNodeIds = validSelections,
            canUndo = historyService.canUndo,
            canRedo = historyService.canRedo,
            undoCount = historyService.undoCount,
            redoCount = historyService.redoCount
        )
    }

    fun redoSteps(steps: Int) {
        val restored = historyService.redoSteps(steps, state.project, state.project.deviceProfile) ?: return
        val validSelections = selectionService.filterValidSelections(state.selectedNodeIds, restored)
        state = state.copy(
            project = restored,
            selectedNodeIds = validSelections,
            canUndo = historyService.canUndo,
            canRedo = historyService.canRedo,
            undoCount = historyService.undoCount,
            redoCount = historyService.redoCount
        )
    }

    fun clearHistory() {
        historyService.clear()
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
        val selected = clipboardService.copySelected(state.selectedNodes) ?: return null
        state = state.copy(clipboard = selected)
        return selected
    }

    /**
     * Pastes the node currently in clipboard (or [sourceNode] if provided).
     * Automatically handles container insertion, slot routing, or canvas root placement.
     */
    fun paste(sourceNode: CanvasNode? = null): CanvasNode? {
        val (updatedProject, pasted) = clipboardService.paste(
            project = state.project,
            sourceNode = sourceNode ?: state.clipboard,
            selectedNode = state.selectedNodes.firstOrNull(),
            targetContainerFinder = { forType, startingFromId ->
                documentEditor.findValidTargetContainer(state.project, forType, startingFromId)
            }
        )
        if (pasted == null) return null

        pushState()
        updateProject(updatedProject, selectedNodeIds = setOf(pasted.id))
        return pasted
    }

    /**
     * Duplicates the currently selected node (Ctrl+D), placing the clone as an immediate sibling.
     */
    fun duplicateSelected(): CanvasNode? {
        val (updatedProject, clone) = clipboardService.duplicateSelected(
            project = state.project,
            selected = state.selectedNodes.firstOrNull(),
            parentFinder = { nodeId -> documentEditor.findParent(state.project, nodeId) }
        )
        if (clone == null) return null

        pushState()
        updateProject(updatedProject, selectedNodeIds = setOf(clone.id))
        return clone
    }

    //endregion

    //region Selection

    fun selectNode(nodeId: String) {
        state = state.copy(
            selectedNodeIds = selectionService.selectNode(state.selectedNodeIds, nodeId, state.isInteractiveMode)
        )
    }

    fun toggleSelectNode(nodeId: String) {
        state = state.copy(
            selectedNodeIds = selectionService.toggleSelectNode(state.selectedNodeIds, nodeId, state.isInteractiveMode)
        )
    }

    fun toggleNodeSelection(nodeId: String) = toggleSelectNode(nodeId)

    fun selectAdditionalNode(nodeId: String) {
        state = state.copy(
            selectedNodeIds = selectionService.selectAdditionalNode(state.selectedNodeIds, nodeId, state.isInteractiveMode)
        )
    }

    fun selectNodes(nodeIds: Collection<String>) {
        state = state.copy(
            selectedNodeIds = selectionService.selectNodes(nodeIds, state.isInteractiveMode)
        )
    }

    fun selectAll() {
        state = state.copy(
            selectedNodeIds = selectionService.selectAll(state.project, state.isInteractiveMode)
        )
    }

    fun clearSelection() {
        state = state.copy(selectedNodeIds = selectionService.clearSelection())
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
        val defaultSize = defaultSizeFor(type)
        val (updatedProject, node) = documentEditor.addNode(state.project, type, position, defaultSize)
        if (node == null) return false

        pushState()
        updateProject(newProject = updatedProject, selectedNodeIds = setOf(node.id))
        return true
    }

    fun findValidTargetContainer(forType: ComponentType, startingFromNodeId: String? = null): CanvasNode? {
        return documentEditor.findValidTargetContainer(state.project, forType, startingFromNodeId ?: state.selectedNodeIds.firstOrNull())
    }

    /** Adds a new child of [type] inside the container with [containerId]. Returns true if successfully added, false if rejected. */
    fun addChildToContainer(containerId: String, type: ComponentType): Boolean {
        val defaultSize = defaultSizeFor(type)
        val (updatedProject, child) = documentEditor.addChildToContainer(state.project, containerId, type, defaultSize)
        if (child == null) return false

        pushState()
        updateProject(newProject = updatedProject, selectedNodeIds = setOf(child.id))
        return true
    }

    /** Sets the slot role for [nodeId], replacing single-occupant occupants if necessary. */
    fun setNodeSlot(nodeId: String, slotRole: SlotRole?) {
        val updated = documentEditor.setNodeSlot(state.project, nodeId, slotRole)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Removes the node with [nodeId] from anywhere in the tree. */
    fun removeNode(nodeId: String) {
        pushState()
        updateProject(
            newProject = documentEditor.removeNode(state.project, nodeId),
            selectedNodeIds = state.selectedNodeIds - nodeId
        )
    }

    /** Deletes all currently selected nodes and clears the selection. */
    fun deleteSelected() {
        val ids = state.selectedNodeIds
        if (ids.isEmpty()) return
        pushState()
        val updated = documentEditor.deleteNodes(state.project, ids)
        updateProject(newProject = updated, selectedNodeIds = emptySet())
    }

    /**
     * Nudges selected top-level nodes by the given delta. Children are skipped
     * because their position is controlled by their container's layout.
     */
    fun nudgeSelected(dx: Float, dy: Float) {
        val ids = state.selectedNodeIds
        if (ids.isEmpty()) return
        val (updated, movedAny) = documentEditor.nudgeNodes(state.project, ids, dx, dy)
        if (movedAny) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Applies or replaces a [property] on the node with [nodeId], wherever it is. */
    fun updateNodeProperty(nodeId: String, property: ComponentProperty) {
        val updated = documentEditor.updateNodeProperty(state.project, nodeId, property)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Renames the node with [nodeId]. */
    fun renameNode(nodeId: String, name: String) {
        val updated = documentEditor.renameNode(state.project, nodeId, name)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Replaces the layout config of the container with [nodeId]. */
    fun updateLayoutConfig(nodeId: String, config: LayoutConfig) {
        val updated = documentEditor.updateLayoutConfig(state.project, nodeId, config)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    //endregion

    //region Modifier chain

    /** Appends [spec] to the end of the modifier chain of [nodeId]. */
    fun addModifier(nodeId: String, spec: ModifierSpec) {
        val updated = documentEditor.addModifier(state.project, nodeId, spec)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Removes the modifier matching [specId] from [nodeId]. */
    fun removeModifier(nodeId: String, specId: String) {
        val updated = documentEditor.removeModifier(state.project, nodeId, specId)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Replaces the modifier matching [spec.id] on [nodeId]. */
    fun updateModifier(nodeId: String, spec: ModifierSpec) {
        val updated = documentEditor.updateModifier(state.project, nodeId, spec)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Moves the modifier matching [specId] one position earlier. */
    fun moveModifierUp(nodeId: String, specId: String) {
        val updated = documentEditor.moveModifier(state.project, nodeId, specId, delta = -1)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Moves the modifier matching [specId] one position later. */
    fun moveModifierDown(nodeId: String, specId: String) {
        val updated = documentEditor.moveModifier(state.project, nodeId, specId, delta = 1)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    //endregion

    //region Reorder

    /** Moves the node one position earlier among its siblings (or root nodes). */
    fun moveNodeUp(nodeId: String) {
        val updated = documentEditor.reorderNode(state.project, nodeId, delta = -1)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Moves the node one position later among its siblings (or root nodes). */
    fun moveNodeDown(nodeId: String) {
        val updated = documentEditor.reorderNode(state.project, nodeId, delta = 1)
        if (updated != state.project) {
            pushState()
            updateProject(newProject = updated)
        }
    }

    /** Finds the parent container node of [nodeId], or null if it is a root node. */
    fun findParent(nodeId: String): CanvasNode? {
        return documentEditor.findParent(state.project, nodeId)
    }

    //endregion

    //region Drag

    /** Begins a drag on the node with [nodeId]. If multiple nodes are selected, all non-locked selected nodes move together. */
    fun startDrag(nodeId: String) {
        val drag = dragDropService.startDrag(state.project, nodeId, state.selectedNodeIds, state.isInteractiveMode) ?: return
        preDragProject = state.project
        state = state.copy(drag = drag)
    }

    fun updateDrag(deltaX: Float, deltaY: Float) {
        val drag = state.drag ?: return
        val (updatedProject, guides) = dragDropService.updateDrag(
            drag = drag,
            project = state.project,
            deltaX = deltaX,
            deltaY = deltaY,
            snappingEnabled = snappingEnabled,
            alignmentSnapper = alignmentSnapper
        )
        state = state.copy(project = updatedProject, alignmentGuides = guides)
    }

    fun endDrag() {
        val prior = preDragProject
        preDragProject = null
        if (prior != null && prior != state.project) {
            historyService.push(prior)
        }
        state = state.copy(
            drag = null,
            alignmentGuides = emptyList(),
            canUndo = historyService.canUndo,
            canRedo = historyService.canRedo
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

    fun setDeviceProfile(profile: DeviceProfile) {
        if (state.project.deviceProfile != profile) {
            val adaptedProject = HistoryService.adaptProjectToDevice(state.project, profile)
            historyService.map { snapshot -> HistoryService.adaptProjectToDevice(snapshot, profile) }
            preDragProject = preDragProject?.let { HistoryService.adaptProjectToDevice(it, profile) }
            state = state.copy(project = adaptedProject)
        }
    }

    /**
     * Resizes the device viewport to a custom width and height (clamped between 300dp-1200dp width and 400dp-1400dp height).
     * Automatically adapts the project nodes and sets the profile category to CUSTOM.
     */
    fun resizeViewport(widthDp: Float, heightDp: Float, isDragging: Boolean = false) {
        val clampedWidth = widthDp.coerceIn(300f, 1200f)
        val clampedHeight = heightDp.coerceIn(400f, 1400f)
        val currentProfile = state.project.deviceProfile
        val newProfile = currentProfile.copy(
            id = "custom_${clampedWidth.toInt()}x${clampedHeight.toInt()}",
            displayName = "Custom (${clampedWidth.toInt()} × ${clampedHeight.toInt()})",
            size = CanvasSize(width = clampedWidth, height = clampedHeight),
            category = DeviceCategory.CUSTOM
        )
        val adaptedProject = HistoryService.adaptProjectToDevice(state.project, newProfile)
        state = state.copy(
            project = adaptedProject,
            isViewportResizing = isDragging
        )
    }

    fun setViewportResizing(resizing: Boolean) {
        state = state.copy(isViewportResizing = resizing)
    }

    /**
     * Toggles between Portrait and Landscape orientation.
     */
    fun toggleDeviceOrientation() {
        val current = state.project.deviceProfile
        val nextOrientation = if (current.orientation == DeviceOrientation.PORTRAIT) {
            DeviceOrientation.LANDSCAPE
        } else {
            DeviceOrientation.PORTRAIT
        }
        val swappedSize = CanvasSize(width = current.size.height, height = current.size.width)
        val updated = current.copy(
            orientation = nextOrientation,
            size = swappedSize
        )
        setDeviceProfile(updated)
    }

    /**
     * Configures the device screen density (e.g. 1.0f, 2.0f, 2.75f, 3.5f).
     */
    fun setDeviceDensity(density: Float) {
        val current = state.project.deviceProfile
        val updated = current.copy(density = density.coerceIn(0.5f, 5.0f))
        setDeviceProfile(updated)
    }

    /**
     * Configures the device font scale (e.g. 0.85f, 1.0f, 1.15f, 1.3f).
     */
    fun setDeviceFontScale(fontScale: Float) {
        val current = state.project.deviceProfile
        val updated = current.copy(fontScale = fontScale.coerceIn(0.5f, 2.5f))
        setDeviceProfile(updated)
    }

    /**
     * Applies a quick window size class preset (Compact: 412x915, Medium: 600x840, Expanded: 1200x800).
     */
    fun setWindowSizePreset(widthClass: WindowWidthSizeClass) {
        val targetProfile = when (widthClass) {
            WindowWidthSizeClass.COMPACT -> DeviceProfile.presets.first { it.id == "pixel_8" }
            WindowWidthSizeClass.MEDIUM -> DeviceProfile.presets.first { it.id == "pixel_fold" }
            WindowWidthSizeClass.EXPANDED -> DeviceProfile.presets.first { it.id == "desktop_window" }
        }
        setDeviceProfile(targetProfile)
    }

    /**
     * Toggles visibility of simulated system insets (status bar & gesture navigation bar).
     */
    fun toggleSystemInsets() {
        state = state.copy(showSystemInsets = !state.showSystemInsets)
    }

    /**
     * Aligns all currently selected nodes along the specified [alignment] axis.
     */
    fun alignSelectedNodes(alignment: AlignmentType) {
        if (state.selectedNodeIds.size < 2) return
        val updatedProject = documentEditor.alignNodes(state.project, state.selectedNodeIds, alignment)
        if (updatedProject != state.project) {
            pushState()
            updateProject(updatedProject)
        }
    }

    /**
     * Distributes 3 or more currently selected nodes with equal spacing along [distribution] axis.
     */
    fun distributeSelectedNodes(distribution: DistributionType) {
        if (state.selectedNodeIds.size < 3) return
        val updatedProject = documentEditor.distributeNodes(state.project, state.selectedNodeIds, distribution)
        if (updatedProject != state.project) {
            pushState()
            updateProject(updatedProject)
        }
    }

    /**
     * Toggles visibility of canvas rulers.
     */
    fun toggleRulers() {
        state = state.copy(showRulers = !state.showRulers)
    }

    /**
     * Toggles visibility of distance and spacing measurement overlays.
     */
    fun toggleMeasurements() {
        state = state.copy(showMeasurements = !state.showMeasurements)
    }

    /** Replaces the entire project and resets transient UI state (used by Load). */
    fun replaceProject(project: M3EProject) {
        historyService.clear()
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
        return documentEditor.validateHierarchy(state.project)
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