@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.state.EditorController

/**
 * Renders a single [CanvasNode] as its corresponding composable.
 * Supports variant-aware rendering and applies the node's stored modifier
 * chain in user order before the system-owned modifiers.
 */
@Composable
fun CanvasNodeRenderer(
    node: CanvasNode,
    controller: EditorController
) {
    when (node.type) {
        ComponentType.COLUMN -> {
            val scrollState = rememberScrollState()
            Column(
                modifier = node.modifiers.toModifier()
                    .fillMaxSize()
                    .then(
                        if (node.layoutConfig.scrollable) {
                            Modifier.verticalScroll(state = scrollState)
                        } else {
                            Modifier
                        }
                    )
                    .padding(all = node.layoutConfig.padding.dp),
                verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig),
                horizontalAlignment = resolveHorizontalAlignment(config = node.layoutConfig)
            ) {
                node.children.forEach { child ->
                    val weightSpec = child.modifiers.filterIsInstance<ModifierSpec.Weight>().firstOrNull()
                    val alignSpec = child.modifiers.filterIsInstance<ModifierSpec.Align>().firstOrNull()
                    val scopeModifier = Modifier
                        .then(if (weightSpec != null) Modifier.weight(weightSpec.weight, weightSpec.fill) else Modifier)
                        .then(if (alignSpec != null) Modifier.align(alignSpec.alignment.toColumnAlignment()) else Modifier)
                    ContainerChild(child = child, controller = controller, modifier = scopeModifier)
                }
            }
        }

        ComponentType.ROW -> {
            val scrollState = rememberScrollState()
            Row(
                modifier = node.modifiers.toModifier()
                    .fillMaxSize()
                    .then(
                        if (node.layoutConfig.scrollable) {
                            Modifier.horizontalScroll(state = scrollState)
                        } else {
                            Modifier
                        }
                    )
                    .padding(all = node.layoutConfig.padding.dp),
                horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig),
                verticalAlignment = resolveVerticalAlignment(config = node.layoutConfig)
            ) {
                node.children.forEach { child ->
                    val weightSpec = child.modifiers.filterIsInstance<ModifierSpec.Weight>().firstOrNull()
                    val alignSpec = child.modifiers.filterIsInstance<ModifierSpec.Align>().firstOrNull()
                    val scopeModifier = Modifier
                        .then(if (weightSpec != null) Modifier.weight(weightSpec.weight, weightSpec.fill) else Modifier)
                        .then(if (alignSpec != null) Modifier.align(alignSpec.alignment.toRowAlignment()) else Modifier)
                    ContainerChild(child = child, controller = controller, modifier = scopeModifier)
                }
            }
        }

        ComponentType.BOX -> Box(
            modifier = node.modifiers.toModifier()
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            contentAlignment = resolveContentAlignment(config = node.layoutConfig)
        ) {
            node.children.forEach { child ->
                val alignSpec = child.modifiers.filterIsInstance<ModifierSpec.Align>().firstOrNull()
                val scopeModifier = if (alignSpec != null) Modifier.align(alignSpec.alignment.toBoxAlignment()) else Modifier
                ContainerChild(child = child, controller = controller, modifier = scopeModifier)
            }
        }

        ComponentType.LAZY_COLUMN -> LazyColumn(
            modifier = node.modifiers.toModifier()
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig),
            horizontalAlignment = resolveHorizontalAlignment(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.LAZY_ROW -> LazyRow(
            modifier = node.modifiers.toModifier()
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig),
            verticalAlignment = resolveVerticalAlignment(config = node.layoutConfig)
        ) {
            items(items = node.children, key = { child -> child.id }) { child ->
                ContainerChild(child = child, controller = controller)
            }
        }

        ComponentType.LAZY_VERTICAL_GRID -> RenderLazyVerticalGrid(node = node, controller = controller)
        ComponentType.FLOW_ROW -> RenderFlowRow(node = node, controller = controller)
        ComponentType.FLOW_COLUMN -> RenderFlowColumn(node = node, controller = controller)
        ComponentType.SPACER -> RenderSpacer(node = node)
        ComponentType.SURFACE -> RenderSurface(node = node, controller = controller)
        ComponentType.SCAFFOLD -> RenderScaffold(node = node, controller = controller)

        // Action
        ComponentType.BUTTON -> RenderButton(node = node)
        ComponentType.ICON_BUTTON -> RenderIconButton(node = node)
        ComponentType.FAB -> RenderFab(node = node)
        ComponentType.EXTENDED_FAB -> RenderExtendedFab(node = node)
        ComponentType.SEGMENTED_BUTTON -> RenderSegmentedButton(node = node)
        ComponentType.SPLIT_BUTTON -> RenderSplitButton(node = node)
        ComponentType.BUTTON_GROUP -> RenderButtonGroup(node = node)

        // Containment
        ComponentType.CARD -> RenderCard(node = node)
        ComponentType.LISTS -> RenderListItem(node = node)
        ComponentType.SHEETS -> RenderBottomSheet(node = node)
        ComponentType.DIALOG -> RenderDialog(node = node)

        // Communication
        ComponentType.SNACKBAR -> RenderSnackbar(node = node)
        ComponentType.BADGE -> RenderBadge(node = node)
        ComponentType.TOOLTIP -> RenderTooltip(node = node)
        ComponentType.PROGRESS_INDICATOR -> RenderProgressIndicator(node = node)
        ComponentType.LOADING_INDICATOR -> RenderLoadingIndicator(node = node)

        // Navigation
        ComponentType.TOP_APP_BAR -> RenderTopAppBar(node = node)
        ComponentType.NAVIGATION_BAR -> RenderNavigationBar(node = node)
        ComponentType.BOTTOM_APP_BAR -> RenderBottomAppBar(node = node)
        ComponentType.NAVIGATION_RAIL -> RenderNavigationRail(node = node)
        ComponentType.NAVIGATION_DRAWER -> RenderNavigationDrawer(node = node)
        ComponentType.TABS -> RenderTabs(node = node)
        ComponentType.SEARCH -> RenderSearch(node = node)

        // Selection
        ComponentType.CHECKBOX -> RenderCheckbox(node = node)
        ComponentType.RADIO_BUTTON -> RenderRadioButton(node = node)
        ComponentType.SWITCH -> RenderSwitch(node = node)
        ComponentType.SLIDER -> RenderSlider(node = node)
        ComponentType.RANGE_SLIDER -> RenderRangeSlider(node = node)
        ComponentType.CHIPS -> RenderChip(node = node)
        ComponentType.DATE_PICKER -> RenderDatePicker(node = node)
        ComponentType.TIME_PICKER -> RenderTimePicker(node = node)
        ComponentType.MENUS -> RenderMenu(node = node)

        // Text input
        ComponentType.TEXT_FIELD -> RenderTextField(node = node)

        // Typography
        ComponentType.TEXT -> RenderText(node = node)

        // Graphics
        ComponentType.ICON -> RenderIcon(node = node)
        ComponentType.IMAGE -> RenderImage(node = node)
        ComponentType.HORIZONTAL_DIVIDER -> RenderHorizontalDivider(node = node)
        ComponentType.VERTICAL_DIVIDER -> RenderVerticalDivider(node = node)
    }
}

