package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.SlotRole
import dev.chandradsl.m3ecanvas.editor.palette.icon
import dev.chandradsl.m3ecanvas.editor.state.EditorController

/**
 * The layers panel. Shows the node tree with indentation, allows selecting
 * any node by click, and exposes reorder/delete actions on the selected row.
 * Supports collapsing into a compact header row.
 */
@Composable
fun LayersPanel(
    controller: EditorController,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null
) {
    val nodes = controller.state.project.nodes
    val allNodesCount = controller.state.project.allNodes().size

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onToggleExpand != null) { onToggleExpand?.invoke() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Layers",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "$allNodesCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            if (onToggleExpand != null) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Layers" else "Expand Layers",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider()

        if (isExpanded) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
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
                    items = flattenNodes(nodes = nodes, collapsedIds = controller.state.collapsedNodeIds),
                    key = { it.first.id }
                ) { (node, depth) ->
                    LayerRow(node = node, depth = depth, controller = controller)
                }
            }
        }
    }
}

/**
 * Flattens the node tree into (node, depth) pairs for lazy rendering
 * while preserving hierarchy order, indentation, and respecting collapsed containers.
 */
private fun flattenNodes(
    nodes: List<CanvasNode>,
    collapsedIds: Set<String>,
    depth: Int = 0
): List<Pair<CanvasNode, Int>> {
    val result = mutableListOf<Pair<CanvasNode, Int>>()
    for (node in nodes) {
        result.add(element = node to depth)
        if (node.children.isNotEmpty() && node.id !in collapsedIds) {
            result.addAll(elements = flattenNodes(nodes = node.children, collapsedIds = collapsedIds, depth = depth + 1))
        }
    }
    return result
}

/**
 * A single row in the layers tree. Indented by [depth], highlighted when
 * selected, and exposing visibility toggles, locking, inline renaming,
 * and reorder/delete actions.
 */
@Composable
private fun LayerRow(
    node: CanvasNode,
    depth: Int,
    controller: EditorController
) {
    val isSelected = controller.state.isNodeSelected(nodeId = node.id)
    val hasChildren = node.children.isNotEmpty()
    val isCollapsed = node.id in controller.state.collapsedNodeIds
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember(node.name) { mutableStateOf(node.name) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            try { focusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(node.id) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    val isMulti = currentEvent.keyboardModifiers.isShiftPressed ||
                            currentEvent.keyboardModifiers.isCtrlPressed ||
                            currentEvent.keyboardModifiers.isMetaPressed
                    if (isMulti) {
                        controller.toggleSelectNode(nodeId = node.id)
                    } else {
                        controller.selectNode(nodeId = node.id)
                    }
                }
            }
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }
            )
            .padding(start = (8 + depth * 16).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Expand / Collapse chevron for container nodes with children
        if (hasChildren) {
            Icon(
                imageVector = if (isCollapsed) Icons.AutoMirrored.Outlined.KeyboardArrowRight else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { controller.toggleCollapseNode(node.id) }
                    .padding(2.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 2. Component type icon
        Icon(
            imageVector = node.type.icon(),
            contentDescription = node.type.displayName,
            tint = if (node.isVisible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // 3. Node name / Inline editor & slot role badge
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isEditing) {
                BasicTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && isEditing) {
                                if (tempName.isNotBlank()) {
                                    controller.renameNode(node.id, tempName)
                                }
                                isEditing = false
                            }
                        }
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                if (event.key == Key.Enter) {
                                    if (tempName.isNotBlank()) {
                                        controller.renameNode(node.id, tempName)
                                    }
                                    isEditing = false
                                    true
                                } else if (event.key == Key.Escape) {
                                    tempName = node.name
                                    isEditing = false
                                    true
                                } else false
                            } else false
                        }
                )
            } else {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (node.isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }

            if (node.slot != null && node.slot != SlotRole.CONTENT) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = node.slot.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // 4. Quick Actions: Visibility and Lock toggles
        LayerActionIcon(
            imageVector = if (node.isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            contentDescription = if (node.isVisible) "Hide layer" else "Show layer",
            tint = if (node.isVisible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            onClick = { controller.toggleNodeVisibility(node.id) }
        )

        LayerActionIcon(
            imageVector = if (node.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            contentDescription = if (node.isLocked) "Unlock layer" else "Lock layer",
            tint = if (node.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            onClick = { controller.toggleNodeLock(node.id) }
        )

        // 5. Selected row actions: Rename, Reorder, Delete
        if (isSelected) {
            LayerActionIcon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Rename",
                onClick = { isEditing = true }
            )
            if (!node.isLocked) {
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
}

/**
 * A compact icon action button for layer rows.
 */
@Composable
private fun LayerActionIcon(
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier
            .clickable { onClick() }
            .padding(all = 4.dp)
            .size(size = 16.dp)
    )
}