package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.state.EditorController

/**
 * Renders a single [CanvasNode] as its corresponding composable.
 * Supports variant-aware rendering and applies the node's stored modifier
 * chain in user order before the system-owned modifiers.
 */
@Composable
fun CanvasNodeRenderer(
    node: CanvasNode,
    controller: EditorController
) {
    when (node.type) {
        ComponentType.COLUMN -> {
            val scrollState = rememberScrollState()
            Column(
                modifier = node.modifiers.toModifier()
                    .fillMaxSize()
                    .then(
                        if (node.layoutConfig.scrollable) {
                            Modifier.verticalScroll(state = scrollState)
                        } else {
                            Modifier
                        }
                    )
                    .padding(all = node.layoutConfig.padding.dp),
                verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
            ) {
                node.children.forEach { child ->
                    ContainerChild(child = child, controller = controller)
                }
            }
        }

        ComponentType.ROW -> {
            val scrollState = rememberScrollState()
            Row(
                modifier = node.modifiers.toModifier()
                    .fillMaxSize()
                    .then(
                        if (node.layoutConfig.scrollable) {
                            Modifier.horizontalScroll(state = scrollState)
                        } else {
                            Modifier
                        }
                    )
                    .padding(all = node.layoutConfig.padding.dp),
                horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
            ) {
                node.children.forEach { child ->
                    ContainerChild(child = child, controller = controller)
                }
            }
        }

        ComponentType.BOX -> Box(
            modifier = node.modifiers.toModifier()
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp)
        ) {
            node.children.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.LAZY_COLUMN -> LazyColumn(
            modifier = node.modifiers.toModifier()
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.LAZY_ROW -> LazyRow(
            modifier = node.modifiers.toModifier()
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.BUTTON -> RenderButton(node = node)
        ComponentType.CARD -> RenderCard(node = node)
        ComponentType.ICON_BUTTON -> RenderIconButton(node = node)
        ComponentType.CHIPS -> RenderChip(node = node)

        ComponentType.TEXT_FIELD -> TextField(
            value = node.textOrDefault(default = ""),
            onValueChange = {},
            modifier = node.modifiers.toModifier().fillMaxSize()
        )

        else -> NodePlaceholder(node = node)
    }
}

@Composable
private fun RenderButton(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Button
        ?: MaterialVariant.Button.FILLED
    val text = node.textOrDefault(default = "Button")
    val onClick = {}
    val modifier = node.modifiers.toModifier().fillMaxSize()

    when (variant) {
        MaterialVariant.Button.FILLED -> Button(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.TONAL -> FilledTonalButton(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.OUTLINED -> OutlinedButton(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.ELEVATED -> ElevatedButton(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.TEXT -> TextButton(onClick = onClick, modifier = modifier) { Text(text = text) }
    }
}

@Composable
private fun RenderCard(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Card
        ?: MaterialVariant.Card.FILLED
    val modifier = node.modifiers.toModifier().fillMaxSize()

    when (variant) {
        MaterialVariant.Card.FILLED -> Card(modifier = modifier) { Box(modifier = Modifier.fillMaxSize()) }
        MaterialVariant.Card.ELEVATED -> ElevatedCard(modifier = modifier) { Box(modifier = Modifier.fillMaxSize()) }
        MaterialVariant.Card.OUTLINED -> OutlinedCard(modifier = modifier) { Box(modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun RenderIconButton(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.IconButton
        ?: MaterialVariant.IconButton.STANDARD
    val onClick = {}
    val modifier = node.modifiers.toModifier().fillMaxSize()
    val icon = @Composable { Icon(imageVector = Icons.Filled.Add, contentDescription = null) }

    when (variant) {
        MaterialVariant.IconButton.STANDARD -> IconButton(onClick = onClick, modifier = modifier, content = icon)
        MaterialVariant.IconButton.FILLED -> FilledIconButton(onClick = onClick, modifier = modifier, content = icon)
        MaterialVariant.IconButton.TONAL -> FilledTonalIconButton(onClick = onClick, modifier = modifier, content = icon)
        MaterialVariant.IconButton.OUTLINED -> OutlinedIconButton(onClick = onClick, modifier = modifier, content = icon)
    }
}

@Composable
private fun RenderChip(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Chip
        ?: MaterialVariant.Chip.ASSIST
    val label = @Composable { Text(text = node.textOrDefault(default = "Chip")) }
    val onClick = {}

    when (variant) {
        MaterialVariant.Chip.ASSIST -> AssistChip(onClick = onClick, label = label)
        MaterialVariant.Chip.FILTER -> FilterChip(selected = false, onClick = onClick, label = label)
        MaterialVariant.Chip.INPUT -> InputChip(selected = false, onClick = onClick, label = label)
        MaterialVariant.Chip.SUGGESTION -> SuggestionChip(onClick = onClick, label = label)
    }
}

@Composable
private fun ContainerChild(
    child: CanvasNode,
    controller: EditorController
) {
    val isSelected = controller.state.isNodeSelected(nodeId = child.id)
    Box(
        modifier = Modifier
            .size(width = child.size.width.dp, height = child.size.height.dp)
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

@Composable
internal fun Modifier.selectOnPress(
    nodeId: String,
    controller: EditorController
): Modifier {
    return this.pointerInput(nodeId) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            controller.selectNode(nodeId = nodeId)
        }
    }
}

@Composable
private fun NodePlaceholder(node: CanvasNode) {
    Box(
        modifier = node.modifiers.toModifier()
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
 * Folds the stored modifier chain into a real Compose [Modifier],
 * preserving user order exactly.
 */
private fun List<ModifierSpec>.toModifier(): Modifier {
    return fold(Modifier) { acc: Modifier, spec: ModifierSpec ->
        acc.then(
            when (spec) {
                is ModifierSpec.Padding -> Modifier.padding(all = spec.all.dp)
                is ModifierSpec.Background -> Modifier.background(color = parseHex(hex = spec.colorHex))
                is ModifierSpec.Border -> Modifier.border(
                    width = spec.width.dp,
                    color = parseHex(hex = spec.colorHex),
                    shape = RectangleShape
                )
                is ModifierSpec.Clip -> Modifier.clip(
                    shape = RoundedCornerShape(corner = CornerSize(spec.cornerRadius.dp))
                )
                is ModifierSpec.Alpha -> Modifier.alpha(alpha = spec.value)
                is ModifierSpec.Rotation -> Modifier.rotate(degrees = spec.degrees)
                is ModifierSpec.Scale -> Modifier.scale(scale = spec.value)
                is ModifierSpec.FillMaxWidth -> Modifier.fillMaxWidth()
                is ModifierSpec.FillMaxHeight -> Modifier.fillMaxHeight()
            }
        )
    }
}

/** Parses a "#AARRGGBB" or "RRGGBB"-style hex string into a [Color]. */
private fun parseHex(hex: String): Color {
    return try {
        Color(color = hex.removePrefix(prefix = "#").toLong(radix = 16))
    } catch (_: NumberFormatException) {
        Color.Transparent
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