package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Defines how a layout container distributes its children along its main axis.
 *
 * Maps to Compose `Arrangement` for `Column`, `Row`, `LazyColumn`, and `LazyRow`.
 * For a `Column` the axis is vertical; for a `Row` it is horizontal.
 */
enum class LayoutArrangement(val displayName: String) {
    START(displayName = "Start"),
    CENTER(displayName = "Center"),
    END(displayName = "End"),
    SPACE_BETWEEN(displayName = "Space Between"),
    SPACE_AROUND(displayName = "Space Around"),
    SPACE_EVENLY(displayName = "Space Evenly")
}

/**
 * Configuration for a layout container node.
 *
 * Controls how a container arranges, spaces, and pads its children,
 * and whether the container scrolls. Applied to COLUMN, ROW, BOX,
 * LAZY_COLUMN, and LAZY_ROW nodes.
 */
@Serializable
data class LayoutConfig(
    val arrangement: LayoutArrangement = LayoutArrangement.START,
    val spacing: Float = 0f,
    val padding: Float = 0f,
    val scrollable: Boolean = false
) {
    companion object {
        /** Default for Column, Row, and Box. Non-scrolling. */
        val default = LayoutConfig()

        /** Default for scrollable containers like LazyColumn and LazyRow. */
        val scrollable = LayoutConfig(scrollable = true)
    }
}