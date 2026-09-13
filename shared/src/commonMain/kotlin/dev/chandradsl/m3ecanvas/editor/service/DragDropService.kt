package dev.chandradsl.m3ecanvas.editor.service

import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import dev.chandradsl.m3ecanvas.editor.canvas.AlignmentSnapper
import dev.chandradsl.m3ecanvas.editor.canvas.SnapResult
import dev.chandradsl.m3ecanvas.editor.state.AlignmentGuide
import dev.chandradsl.m3ecanvas.editor.state.DragState

/**
 * Service responsible for managing node drag interactions, movement deltas,
 * and alignment guide snapping.
 */
class DragDropService {

    fun startDrag(
        project: M3EProject,
        nodeId: String,
        selectedNodeIds: Set<String>,
        isInteractive: Boolean
    ): DragState? {
        if (isInteractive) return null
        val node = project.findNode(nodeId) ?: return null
        if (node.isLockedInSlot || node.type == ComponentType.SCAFFOLD || node.type.isOverlay()) return null

        val targetNodeIds = if (nodeId in selectedNodeIds && selectedNodeIds.size > 1) {
            selectedNodeIds.filter { id ->
                val n = project.findNode(id)
                n != null && !n.isLockedInSlot && n.type != ComponentType.SCAFFOLD && !n.type.isOverlay()
            }.toSet()
        } else {
            setOf(nodeId)
        }

        val originalPositions = targetNodeIds.mapNotNull { id ->
            val n = project.findNode(id)
            if (n != null) id to n.position else null
        }.toMap()

        return DragState(
            nodeId = nodeId,
            originalNodePosition = node.position,
            nodeIds = targetNodeIds,
            originalPositions = originalPositions
        )
    }

    fun updateDrag(
        drag: DragState,
        project: M3EProject,
        deltaX: Float,
        deltaY: Float,
        snappingEnabled: Boolean,
        alignmentSnapper: AlignmentSnapper
    ): Pair<M3EProject, List<AlignmentGuide>> {
        val primaryNode = project.findNode(drag.nodeId) ?: return Pair(project, emptyList())

        val candidatePos = primaryNode.position.offset(deltaX, deltaY)
        val otherNodes = project.nodes.filter { it.id !in drag.nodeIds && it.type != ComponentType.SCAFFOLD && !it.type.isOverlay() }
        val snapResult = if (snappingEnabled && drag.nodeIds.size == 1) {
            alignmentSnapper.computeSnap(
                node = primaryNode,
                candidatePos = candidatePos,
                otherNodes = otherNodes,
                deviceWidth = project.deviceProfile.size.width,
                deviceHeight = project.deviceProfile.size.height,
                enabled = true
            )
        } else {
            SnapResult(candidatePos, emptyList())
        }

        val effectiveDeltaX = snapResult.snappedPosition.x - primaryNode.position.x
        val effectiveDeltaY = snapResult.snappedPosition.y - primaryNode.position.y

        var updatedProject = project
        for (id in drag.nodeIds) {
            val n = updatedProject.findNode(id) ?: continue
            if (!n.isLockedInSlot && n.type != ComponentType.SCAFFOLD && !n.type.isOverlay()) {
                updatedProject = updatedProject.updateNode(n.movedBy(dx = effectiveDeltaX, dy = effectiveDeltaY))
            }
        }

        return Pair(updatedProject, snapResult.guides)
    }
}
