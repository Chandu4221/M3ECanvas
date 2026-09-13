package dev.chandradsl.m3ecanvas.editor.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.DeviceCategory
import dev.chandradsl.m3ecanvas.domain.model.DeviceOrientation
import dev.chandradsl.m3ecanvas.domain.model.DeviceProfile
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry
import dev.chandradsl.m3ecanvas.editor.service.AlignmentType
import dev.chandradsl.m3ecanvas.editor.service.DistributionType
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.state.EditorMode

/**
 * Filter categories for the Command Palette.
 */
enum class CommandCategory(val label: String) {
    ALL("All"),
    COMPONENTS("Components"),
    ACTIONS("Actions"),
    DEVICES("Devices"),
    LAYERS("Layers")
}

/**
 * A single executable command or actionable item in the Command Palette.
 */
sealed interface CommandItem {
    val id: String
    val title: String
    val subtitle: String
    val category: CommandCategory
    val shortcut: String?
    val icon: ImageVector
    fun execute(controller: EditorController)

    data class ComponentCommand(
        val type: ComponentType,
        val description: String = ComponentRegistry.get(type)?.description.orEmpty(),
        override val title: String = "Insert ${type.displayName}",
        override val subtitle: String = description.ifEmpty { "${type.category.displayName} component" },
        override val category: CommandCategory = CommandCategory.COMPONENTS,
        override val shortcut: String? = null,
        override val icon: ImageVector = type.icon(),
        override val id: String = "comp_${type.name}"
    ) : CommandItem {
        override fun execute(controller: EditorController) {
            controller.insertComponent(type)
        }
    }

    data class ActionCommand(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val shortcut: String? = null,
        override val icon: ImageVector,
        override val category: CommandCategory = CommandCategory.ACTIONS,
        val action: (EditorController) -> Unit
    ) : CommandItem {
        override fun execute(controller: EditorController) {
            action(controller)
        }
    }

    data class DeviceCommand(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val shortcut: String? = null,
        override val icon: ImageVector,
        override val category: CommandCategory = CommandCategory.DEVICES,
        val action: (EditorController) -> Unit
    ) : CommandItem {
        override fun execute(controller: EditorController) {
            action(controller)
        }
    }

    data class LayerCommand(
        val node: CanvasNode,
        override val id: String = "layer_${node.id}",
        override val title: String = "Select: ${node.name}",
        override val subtitle: String = "${node.type.displayName} (x=${node.position.x.toInt()}, y=${node.position.y.toInt()})",
        override val shortcut: String? = null,
        override val icon: ImageVector = node.type.icon(),
        override val category: CommandCategory = CommandCategory.LAYERS
    ) : CommandItem {
        override fun execute(controller: EditorController) {
            if (controller.state.isInteractiveMode) {
                controller.setMode(EditorMode.DESIGN)
            }
            controller.selectNode(node.id)
        }
    }
}

/**
 * Builds the complete list of available commands for the current editor state.
 */
