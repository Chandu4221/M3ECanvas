package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Defines how a layout container distributes its children along its main axis.
 *
 * Maps to Compose `Arrangement` for `Column`, `Row`, `LazyColumn`, `LazyRow`, etc.
 */
@Serializable
enum class LayoutArrangement(val displayName: String) {
    START(displayName = "Start"),
    CENTER(displayName = "Center"),
    END(displayName = "End"),
    SPACE_BETWEEN(displayName = "Space Between"),
    SPACE_AROUND(displayName = "Space Around"),
    SPACE_EVENLY(displayName = "Space Evenly")
}

/**
 * Defines cross-axis alignment for Column, LazyColumn, FlowColumn.
 */
@Serializable
enum class HorizontalAlignment(val displayName: String) {
    START(displayName = "Start"),
    CENTER_HORIZONTALLY(displayName = "Center Horizontally"),
    END(displayName = "End")
}

/**
 * Defines cross-axis alignment for Row, LazyRow, FlowRow.
 */
@Serializable
enum class VerticalAlignment(val displayName: String) {
    TOP(displayName = "Top"),
    CENTER_VERTICALLY(displayName = "Center Vertically"),
    BOTTOM(displayName = "Bottom")
}

/**
 * Defines 2D content alignment for Box and Surface containers.
 */
@Serializable
enum class BoxAlignment(val displayName: String) {
    TOP_START(displayName = "Top Start"),
    TOP_CENTER(displayName = "Top Center"),
    TOP_END(displayName = "Top End"),
    CENTER_START(displayName = "Center Start"),
    CENTER(displayName = "Center"),
    CENTER_END(displayName = "Center End"),
    BOTTOM_START(displayName = "Bottom Start"),
    BOTTOM_CENTER(displayName = "Bottom Center"),
    BOTTOM_END(displayName = "Bottom End")
}

/**
 * Configuration for a layout container node.
 *
 * Controls how a container arranges, aligns, spaces, and pads its children,
 * and whether the container scrolls. Applied to COLUMN, ROW, BOX, SURFACE,
 * LAZY_COLUMN, LAZY_ROW, FLOW_ROW, FLOW_COLUMN, and LAZY_VERTICAL_GRID nodes.
 */
@Serializable
data class LayoutConfig(
    val arrangement: LayoutArrangement = LayoutArrangement.START,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.START,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.TOP,
    val boxAlignment: BoxAlignment = BoxAlignment.TOP_START,
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