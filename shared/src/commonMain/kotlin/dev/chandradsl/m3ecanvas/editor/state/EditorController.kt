package dev.chandradsl.m3ecanvas.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chandradsl.m3ecanvas.domain.model.*

/**
 * Manages the editor's state in response to user actions.
 * Hierarchy-aware: nodes can be top-level or nested inside containers.
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

    /** Deletes all currently selected nodes and clears the selection. */
    fun deleteSelected() {
        val ids = state.selectedNodeIds
        if (ids.isEmpty()) return
        var project = state.project
        for (id in ids) {
            project = project.removeNode(nodeId = id)
        }
        state = state.copy(
            project = project,
            selectedNodeIds = emptySet()
        )
    }

    /**
     * Nudges selected top-level nodes by the given delta. Children are skipped
     * because their position is controlled by their container's layout.
     */
    fun nudgeSelected(dx: Float, dy: Float) {
        val ids = state.selectedNodeIds
        if (ids.isEmpty()) return
        var project = state.project
        for (id in ids) {
            val isTopLevel = project.nodes.any { it.id == id }
            if (!isTopLevel) continue
            val node = project.findNode(nodeId = id) ?: continue
            project = project.updateNode(node = node.movedBy(dx = dx, dy = dy))
        }
        state = state.copy(project = project)
    }

    /** Applies or replaces a [property] on the node with [nodeId], wherever it is. */
    fun updateNodeProperty(nodeId: String, property: ComponentProperty) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        val updatedNode = node.withProperty(property = property)
        state = state.copy(
            project = state.project.updateNode(node = updatedNode)
        )
    }

    /** Renames the node with [nodeId]. */
    fun renameNode(nodeId: String, name: String) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            project = state.project.updateNode(node = node.copy(name = name))
        )
    }

    /** Replaces the layout config of the container with [nodeId]. */
    fun updateLayoutConfig(nodeId: String, config: LayoutConfig) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            project = state.project.updateNode(node = node.copy(layoutConfig = config))
        )
    }

    //endregion

    //region Modifier chain

    /** Appends [spec] to the end of the modifier chain of [nodeId]. */
    fun addModifier(nodeId: String, spec: ModifierSpec) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            project = state.project.updateNode(node = node.withModifier(spec = spec))
        )
    }

    /** Removes the modifier matching [specId] from [nodeId]. */
    fun removeModifier(nodeId: String, specId: String) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            project = state.project.updateNode(node = node.withoutModifier(specId = specId))
        )
    }

    /** Replaces the modifier matching [spec.id] on [nodeId]. */
    fun updateModifier(nodeId: String, spec: ModifierSpec) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            project = state.project.updateNode(node = node.updateModifier(spec = spec))
        )
    }

    /** Moves the modifier matching [specId] one position earlier. */
    fun moveModifierUp(nodeId: String, specId: String) {
        moveModifier(nodeId = nodeId, specId = specId, delta = -1)
    }

    /** Moves the modifier matching [specId] one position later. */
    fun moveModifierDown(nodeId: String, specId: String) {
        moveModifier(nodeId = nodeId, specId = specId, delta = 1)
    }

    private fun moveModifier(nodeId: String, specId: String, delta: Int) {
        val node = state.project.findNode(nodeId = nodeId) ?: return
        state = state.copy(
            project = state.project.updateNode(
                node = node.moveModifier(specId = specId, delta = delta)
            )
        )
    }

    //endregion

    //region Reorder

    /** Moves the node one position earlier among its siblings (or root nodes). */
    fun moveNodeUp(nodeId: String) {
        reorderNode(nodeId = nodeId, delta = -1)
    }

    /** Moves the node one position later among its siblings (or root nodes). */
    fun moveNodeDown(nodeId: String) {
        reorderNode(nodeId = nodeId, delta = 1)
    }

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

    /** Replaces the entire project and resets transient UI state (used by Load). */
    fun replaceProject(project: M3EProject) {
        state = state.copy(
            project = project,
            selectedNodeIds = emptySet(),
            drag = null
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