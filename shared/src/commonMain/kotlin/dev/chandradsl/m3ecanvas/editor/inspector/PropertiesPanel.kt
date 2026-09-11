package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.canvas.AVAILABLE_MATERIAL_ICONS
import dev.chandradsl.m3ecanvas.editor.canvas.resolveMaterialIcon
import dev.chandradsl.m3ecanvas.editor.state.EditorController

/**
 * The inspector panel. Shows project-level settings when nothing is
 * selected, and editable properties for the selected node otherwise.
 */
@Composable
fun PropertiesPanel(
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    val selected = controller.state.selectedNodes.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState())
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Properties",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            if (selected != null) {
                TextButton(
                    onClick = { controller.clearSelection() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Project",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
        HorizontalDivider()
        if (selected == null) {
            ProjectSection(controller = controller)
        } else {
            NodeSection(node = selected, controller = controller)
        }
    }
}

@Composable
private fun NodeSection(node: CanvasNode, controller: EditorController) {
    SectionLabel(text = node.type.displayName)

    if (node.slot != null) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Snapped to Scaffold: ${node.slot.displayName}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectSection(controller: EditorController) {
    var expanded by remember { mutableStateOf(value = false) }
    val current = controller.state.project.deviceProfile

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

    Text(
        text = "Select a component on the canvas to edit its properties.",
        style = MaterialTheme.typography.bodySmall
    )
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
    return when (type) {
        ComponentType.BUTTON -> MaterialVariant.Button.entries
        ComponentType.ICON_BUTTON -> MaterialVariant.IconButton.entries
        ComponentType.FAB -> MaterialVariant.FloatingActionButton.entries
        ComponentType.CARD -> MaterialVariant.Card.entries
        ComponentType.CHIPS -> MaterialVariant.Chip.entries
        ComponentType.TEXT_FIELD -> MaterialVariant.TextField.entries
        ComponentType.SURFACE -> MaterialVariant.Surface.entries
        else -> emptyList()
    }
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