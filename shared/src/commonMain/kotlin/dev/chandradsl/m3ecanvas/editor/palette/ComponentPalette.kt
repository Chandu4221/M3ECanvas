package dev.chandradsl.m3ecanvas.editor.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.ComponentCategory
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry

/**
 * The sidebar listing every available component, grouped by category or filtered by search query.
 * Supports Favorites, Recently Used, parameter preview, and keyboard navigation.
 */
@Composable
fun ComponentPalette(
    onAddComponent: (ComponentType) -> Unit,
    modifier: Modifier = Modifier,
    onCollapse: (() -> Unit)? = null,
    favoriteTypes: Set<ComponentType> = emptySet(),
    onToggleFavorite: ((ComponentType) -> Unit)? = null,
    recentTypes: List<ComponentType> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    var collapsedCategories by remember { mutableStateOf(setOf<ComponentCategory>()) }
    var selectedSearchIndex by remember { mutableStateOf(0) }

    val trimmedQuery = searchQuery.trim()
    val allFilteredTypes = remember(trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            null
        } else {
            ComponentType.entries.filter {
                it.displayName.contains(trimmedQuery, ignoreCase = true) ||
                        it.name.contains(trimmedQuery, ignoreCase = true) ||
                        it.category.displayName.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(allFilteredTypes) {
        selectedSearchIndex = 0
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (allFilteredTypes != null && allFilteredTypes.isNotEmpty()) {
                    when (event.key) {
                        Key.DirectionDown -> {
                            selectedSearchIndex = (selectedSearchIndex + 1) % allFilteredTypes.size
                            true
                        }
                        Key.DirectionUp -> {
                            selectedSearchIndex = (selectedSearchIndex - 1 + allFilteredTypes.size) % allFilteredTypes.size
                            true
                        }
                        Key.Enter -> {
                            if (selectedSearchIndex in allFilteredTypes.indices) {
                                onAddComponent(allFilteredTypes[selectedSearchIndex])
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Sticky Search Input Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Palette",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                collapsedCategories = if (collapsedCategories.isEmpty()) {
                                    ComponentCategory.entries.toSet()
                                } else {
                                    emptySet()
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (collapsedCategories.isEmpty()) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = if (collapsedCategories.isEmpty()) "Collapse All Categories" else "Expand All Categories",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (onCollapse != null) {
                            IconButton(
                                onClick = onCollapse,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Collapse Palette",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search components...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )
            }
        }
        HorizontalDivider()

        if (allFilteredTypes != null && allFilteredTypes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No components found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try a different search keyword",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Favorites Section (only when not searching)
                if (trimmedQuery.isEmpty() && favoriteTypes.isNotEmpty()) {
                    item(key = "header_favorites") {
                        PaletteSpecialHeader(
                            title = "Favorites",
                            count = favoriteTypes.size,
                            icon = Icons.Outlined.Star,
                            iconTint = Color(0xFFFFB300)
                        )
                    }
                    items(items = favoriteTypes.toList(), key = { "fav_${it.name}" }) { type ->
                        PaletteItem(
                            type = type,
                            onAddComponent = onAddComponent,
                            isFavorite = true,
                            onToggleFavorite = { onToggleFavorite?.invoke(type) }
                        )
                    }
                    item(key = "divider_favorites") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                // Recently Used Section (only when not searching)
                val recentsToShow = recentTypes.filterNot { it in favoriteTypes }.take(6)
                if (trimmedQuery.isEmpty() && recentsToShow.isNotEmpty()) {
                    item(key = "header_recent") {
                        PaletteSpecialHeader(
                            title = "Recently Used",
                            count = recentsToShow.size,
                            icon = Icons.Outlined.History,
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(items = recentsToShow, key = { "recent_${it.name}" }) { type ->
                        PaletteItem(
                            type = type,
                            onAddComponent = onAddComponent,
                            isFavorite = type in favoriteTypes,
                            onToggleFavorite = { onToggleFavorite?.invoke(type) }
                        )
                    }
                    item(key = "divider_recent") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                // If searching, show linear list with keyboard selection
                if (allFilteredTypes != null) {
                    itemsIndexed(items = allFilteredTypes, key = { _, type -> "search_${type.name}" }) { index, type ->
                        PaletteItem(
                            type = type,
                            onAddComponent = onAddComponent,
                            isFavorite = type in favoriteTypes,
                            onToggleFavorite = { onToggleFavorite?.invoke(type) },
                            isSelected = index == selectedSearchIndex
                        )
                    }
                } else {
                    // Standard Category Grouping
                    ComponentCategory.entries.forEach { category ->
                        val types = ComponentType.entries.filter { it.category == category }
                        if (types.isNotEmpty()) {
                            val isExpanded = !collapsedCategories.contains(category)
                            item(key = "header_${category.name}") {
                                PaletteCategoryHeader(
                                    category = category,
                                    itemCount = types.size,
                                    isExpanded = isExpanded,
                                    onToggle = {
                                        collapsedCategories = if (collapsedCategories.contains(category)) {
                                            collapsedCategories - category
                                        } else {
                                            collapsedCategories + category
                                        }
                                    }
                                )
                            }
                            if (isExpanded) {
                                items(items = types, key = { it.name }) { type ->
                                    PaletteItem(
                                        type = type,
                                        onAddComponent = onAddComponent,
                                        isFavorite = type in favoriteTypes,
                                        onToggleFavorite = { onToggleFavorite?.invoke(type) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSpecialHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Badge(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun PaletteCategoryHeader(
    category: ComponentCategory,
    itemCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (isExpanded) "Collapse ${category.displayName}" else "Expand ${category.displayName}",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun PaletteItem(
    type: ComponentType,
    onAddComponent: (ComponentType) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    isSelected: Boolean = false
) {
    var showDetails by remember { mutableStateOf(false) }
    val def = remember(type) { ComponentRegistry.get(type) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddComponent(type) }
                .padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = type.icon(),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
            if (def != null && (def.description.isNotEmpty() || def.keyParameters.isNotEmpty())) {
                IconButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Component Info",
                        tint = if (showDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (onToggleFavorite != null) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        if (showDetails && def != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 16.dp, bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (def.description.isNotEmpty()) {
                        Text(
                            text = def.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (def.keyParameters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Params:",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = def.keyParameters.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}