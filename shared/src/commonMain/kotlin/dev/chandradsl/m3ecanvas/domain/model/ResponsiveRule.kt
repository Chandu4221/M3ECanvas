package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * A responsive rule maps a breakpoint to a list of constraints.
 */
@Serializable
data class ResponsiveRule(
    val breakpoint: Breakpoint,
    val constraints: List<Constraint> = emptyList()
)
