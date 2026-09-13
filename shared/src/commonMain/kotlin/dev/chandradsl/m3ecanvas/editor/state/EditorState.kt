package dev.chandradsl.m3ecanvas.editor.state

import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.M3EProject

/**
 * Operating mode of the editor canvas.
 */
enum class EditorMode {
    /** Layout and design mode: selection borders, inspector editing, free canvas manipulation. */
    DESIGN,
    /** Live prototype mode: canvas locked, native scrolling, live controls and dismissible dialogs. */
    INTERACTIVE
}

/**
 * Immutable snapshot of the editor's current state.
 *
 * This is a pure data class with no Compose dependencies, so state
 * transitions can be reasoned about and tested independently of the UI.
 * The [EditorController] wraps this in observable Compose state.
 */
data class EditorState(
    val project: M3EProject,
    val selectedNodeIds: Set<String> = emptySet(),
    val drag: DragState? = null,
    val viewport: ViewportState = ViewportState(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoCount: Int = 0,
    val redoCount: Int = 0,
    val clipboard: CanvasNode? = null,
    val alignmentGuides: List<AlignmentGuide> = emptyList(),
    val multiDevicePreview: Boolean = false,
    val mode: EditorMode = EditorMode.DESIGN,
    val dismissedOverlayIds: Set<String> = emptySet(),
    val showSystemInsets: Boolean = true,
    val isViewportResizing: Boolean = false,
    val showRulers: Boolean = true,
    val showMeasurements: Boolean = true,
    val collapsedNodeIds: Set<String> = emptySet(),
    val favoriteComponentTypes: Set<ComponentType> = emptySet(),
    val recentComponentTypes: List<ComponentType> = emptyList(),
    val isCommandPaletteOpen: Boolean = false
) {

    /** Returns true if the editor is in interactive preview mode. */
    val isInteractiveMode: Boolean get() = mode == EditorMode.INTERACTIVE

    /** Returns true if the node with [nodeId] is currently selected. */
    fun isNodeSelected(nodeId: String): Boolean {
        return nodeId in selectedNodeIds
    }
    /** The currently selected nodes, resolved from anywhere in the tree. */
    val selectedNodes: List<CanvasNode>
        get() = selectedNodeIds.mapNotNull { nodeId ->
            project.findNode(nodeId = nodeId)
        }
}

/**
 * Represents an in-progress drag operation on one or more nodes.
 *
 * [originalNodePosition] and [originalPositions] are kept for future undo or snap-back behaviour.
 * Movement itself is applied incrementally via deltas from drag gestures.
 */
data class DragState(
    val nodeId: String,
    val originalNodePosition: CanvasPosition,
    val nodeIds: Set<String> = setOf(nodeId),
    val originalPositions: Map<String, CanvasPosition> = mapOf(nodeId to originalNodePosition)
)

/**
 * Represents the canvas viewport transform: zoom level and pan offset.
 */
data class ViewportState(
    val zoom: Float = 1f,
    val panOffset: CanvasPosition = CanvasPosition.Zero
)

/**
 * Orientation of visual alignment snap guides on the canvas.
 */
enum class GuideOrientation {
    HORIZONTAL,
    VERTICAL
}

/**
 * Visual alignment snap guide shown while dragging elements.
 */
data class AlignmentGuide(
    val orientation: GuideOrientation,
    val position: Float,
    val label: String? = null
)