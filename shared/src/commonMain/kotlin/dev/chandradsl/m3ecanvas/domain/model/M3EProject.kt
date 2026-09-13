package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable
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
/**
 * Configuration for the canvas's Material 3 theme.
 * Allows choosing a seed color (as a hex string, e.g. "#6750A4") and toggling dark mode.
 */
@Serializable
data class CanvasThemeConfig(
    val seedColorHex: String = DEFAULT_SEED_HEX,
    val isDark: Boolean = false,
    val colorOverrides: Map<String, String> = emptyMap(),
    val typography: DocumentTypographyConfig = DocumentTypographyConfig(),
    val shapes: DocumentShapesConfig = DocumentShapesConfig(),
    val customTokens: List<DesignToken> = emptyList()
) {
    companion object {
        const val DEFAULT_SEED_HEX = "#6750A4"
    }

    fun withColorOverride(role: String, hexColor: String): CanvasThemeConfig {
        return copy(colorOverrides = colorOverrides + (role to hexColor))
    }

    fun withoutColorOverride(role: String): CanvasThemeConfig {
        return copy(colorOverrides = colorOverrides - role)
    }

    fun withToken(token: DesignToken): CanvasThemeConfig {
        return copy(customTokens = customTokens.filterNot { it.id == token.id } + token)
    }

    fun withoutToken(tokenId: String): CanvasThemeConfig {
        return copy(customTokens = customTokens.filterNot { it.id == tokenId })
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class M3EProject(
    val id: String = Uuid.random().toString(),
    val name: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val deviceProfile: DeviceProfile = DeviceProfile.default,
    val themeConfig: CanvasThemeConfig = CanvasThemeConfig(),
    val nodes: List<CanvasNode> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {

    companion object {
        /** Bump this whenever the project file format changes, to support migration. */
        const val CURRENT_SCHEMA_VERSION = 1
    }

    /** Recursively finds a node by [nodeId] across all top-level trees. */
    fun findNode(nodeId: String): CanvasNode? {
        for (node in nodes) {
            val found = node.findNode(nodeId = nodeId)
            if (found != null) return found
        }
        return null
    }

    /** Returns a flat list of all nodes across every tree in this project. */
    fun allNodes(): List<CanvasNode> {
        return nodes.flatMap { it.allNodes() }
    }

    /** Returns a copy with the given node added at the top level. */
    fun withNode(node: CanvasNode): M3EProject {
        return copy(nodes = nodes + node)
    }

    /** Recursively updates a node anywhere in the tree. */
    fun updateNode(node: CanvasNode): M3EProject {
        val updated = nodes.map { it.updateNodeDeep(node = node) }
        return copy(nodes = updated)
    }

    /** Recursively adds [child] to the container matching [containerId]. */
    fun addChildToContainer(containerId: String, child: CanvasNode): M3EProject {
        val updated = nodes.map {
            it.addChildDeep(containerId = containerId, child = child)
        }
        return copy(nodes = updated)
    }

    /** Recursively removes the node matching [nodeId] from anywhere in the tree. */
    fun removeNode(nodeId: String): M3EProject {
        val updated = nodes
            .filterNot { it.id == nodeId }
            .map { it.removeNodeDeep(nodeId = nodeId) }
        return copy(nodes = updated)
    }

    /** Returns a copy with the given theme config applied. */
    fun withTheme(themeConfig: CanvasThemeConfig): M3EProject {
        return copy(themeConfig = themeConfig)
    }
}