package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.state.EditorController

/**
 * Renders a single [CanvasNode] as its corresponding composable.
 * Container types render recursively; leaf types render real Material 3
 * components or a placeholder.
 */
@Composable
fun CanvasNodeRenderer(
    node: CanvasNode,
    controller: EditorController
) {
    when (node.type) {
        ComponentType.COLUMN -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
        ) {
            node.children.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.ROW -> Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
        ) {
            node.children.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.BOX -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp)
        ) {
            node.children.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.LAZY_COLUMN -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.LAZY_ROW -> LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

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

        else -> NodePlaceholder(node = node)
    }
}

/**
 * Renders a child inside a container with an explicit size, a selection
 * border when selected, and press-to-select.
 */
@Composable
private fun ContainerChild(
    child: CanvasNode,
    controller: EditorController
) {
    val isSelected = controller.state.isNodeSelected(nodeId = child.id)

    Box(
        modifier = Modifier
            .size(
                width = child.size.width.dp,
                height = child.size.height.dp
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .selectOnPress(nodeId = child.id, controller = controller)
    ) {
        CanvasNodeRenderer(node = child, controller = controller)
    }
}

/**
 * Selects the node as soon as a pointer press lands on it.
 *
 * Uses the Initial pointer pass and does NOT consume the event, so the
 * composables underneath still receive their own events normally.
 * [awaitEachGesture] is the supported replacement for the deprecated
 * forEachGesture and already provides the AwaitPointerEventScope context.
 */
@Composable
internal fun Modifier.selectOnPress(
    nodeId: String,
    controller: EditorController
): Modifier {
    return this.pointerInput(nodeId) {
        awaitEachGesture {
            awaitFirstDown(
                pass = PointerEventPass.Initial,
                requireUnconsumed = false
            )
            controller.selectNode(nodeId = nodeId)
        }
    }
}

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

private fun CanvasNode.textOrDefault(default: String): String {
    val property = property(key = "text")
    return if (property is ComponentProperty.Text) property.value else default
}

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