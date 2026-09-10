package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlin.math.round

/**
 * Editor for the selected node's modifier chain.
 *
 * Rows follow the same action pattern as the layers panel (up / down /
 * delete). Because Compose modifier order is semantically significant,
 * reordering rows changes the rendered result live.
 */
@Composable
fun ModifierSection(
    node: CanvasNode,
    controller: EditorController
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Modifiers",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            AddModifierMenu(node = node, controller = controller)
        }

        if (node.modifiers.isEmpty()) {
            Text(
                text = "No modifiers yet. Add one to decorate this node.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        node.modifiers.forEach { spec ->
            ModifierRow(node = node, spec = spec, controller = controller)
        }
    }
}

@Composable
private fun AddModifierMenu(
    node: CanvasNode,
    controller: EditorController
) {
    var expanded by remember { mutableStateOf(value = false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = "Add")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ModifierKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(text = kind.displayName) },
                    onClick = {
                        controller.addModifier(nodeId = node.id, spec = kind.create())
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ModifierRow(
    node: CanvasNode,
    spec: ModifierSpec,
    controller: EditorController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 4.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spec.label(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(weight = 1f)
            )
            ModifierActionIcon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = "Move earlier",
                onClick = { controller.moveModifierUp(nodeId = node.id, specId = spec.id) }
            )
            ModifierActionIcon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Move later",
                onClick = { controller.moveModifierDown(nodeId = node.id, specId = spec.id) }
            )
            ModifierActionIcon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Remove modifier",
                onClick = { controller.removeModifier(nodeId = node.id, specId = spec.id) }
            )
        }

        ModifierParams(spec = spec) { updated ->
            controller.updateModifier(nodeId = node.id, spec = updated)
        }
    }
}

@Composable
private fun ModifierActionIcon(
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

@Composable
private fun ModifierParams(
    spec: ModifierSpec,
    onChange: (ModifierSpec) -> Unit
) {
    when (spec) {
        is ModifierSpec.Padding -> MiniSlider(
            label = "All",
            value = spec.all,
            valueRange = 0f..64f,
            onValueChange = { onChange(spec.copy(all = it)) }
        )

        is ModifierSpec.Background -> HexField(
            value = spec.colorHex,
            onChange = { onChange(spec.copy(colorHex = it)) }
        )

        is ModifierSpec.Border -> {
            MiniSlider(
                label = "Width",
                value = spec.width,
                valueRange = 0f..16f,
                onValueChange = { onChange(spec.copy(width = it)) }
            )
            HexField(
                value = spec.colorHex,
                onChange = { onChange(spec.copy(colorHex = it)) }
            )
        }

        is ModifierSpec.Clip -> MiniSlider(
            label = "Radius",
            value = spec.cornerRadius,
            valueRange = 0f..32f,
            onValueChange = { onChange(spec.copy(cornerRadius = it)) }
        )

        is ModifierSpec.Alpha -> MiniSlider(
            label = "Alpha",
            value = spec.value,
            valueRange = 0f..1f,
            onValueChange = { onChange(spec.copy(value = it)) }
        )

        is ModifierSpec.Rotation -> MiniSlider(
            label = "Degrees",
            value = spec.degrees,
            valueRange = -180f..180f,
            onValueChange = { onChange(spec.copy(degrees = it)) }
        )

        is ModifierSpec.Scale -> MiniSlider(
            label = "Scale",
            value = spec.value,
            valueRange = 0.5f..2f,
            onValueChange = { onChange(spec.copy(value = it)) }
        )

        is ModifierSpec.FillMaxWidth -> Unit
        is ModifierSpec.FillMaxHeight -> Unit
    }
}

@Composable
private fun MiniSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(width = 56.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(weight = 1f)
        )
        Text(
            text = (round(value * 100f) / 100f).toString(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(width = 48.dp)
        )
    }
}

@Composable
private fun HexField(
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(text = "Color hex") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}