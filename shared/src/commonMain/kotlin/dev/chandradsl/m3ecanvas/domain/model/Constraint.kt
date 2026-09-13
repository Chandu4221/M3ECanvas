// Constraint.kt
package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable
/**
 * Enum representing different screen size breakpoints.
 */
enum class Breakpoint {
    XS, // Extra Small (e.g., phones)
    SM, // Small (e.g., portrait tablets)
    MD, // Medium (e.g., landscape tablets)
    LG, // Large (e.g., small desktops)
    XL  // Extra Large (e.g., large desktops)
}

/**
 * A constraint applied at a specific breakpoint.
 *
 * @property breakpoint The breakpoint this constraint applies to.
 * @property minWidth Optional minimum width in dp.
 * @property maxWidth Optional maximum width in dp.
 * @property minHeight Optional minimum height in dp.
 * @property maxHeight Optional maximum height in dp.
 */
@Serializable
data class Constraint(
    val breakpoint: Breakpoint,
    val minWidth: Int? = null,
    val maxWidth: Int? = null,
    val minHeight: Int? = null,
    val maxHeight: Int? = null
)
