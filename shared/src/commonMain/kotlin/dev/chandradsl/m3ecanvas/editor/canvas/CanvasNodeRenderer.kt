package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.ComponentProperty
import dev.chandradsl.m3ecanvas.domain.model.ComponentType

/**
 * Renders a single [CanvasNode] as its corresponding Material 3 component.
 *
 * Components without a renderer yet show a labelled placeholder, so every
 * [ComponentType] can be added to the palette immediately and have its real
 * renderer filled in incrementally.
 */
@Composable
fun CanvasNodeRenderer(node: CanvasNode) {
    when (node.type) {
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