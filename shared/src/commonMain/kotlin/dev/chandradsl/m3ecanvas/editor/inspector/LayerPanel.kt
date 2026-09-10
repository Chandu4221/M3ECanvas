package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.editor.state.EditorController

/**
 * The layers panel. Shows the node tree with indentation and lets the user
 * select any node — including nested children — by clicking its row.
 */
@Composable
fun LayersPanel(
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    val nodes = controller.state.project.nodes

    LazyColumn(modifier = modifier.fillMaxHeight()) {
        item(key = "layers_header") {
            Text(
                text = "Layers",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            HorizontalDivider()
        }
        items(
            items = flattenNodes(nodes = nodes),
            key = { it.first.id }
        ) { (node, depth) ->
            LayerRow(node = node, depth = depth, controller = controller)
        }
    }
}

/**
 * Flattens the node tree into a list of (node, depth) pairs so it can be
 * rendered in a LazyColumn while preserving hierarchy order and indentation.
 */
private fun flattenNodes(
    nodes: List<CanvasNode>,
    depth: Int = 0
): List<Pair<CanvasNode, Int>> {
    val result = mutableListOf<Pair<CanvasNode, Int>>()
    for (node in nodes) {
        result.add(element = node to depth)
        result.addAll(elements = flattenNodes(nodes = node.children, depth = depth + 1))
    }
    return result
}

/**
 * A single row in the layers tree. Indented by [depth] and highlighted when selected.
 */
@Composable
private fun LayerRow(
    node: CanvasNode,
    depth: Int,
    controller: EditorController
) {
    val isSelected = controller.state.isNodeSelected(nodeId = node.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { controller.selectNode(nodeId = node.id) }
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }
            )
            .padding(
                start = (16 + depth * 16).dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}