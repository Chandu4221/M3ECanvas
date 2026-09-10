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

    fun startDrag(nodeId: String) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            drag = DragState(
                nodeId = nodeId,
                originalNodePosition = node.position
            ),
            selectedNodeIds = setOf(nodeId)
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