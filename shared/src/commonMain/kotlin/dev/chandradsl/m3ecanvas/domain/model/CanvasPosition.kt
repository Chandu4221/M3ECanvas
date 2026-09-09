package dev.chandradsl.m3ecanvas.domain.model

/**
 * Represents the position of a component on the canvas.
 * Uses Float to support sub-pixel precision during drag, zoom, and pan operations.
 */
data class CanvasPosition(
    val x: Float,
    val y: Float
) {
    companion object {
        /** The origin point of the canvas. */
        val Zero = CanvasPosition(x = 0f, y = 0f)
    }

    /**
     * Returns a new position shifted by the given deltas.
     * Used during drag operations to apply movement.
     */
    fun offset(dx: Float, dy: Float): CanvasPosition {
        return CanvasPosition(
            x = x + dx,
            y = y + dy
        )
    }
}