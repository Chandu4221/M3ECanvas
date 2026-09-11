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
        is ModifierSpec.Padding -> {
            MiniSlider(
                label = "All",
                value = spec.all,
                valueRange = 0f..64f,
                onValueChange = { onChange(spec.copy(all = it)) }
            )
            MiniSlider(
                label = "Horiz",
                value = spec.horizontal,
                valueRange = 0f..64f,
                onValueChange = { onChange(spec.copy(horizontal = it)) }
            )
            MiniSlider(
                label = "Vert",
                value = spec.vertical,
                valueRange = 0f..64f,
                onValueChange = { onChange(spec.copy(vertical = it)) }
            )
        }

        is ModifierSpec.Background -> {
            HexField(
                value = spec.colorHex,
                onChange = { onChange(spec.copy(colorHex = it)) }
            )
            MiniSlider(
                label = "Radius",
                value = spec.cornerRadius,
                valueRange = 0f..32f,
                onValueChange = { onChange(spec.copy(cornerRadius = it)) }
            )
        }

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
            MiniSlider(
                label = "Radius",
                value = spec.cornerRadius,
                valueRange = 0f..32f,
                onValueChange = { onChange(spec.copy(cornerRadius = it)) }
            )
        }

        is ModifierSpec.Clip -> MiniSlider(
            label = "Radius",
            value = spec.cornerRadius,
            valueRange = 0f..64f,
            onValueChange = { onChange(spec.copy(cornerRadius = it)) }
        )

        is ModifierSpec.Shadow -> {
            MiniSlider(
                label = "Elevation",
                value = spec.elevation,
                valueRange = 0f..24f,
                onValueChange = { onChange(spec.copy(elevation = it)) }
            )
            MiniSlider(
                label = "Radius",
                value = spec.cornerRadius,
                valueRange = 0f..32f,
                onValueChange = { onChange(spec.copy(cornerRadius = it)) }
            )
        }

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
            valueRange = 0.1f..3f,
            onValueChange = { onChange(spec.copy(value = it)) }
        )

        is ModifierSpec.FillMaxSize -> MiniSlider(
            label = "Fraction",
            value = spec.fraction,
            valueRange = 0.1f..1f,
            onValueChange = { onChange(spec.copy(fraction = it)) }
        )

        is ModifierSpec.FillMaxWidth -> Unit
        is ModifierSpec.FillMaxHeight -> Unit
        is ModifierSpec.WrapContentSize -> Unit

        is ModifierSpec.Width -> MiniSlider(
            label = "Width",
            value = spec.dp,
            valueRange = 10f..400f,
            onValueChange = { onChange(spec.copy(dp = it)) }
        )

        is ModifierSpec.Height -> MiniSlider(
            label = "Height",
            value = spec.dp,
            valueRange = 10f..400f,
            onValueChange = { onChange(spec.copy(dp = it)) }
        )

        is ModifierSpec.Size -> {
            MiniSlider(
                label = "Width",
                value = spec.width,
                valueRange = 10f..400f,
                onValueChange = { onChange(spec.copy(width = it)) }
            )
            MiniSlider(
                label = "Height",
                value = spec.height,
                valueRange = 10f..400f,
                onValueChange = { onChange(spec.copy(height = it)) }
            )
        }

        is ModifierSpec.AspectRatio -> MiniSlider(
            label = "Ratio",
            value = spec.ratio,
            valueRange = 0.25f..3f,
            onValueChange = { onChange(spec.copy(ratio = it)) }
        )

        is ModifierSpec.Offset -> {
            MiniSlider(
                label = "X",
                value = spec.x,
                valueRange = -100f..100f,
                onValueChange = { onChange(spec.copy(x = it)) }
            )
            MiniSlider(
                label = "Y",
                value = spec.y,
                valueRange = -100f..100f,
                onValueChange = { onChange(spec.copy(y = it)) }
            )
        }

        is ModifierSpec.Weight -> {
            MiniSlider(
                label = "Weight",
                value = spec.weight,
                valueRange = 0.1f..10f,
                onValueChange = { onChange(spec.copy(weight = it)) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Fill remaining", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = spec.fill,
                    onCheckedChange = { onChange(spec.copy(fill = it)) }
                )
            }
        }

        is ModifierSpec.Align -> {
            AlignDropdown(
                current = spec.alignment,
                onChange = { onChange(spec.copy(alignment = it)) }
            )
        }

        is ModifierSpec.ZIndex -> MiniSlider(
            label = "Z-Index",
            value = spec.value,
            valueRange = -10f..10f,
            onValueChange = { onChange(spec.copy(value = it)) }
        )

        is ModifierSpec.Clickable -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Enabled", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = spec.enabled,
                    onCheckedChange = { onChange(spec.copy(enabled = it)) }
                )
            }
        }
    }
}

@Composable
private fun AlignDropdown(
    current: AlignTarget,
    onChange: (AlignTarget) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Alignment: ${current.displayName}")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AlignTarget.entries.forEach { target ->
                DropdownMenuItem(
                    text = { Text(text = target.displayName) },
                    onClick = {
                        onChange(target)
                        expanded = false
                    }
                )
            }
        }
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