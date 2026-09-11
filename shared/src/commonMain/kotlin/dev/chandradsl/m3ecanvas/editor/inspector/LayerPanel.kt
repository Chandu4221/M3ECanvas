package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.editor.state.EditorController

/**
 * The layers panel. Shows the node tree with indentation, allows selecting
 * any node by click, and exposes reorder/delete actions on the selected row.
 */
@Composable
fun LayersPanel(
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    val nodes = controller.state.project.nodes

    LazyColumn(modifier = modifier.fillMaxHeight()) {
        item(key = "layers_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layers",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                if (controller.state.selectedNodeIds.isNotEmpty()) {
                    TextButton(
                        onClick = { controller.clearSelection() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Project",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
            HorizontalDivider()
        }
        item(key = "project_root_row") {
            val isProjectSelected = controller.state.selectedNodeIds.isEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isProjectSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else Color.Transparent
                    )
                    .clickable { controller.clearSelection() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isProjectSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Project (${controller.state.project.deviceProfile.displayName})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isProjectSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isProjectSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
 * Flattens the node tree into (node, depth) pairs for lazy rendering
 * while preserving hierarchy order and indentation.
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
 * A single row in the layers tree. Indented by [depth], highlighted when
 * selected, and showing reorder/delete icons while selected.
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
            .padding(start = (16 + depth * 16).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(weight = 1f)
                .padding(start = 0.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        )
        if (isSelected) {
            LayerActionIcon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = "Move up",
                onClick = { controller.moveNodeUp(nodeId = node.id) }
            )
            LayerActionIcon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Move down",
                onClick = { controller.moveNodeDown(nodeId = node.id) }
            )
            LayerActionIcon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete",
                onClick = { controller.removeNode(nodeId = node.id) }
            )
        }
    }
}

/**
 * A compact icon action button for layer rows.
 */
@Composable
private fun LayerActionIcon(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable { onClick() }
            .padding(all = 4.dp)
            .size(size = 16.dp)
    )
}