@Composable
private fun RenderButton(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Button
        ?: MaterialVariant.Button.FILLED
    val text = node.textOrDefault(default = "Button")
    val onClick = {}
    val modifier = node.modifiers.toModifier().fillMaxSize()

    when (variant) {
        MaterialVariant.Button.FILLED -> Button(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.TONAL -> FilledTonalButton(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.OUTLINED -> OutlinedButton(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.ELEVATED -> ElevatedButton(onClick = onClick, modifier = modifier) { Text(text = text) }
        MaterialVariant.Button.TEXT -> TextButton(onClick = onClick, modifier = modifier) { Text(text = text) }
    }
}

@Composable
private fun RenderCard(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Card
        ?: MaterialVariant.Card.FILLED
    val modifier = node.modifiers.toModifier().fillMaxSize()

    when (variant) {
        MaterialVariant.Card.FILLED -> Card(modifier = modifier) { Box(modifier = Modifier.fillMaxSize()) }
        MaterialVariant.Card.ELEVATED -> ElevatedCard(modifier = modifier) { Box(modifier = Modifier.fillMaxSize()) }
        MaterialVariant.Card.OUTLINED -> OutlinedCard(modifier = modifier) { Box(modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun RenderIconButton(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.IconButton
        ?: MaterialVariant.IconButton.STANDARD
    val iconName = node.iconProperty(key = "icon", default = "Add")
    val onClick = {}
    val modifier = node.modifiers.toModifier().fillMaxSize()
    val icon = @Composable { Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = null) }

    when (variant) {
        MaterialVariant.IconButton.STANDARD -> IconButton(onClick = onClick, modifier = modifier, content = icon)
        MaterialVariant.IconButton.FILLED -> FilledIconButton(onClick = onClick, modifier = modifier, content = icon)
        MaterialVariant.IconButton.TONAL -> FilledTonalIconButton(onClick = onClick, modifier = modifier, content = icon)
        MaterialVariant.IconButton.OUTLINED -> OutlinedIconButton(onClick = onClick, modifier = modifier, content = icon)
    }
}

@Composable
private fun RenderChip(node: CanvasNode) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Chip
        ?: MaterialVariant.Chip.ASSIST
    val label = @Composable { Text(text = node.textOrDefault(default = "Chip")) }
    val onClick = {}

    when (variant) {
        MaterialVariant.Chip.ASSIST -> AssistChip(onClick = onClick, label = label)
        MaterialVariant.Chip.FILTER -> FilterChip(selected = false, onClick = onClick, label = label)
        MaterialVariant.Chip.INPUT -> InputChip(selected = false, onClick = onClick, label = label)
        MaterialVariant.Chip.SUGGESTION -> SuggestionChip(onClick = onClick, label = label)
    }
}

@Composable
private fun RenderScaffold(
    node: CanvasNode,
    controller: EditorController
) {
    val topBarChild = node.childInSlot(SlotRole.TOP_BAR)
    val bottomBarChild = node.childInSlot(SlotRole.BOTTOM_BAR)
    val railChild = node.childInSlot(SlotRole.RAIL)
    val drawerChild = node.childInSlot(SlotRole.DRAWER)
    val fabChild = node.childInSlot(SlotRole.FAB)
    val snackbarChild = node.childInSlot(SlotRole.SNACKBAR)
    val contentChildren = node.children.filter { it.slot == null || it.slot == SlotRole.CONTENT }

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            modifier = node.modifiers.toModifier().fillMaxSize(),
            topBar = {
                if (topBarChild != null) {
                    SlotContainer(child = topBarChild, controller = controller)
                }
            },
            bottomBar = {
                if (bottomBarChild != null) {
                    SlotContainer(child = bottomBarChild, controller = controller)
                }
            },
            snackbarHost = {
                if (snackbarChild != null) {
                    SlotContainer(child = snackbarChild, controller = controller)
                }
            },
            floatingActionButton = {
                if (fabChild != null) {
                    SlotContainer(child = fabChild, controller = controller)
                }
            }
        ) { innerPadding ->
            if (railChild != null) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    SlotContainer(child = railChild, controller = controller)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        contentChildren.forEach { child ->
                            ContainerChild(child = child, controller = controller)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    contentChildren.forEach { child ->
                        ContainerChild(child = child, controller = controller)
                    }
                }
            }
        }
    }

    if (drawerChild != null) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    SlotContainer(child = drawerChild, controller = controller)
                }
            },
            content = scaffoldContent
        )
    } else {
        scaffoldContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderTopAppBar(node: CanvasNode) {
    val titleText = (node.property("title") as? ComponentProperty.Text)?.value
        ?: (node.property("text") as? ComponentProperty.Text)?.value
        ?: node.name

    TopAppBar(
        title = { Text(text = titleText, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Navigation"
                )
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More"
                )
            }
        },
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderNavigationBar(node: CanvasNode) {
    NavigationBar(
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text(text = "Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = "Explore") },
            label = { Text(text = "Explore") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text(text = "Profile") }
        )
    }
}

@Composable
private fun RenderFab(node: CanvasNode) {
    val iconName = node.iconProperty(key = "icon", default = "Add")
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.FloatingActionButton
        ?: MaterialVariant.FloatingActionButton.SECONDARY
    val containerColor = when (variant) {
        MaterialVariant.FloatingActionButton.SURFACE -> MaterialTheme.colorScheme.surfaceContainerHigh
        MaterialVariant.FloatingActionButton.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        MaterialVariant.FloatingActionButton.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (variant) {
        MaterialVariant.FloatingActionButton.SURFACE -> MaterialTheme.colorScheme.primary
        MaterialVariant.FloatingActionButton.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
        MaterialVariant.FloatingActionButton.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    FloatingActionButton(
        onClick = {},
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = node.modifiers.toModifier()
    ) {
        Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = "Add")
    }
}

@Composable
private fun RenderExtendedFab(node: CanvasNode) {
    val text = node.textOrDefault(default = "Action")
    val iconName = node.iconProperty(key = "icon", default = "Add")
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.FloatingActionButton
        ?: MaterialVariant.FloatingActionButton.SECONDARY
    val containerColor = when (variant) {
        MaterialVariant.FloatingActionButton.SURFACE -> MaterialTheme.colorScheme.surfaceContainerHigh
        MaterialVariant.FloatingActionButton.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        MaterialVariant.FloatingActionButton.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (variant) {
        MaterialVariant.FloatingActionButton.SURFACE -> MaterialTheme.colorScheme.primary
        MaterialVariant.FloatingActionButton.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
        MaterialVariant.FloatingActionButton.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    ExtendedFloatingActionButton(
        onClick = {},
        containerColor = containerColor,
        contentColor = contentColor,
        icon = { Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = null) },
        text = { Text(text = text) },
        modifier = node.modifiers.toModifier()
    )
}

@Composable
private fun SlotContainer(
    child: CanvasNode,
    controller: EditorController
) {
    val isSelected = controller.state.isNodeSelected(nodeId = child.id)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            try {
                focusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    Box(
        modifier = Modifier
            .then(
                if (child.type == ComponentType.TOP_APP_BAR || child.type == ComponentType.NAVIGATION_BAR || child.type == ComponentType.BOTTOM_APP_BAR) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.wrapContentSize()
                }
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (isSelected && event.type == KeyEventType.KeyDown && (event.key == Key.Delete || event.key == Key.Backspace)) {
                    controller.deleteSelected()
                    true
                } else {
                    false
                }
            }
            .selectOnPress(nodeId = child.id, controller = controller, focusRequester = focusRequester)
    ) {
        CanvasNodeRenderer(node = child, controller = controller)
    }
}

@Composable
private fun ContainerChild(
    child: CanvasNode,
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    val isSelected = controller.state.isNodeSelected(nodeId = child.id)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            try {
                focusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    val fillMaxSize = child.modifiers.filterIsInstance<ModifierSpec.FillMaxSize>().firstOrNull()
    val fillMaxWidth = child.modifiers.filterIsInstance<ModifierSpec.FillMaxWidth>().firstOrNull()
    val fillMaxHeight = child.modifiers.filterIsInstance<ModifierSpec.FillMaxHeight>().firstOrNull()
    val widthSpec = child.modifiers.filterIsInstance<ModifierSpec.Width>().firstOrNull()
    val heightSpec = child.modifiers.filterIsInstance<ModifierSpec.Height>().firstOrNull()
    val sizeSpec = child.modifiers.filterIsInstance<ModifierSpec.Size>().firstOrNull()
    val wrapContent = child.modifiers.filterIsInstance<ModifierSpec.WrapContentSize>().firstOrNull()

    val sizeModifier = when {
        fillMaxSize != null -> Modifier.fillMaxSize(fraction = fillMaxSize.fraction)
        fillMaxWidth != null && fillMaxHeight != null -> Modifier.fillMaxWidth().fillMaxHeight()
        fillMaxWidth != null -> Modifier.fillMaxWidth().height(heightSpec?.dp?.dp ?: sizeSpec?.height?.dp ?: child.size.height.dp)
        fillMaxHeight != null -> Modifier.fillMaxHeight().width(widthSpec?.dp?.dp ?: sizeSpec?.width?.dp ?: child.size.width.dp)
        sizeSpec != null -> Modifier.size(width = sizeSpec.width.dp, height = sizeSpec.height.dp)
        widthSpec != null && heightSpec != null -> Modifier.size(width = widthSpec.dp.dp, height = heightSpec.dp.dp)
        widthSpec != null -> Modifier.width(widthSpec.dp.dp).height(child.size.height.dp)
        heightSpec != null -> Modifier.width(child.size.width.dp).height(heightSpec.dp.dp)
        wrapContent != null -> Modifier.wrapContentSize()
        else -> Modifier.size(width = child.size.width.dp, height = child.size.height.dp)
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (isSelected && event.type == KeyEventType.KeyDown && (event.key == Key.Delete || event.key == Key.Backspace)) {
                    controller.deleteSelected()
                    true
                } else {
                    false
                }
            }
            .selectOnPress(nodeId = child.id, controller = controller, focusRequester = focusRequester)
    ) {
        CanvasNodeRenderer(node = child, controller = controller)
    }
}

@Composable
internal fun Modifier.selectOnPress(
    nodeId: String,
    controller: EditorController,
    focusRequester: FocusRequester? = null
): Modifier {
    val focusManager = LocalFocusManager.current
    return this.pointerInput(nodeId) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            focusManager.clearFocus()
            controller.selectNode(nodeId = nodeId)
            try {
                focusRequester?.requestFocus()
            } catch (_: Throwable) {}
        }
    }
}

@Composable
private fun RenderTextField(node: CanvasNode) {
    val text = node.textOrDefault(default = "")
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.TextField
        ?: MaterialVariant.TextField.OUTLINED
    val modifier = node.modifiers.toModifier().fillMaxSize()

    when (variant) {
        MaterialVariant.TextField.OUTLINED -> OutlinedTextField(
            value = text,
            onValueChange = {},
            label = { Text(node.name) },
            placeholder = { Text("Enter text...") },
            readOnly = true,
            modifier = modifier
        )
        MaterialVariant.TextField.FILLED -> TextField(
            value = text,
            onValueChange = {},
            label = { Text(node.name) },
            placeholder = { Text("Enter text...") },
            readOnly = true,
            modifier = modifier
        )
    }
}

@Composable
private fun RenderSegmentedButton(node: CanvasNode) {
    val options = listOf("Day", "Week", "Month")
    SingleChoiceSegmentedButtonRow(
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = {},
                selected = index == 0
            ) {
                Text(text = label)
            }
        }
    }
}

