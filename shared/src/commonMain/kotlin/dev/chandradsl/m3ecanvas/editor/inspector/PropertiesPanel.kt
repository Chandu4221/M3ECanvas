@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.canvas.AVAILABLE_MATERIAL_ICONS
import dev.chandradsl.m3ecanvas.editor.canvas.resolveMaterialIcon
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.theme.CanvasThemeGenerator

/**
 * The inspector panel. Shows project-level settings when nothing is
 * selected, editable properties for a single selected node, or group actions
 * when multiple nodes are selected.
 * Supports collapsing into a compact header row.
 */
@Composable
fun PropertiesPanel(
    controller: EditorController,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null
) {
    val selectedNodes = controller.state.selectedNodes

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
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
                    text = "Properties",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (selectedNodes.isNotEmpty()) {
                    TextButton(
                        onClick = { controller.clearSelection() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = "Project",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
            if (onToggleExpand != null) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Properties" else "Expand Properties",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider()

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState())
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                when {
                    selectedNodes.isEmpty() -> ProjectSection(controller = controller)
                    selectedNodes.size == 1 -> NodeSection(node = selectedNodes.first(), controller = controller)
                    else -> MultiSelectSection(selectedNodes = selectedNodes, controller = controller)
                }
            }
        }
    }
}

