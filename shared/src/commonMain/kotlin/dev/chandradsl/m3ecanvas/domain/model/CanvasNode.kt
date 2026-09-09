package dev.chandradsl.m3ecanvas.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a single component instance placed on the canvas.
 *
 * A node combines the component type, its transform (position and size),
 * and its editable properties. The canvas itself is a collection of nodes.
 *
 * Each node has a unique [id] generated via UUID, so multiple instances
 * of the same [ComponentType] are fully supported.
 */
@OptIn(ExperimentalUuidApi::class)
data class CanvasNode(
    val id: String = Uuid.random().toString(),
    val type: ComponentType,
    val name: String,
    val position: CanvasPosition,
    val size: CanvasSize,
    val properties: List<ComponentProperty> = emptyList()
) {

    /** Returns the property matching [key], or null if it is not present. */
    fun property(key: String): ComponentProperty? {
        return properties.firstOrNull { it.key == key }
    }

    /** Returns a copy with the given property added or replaced. */
    fun withProperty(property: ComponentProperty): CanvasNode {
        val updated = properties.filterNot { it.key == property.key } + property
        return copy(properties = updated)
    }

    /** Returns a copy moved by the given deltas. */
    fun movedBy(dx: Float, dy: Float): CanvasNode {
        return copy(
            position = position.offset(dx = dx, dy = dy)
        )
    }

    /** Returns a copy resized by the given deltas. */
    fun resizedBy(dWidth: Float, dHeight: Float): CanvasNode {
        return copy(
            size = size.resize(dWidth = dWidth, dHeight = dHeight)
        )
    }
}