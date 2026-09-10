package dev.chandradsl.m3ecanvas.editor.palette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.ComponentCategory
import dev.chandradsl.m3ecanvas.domain.model.ComponentType

/**
 * The sidebar listing every available component, grouped by category.
 * Clicking a component adds it to the canvas via [onAddComponent].
 */
@Composable
fun ComponentPalette(
    onAddComponent: (ComponentType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxHeight()) {
        ComponentCategory.entries.forEach { category ->
            val types = ComponentType.entries.filter { it.category == category }
            if (types.isNotEmpty()) {
                item(key = "header_${category.name}") {
                    PaletteCategoryHeader(category = category)
                }
                items(items = types, key = { it.name }) { type ->
                    PaletteItem(type = type, onAddComponent = onAddComponent)
                }
            }
        }
    }
}

@Composable
private fun PaletteCategoryHeader(category: ComponentCategory) {
    Text(
        text = category.displayName,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
    HorizontalDivider()
}

@Composable
private fun PaletteItem(
    type: ComponentType,
    onAddComponent: (ComponentType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddComponent(type) }
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size = 18.dp)
        )
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}