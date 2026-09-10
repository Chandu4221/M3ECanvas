package dev.chandradsl.m3ecanvas.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chandradsl.m3ecanvas.domain.model.*

/**
 * Manages the editor's state in response to user actions.
 *
 * Hierarchy-aware: nodes can be top-level or nested inside containers,
 * and all operations search the full tree.
 */
class EditorController(
    initialProject: M3EProject
) {

    var state: EditorState by mutableStateOf(EditorState(project = initialProject))
        private set

    //region Selection

    fun selectNode(nodeId: String) {
        state = state.copy(selectedNodeIds = setOf(nodeId))
    }

    fun clearSelection() {
        state = state.copy(selectedNodeIds = emptySet())
    }

    //endregion

    //region Node editing

    /** Adds a new top-level component of [type] at [position] and selects it. */
    fun addNode(type: ComponentType, position: CanvasPosition) {
        val existingCount = state.project.nodes.count { it.type == type }
        val name = "${type.displayName} ${existingCount + 1}"

        val node = CanvasNode(
            type = type,
            name = name,
            position = position,
            size = defaultSizeFor(type = type)
        )

        state = state.copy(
            project = state.project.withNode(node = node),
            selectedNodeIds = setOf(node.id)
        )
    }

    /** Adds a new child of [type] inside the container with [containerId]. */
    fun addChildToContainer(containerId: String, type: ComponentType) {
        val container = state.project.findNode(nodeId = containerId) ?: return
        if (!container.isContainer) return

        val childCount = container.children.count { it.type == type }
        val name = "${type.displayName} ${childCount + 1}"

        val child = CanvasNode(
            type = type,
            name = name,
            position = CanvasPosition.Zero,
            size = defaultSizeFor(type = type)
        )

        state = state.copy(
            project = state.project.addChildToContainer(
                containerId = containerId,
                child = child
            ),
            selectedNodeIds = setOf(child.id)
        )
    }

    /** Removes the node with [nodeId] from anywhere in the tree. */
    fun removeNode(nodeId: String) {
        state = state.copy(
            project = state.project.removeNode(nodeId = nodeId),
            selectedNodeIds = state.selectedNodeIds - nodeId
        )
    }

    /** Moves the node one position earlier among its siblings (or root nodes). */
    fun moveNodeUp(nodeId: String) {
        reorderNode(nodeId = nodeId, delta = -1)
    }

    /** Moves the node one position later among its siblings (or root nodes). */
    fun moveNodeDown(nodeId: String) {
        reorderNode(nodeId = nodeId, delta = 1)
    }

    /**
     * Reorders a node within its parent's children, or within the root list
     * if it is a top-level node. [delta] of -1 moves earlier, 1 moves later.
     */
    private fun reorderNode(nodeId: String, delta: Int) {
        val parent = findParentInProject(nodeId = nodeId)

        if (parent == null) {
            val nodes = state.project.nodes
            val index = nodes.indexOfFirst { it.id == nodeId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= nodes.size) return
            val reordered = nodes.toMutableList().apply {
                val item = removeAt(index = index)
                add(index = target, element = item)
            }
            state = state.copy(project = state.project.copy(nodes = reordered))
        } else {
            val children = parent.children
            val index = children.indexOfFirst { it.id == nodeId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= children.size) return
            val reordered = children.toMutableList().apply {
                val item = removeAt(index = index)
                add(index = target, element = item)
            }
            val updatedParent = parent.copy(children = reordered)
            state = state.copy(
                project = state.project.updateNode(node = updatedParent)
            )
        }
    }

    /** Finds the parent of [nodeId] anywhere in the tree, or null if top-level. */
    private fun findParentInProject(nodeId: String): CanvasNode? {
        for (root in state.project.nodes) {
            val found = findParent(node = root, targetId = nodeId)
            if (found != null) return found
        }
        return null
    }

    private fun findParent(node: CanvasNode, targetId: String): CanvasNode? {
        for (child in node.children) {
            if (child.id == targetId) return node
            val deeper = findParent(node = child, targetId = targetId)
            if (deeper != null) return deeper
        }
        return null
    }

    /** Applies or replaces a [property] on the node with [nodeId], wherever it is. */
    fun updateNodeProperty(nodeId: String, property: ComponentProperty) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        val updatedNode = node.withProperty(property = property)
        state = state.copy(
            project = state.project.updateNode(node = updatedNode)
        )
    }

    //endregion

    //region Drag

    /** Begins a drag on the node with [nodeId]. Selection is not changed here. */
    fun startDrag(nodeId: String) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            drag = DragState(
                nodeId = nodeId,
                originalNodePosition = node.position
            )
        )
    }

    fun updateDrag(deltaX: Float, deltaY: Float) {
        val drag = state.drag ?: return
        val node = state.project.findNode(nodeId = drag.nodeId) ?: return
        val movedNode = node.movedBy(dx = deltaX, dy = deltaY)
        state = state.copy(
            project = state.project.updateNode(node = movedNode)
        )
    }

    fun endDrag() {
        state = state.copy(drag = null)
    }

    //endregion

    //region Device

    fun setDeviceProfile(profile: DeviceProfile) {
        state = state.copy(
            project = state.project.copy(deviceProfile = profile)
        )
    }

    //endregion

    /**
     * Placeholder sizes per component. The ComponentRegistry will replace this
     * with accurate Material defaults in a later step.
     */
    private fun defaultSizeFor(type: ComponentType): CanvasSize {
        return when (type) {
            ComponentType.BUTTON -> CanvasSize(width = 120f, height = 40f)
            ComponentType.CARD -> CanvasSize(width = 280f, height = 160f)
            ComponentType.TEXT_FIELD -> CanvasSize(width = 280f, height = 56f)
            ComponentType.COLUMN,
            ComponentType.LAZY_COLUMN -> CanvasSize(width = 300f, height = 400f)
            ComponentType.ROW,
            ComponentType.LAZY_ROW -> CanvasSize(width = 400f, height = 120f)
            ComponentType.BOX -> CanvasSize(width = 300f, height = 300f)
            else -> CanvasSize(width = 160f, height = 48f)
        }
    }

    companion object {
        fun newProject(name: String = "Untitled"): M3EProject {
            return M3EProject(
                name = name,
                deviceProfile = DeviceProfile.default
            )
        }
    }
}