@Composable
private fun RenderSplitButton(node: CanvasNode) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        FilledTonalButton(
            onClick = {},
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)
        ) {
            Text(text = node.textOrDefault(default = "Action"))
        }
        Spacer(modifier = Modifier.width(2.dp))
        FilledTonalButton(
            onClick = {},
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = "More")
        }
    }
}

@Composable
private fun RenderButtonGroup(node: CanvasNode) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        Button(onClick = {}) { Text("Primary") }
        FilledTonalButton(onClick = {}) { Text("Tonal") }
        OutlinedButton(onClick = {}) { Text("Outlined") }
    }
}

@Composable
private fun RenderListItem(node: CanvasNode) {
    ListItem(
        headlineContent = { Text(text = node.textOrDefault(default = "Headline text")) },
        supportingContent = { Text(text = "Secondary supporting description") },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Text(
                text = "10:30",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = node.modifiers.toModifier().fillMaxWidth().clip(RoundedCornerShape(8.dp))
    )
}

@Composable
private fun RenderBottomSheet(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = node.textOrDefault(default = "Bottom Sheet Title"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Expressive bottom sheet preview anchored to the viewport.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Action Button")
            }
        }
    }
}

@Composable
private fun RenderDialog(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = node.textOrDefault(default = "Dialog Title"),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dialogs inform users about a task and can contain critical information.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {}) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {}) { Text("Confirm") }
            }
        }
    }
}

