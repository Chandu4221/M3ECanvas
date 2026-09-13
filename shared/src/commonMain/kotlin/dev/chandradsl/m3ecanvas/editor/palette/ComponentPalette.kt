package dev.chandradsl.m3ecanvas.editor.palette

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.ComponentCategory
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry

/**
 * The sidebar listing every available component, grouped by category or filtered by search query.
 * Built according to Brutal Redesign spec: dense, quiet chrome, high hierarchy, unmistakable states.
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
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
        // Sticky Header: Title + Collapse + Search
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Components",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
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
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (collapsedCategories.isEmpty()) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                                contentDescription = if (collapsedCategories.isEmpty()) "Collapse All" else "Expand All",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (onCollapse != null) {
                            IconButton(
                                onClick = onCollapse,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Collapse Palette",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Rounded Search Pill (20dp corner radius)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search components...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                )
            }
        }

        // Horizontal Recent / Favorites Strip (Compact, sticky under search)
        if (trimmedQuery.isEmpty()) {
            val quickItems = (favoriteTypes + recentTypes).distinct().take(8)
            if (quickItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(quickItems, key = { "quick_${it.name}" }) { type ->
                            val isFav = type in favoriteTypes
                            Surface(
                                onClick = { onAddComponent(type) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isFav) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = type.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Component List
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No components match",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // If searching: flat list with keyboard highlight
                if (allFilteredTypes != null) {
                    itemsIndexed(items = allFilteredTypes, key = { _, type -> "search_${type.name}" }) { index, type ->
                        VisualPaletteCard(
                            type = type,
                            onAddComponent = onAddComponent,
                            isFavorite = type in favoriteTypes,
                            onToggleFavorite = { onToggleFavorite?.invoke(type) },
                            isSelected = index == selectedSearchIndex
                        )
                    }
                } else {
                    // Category grouping
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
                                    VisualPaletteCard(
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
private fun PaletteCategoryHeader(
    category: ComponentCategory,
    itemCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "$itemCount",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun VisualPaletteCard(
    type: ComponentType,
    onAddComponent: (ComponentType) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    isSelected: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showDetails by remember { mutableStateOf(false) }
    val def = remember(type) { ComponentRegistry.get(type) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else if (isHovered) MaterialTheme.colorScheme.outlineVariant
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            shadowElevation = if (isHovered) 2.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource = interactionSource)
                .clickable { onAddComponent(type) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tinted icon container (32dp, primaryContainer tint)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = type.icon(),
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (def != null && def.description.isNotEmpty()) {
                        Text(
                            text = def.description,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Info hint toggle
                if (def != null && (def.description.isNotEmpty() || def.keyParameters.isNotEmpty())) {
                    IconButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Component Info",
                            tint = if (showDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Favorite toggle
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Favorite" else "Add favorite",
                            tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Expandable Parameter / Description details
        if (showDetails && def != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 2.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (def.description.isNotEmpty()) {
                        Text(
                            text = def.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (def.keyParameters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Parameters: " + def.keyParameters.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}