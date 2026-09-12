package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a single component instance placed on the canvas.
 *
 * A node combines the component type, its transform, its editable properties,
 * its [children] and [layoutConfig] for containers, and its [modifiers]
 * chain for visual decoration.
 */
@Serializable
@OptIn(ExperimentalUuidApi::class)
data class CanvasNode(
    val id: String = Uuid.random().toString(),
    val type: ComponentType,
    val name: String,
    val position: CanvasPosition,
    val size: CanvasSize,
    val properties: List<ComponentProperty> = emptyList(),
    val children: List<CanvasNode> = emptyList(),
    val layoutConfig: LayoutConfig = LayoutConfig.default,
    val modifiers: List<ModifierSpec> = emptyList(),
    val slot: SlotRole? = null
) {

    /** Whether this node can hold children, based on its type. */
    val isContainer: Boolean
        get() = type.isContainer

    /** Whether this node is locked into a structural slot (e.g. Scaffold topBar, bottomBar, FAB). */
    val isLockedInSlot: Boolean
        get() = slot != null && slot != SlotRole.CONTENT

    //region Slot operations

    /** Returns the first child assigned to [slotRole], or null if none. */
    fun childInSlot(slotRole: SlotRole): CanvasNode? {
        return children.firstOrNull { it.slot == slotRole }
    }

    /** Returns all children assigned to [slotRole]. */
    fun childrenInSlot(slotRole: SlotRole): List<CanvasNode> {
        return children.filter { it.slot == slotRole }
    }

    /** Returns a copy with the child added into [slotRole], replacing any existing single-slot occupant. */
    fun withChildInSlot(child: CanvasNode, slotRole: SlotRole): CanvasNode {
        val slottedChild = child.copy(slot = slotRole)
        val updatedChildren = if (slotRole != SlotRole.CONTENT) {
            children.filterNot { it.slot == slotRole } + slottedChild
        } else {
            children + slottedChild
        }
        return copy(children = updatedChildren)
    }

    //endregion

    //region Properties

    /** Returns the property matching [key], or null if it is not present. */
    fun property(key: String): ComponentProperty? {
        return properties.firstOrNull { it.key == key }
    }

    /** Returns a copy with the given property added or replaced. */
    fun withProperty(property: ComponentProperty): CanvasNode {
        val updated = properties.filterNot { it.key == property.key } + property
        return copy(properties = updated)
    }

    /** Convenience helper for string/text properties. */
    fun textProperty(key: String, default: String = ""): String {
        return (property(key = key) as? ComponentProperty.Text)?.value ?: default
    }

    /** Convenience helper for boolean properties. */
    fun booleanProperty(key: String, default: Boolean = false): Boolean {
        return (property(key = key) as? ComponentProperty.BooleanFlag)?.value ?: default
    }

    /** Convenience helper for numeric properties. */
    fun numericProperty(key: String, default: Float = 0f): Float {
        return (property(key = key) as? ComponentProperty.Numeric)?.value ?: default
    }

    /** Convenience helper for icon properties. */
    fun iconProperty(key: String, default: String = "Favorite"): String {
        return (property(key = key) as? ComponentProperty.Icon)?.iconName ?: default
    }

    //endregion

    //region Transform

    /** Returns a copy moved by the given deltas. */
    fun movedBy(dx: Float, dy: Float): CanvasNode {
        return copy(position = position.offset(dx = dx, dy = dy))
    }

    /** Returns a copy resized by the given deltas. */
    fun resizedBy(dWidth: Float, dHeight: Float): CanvasNode {
        return copy(size = size.resize(dWidth = dWidth, dHeight = dHeight))
    }

    //endregion

    //region Direct children

    /** Returns a copy with the given child added. */
    fun withChild(child: CanvasNode): CanvasNode {
        return copy(children = children + child)
    }

    /** Returns a copy with the child matching [childId] removed. */
    fun withoutChild(childId: String): CanvasNode {
        return copy(children = children.filterNot { it.id == childId })
    }

    /** Returns a copy with the given child replaced. */
    fun updateChild(child: CanvasNode): CanvasNode {
        val updated = children.map { if (it.id == child.id) child else it }
        return copy(children = updated)
    }

    /** Returns the direct child matching [childId], or null. */
    fun childById(childId: String): CanvasNode? {
        return children.firstOrNull { it.id == childId }
    }

    //endregion

    //region Recursive tree operations

    /** Recursively searches this node and all descendants for [nodeId]. */
    fun findNode(nodeId: String): CanvasNode? {
        if (id == nodeId) return this
        for (child in children) {
            val found = child.findNode(nodeId = nodeId)
            if (found != null) return found
        }
        return null
    }

    /** Recursively replaces the node matching [node.id] anywhere below. */
    fun updateNodeDeep(node: CanvasNode): CanvasNode {
        if (id == node.id) return node
        val updatedChildren = children.map { it.updateNodeDeep(node = node) }
        return copy(children = updatedChildren)
    }

    /** Recursively adds [child] to the container matching [containerId]. */
    fun addChildDeep(containerId: String, child: CanvasNode): CanvasNode {
        if (id == containerId) return withChild(child = child)
        val updatedChildren = children.map {
            it.addChildDeep(containerId = containerId, child = child)
        }
        return copy(children = updatedChildren)
    }

    /** Recursively removes the node matching [nodeId] from anywhere below. */
    fun removeNodeDeep(nodeId: String): CanvasNode {
        val updatedChildren = children
            .filterNot { it.id == nodeId }
            .map { it.removeNodeDeep(nodeId = nodeId) }
        return copy(children = updatedChildren)
    }

    /** Returns a flat list containing this node and all of its recursive descendants. */
    fun allNodes(): List<CanvasNode> {
        val list = mutableListOf<CanvasNode>()
        fun traverse(n: CanvasNode) {
            list.add(n)
            n.children.forEach { traverse(it) }
        }
        traverse(this)
        return list
    }

    //endregion

    //region Modifier chain

    /** Returns a copy with [spec] appended to the end of the chain. */
    fun withModifier(spec: ModifierSpec): CanvasNode {
        return copy(modifiers = modifiers + spec)
    }

    /** Returns a copy with the modifier matching [specId] removed. */
    fun withoutModifier(specId: String): CanvasNode {
        return copy(modifiers = modifiers.filterNot { it.id == specId })
    }

    /** Returns a copy with the modifier matching [spec.id] replaced. */
    fun updateModifier(spec: ModifierSpec): CanvasNode {
        val updated = modifiers.map { if (it.id == spec.id) spec else it }
        return copy(modifiers = updated)
    }

    /**
     * Returns a copy with the modifier matching [specId] moved by [delta]
     * (-1 earlier, +1 later). Order is semantically significant in Compose.
     */
    fun moveModifier(specId: String, delta: Int): CanvasNode {
        val index = modifiers.indexOfFirst { it.id == specId }
        val target = index + delta
        if (index < 0 || target < 0 || target >= modifiers.size) return this
        val reordered = modifiers.toMutableList().apply {
            val item = removeAt(index = index)
            add(index = target, element = item)
        }
        return copy(modifiers = reordered)
    }

    //endregion

    //region Cloning

    /**
     * Creates a deep clone of this node with newly generated UUIDs for the node,
     * its modifiers, and all nested descendants recursively.
     *
     * @param offsetPosition When true, offsets the position by ([dx], [dy]) so the duplicate does not occlude the original.
     * @param newName Optional override for the cloned node's name. Defaults to the original name.
     */
    fun deepCloneWithNewIds(
        offsetPosition: Boolean = true,
        dx: Float = 24f,
        dy: Float = 24f,
        newName: String = name
    ): CanvasNode {
        val newId = Uuid.random().toString()
        val newPosition = if (offsetPosition) position.offset(dx = dx, dy = dy) else position
        val newModifiers = modifiers.map { it.withNewId() }
        val newChildren = children.map { it.deepCloneWithNewIds(offsetPosition = false) }
        return copy(
            id = newId,
            name = newName,
            position = newPosition,
            modifiers = newModifiers,
            children = newChildren
        )
    }

    //endregion

    //region Sizing Resolution

    /** Whether this node defines a fillMaxWidth or fillMaxSize modifier. */
    fun hasFillMaxWidth(): Boolean =
        modifiers.any { it is ModifierSpec.FillMaxWidth || it is ModifierSpec.FillMaxSize }

    /** Whether this node defines a fillMaxHeight or fillMaxSize modifier. */
    fun hasFillMaxHeight(): Boolean =
        modifiers.any { it is ModifierSpec.FillMaxHeight || it is ModifierSpec.FillMaxSize }

    /** Whether this node defines explicit width modifiers. */
    fun hasExplicitWidth(): Boolean =
        hasFillMaxWidth() || modifiers.any { it is ModifierSpec.Width || it is ModifierSpec.Size }

    /** Whether this node defines explicit height modifiers. */
    fun hasExplicitHeight(): Boolean =
        hasFillMaxHeight() || modifiers.any { it is ModifierSpec.Height || it is ModifierSpec.Size }

    //endregion
}