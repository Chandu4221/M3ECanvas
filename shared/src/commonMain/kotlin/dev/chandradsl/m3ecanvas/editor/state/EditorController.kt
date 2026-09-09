package dev.chandradsl.m3ecanvas.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chandradsl.m3ecanvas.domain.model.*

/**
 * Manages the editor's state in response to user actions.
 *
 * The controller holds a Compose-observable [EditorState]. Every user action
 * produces a new immutable state, which triggers UI recomposition. All actual
 * edit logic lives in the pure domain models; the controller only orchestrates.
 */
class EditorController(
    initialProject: M3EProject
) {

    var state: EditorState by mutableStateOf(EditorState(project = initialProject))
        private set

    //region Selection

    /** Selects a single node, replacing any existing selection. */
    fun selectNode(nodeId: String) {
        state = state.copy(selectedNodeIds = setOf(nodeId))
    }

    /** Clears the current selection. */
    fun clearSelection() {
        state = state.copy(selectedNodeIds = emptySet())
    }

    //endregion

    //region Node editing

    /** Adds a new component of [type] at [position] and selects it. */
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

    /** Removes the node with [nodeId] and deselects it. */
    fun removeNode(nodeId: String) {
        state = state.copy(
            project = state.project.withoutNode(nodeId = nodeId),
            selectedNodeIds = state.selectedNodeIds - nodeId
        )
    }

    /** Applies or replaces a single [property] on the node with [nodeId]. */
    fun updateNodeProperty(nodeId: String, property: ComponentProperty) {
        val node = state.project.nodeById(nodeId = nodeId) ?: return
        val updatedNode = node.withProperty(property = property)
        state = state.copy(
            project = state.project.updateNode(node = updatedNode)
        )
    }

    //endregion

    //region Drag

    /** Begins a drag on the node with [nodeId] and selects it. */
    fun startDrag(nodeId: String) {
        val node = state.project.nodeById(nodeId = nodeId) ?: return
        state = state.copy(
            drag = DragState(
                nodeId = nodeId,
                originalNodePosition = node.position
            ),
            selectedNodeIds = setOf(nodeId)
        )
    }

    /** Applies a drag delta to the node currently being dragged. */
    fun updateDrag(deltaX: Float, deltaY: Float) {
        val drag = state.drag ?: return
        val node = state.project.nodeById(nodeId = drag.nodeId) ?: return
        val movedNode = node.movedBy(dx = deltaX, dy = deltaY)
        state = state.copy(
            project = state.project.updateNode(node = movedNode)
        )
    }

    /** Ends the current drag operation. */
    fun endDrag() {
        state = state.copy(drag = null)
    }

//endregion

    //endregion

    //region Device

    /** Changes the project's target device frame. */
    fun setDeviceProfile(profile: DeviceProfile) {
        state = state.copy(
            project = state.project.copy(deviceProfile = profile)
        )
    }

    //endregion

    /**
     * Placeholder sizes per component. The ComponentRegistry built in a later
     * step will provide accurate Material defaults and default properties.
     */
    private fun defaultSizeFor(type: ComponentType): CanvasSize {
        return when (type) {
            ComponentType.BUTTON -> CanvasSize(width = 120f, height = 40f)
            ComponentType.CARD -> CanvasSize(width = 280f, height = 160f)
            ComponentType.TEXT_FIELD -> CanvasSize(width = 280f, height = 56f)
            else -> CanvasSize(width = 160f, height = 48f)
        }
    }

    companion object {
        /** Creates a fresh project with the default device profile. */
        fun newProject(name: String = "Untitled"): M3EProject {
            return M3EProject(
                name = name,
                deviceProfile = DeviceProfile.default
            )
        }
    }
}