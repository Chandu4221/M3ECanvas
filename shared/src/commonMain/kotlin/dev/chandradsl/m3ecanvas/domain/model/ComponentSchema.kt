// ComponentSchema.kt
package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Schema definition for a component type, used by the editor to know what slots are
 * available and any other static metadata.
 */
@Serializable
data class ComponentSchema(
    val type: ComponentType,
    /**
     * List of named slots that this component can contain children for. Empty list if none.
     */
    val slots: List<SlotDefinition> = emptyList()
)
