package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.M3EProject

/**
 * CompositionLocal providing the canvas root [LayoutCoordinates]
 * to any node renderer in the tree.
 */
val LocalCanvasCoordinates = staticCompositionLocalOf<LayoutCoordinates?> { null }

/**
 * Encapsulates the actual rendered layout geometry of a CanvasNode
 * as measured and positioned directly by Jetpack Compose.
 */
data class RenderedBounds(
    /** Coordinates and size of the component relative to the Canvas root surface. */
    val boundsInCanvas: Rect,
    /** Absolute screen window bounds. */
    val boundsInWindow: Rect = boundsInCanvas,
    /** Unscaled integer pixel size reported by LayoutCoordinates. */
    val sizePx: IntSize = IntSize(boundsInCanvas.width.toInt(), boundsInCanvas.height.toInt())
) {
    val x: Float get() = boundsInCanvas.left
    val y: Float get() = boundsInCanvas.top
    val width: Float get() = boundsInCanvas.width
    val height: Float get() = boundsInCanvas.height

    fun contains(x: Float, y: Float): Boolean =
        boundsInCanvas.contains(Offset(x, y))
}

/**
 * Port & store for Compose-owned rendered layout geometry.
 * Sole source of truth for component coordinates and sizes on canvas.
 */
interface LayoutGeometryStore {
    fun getBounds(nodeId: String): RenderedBounds?
    fun updateBounds(nodeId: String, bounds: RenderedBounds)
    fun removeBounds(nodeId: String)
    fun clear()
    fun findNodeAt(offset: Offset, project: M3EProject): CanvasNode?
    fun findNodesIn(rect: Rect, project: M3EProject): List<CanvasNode>
    val allBounds: Map<String, RenderedBounds>
}

/**
 * In-memory reactive adapter for [LayoutGeometryStore].
 * Backed by Compose's [mutableStateMapOf] for fine-grained recomposition.
 */
class InMemoryLayoutGeometryStore : LayoutGeometryStore {
    private val _bounds = mutableStateMapOf<String, RenderedBounds>()

    override fun getBounds(nodeId: String): RenderedBounds? = _bounds[nodeId]

    override fun updateBounds(nodeId: String, bounds: RenderedBounds) {
        val current = _bounds[nodeId]
        if (current == null || current != bounds) {
            _bounds[nodeId] = bounds
        }
    }

    override fun removeBounds(nodeId: String) {
        _bounds.remove(nodeId)
    }

    override fun clear() {
        _bounds.clear()
    }

    override val allBounds: Map<String, RenderedBounds>
        get() = _bounds

    /**
     * Hit-tests all nodes at the given Canvas-relative offset.
     * Selects the deepest (innermost leaf) matching node, or the topmost container.
     */
    override fun findNodeAt(offset: Offset, project: M3EProject): CanvasNode? {
        val allNodes = project.allNodes()
        val candidates = allNodes.filter { node ->
            if (!node.isVisible || node.isLocked) return@filter false
            val b = _bounds[node.id] ?: return@filter false
            b.contains(offset.x, offset.y)
        }
        if (candidates.isEmpty()) return null

        // Prefer leaf nodes over containers; if both are leaves or both are containers, prefer smaller area (innermost)
        return candidates.minByOrNull { node ->
            val b = _bounds[node.id]!!
            val area = b.width * b.height
            val depthBias = if (node.children.isEmpty()) 0f else 1_000_000f
            area + depthBias
        }
    }

    /**
     * Finds all nodes whose rendered bounds intersect with the given selection rectangle.
     */
    override fun findNodesIn(rect: Rect, project: M3EProject): List<CanvasNode> {
        val allNodes = project.allNodes()
        return allNodes.filter { node ->
            if (!node.isVisible || node.isLocked) return@filter false
            val b = _bounds[node.id] ?: return@filter false
            !(rect.right < b.boundsInCanvas.left || rect.left > b.boundsInCanvas.right ||
              rect.bottom < b.boundsInCanvas.top || rect.top > b.boundsInCanvas.bottom)
        }
    }
}
