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
enum class SlotRole(val displayName: String) {
    TOP_BAR(displayName = "Top App Bar"),
    BOTTOM_BAR(displayName = "Bottom Bar"),
    FAB(displayName = "Floating Action Button"),
    SNACKBAR(displayName = "Snackbar"),
    CONTENT(displayName = "Content")
}
