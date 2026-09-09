package dev.chandradsl.m3ecanvas.domain.model

/**
 * Represents the size of a component on the canvas.
 * Uses Float to support smooth resizing and zoom calculations.
 */
data class CanvasSize(
    val width: Float,
    val height: Float
) {
    companion object {
        /** A placeholder size used before a component is measured. */
        val Unspecified = CanvasSize(width = 0f, height = 0f)
    }

    /**
     * Returns a new size adjusted by the given deltas.
     * Used during resize operations.
     */
    fun resize(dWidth: Float, dHeight: Float): CanvasSize {
        return CanvasSize(width + dWidth, height + dHeight)
    }
}