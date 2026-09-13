package dev.chandradsl.m3ecanvas.editor.service

import androidx.compose.ui.geometry.Rect
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import dev.chandradsl.m3ecanvas.editor.canvas.LayoutGeometryStore

/**
 * Service responsible for managing node selection state and marquee selection queries.
 */
class SelectionService {

    fun selectNode(currentSelection: Set<String>, nodeId: String, isInteractive: Boolean): Set<String> {
        if (isInteractive) return currentSelection
        return setOf(nodeId)
    }

    fun toggleSelectNode(currentSelection: Set<String>, nodeId: String, isInteractive: Boolean): Set<String> {
        if (isInteractive) return currentSelection
        return if (nodeId in currentSelection) currentSelection - nodeId else currentSelection + nodeId
    }

    fun selectAdditionalNode(currentSelection: Set<String>, nodeId: String, isInteractive: Boolean): Set<String> {
        if (isInteractive) return currentSelection
        return currentSelection + nodeId
    }

    fun selectNodes(nodeIds: Collection<String>, isInteractive: Boolean): Set<String> {
        if (isInteractive) return emptySet()
        return nodeIds.toSet()
    }

    fun selectAll(project: M3EProject, isInteractive: Boolean): Set<String> {
        if (isInteractive) return emptySet()
        return project.nodes.map { it.id }.toSet()
    }

    fun clearSelection(): Set<String> = emptySet()

    fun filterValidSelections(selectedIds: Set<String>, project: M3EProject): Set<String> {
        return selectedIds.filter { project.findNode(it) != null }.toSet()
    }

    fun selectNodesInMarquee(
        project: M3EProject,
        rect: Rect,
        geometryStore: LayoutGeometryStore,
        isInteractive: Boolean
    ): Set<String> {
        if (isInteractive) return emptySet()
        val matchingNodes = geometryStore.findNodesIn(rect = rect, project = project)
        return matchingNodes.map { it.id }.toSet()
    }

    fun resolveSelectedNodes(selectedIds: Set<String>, project: M3EProject): List<CanvasNode> {
        return selectedIds.mapNotNull { project.findNode(it) }
    }
}
