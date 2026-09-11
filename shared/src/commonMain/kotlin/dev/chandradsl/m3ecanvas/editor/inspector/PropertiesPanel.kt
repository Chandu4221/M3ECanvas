package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
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
        Text(
            text = "Properties",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
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

    SectionLabel(text = "Layout")

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

    ArrangementDropdown(node = node, config = config, controller = controller)

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
private fun ArrangementDropdown(
    node: CanvasNode,
    config: LayoutConfig,
    controller: EditorController
) {
    var expanded by remember { mutableStateOf(value = false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = config.arrangement.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Arrangement") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LayoutArrangement.entries.forEach { arrangement ->
                DropdownMenuItem(
                    text = { Text(text = arrangement.displayName) },
                    onClick = {
                        controller.updateLayoutConfig(
                            nodeId = node.id,
                            config = config.copy(arrangement = arrangement)
                        )
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
            node.property(key = "text") != null
}

private fun variantsFor(type: ComponentType): List<MaterialVariant> {
    return when (type) {
        ComponentType.BUTTON -> MaterialVariant.Button.entries
        ComponentType.ICON_BUTTON -> MaterialVariant.IconButton.entries
        ComponentType.FAB -> MaterialVariant.FloatingActionButton.entries
        ComponentType.CARD -> MaterialVariant.Card.entries
        ComponentType.CHIPS -> MaterialVariant.Chip.entries
        else -> emptyList()
    }
}