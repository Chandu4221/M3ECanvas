package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*

/**
 * Renders a single [CanvasNode] as its corresponding composable.
 *
 * Container types render recursively, laying out their children.
 * Leaf types render as real Material 3 components, or a placeholder
 * if a renderer is not implemented yet.
 */
@Composable
fun CanvasNodeRenderer(node: CanvasNode) {
    when (node.type) {
        // Layout containers — render recursively.
        ComponentType.COLUMN -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
        ) {
            node.children.forEach { child -> ContainerChild(child = child) }
        }

        ComponentType.ROW -> Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
        ) {
            node.children.forEach { child -> ContainerChild(child = child) }
        }

        ComponentType.BOX -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp)
        ) {
            node.children.forEach { child -> ContainerChild(child = child) }
        }

        ComponentType.LAZY_COLUMN -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child)
            }
        }

        ComponentType.LAZY_ROW -> LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child)
            }
        }

        // Leaf components — real Material 3 rendering.
        ComponentType.BUTTON -> Button(
            onClick = {},
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = node.textOrDefault(default = "Button"))
        }

        ComponentType.CARD -> Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        ComponentType.TEXT_FIELD -> TextField(
            value = node.textOrDefault(default = ""),
            onValueChange = {},
            modifier = Modifier.fillMaxSize()
        )

        // Everything else — labelled placeholder.
        else -> NodePlaceholder(node = node)
    }
}

/**
 * Renders a child inside a container, wrapped in a box that respects the
 * child's explicit size.
 */
@Composable
private fun ContainerChild(child: CanvasNode) {
    Box(
        modifier = Modifier.size(
            width = child.size.width.dp,
            height = child.size.height.dp
        )
    ) {
        CanvasNodeRenderer(node = child)
    }
}

/**
 * Placeholder for components whose renderer is not implemented yet.
 */
@Composable
private fun NodePlaceholder(node: CanvasNode) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDDDDDD)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = node.type.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF555555)
        )
    }
}

/**
 * Reads the node's "text" property, falling back to [default] if absent.
 */
private fun CanvasNode.textOrDefault(default: String): String {
    val property = property(key = "text")
    return if (property is ComponentProperty.Text) property.value else default
}

/**
 * Resolves the arrangement for a vertical container (Column / LazyColumn).
 * Maps START to Top and END to Bottom, since "start" means the top edge
 * in a vertical layout.
 */
private fun resolveVerticalArrangement(config: LayoutConfig): Arrangement.Vertical {
    if (config.spacing > 0f) {
        return Arrangement.spacedBy(space = config.spacing.dp)
    }
    return when (config.arrangement) {
        LayoutArrangement.START -> Arrangement.Top
        LayoutArrangement.CENTER -> Arrangement.Center
        LayoutArrangement.END -> Arrangement.Bottom
        LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
        LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
        LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }
}

/**
 * Resolves the arrangement for a horizontal container (Row / LazyRow).
 * Maps START to Start and END to End, since "start" means the leading edge
 * in a horizontal layout.
 */
private fun resolveHorizontalArrangement(config: LayoutConfig): Arrangement.Horizontal {
    if (config.spacing > 0f) {
        return Arrangement.spacedBy(space = config.spacing.dp)
    }
    return when (config.arrangement) {
        LayoutArrangement.START -> Arrangement.Start
        LayoutArrangement.CENTER -> Arrangement.Center
        LayoutArrangement.END -> Arrangement.End
        LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
        LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
        LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }
}