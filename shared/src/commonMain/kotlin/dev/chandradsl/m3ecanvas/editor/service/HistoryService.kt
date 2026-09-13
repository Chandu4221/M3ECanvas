package dev.chandradsl.m3ecanvas.editor.service

import dev.chandradsl.m3ecanvas.domain.model.CanvasSize
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.DeviceProfile
import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import dev.chandradsl.m3ecanvas.domain.model.SlotRole
import dev.chandradsl.m3ecanvas.editor.history.HistoryStack

/**
 * Service responsible for managing project undo/redo history snapshots
 * and adapting historical snapshots to the active device configuration.
 */
class HistoryService(
    val stack: HistoryStack<M3EProject> = HistoryStack()
) {
    val canUndo: Boolean get() = stack.canUndo
    val canRedo: Boolean get() = stack.canRedo
    val undoCount: Int get() = stack.undoCount
    val redoCount: Int get() = stack.redoCount

    fun push(project: M3EProject) {
        stack.push(project)
    }

    fun undo(currentProject: M3EProject, deviceProfile: DeviceProfile): M3EProject? {
        val previous = stack.undo(currentProject) ?: return null
        return adaptProjectToDevice(previous, deviceProfile)
    }

    fun redo(currentProject: M3EProject, deviceProfile: DeviceProfile): M3EProject? {
        val next = stack.redo(currentProject) ?: return null
        return adaptProjectToDevice(next, deviceProfile)
    }

    fun undoSteps(steps: Int, currentProject: M3EProject, deviceProfile: DeviceProfile): M3EProject? {
        val previous = stack.undoSteps(steps, currentProject) ?: return null
        return adaptProjectToDevice(previous, deviceProfile)
    }

    fun redoSteps(steps: Int, currentProject: M3EProject, deviceProfile: DeviceProfile): M3EProject? {
        val next = stack.redoSteps(steps, currentProject) ?: return null
        return adaptProjectToDevice(next, deviceProfile)
    }

    fun clear() {
        stack.clear()
    }

    fun getUndoSnapshots(): List<M3EProject> = stack.getUndoList()
    fun getRedoSnapshots(): List<M3EProject> = stack.getRedoList()

    fun map(transform: (M3EProject) -> M3EProject) {
        stack.map(transform)
    }

    companion object {
        fun adaptProjectToDevice(project: M3EProject, profile: DeviceProfile): M3EProject {
            val updatedNodes = project.nodes.map { node ->
                if (node.type == ComponentType.SCAFFOLD) {
                    val updatedChildren = node.children.map { child ->
                        if (child.slot == SlotRole.CONTENT) {
                            child.copy(size = CanvasSize(width = profile.size.width, height = (profile.size.height - 144f).coerceAtLeast(0f)))
                        } else {
                            child
                        }
                    }
                    node.copy(size = profile.size, children = updatedChildren)
                } else {
                    node
                }
            }
            return project.copy(deviceProfile = profile, nodes = updatedNodes)
        }
    }
}
