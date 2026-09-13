package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Defines a slot that a component can contain.
 *
 * - `name`: Identifier for the slot.
 * - `allowedComponentTypes`: List of component types that are permitted in this slot.
 * - `minCount`: Minimum number of child components required (null = no minimum).
 * - `maxCount`: Maximum number of child components allowed (null = unlimited).
 */
@Serializable
data class SlotDefinition(
    val name: String,
    val allowedComponentTypes: List<ComponentType> = emptyList(),
    val minCount: Int? = null,
    val maxCount: Int? = null
)