@Composable
private fun NodeSection(node: CanvasNode, controller: EditorController) {
    SectionLabel(text = node.type.displayName)

    val parent = controller.findParent(node.id)
    val availableSlots = if (parent != null) node.type.allowedSlotsIn(parent.type) else emptyList()

    if (node.slot != null || availableSlots.size > 1) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (node.slot != null) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (parent?.type == ComponentType.SCAFFOLD) {
                            "Snapped to Scaffold: ${node.slot.displayName}"
                        } else {
                            "Slot: ${node.slot.displayName}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            if (availableSlots.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Slot: ${node.slot?.displayName ?: "None"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = "Select Slot"
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableSlots.forEach { slotRole ->
                            DropdownMenuItem(
                                text = { Text(slotRole.displayName) },
                                onClick = {
                                    controller.setNodeSlot(node.id, slotRole)
                                    expanded = false
                                },
                                leadingIcon = if (node.slot == slotRole) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = node.name,
        onValueChange = { controller.renameNode(nodeId = node.id, name = it) },
        label = { Text(text = "Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    if (node.type == ComponentType.TOP_APP_BAR) {
        val currentTitle = (node.property(key = "title") as? ComponentProperty.Text)?.value
            ?: (node.property(key = "text") as? ComponentProperty.Text)?.value
            ?: ""
        OutlinedTextField(
            value = currentTitle,
            onValueChange = {
                controller.updateNodeProperty(
                    nodeId = node.id,
                    property = ComponentProperty.Text(key = "title", value = it)
                )
            },
            label = { Text(text = "AppBar Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    if (supportsText(node = node)) {
        OutlinedTextField(
            value = (node.property(key = "text") as? ComponentProperty.Text)?.value.orEmpty(),
            onValueChange = {
                controller.updateNodeProperty(
                    nodeId = node.id,
                    property = ComponentProperty.Text(key = "text", value = it)
                )
            },
            label = { Text(text = "Text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    if (node.type == ComponentType.TEXT) {
        TypographyDropdown(node = node, controller = controller)
    }

    if (node.type == ComponentType.ICON || node.type == ComponentType.ICON_BUTTON || node.type == ComponentType.FAB || node.type == ComponentType.EXTENDED_FAB) {
        IconDropdown(node = node, controller = controller)
    }

    if (node.type == ComponentType.CHECKBOX) {
        BooleanPropertySwitch(label = "Checked", key = "checked", node = node, controller = controller, default = true)
    } else if (node.type == ComponentType.RADIO_BUTTON) {
        BooleanPropertySwitch(label = "Selected", key = "selected", node = node, controller = controller, default = true)
    } else if (node.type == ComponentType.SWITCH) {
        BooleanPropertySwitch(label = "Checked", key = "checked", node = node, controller = controller, default = true)
    }

    if (node.type == ComponentType.SLIDER) {
        FloatSliderRow(label = "Slider Value", key = "value", node = node, controller = controller, default = 0.6f)
    } else if (node.type == ComponentType.RANGE_SLIDER) {
        FloatSliderRow(label = "Range Start", key = "startValue", node = node, controller = controller, default = 0.2f)
        FloatSliderRow(label = "Range End", key = "endValue", node = node, controller = controller, default = 0.8f)
    }

    val variants = variantsFor(type = node.type)
    if (variants.isNotEmpty()) {
        VariantDropdown(node = node, variants = variants, controller = controller)
    }

    if (node.isContainer) {
        LayoutSection(node = node, controller = controller)
    }
    ModifierSection(node = node, controller = controller)
    AccessibilitySection(node = node, controller = controller)

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Button(
        onClick = { controller.removeNode(nodeId = node.id) },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Delete ${node.type.displayName}")
    }
}

@Composable
private fun ProjectSection(controller: EditorController) {
    var expanded by remember { mutableStateOf(value = false) }
    val current = controller.state.project.deviceProfile
    val themeConfig = controller.state.project.themeConfig

    SectionLabel(text = "Project")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Device") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DeviceProfile.presets.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(text = profile.displayName) },
                    onClick = {
                        controller.setDeviceProfile(profile = profile)
                        expanded = false
                    }
                )
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    SectionLabel(text = "Theme & Colors")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (themeConfig.isDark) "Dark Mode" else "Light Mode",
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = themeConfig.isDark,
            onCheckedChange = { controller.toggleThemeDarkMode() }
        )
    }

    Text(
        text = "Seed Color Presets",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CanvasThemeGenerator.PRESETS.forEach { preset ->
            val isSelected = themeConfig.seedColorHex.equals(preset.hexColor, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { controller.setThemeSeed(preset.hexColor) },
                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(preset.color, shape = CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                    )
                }
            )
        }
    }

    var hexInput by remember(themeConfig.seedColorHex) { mutableStateOf(themeConfig.seedColorHex) }
    val isValid = CanvasThemeGenerator.isValidHexColor(hexInput)

    OutlinedTextField(
        value = hexInput,
        onValueChange = { input ->
            hexInput = input
            if (CanvasThemeGenerator.isValidHexColor(input)) {
                val formatted = if (input.startsWith("#")) input else "#$input"
                controller.setThemeSeed(formatted)
            }
        },
        label = { Text(text = "Custom Seed Hex") },
        placeholder = { Text("#6750A4") },
        isError = !isValid && hexInput.isNotBlank(),
        trailingIcon = {
            val previewColor = if (isValid) CanvasThemeGenerator.parseHexColor(hexInput) else Color.Transparent
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(previewColor, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    Text(
        text = "Select a component on the canvas or layer tree to edit its properties.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MultiSelectSection(
    selectedNodes: List<CanvasNode>,
    controller: EditorController
) {
    SectionLabel(text = "Selection (${selectedNodes.size} items)")

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${selectedNodes.size} components selected",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            val summaryNames = selectedNodes.take(4).joinToString(", ") { it.name }
            val suffix = if (selectedNodes.size > 4) " + ${selectedNodes.size - 4} more" else ""
            Text(
                text = "$summaryNames$suffix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    SectionLabel(text = "Quick Actions")

    Button(
        onClick = { controller.deleteSelected() },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = "Delete All", modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Delete Selected (${selectedNodes.size})")
    }

    Text(
        text = "Group Nudge",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { controller.nudgeSelected(0f, -4f) },
            modifier = Modifier.weight(1f)
        ) {
            Text("↑ Up")
        }
        OutlinedButton(
            onClick = { controller.nudgeSelected(0f, 4f) },
            modifier = Modifier.weight(1f)
        ) {
            Text("↓ Down")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { controller.nudgeSelected(-4f, 0f) },
            modifier = Modifier.weight(1f)
        ) {
            Text("← Left")
        }
        OutlinedButton(
            onClick = { controller.nudgeSelected(4f, 0f) },
            modifier = Modifier.weight(1f)
        ) {
            Text("→ Right")
        }
    }

    OutlinedButton(
        onClick = { controller.clearSelection() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Clear Selection")
    }
}

@Composable
private fun LayoutSection(node: CanvasNode, controller: EditorController) {
    val config = node.layoutConfig

    SectionLabel(text = "Layout & Alignment")

    when (node.type) {
        ComponentType.COLUMN, ComponentType.LAZY_COLUMN -> {
            InspectorDropdown(
                label = "Vertical Arrangement",
                value = config.arrangement.displayName,
                options = LayoutArrangement.entries.map { it.displayName },
                onSelect = { selected ->
                    val arr = LayoutArrangement.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(arrangement = arr))
                }
            )
            InspectorDropdown(
                label = "Horizontal Alignment",
                value = config.horizontalAlignment.displayName,
                options = HorizontalAlignment.entries.map { it.displayName },
                onSelect = { selected ->
                    val align = HorizontalAlignment.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(horizontalAlignment = align))
                }
            )
        }
        ComponentType.ROW, ComponentType.LAZY_ROW -> {
            InspectorDropdown(
                label = "Horizontal Arrangement",
                value = config.arrangement.displayName,
                options = LayoutArrangement.entries.map { it.displayName },
                onSelect = { selected ->
                    val arr = LayoutArrangement.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(arrangement = arr))
                }
            )
            InspectorDropdown(
                label = "Vertical Alignment",
                value = config.verticalAlignment.displayName,
                options = VerticalAlignment.entries.map { it.displayName },
                onSelect = { selected ->
                    val align = VerticalAlignment.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(verticalAlignment = align))
                }
            )
        }
        ComponentType.BOX, ComponentType.SURFACE -> {
            InspectorDropdown(
                label = "Content Alignment",
                value = config.boxAlignment.displayName,
                options = BoxAlignment.entries.map { it.displayName },
                onSelect = { selected ->
                    val align = BoxAlignment.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(boxAlignment = align))
                }
            )
        }
        ComponentType.FLOW_ROW -> {
            InspectorDropdown(
                label = "Horizontal Arrangement",
                value = config.arrangement.displayName,
                options = LayoutArrangement.entries.map { it.displayName },
                onSelect = { selected ->
                    val arr = LayoutArrangement.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(arrangement = arr))
                }
            )
            InspectorDropdown(
                label = "Vertical Alignment",
                value = config.verticalAlignment.displayName,
                options = VerticalAlignment.entries.map { it.displayName },
                onSelect = { selected ->
                    val align = VerticalAlignment.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(verticalAlignment = align))
                }
            )
        }
        ComponentType.FLOW_COLUMN -> {
            InspectorDropdown(
                label = "Vertical Arrangement",
                value = config.arrangement.displayName,
                options = LayoutArrangement.entries.map { it.displayName },
                onSelect = { selected ->
                    val arr = LayoutArrangement.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(arrangement = arr))
                }
            )
            InspectorDropdown(
                label = "Horizontal Alignment",
                value = config.horizontalAlignment.displayName,
                options = HorizontalAlignment.entries.map { it.displayName },
                onSelect = { selected ->
                    val align = HorizontalAlignment.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(horizontalAlignment = align))
                }
            )
        }
        ComponentType.LAZY_VERTICAL_GRID -> {
            InspectorDropdown(
                label = "Vertical Arrangement",
                value = config.arrangement.displayName,
                options = LayoutArrangement.entries.map { it.displayName },
                onSelect = { selected ->
                    val arr = LayoutArrangement.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(arrangement = arr))
                }
            )
        }
        else -> {
            InspectorDropdown(
                label = "Arrangement",
                value = config.arrangement.displayName,
                options = LayoutArrangement.entries.map { it.displayName },
                onSelect = { selected ->
                    val arr = LayoutArrangement.entries.first { it.displayName == selected }
                    controller.updateLayoutConfig(node.id, config.copy(arrangement = arr))
                }
            )
        }
    }

    SliderRow(
        label = "Spacing",
        value = config.spacing,
        onValueChange = {
            controller.updateLayoutConfig(
                nodeId = node.id,
                config = config.copy(spacing = it)
            )
        }
    )

    SliderRow(
        label = "Padding",
        value = config.padding,
        onValueChange = {
            controller.updateLayoutConfig(
                nodeId = node.id,
                config = config.copy(padding = it)
            )
        }
    )

    if (node.type == ComponentType.COLUMN || node.type == ComponentType.ROW) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Scrollable", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = config.scrollable,
                onCheckedChange = {
                    controller.updateLayoutConfig(
                        nodeId = node.id,
                        config = config.copy(scrollable = it)
                    )
                }
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "${value.toInt()} dp", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..64f
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantDropdown(
    node: CanvasNode,
    variants: List<MaterialVariant>,
    controller: EditorController
) {
    var expanded by remember { mutableStateOf(value = false) }
    val current = (node.property(key = "variant") as? ComponentProperty.Variant)?.value
        ?: variants.first()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Variant") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            variants.forEach { variant ->
                DropdownMenuItem(
                    text = { Text(text = variant.displayName) },
                    onClick = {
                        controller.updateNodeProperty(
                            nodeId = node.id,
                            property = ComponentProperty.Variant(key = "variant", value = variant)
                        )
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InspectorDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(value = false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

private fun supportsText(node: CanvasNode): Boolean {
    return node.type == ComponentType.BUTTON ||
            node.type == ComponentType.TEXT_FIELD ||
            node.type == ComponentType.EXTENDED_FAB ||
            node.type == ComponentType.TEXT ||
            node.type == ComponentType.CHIPS ||
            node.type == ComponentType.BADGE ||
            node.type == ComponentType.TOOLTIP ||
            node.type == ComponentType.SNACKBAR ||
            node.type == ComponentType.CHECKBOX ||
            node.type == ComponentType.RADIO_BUTTON ||
            node.type == ComponentType.SWITCH ||
            node.property(key = "text") != null
}

private fun variantsFor(type: ComponentType): List<MaterialVariant> {
    return ComponentRegistry.default.variantsFor(type)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypographyDropdown(node: CanvasNode, controller: EditorController) {
    var expanded by remember { mutableStateOf(false) }
    val current = (node.property(key = "typography") as? ComponentProperty.Text)?.value ?: "bodyLarge"
    val typographyOptions = listOf(
        "displayLarge", "displayMedium", "displaySmall",
        "headlineLarge", "headlineMedium", "headlineSmall",
        "titleLarge", "titleMedium", "titleSmall",
        "bodyLarge", "bodyMedium", "bodySmall",
        "labelLarge", "labelMedium", "labelSmall"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Typography") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            typographyOptions.forEach { styleName ->
                DropdownMenuItem(
                    text = { Text(text = styleName) },
                    onClick = {
                        controller.updateNodeProperty(
                            nodeId = node.id,
                            property = ComponentProperty.Text(key = "typography", value = styleName)
                        )
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconDropdown(node: CanvasNode, controller: EditorController) {
    var expanded by remember { mutableStateOf(false) }
    val current = node.iconProperty(key = "icon", default = "Favorite")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Material Icon") },
            leadingIcon = {
                Icon(
                    imageVector = safeResolveIcon(iconName = current),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AVAILABLE_MATERIAL_ICONS.forEach { iconName ->
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = safeResolveIcon(iconName = iconName),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = { Text(text = iconName) },
                    onClick = {
                        controller.updateNodeProperty(
                            nodeId = node.id,
                            property = ComponentProperty.Icon(key = "icon", iconName = iconName)
                        )
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun safeResolveIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return try {
        resolveMaterialIcon(iconName = iconName)
    } catch (_: Throwable) {
        Icons.Outlined.Star
    }
}

@Composable
private fun BooleanPropertySwitch(
    label: String,
    key: String,
    node: CanvasNode,
    controller: EditorController,
    default: Boolean = true
) {
    val current = node.booleanProperty(key = key, default = default)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = current,
            onCheckedChange = {
                controller.updateNodeProperty(
                    nodeId = node.id,
                    property = ComponentProperty.BooleanFlag(key = key, value = it)
                )
            }
        )
    }
}

@Composable
private fun FloatSliderRow(
    label: String,
    key: String,
    node: CanvasNode,
    controller: EditorController,
    default: Float = 0.5f,
    range: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val current = node.numericProperty(key = key, default = default).coerceIn(range.start, range.endInclusive)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "${(current * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = current,
            onValueChange = {
                controller.updateNodeProperty(
                    nodeId = node.id,
                    property = ComponentProperty.Numeric(key = key, value = it)
                )
            },
            valueRange = range
        )
    }
}

@Composable
private fun AccessibilitySection(node: CanvasNode, controller: EditorController) {
    SectionLabel(text = "Accessibility & Semantics")

    // Content Description (A11y)
    val contentDesc = (node.property("contentDescription") as? ComponentProperty.Text)?.value.orEmpty()
    OutlinedTextField(
        value = contentDesc,
        onValueChange = {
            controller.updateNodeProperty(
                nodeId = node.id,
                property = ComponentProperty.Text(key = "contentDescription", value = it)
            )
        },
        label = { Text("Content Description") },
        placeholder = { Text("Spoken by screen readers") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // Semantics Role
    val currentRole = (node.property("semanticsRole") as? ComponentProperty.Text)?.value ?: "None"
    val availableRoles = listOf("None", "Button", "Checkbox", "Switch", "Image", "Tab", "Heading", "DropdownList")
    var roleExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = roleExpanded,
        onExpandedChange = { roleExpanded = it }
    ) {
        OutlinedTextField(
            value = currentRole,
            onValueChange = {},
            readOnly = true,
            label = { Text("Semantics Role") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = roleExpanded,
            onDismissRequest = { roleExpanded = false }
        ) {
            availableRoles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role) },
                    onClick = {
                        controller.updateNodeProperty(
                            nodeId = node.id,
                            property = ComponentProperty.Text(key = "semanticsRole", value = role)
                        )
                        roleExpanded = false
                    }
                )
            }
        }
    }

    // Live Contrast Ratio Analysis
    val themeConfig = controller.state.project.themeConfig
    val colorScheme = remember(themeConfig) { CanvasThemeGenerator.generateColorScheme(themeConfig) }

    val bgModifier = node.modifiers.filterIsInstance<ModifierSpec.Background>().lastOrNull()
    val bgColor = if (bgModifier != null) {
        ContrastCalculator.parseColorHex(bgModifier.colorHex, fallback = colorScheme.surface)
    } else {
        if (node.type == ComponentType.BUTTON) colorScheme.primary else colorScheme.surface
    }

    val fgColor = if (node.type == ComponentType.BUTTON) {
        colorScheme.onPrimary
    } else {
        colorScheme.onSurface
    }

    val contrastResult = remember(fgColor, bgColor) {
        ContrastCalculator.evaluate(fgColor, bgColor)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (contrastResult.level.isPassing) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WCAG Contrast: ${contrastResult.formattedRatio}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (contrastResult.level.isPassing) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = contrastResult.level.label,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (contrastResult.level.isPassing) {
                    "Meets WCAG AA standard (>= 4.5:1) for readable foreground content."
                } else {
                    "Low contrast warning. Increase color difference for better accessibility."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}