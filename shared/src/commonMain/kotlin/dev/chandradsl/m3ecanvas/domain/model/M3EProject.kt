package dev.chandradsl.m3ecanvas.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents an entire M3E Canvas project.
 *
 * This is the root model that gets serialized when saving and deserialized
 * when loading. It ties together the device frame, all canvas nodes,
 * and project metadata.
 *
 * Timestamps are stored as epoch milliseconds and are populated by the
 * persistence layer, keeping this domain model free of platform time APIs.
 */
@OptIn(ExperimentalUuidApi::class)
data class M3EProject(
    val id: String = Uuid.random().toString(),
    val name: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val deviceProfile: DeviceProfile,
    val nodes: List<CanvasNode> = emptyList(),
    val themeId: String = DEFAULT_THEME_ID,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {

    companion object {
        /** Bump this whenever the project file format changes, to support migration. */
        const val CURRENT_SCHEMA_VERSION = 1

        /** Identifier for the baseline Material 3 theme used before custom theming. */
        const val DEFAULT_THEME_ID = "m3_baseline"
    }

    /** Returns the node matching [nodeId], or null if it is not present. */
    fun nodeById(nodeId: String): CanvasNode? {
        return nodes.firstOrNull { it.id == nodeId }
    }

    /** Returns a copy with the given node added. */
    fun withNode(node: CanvasNode): M3EProject {
        return copy(nodes = nodes + node)
    }

    /** Returns a copy with the given node replaced. */
    fun updateNode(node: CanvasNode): M3EProject {
        val updated = nodes.map { if (it.id == node.id) node else it }
        return copy(nodes = updated)
    }

    /** Returns a copy with the node matching [nodeId] removed. */
    fun withoutNode(nodeId: String): M3EProject {
        return copy(nodes = nodes.filterNot { it.id == nodeId })
    }
}