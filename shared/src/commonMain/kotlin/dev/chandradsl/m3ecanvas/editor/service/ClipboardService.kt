package dev.chandradsl.m3ecanvas.editor.service

import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.M3EProject

/**
 * Service responsible for clipboard operations: copying, pasting, and duplicating nodes.
 */
class ClipboardService {

    fun copySelected(selectedNodes: List<CanvasNode>): CanvasNode? {
        return selectedNodes.firstOrNull()
    }

    fun paste(
        project: M3EProject,
        sourceNode: CanvasNode?,
        selectedNode: CanvasNode?,
        targetContainerFinder: (ComponentType, String?) -> CanvasNode?
    ): Pair<M3EProject, CanvasNode?> {
        val nodeToPaste = sourceNode ?: return Pair(project, null)
        val clone = nodeToPaste.deepCloneWithNewIds(offsetPosition = true)
        val hasScaffold = project.nodes.any { it.type == ComponentType.SCAFFOLD }

        if (clone.type == ComponentType.SCAFFOLD && hasScaffold) {
            return Pair(project, null)
        }

        val targetContainer = targetContainerFinder(clone.type, selectedNode?.id)

        val updatedProject = if (targetContainer != null) {
            val supported = targetContainer.type.supportedSlots()
            val targetSlot = if (supported.isNotEmpty()) {
                val allowed = clone.type.allowedSlotsIn(targetContainer.type)
                val occupiedSlots = targetContainer.children.mapNotNull { it.slot }.toSet()
                allowed.firstOrNull { it.isMultiOccupant || it !in occupiedSlots }
                    ?: clone.type.canonicalSlotFor(targetContainer.type)
            } else {
                null
            }
            val positionedClone = clone.copy(slot = targetSlot)
            if (targetSlot != null && !targetSlot.isMultiOccupant) {
                project.updateNode(targetContainer.withChildInSlot(positionedClone, targetSlot))
            } else {
                project.updateNode(targetContainer.withChild(positionedClone))
            }
        } else if (clone.type.isAllowedAtRoot(hasScaffold)) {
            project.withNode(clone)
        } else {
            return Pair(project, null)
        }

        return Pair(updatedProject, clone)
    }

    fun duplicateSelected(
        project: M3EProject,
        selected: CanvasNode?,
        parentFinder: (String) -> CanvasNode?
    ): Pair<M3EProject, CanvasNode?> {
        if (selected == null) return Pair(project, null)
        if (selected.isLockedInSlot) return Pair(project, null)
        if (selected.type == ComponentType.SCAFFOLD) return Pair(project, null)

        val parent = parentFinder(selected.id)
        if (parent != null) {
            if (!parent.type.canAcceptChild(selected.type)) return Pair(project, null)
            val slot = selected.slot
            if (slot != null && !slot.isMultiOccupant) return Pair(project, null)
        } else {
            val hasScaffold = project.nodes.any { it.type == ComponentType.SCAFFOLD }
            if (!selected.type.isAllowedAtRoot(hasScaffold)) return Pair(project, null)
        }

        val clone = selected.deepCloneWithNewIds(offsetPosition = !selected.type.isOverlay())

        val updatedProject = if (parent != null) {
            val index = parent.children.indexOfFirst { it.id == selected.id }
            val updatedChildren = parent.children.toMutableList().apply {
                if (index >= 0) add(index + 1, clone) else add(clone)
            }
            project.updateNode(parent.copy(children = updatedChildren))
        } else {
            val index = project.nodes.indexOfFirst { it.id == selected.id }
            val updatedNodes = project.nodes.toMutableList().apply {
                if (index >= 0) add(index + 1, clone) else add(clone)
            }
            project.copy(nodes = updatedNodes)
        }

        return Pair(updatedProject, clone)
    }
}
