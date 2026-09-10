package dev.chandradsl.m3ecanvas.editor.state

import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.M3EProject

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
    val viewport: ViewportState = ViewportState()
) {

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
 * Represents an in-progress drag operation on a single node.
 *
 * [originalNodePosition] is kept for future undo or snap-back behaviour.
 * Movement itself is applied incrementally via deltas from drag gestures.
 */
data class DragState(
    val nodeId: String,
    val originalNodePosition: CanvasPosition
)

/**
 * Represents the canvas viewport transform: zoom level and pan offset.
 */
data class ViewportState(
    val zoom: Float = 1f,
    val panOffset: CanvasPosition = CanvasPosition.Zero
)