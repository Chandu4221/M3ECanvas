package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Defines the structural slot a component occupies within a container composable
 * such as [ComponentType.SCAFFOLD].
 *
 * In Compose, components placed in structural slots (e.g. `topBar`, `bottomBar`,
 * `floatingActionButton`) are snapped directly to their canonical layout positions
 * governed by the Scaffold layout rules, rather than arbitrary X/Y offsets.
 */
@Serializable
enum class SlotRole(
    val displayName: String,
    val isMultiOccupant: Boolean = false
) {
    // Scaffold structural slots
    TOP_BAR(displayName = "Top App Bar"),
    BOTTOM_BAR(displayName = "Bottom Bar"),
    RAIL(displayName = "Navigation Rail"),
    DRAWER(displayName = "Navigation Drawer"),
    FAB(displayName = "Floating Action Button"),
    SNACKBAR(displayName = "Snackbar"),
    CONTENT(displayName = "Content", isMultiOccupant = true),

    // Component sub-slots
    NAVIGATION_ICON(displayName = "Navigation Icon"),
    TITLE(displayName = "Title"),
    ACTIONS(displayName = "Actions", isMultiOccupant = true),
    LEADING(displayName = "Leading"),
    TRAILING(displayName = "Trailing"),
    HEADLINE(displayName = "Headline"),
    SUPPORTING(displayName = "Supporting"),
    OVERLINE(displayName = "Overline"),
    CONFIRM_BUTTON(displayName = "Confirm Button"),
    DISMISS_BUTTON(displayName = "Dismiss Button"),
    ICON(displayName = "Icon"),
    BADGE(displayName = "Badge"),
    SHEET_CONTENT(displayName = "Sheet Content", isMultiOccupant = true),
    HEADER(displayName = "Header")
}