@Composable
private fun RenderSnackbar(node: CanvasNode) {
    Snackbar(
        action = {
            TextButton(onClick = {}) {
                Text("Action", color = MaterialTheme.colorScheme.inversePrimary)
            }
        },
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Text(text = node.textOrDefault(default = "Material 3 Snackbar notification."))
    }
}

@Composable
private fun RenderBadge(node: CanvasNode) {
    BadgedBox(
        badge = {
            Badge { Text(text = "3") }
        },
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RenderTooltip(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.size(16.dp).padding(end = 4.dp)
            )
            Text(
                text = node.textOrDefault(default = "Helpful tooltip label"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

@Composable
private fun RenderProgressIndicator(node: CanvasNode) {
    LinearProgressIndicator(
        progress = { 0.7f },
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderLoadingIndicator(node: CanvasNode) {
    CircularProgressIndicator(
        modifier = node.modifiers.toModifier().size(40.dp)
    )
}

@Composable
private fun RenderNavigationRail(node: CanvasNode) {
    NavigationRail(
        modifier = node.modifiers.toModifier().fillMaxHeight().width(80.dp)
    ) {
        NavigationRailItem(
            selected = true,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationRailItem(
            selected = false,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search") },
            label = { Text("Search") }
        )
        NavigationRailItem(
            selected = false,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
private fun RenderNavigationDrawer(node: CanvasNode) {
    ModalDrawerSheet(
        modifier = node.modifiers.toModifier().fillMaxHeight().width(300.dp)
    ) {
        Text(
            text = node.textOrDefault(default = "Navigation Menu"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        NavigationDrawerItem(
            label = { Text("Inbox") },
            selected = true,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.Inbox, contentDescription = null) },
            badge = { Text("12") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label = { Text("Outbox") },
            selected = false,
            onClick = {},
            icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.Send, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label = { Text("Favorites") },
            selected = false,
            onClick = {},
            icon = { Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun RenderTabs(node: CanvasNode) {
    PrimaryTabRow(
        selectedTabIndex = 0,
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Tab(
            selected = true,
            onClick = {},
            text = { Text("Overview") },
            icon = { Icon(imageVector = Icons.Outlined.Dashboard, contentDescription = null) }
        )
        Tab(
            selected = false,
            onClick = {},
            text = { Text("Analytics") },
            icon = { Icon(imageVector = Icons.Outlined.Analytics, contentDescription = null) }
        )
        Tab(
            selected = false,
            onClick = {},
            text = { Text("Settings") },
            icon = { Icon(imageVector = Icons.Outlined.Tune, contentDescription = null) }
        )
    }
}

@Composable
private fun RenderSearch(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = node.modifiers.toModifier().fillMaxWidth().height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = node.textOrDefault(default = "Search components..."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Voice",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RenderCheckbox(node: CanvasNode) {
    val checked = node.booleanProperty(key = "checked", default = true)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        Checkbox(checked = checked, onCheckedChange = {})
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.textOrDefault(default = "Checkbox Option"),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RenderRadioButton(node: CanvasNode) {
    val selected = node.booleanProperty(key = "selected", default = true)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        RadioButton(selected = selected, onClick = {})
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.textOrDefault(default = "Radio Option"),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RenderSwitch(node: CanvasNode) {
    val checked = node.booleanProperty(key = "checked", default = true)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Text(
            text = node.textOrDefault(default = "Enable feature"),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = {})
    }
}

@Composable
private fun RenderSlider(node: CanvasNode) {
    val value = node.numericProperty(key = "value", default = 0.6f).coerceIn(0f, 1f)
    Slider(
        value = value,
        onValueChange = {},
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderDatePicker(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Select Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mon, Sep 15",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("14", "15", "16", "17", "18", "19", "20").forEach { date ->
                    val isSelected = date == "15"
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                else Modifier
                            )
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderTimePicker(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(width = 64.dp, height = 56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "10", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(text = ":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(width = 64.dp, height = 56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "30", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Surface(
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(width = 44.dp, height = 26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(width = 44.dp, height = 26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderMenu(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = 4.dp)) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {},
                leadingIcon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                onClick = {},
                leadingIcon = { Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {},
                leadingIcon = { Icon(imageVector = Icons.Outlined.Delete, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun RenderText(node: CanvasNode) {
    val text = (node.property("text") as? ComponentProperty.Text)?.value ?: node.name
    val styleName = (node.property("typography") as? ComponentProperty.Text)?.value ?: "bodyLarge"
    Text(
        text = text,
        style = resolveTypographyStyle(name = styleName),
        modifier = node.modifiers.toModifier()
    )
}

@Composable
private fun RenderIcon(node: CanvasNode) {
    val iconName = node.iconProperty("icon", default = "Favorite")
    Icon(
        imageVector = resolveMaterialIcon(iconName = iconName),
        contentDescription = iconName,
        tint = MaterialTheme.colorScheme.primary,
        modifier = node.modifiers.toModifier().fillMaxSize()
    )
}

@Composable
private fun RenderImage(node: CanvasNode) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = node.modifiers.toModifier().fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "Image placeholder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RenderHorizontalDivider(node: CanvasNode) {
    HorizontalDivider(
        modifier = node.modifiers.toModifier().fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun RenderVerticalDivider(node: CanvasNode) {
    VerticalDivider(
        modifier = node.modifiers.toModifier().fillMaxHeight(),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun RenderSpacer(node: CanvasNode) {
    Spacer(modifier = node.modifiers.toModifier().fillMaxSize())
}

@Composable
private fun RenderSurface(
    node: CanvasNode,
    controller: EditorController
) {
    val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Surface
        ?: MaterialVariant.Surface.ROUNDED
    val shape = when (variant) {
        MaterialVariant.Surface.ROUNDED -> RoundedCornerShape(12.dp)
        MaterialVariant.Surface.RECTANGLE -> RectangleShape
        MaterialVariant.Surface.CIRCLE -> CircleShape
    }

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = node.modifiers.toModifier().fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            contentAlignment = resolveContentAlignment(config = node.layoutConfig)
        ) {
            node.children.forEach { child ->
                val alignSpec = child.modifiers.filterIsInstance<ModifierSpec.Align>().firstOrNull()
                val scopeModifier = if (alignSpec != null) Modifier.align(alignSpec.alignment.toBoxAlignment()) else Modifier
                ContainerChild(child = child, controller = controller, modifier = scopeModifier)
            }
        }
    }
}

@Composable
private fun RenderBottomAppBar(node: CanvasNode) {
    BottomAppBar(
        actions = {
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Menu, contentDescription = "Menu")
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = "Favorite")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
            }
        },
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderRangeSlider(node: CanvasNode) {
    val start = node.numericProperty("startValue", default = 0.2f).coerceIn(0f, 1f)
    val end = node.numericProperty("endValue", default = 0.8f).coerceIn(0f, 1f)
    val range = if (start <= end) start..end else end..start
    RangeSlider(
        value = range,
        onValueChange = {},
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderLazyVerticalGrid(
    node: CanvasNode,
    controller: EditorController
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = node.modifiers.toModifier()
            .fillMaxSize()
            .padding(all = node.layoutConfig.padding.dp),
        verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig),
        horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
    ) {
        items(items = node.children, key = { child -> child.id }) { child ->
            ContainerChild(child = child, controller = controller)
        }
    }
}

@Composable
private fun RenderFlowRow(
    node: CanvasNode,
    controller: EditorController
) {
    FlowRow(
        modifier = node.modifiers.toModifier()
            .fillMaxSize()
            .padding(all = node.layoutConfig.padding.dp),
        horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig),
        verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
    ) {
        node.children.forEach { child ->
            ContainerChild(child = child, controller = controller)
        }
    }
}

@Composable
private fun RenderFlowColumn(
    node: CanvasNode,
    controller: EditorController
) {
    FlowColumn(
        modifier = node.modifiers.toModifier()
            .fillMaxSize()
            .padding(all = node.layoutConfig.padding.dp),
        horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig),
        verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
    ) {
        node.children.forEach { child ->
            ContainerChild(child = child, controller = controller)
        }
    }
}

@Composable
private fun resolveTypographyStyle(name: String): androidx.compose.ui.text.TextStyle {
    val typography = MaterialTheme.typography
    return when (name.lowercase()) {
        "displaylarge" -> typography.displayLarge
        "displaymedium" -> typography.displayMedium
        "displaysmall" -> typography.displaySmall
        "headlinelarge" -> typography.headlineLarge
        "headlinemedium" -> typography.headlineMedium
        "headlinesmall" -> typography.headlineSmall
        "titlelarge" -> typography.titleLarge
        "titlemedium" -> typography.titleMedium
        "titlesmall" -> typography.titleSmall
        "bodylarge" -> typography.bodyLarge
        "bodymedium" -> typography.bodyMedium
        "bodysmall" -> typography.bodySmall
        "labellarge" -> typography.labelLarge
        "labelmedium" -> typography.labelMedium
        "labelsmall" -> typography.labelSmall
        else -> typography.bodyLarge
    }
}


/**
 * Folds the stored modifier chain into a real Compose [Modifier],
 * preserving user order exactly.
 */
private fun List<ModifierSpec>.toModifier(): Modifier {
    return fold(Modifier) { acc: Modifier, spec: ModifierSpec ->
        acc.then(
            when (spec) {
                is ModifierSpec.Padding -> {
                    if (spec.horizontal > 0f || spec.vertical > 0f) {
                        Modifier.padding(horizontal = spec.horizontal.dp, vertical = spec.vertical.dp)
                    } else {
                        Modifier.padding(all = spec.all.dp)
                    }
                }
                is ModifierSpec.Background -> {
                    if (spec.cornerRadius > 0f) {
                        Modifier.background(
                            color = parseHex(hex = spec.colorHex),
                            shape = RoundedCornerShape(spec.cornerRadius.dp)
                        )
                    } else {
                        Modifier.background(color = parseHex(hex = spec.colorHex))
                    }
                }
                is ModifierSpec.Border -> Modifier.border(
                    width = spec.width.dp,
                    color = parseHex(hex = spec.colorHex),
                    shape = if (spec.cornerRadius > 0f) RoundedCornerShape(spec.cornerRadius.dp) else RectangleShape
                )
                is ModifierSpec.Clip -> Modifier.clip(
                    shape = RoundedCornerShape(corner = CornerSize(spec.cornerRadius.dp))
                )
                is ModifierSpec.Shadow -> Modifier.shadow(
                    elevation = spec.elevation.dp,
                    shape = RoundedCornerShape(spec.cornerRadius.dp)
                )
                is ModifierSpec.Alpha -> Modifier.alpha(alpha = spec.value)
                is ModifierSpec.Rotation -> Modifier.rotate(degrees = spec.degrees)
                is ModifierSpec.Scale -> Modifier.scale(scale = spec.value)
                is ModifierSpec.FillMaxSize -> Modifier.fillMaxSize(fraction = spec.fraction)
                is ModifierSpec.FillMaxWidth -> Modifier.fillMaxWidth()
                is ModifierSpec.FillMaxHeight -> Modifier.fillMaxHeight()
                is ModifierSpec.WrapContentSize -> Modifier.wrapContentSize()
                is ModifierSpec.Width -> Modifier.width(spec.dp.dp)
                is ModifierSpec.Height -> Modifier.height(spec.dp.dp)
                is ModifierSpec.Size -> Modifier.size(width = spec.width.dp, height = spec.height.dp)
                is ModifierSpec.AspectRatio -> Modifier.aspectRatio(ratio = spec.ratio)
                is ModifierSpec.Offset -> Modifier.offset(x = spec.x.dp, y = spec.y.dp)
                is ModifierSpec.Weight -> Modifier
                is ModifierSpec.Align -> Modifier
                is ModifierSpec.ZIndex -> Modifier.zIndex(spec.value)
                is ModifierSpec.Clickable -> Modifier.clickable(enabled = spec.enabled) { }
            }
        )
    }
}

/** Parses a "#AARRGGBB" or "RRGGBB"-style hex string into a [Color]. */
private fun parseHex(hex: String): Color {
    return try {
        Color(color = hex.removePrefix(prefix = "#").toLong(radix = 16))
    } catch (_: NumberFormatException) {
        Color.Transparent
    }
}

private fun CanvasNode.textOrDefault(default: String): String {
    val property = property(key = "text")
    return if (property is ComponentProperty.Text) property.value else default
}

private fun resolveVerticalArrangement(config: LayoutConfig): Arrangement.Vertical {
    if (config.spacing > 0f) return Arrangement.spacedBy(space = config.spacing.dp)
    return when (config.arrangement) {
        LayoutArrangement.START -> Arrangement.Top
        LayoutArrangement.END -> Arrangement.Bottom
        else -> resolveSharedArrangement(config.arrangement)
    }
}

private fun resolveHorizontalArrangement(config: LayoutConfig): Arrangement.Horizontal {
    if (config.spacing > 0f) return Arrangement.spacedBy(space = config.spacing.dp)
    return when (config.arrangement) {
        LayoutArrangement.START -> Arrangement.Start
        LayoutArrangement.END -> Arrangement.End
        else -> resolveSharedArrangement(config.arrangement)
    }
}

private fun resolveSharedArrangement(arrangement: LayoutArrangement): Arrangement.HorizontalOrVertical {
    return when (arrangement) {
        LayoutArrangement.CENTER -> Arrangement.Center
        LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
        LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
        LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
        else -> Arrangement.Center
    }
}

private fun resolveHorizontalAlignment(config: LayoutConfig): Alignment.Horizontal {
    return when (config.horizontalAlignment) {
        HorizontalAlignment.START -> Alignment.Start
        HorizontalAlignment.CENTER_HORIZONTALLY -> Alignment.CenterHorizontally
        HorizontalAlignment.END -> Alignment.End
    }
}

private fun resolveVerticalAlignment(config: LayoutConfig): Alignment.Vertical {
    return when (config.verticalAlignment) {
        VerticalAlignment.TOP -> Alignment.Top
        VerticalAlignment.CENTER_VERTICALLY -> Alignment.CenterVertically
        VerticalAlignment.BOTTOM -> Alignment.Bottom
    }
}

private fun resolveContentAlignment(config: LayoutConfig): Alignment {
    return when (config.boxAlignment) {
        BoxAlignment.TOP_START -> Alignment.TopStart
        BoxAlignment.TOP_CENTER -> Alignment.TopCenter
        BoxAlignment.TOP_END -> Alignment.TopEnd
        BoxAlignment.CENTER_START -> Alignment.CenterStart
        BoxAlignment.CENTER -> Alignment.Center
        BoxAlignment.CENTER_END -> Alignment.CenterEnd
        BoxAlignment.BOTTOM_START -> Alignment.BottomStart
        BoxAlignment.BOTTOM_CENTER -> Alignment.BottomCenter
        BoxAlignment.BOTTOM_END -> Alignment.BottomEnd
    }
}

private fun AlignTarget.toBoxAlignment(): Alignment = when (this) {
    AlignTarget.TOP_START -> Alignment.TopStart
    AlignTarget.TOP_CENTER -> Alignment.TopCenter
    AlignTarget.TOP_END -> Alignment.TopEnd
    AlignTarget.CENTER_START -> Alignment.CenterStart
    AlignTarget.CENTER -> Alignment.Center
    AlignTarget.CENTER_END -> Alignment.CenterEnd
    AlignTarget.BOTTOM_START -> Alignment.BottomStart
    AlignTarget.BOTTOM_CENTER -> Alignment.BottomCenter
    AlignTarget.BOTTOM_END -> Alignment.BottomEnd
    AlignTarget.START -> Alignment.CenterStart
    AlignTarget.CENTER_HORIZONTALLY -> Alignment.Center
    AlignTarget.END -> Alignment.CenterEnd
    AlignTarget.TOP -> Alignment.TopCenter
    AlignTarget.CENTER_VERTICALLY -> Alignment.Center
    AlignTarget.BOTTOM -> Alignment.BottomCenter
}

private fun AlignTarget.toColumnAlignment(): Alignment.Horizontal = when (this) {
    AlignTarget.START, AlignTarget.TOP_START, AlignTarget.CENTER_START, AlignTarget.BOTTOM_START -> Alignment.Start
    AlignTarget.CENTER_HORIZONTALLY, AlignTarget.TOP_CENTER, AlignTarget.CENTER, AlignTarget.BOTTOM_CENTER -> Alignment.CenterHorizontally
    AlignTarget.END, AlignTarget.TOP_END, AlignTarget.CENTER_END, AlignTarget.BOTTOM_END -> Alignment.End
    else -> Alignment.CenterHorizontally
}

private fun AlignTarget.toRowAlignment(): Alignment.Vertical = when (this) {
    AlignTarget.TOP, AlignTarget.TOP_START, AlignTarget.TOP_CENTER, AlignTarget.TOP_END -> Alignment.Top
    AlignTarget.CENTER_VERTICALLY, AlignTarget.CENTER_START, AlignTarget.CENTER, AlignTarget.CENTER_END -> Alignment.CenterVertically
    AlignTarget.BOTTOM, AlignTarget.BOTTOM_START, AlignTarget.BOTTOM_CENTER, AlignTarget.BOTTOM_END -> Alignment.Bottom
    else -> Alignment.CenterVertically
}