fun buildAllCommands(controller: EditorController): List<CommandItem> {
    val state = controller.state
    val list = mutableListOf<CommandItem>()

    // 1. Actions
    list.add(
        CommandItem.ActionCommand(
            id = "toggle_mode",
            title = if (state.isInteractiveMode) "Switch to Design Mode" else "Switch to Interactive Preview Mode",
            subtitle = if (state.isInteractiveMode) "Enable drag, resize, and inspector editing" else "Test live buttons, inputs, and dialogs",
            shortcut = "Ctrl+P",
            icon = if (state.isInteractiveMode) Icons.Outlined.Edit else Icons.Outlined.PlayArrow,
            action = { it.toggleInteractiveMode() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "undo",
            title = "Undo",
            subtitle = "Revert last change",
            shortcut = "Ctrl+Z",
            icon = Icons.AutoMirrored.Outlined.Undo,
            action = { it.undo() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "redo",
            title = "Redo",
            subtitle = "Reapply reverted change",
            shortcut = "Ctrl+Y",
            icon = Icons.AutoMirrored.Outlined.Redo,
            action = { it.redo() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "zoom_in",
            title = "Zoom In",
            subtitle = "Magnify canvas viewport",
            shortcut = "Ctrl++",
            icon = Icons.Outlined.ZoomIn,
            action = { it.zoomIn() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "zoom_out",
            title = "Zoom Out",
            subtitle = "Demagnify canvas viewport",
            shortcut = "Ctrl+-",
            icon = Icons.Outlined.ZoomOut,
            action = { it.zoomOut() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "zoom_fit",
            title = "Fit to Screen",
            subtitle = "Fit canvas viewport into available space",
            shortcut = "Ctrl+0",
            icon = Icons.Outlined.FitScreen,
            action = { it.fitToScreen() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "zoom_reset",
            title = "Zoom to 100%",
            subtitle = "Reset zoom to 1:1 scale",
            shortcut = "Ctrl+1",
            icon = Icons.Outlined.RestartAlt,
            action = { it.resetZoom() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "select_all",
            title = "Select All Components",
            subtitle = "Select all components in the document",
            shortcut = "Ctrl+A",
            icon = Icons.Outlined.SelectAll,
            action = { it.selectAll() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "clear_selection",
            title = "Clear Selection",
            subtitle = "Deselect currently selected nodes",
            shortcut = "Esc",
            icon = Icons.Outlined.Deselect,
            action = { it.clearSelection() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "group_selection",
            title = "Group Selected",
            subtitle = "Wrap selected nodes into a container",
            shortcut = "Ctrl+G",
            icon = Icons.Outlined.Folder,
            action = { it.groupSelected() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "ungroup_selection",
            title = "Ungroup Container",
            subtitle = "Dissolve selected container into parent/root",
            shortcut = "Ctrl+Shift+G",
            icon = Icons.Outlined.FolderOpen,
            action = { it.ungroupSelected() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "duplicate_selected",
            title = "Duplicate Selected",
            subtitle = "Clone selected component with offset",
            shortcut = "Ctrl+D",
            icon = Icons.Outlined.ContentCopy,
            action = { it.duplicateSelected() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "delete_selected",
            title = "Delete Selected",
            subtitle = "Remove selected nodes from document",
            shortcut = "Del",
            icon = Icons.Outlined.Delete,
            action = { it.deleteSelected() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "toggle_rulers",
            title = if (state.showRulers) "Hide Canvas Rulers" else "Show Canvas Rulers",
            subtitle = "Toggle pixel rulers on canvas borders",
            icon = Icons.Outlined.Straighten,
            action = { it.toggleRulers() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "toggle_measurements",
            title = if (state.showMeasurements) "Hide Spacing Measurements" else "Show Spacing Measurements",
            subtitle = "Toggle distance indicator callouts between components",
            icon = Icons.Outlined.SquareFoot,
            action = { it.toggleMeasurements() }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "toggle_insets",
            title = if (state.showSystemInsets) "Hide System Insets" else "Show System Insets",
            subtitle = "Toggle status bar and navigation bar insets",
            icon = Icons.Outlined.Layers,
            action = { it.toggleSystemInsets() }
        )
    )

    // Alignment & Distribution
    list.add(
        CommandItem.ActionCommand(
            id = "align_left",
            title = "Align Left",
            subtitle = "Align selected nodes to leftmost edge",
            icon = Icons.Outlined.FormatAlignLeft,
            action = { it.alignSelectedNodes(AlignmentType.LEFT) }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "align_center",
            title = "Align Center",
            subtitle = "Align selected nodes to horizontal center",
            icon = Icons.Outlined.FormatAlignCenter,
            action = { it.alignSelectedNodes(AlignmentType.CENTER_HORIZONTALLY) }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "align_right",
            title = "Align Right",
            subtitle = "Align selected nodes to rightmost edge",
            icon = Icons.Outlined.FormatAlignRight,
            action = { it.alignSelectedNodes(AlignmentType.RIGHT) }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "align_top",
            title = "Align Top",
            subtitle = "Align selected nodes to top edge",
            icon = Icons.Outlined.VerticalAlignTop,
            action = { it.alignSelectedNodes(AlignmentType.TOP) }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "align_middle",
            title = "Align Middle",
            subtitle = "Align selected nodes to vertical middle",
            icon = Icons.Outlined.VerticalAlignCenter,
            action = { it.alignSelectedNodes(AlignmentType.CENTER_VERTICALLY) }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "align_bottom",
            title = "Align Bottom",
            subtitle = "Align selected nodes to bottom edge",
            icon = Icons.Outlined.VerticalAlignBottom,
            action = { it.alignSelectedNodes(AlignmentType.BOTTOM) }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "distribute_horizontally",
            title = "Distribute Horizontally",
            subtitle = "Space selected nodes evenly horizontally",
            icon = Icons.Outlined.HorizontalDistribute,
            action = { it.distributeSelectedNodes(DistributionType.HORIZONTALLY) }
        )
    )
    list.add(
        CommandItem.ActionCommand(
            id = "distribute_vertically",
            title = "Distribute Vertically",
            subtitle = "Space selected nodes evenly vertically",
            icon = Icons.Outlined.VerticalDistribute,
            action = { it.distributeSelectedNodes(DistributionType.VERTICALLY) }
        )
    )

    // 2. Devices
    DeviceProfile.presets.forEach { profile ->
        list.add(
            CommandItem.DeviceCommand(
                id = "device_${profile.id}",
                title = "Switch Device: ${profile.displayName}",
                subtitle = "${profile.category.displayName} (${profile.effectiveWidth.toInt()} × ${profile.effectiveHeight.toInt()} dp)",
                icon = when (profile.category) {
                    DeviceCategory.TABLET -> Icons.Outlined.Tablet
                    DeviceCategory.DESKTOP -> Icons.Outlined.Computer
                    DeviceCategory.FOLDABLE -> Icons.Outlined.Devices
                    else -> Icons.Outlined.Smartphone
                },
                action = { it.setDeviceProfile(profile) }
            )
        )
    }
    list.add(
        CommandItem.DeviceCommand(
            id = "toggle_orientation",
            title = if (state.project.deviceProfile.orientation == DeviceOrientation.PORTRAIT) "Switch Orientation to Landscape" else "Switch Orientation to Portrait",
            subtitle = "Rotate device viewport 90 degrees",
            icon = Icons.Outlined.ScreenRotation,
            action = { it.toggleDeviceOrientation() }
        )
    )

    // 3. Components (all types)
    ComponentType.entries.forEach { type ->
        list.add(CommandItem.ComponentCommand(type = type))
    }

    // 4. Layers (all nodes in document)
    state.project.allNodes().forEach { node ->
        list.add(CommandItem.LayerCommand(node = node))
    }

    return list
}

/**
 * Command Palette modal dialog triggered by Ctrl/Cmd + K.
 */
@Composable
fun CommandPalette(
    controller: EditorController,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CommandCategory.ALL) }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val allCommands = remember(controller.state.project, controller.state.mode, controller.state.selectedNodeIds) {
        buildAllCommands(controller)
    }

    val filteredCommands = remember(searchQuery, selectedCategory, allCommands) {
        val query = searchQuery.trim()
        allCommands.filter { item ->
            val matchesCategory = selectedCategory == CommandCategory.ALL || item.category == selectedCategory
            val matchesQuery = query.isEmpty() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.subtitle.contains(query, ignoreCase = true) ||
                    (item is CommandItem.ComponentCommand && item.type.name.contains(query, ignoreCase = true))
            matchesCategory && matchesQuery
        }
    }

    LaunchedEffect(filteredCommands.size) {
        if (selectedIndex >= filteredCommands.size) {
            selectedIndex = (filteredCommands.size - 1).coerceAtLeast(0)
        }
    }

    LaunchedEffect(selectedIndex) {
        if (filteredCommands.isNotEmpty() && selectedIndex in filteredCommands.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .width(620.dp)
                    .wrapContentHeight()
                    .clickable(enabled = false) {}
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> {
                                    if (filteredCommands.isNotEmpty()) {
                                        selectedIndex = (selectedIndex + 1) % filteredCommands.size
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (filteredCommands.isNotEmpty()) {
                                        selectedIndex = (selectedIndex - 1 + filteredCommands.size) % filteredCommands.size
                                    }
                                    true
                                }
                                Key.Enter -> {
                                    if (filteredCommands.isNotEmpty() && selectedIndex in filteredCommands.indices) {
                                        val item = filteredCommands[selectedIndex]
                                        item.execute(controller)
                                        onDismissRequest()
                                    }
                                    true
                                }
                                Key.Escape -> {
                                    onDismissRequest()
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    // Search Bar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                selectedIndex = 0
                            },
                            placeholder = {
                                Text(
                                    "Type a command, component, device, or layer...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "ESC",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    // Category Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CommandCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    selectedIndex = 0
                                },
                                label = { Text(category.label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Command List
                    if (filteredCommands.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No matching commands or components",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .padding(vertical = 6.dp)
                        ) {
                            itemsIndexed(
                                items = filteredCommands,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                val isSelected = index == selectedIndex
                                CommandRowItem(
                                    item = item,
                                    isSelected = isSelected,
                                    onClick = {
                                        item.execute(controller)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Footer keyboard hints
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text("↑↓", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                                Text("Navigate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text("↵ Enter", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                                Text("Select", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = "${filteredCommands.size} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandRowItem(
    item: CommandItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (item.subtitle.isNotEmpty()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
        if (item.shortcut != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = item.shortcut!!,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Badge(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = item.category.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                )
            }
        }
    }
}
