@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.asset.DefaultAssetRepository
import dev.chandradsl.m3ecanvas.editor.asset.drawSampleAsset
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
    if (!node.isVisible) return
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

        ComponentType.BOX_WITH_CONSTRAINTS -> BoxWithConstraints(
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
        ComponentType.LAZY_HORIZONTAL_GRID -> RenderLazyHorizontalGrid(node = node, controller = controller)
        ComponentType.LAZY_VERTICAL_STAGGERED_GRID -> RenderLazyVerticalStaggeredGrid(node = node, controller = controller)
        ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID -> RenderLazyHorizontalStaggeredGrid(node = node, controller = controller)
        ComponentType.HORIZONTAL_PAGER -> RenderHorizontalPager(node = node, controller = controller)
        ComponentType.CAROUSEL -> RenderCarousel(node = node, controller = controller)
        ComponentType.FLOW_ROW -> RenderFlowRow(node = node, controller = controller)
        ComponentType.FLOW_COLUMN -> RenderFlowColumn(node = node, controller = controller)
        ComponentType.SPACER -> RenderSpacer(node = node)
        ComponentType.SURFACE -> RenderSurface(node = node, controller = controller)
        ComponentType.SCAFFOLD -> RenderScaffold(node = node, controller = controller)
        ComponentType.BOTTOM_SHEET_SCAFFOLD -> RenderBottomSheetScaffold(node = node, controller = controller)

        // Action
        ComponentType.BUTTON -> RenderButton(node = node, controller = controller)
        ComponentType.ICON_BUTTON -> RenderIconButton(node = node, controller = controller)
        ComponentType.FAB -> RenderFab(node = node)
        ComponentType.EXTENDED_FAB -> RenderExtendedFab(node = node)
        ComponentType.SEGMENTED_BUTTON -> RenderSegmentedButton(node = node, controller = controller)
        ComponentType.SPLIT_BUTTON -> RenderSplitButton(node = node, controller = controller)
        ComponentType.BUTTON_GROUP -> RenderButtonGroup(node = node, controller = controller)
        ComponentType.TOGGLE_BUTTON -> RenderToggleButton(node = node)

        // Containment
        ComponentType.CARD -> RenderCard(node = node, controller = controller)
        ComponentType.ELEVATED_CARD -> RenderCard(node = node, controller = controller, forcedVariant = MaterialVariant.Card.ELEVATED)
        ComponentType.OUTLINED_CARD -> RenderCard(node = node, controller = controller, forcedVariant = MaterialVariant.Card.OUTLINED)
        ComponentType.LIST_ITEM -> RenderListItem(node = node, controller = controller)
        ComponentType.MODAL_BOTTOM_SHEET -> RenderBottomSheet(node = node, controller = controller)
        ComponentType.ALERT_DIALOG -> RenderDialog(node = node, controller = controller)
        ComponentType.BASIC_ALERT_DIALOG -> RenderBasicDialog(node = node, controller = controller)

        // Communication
        ComponentType.SNACKBAR -> RenderSnackbar(node = node)
        ComponentType.SNACKBAR_HOST -> RenderSnackbarHost(node = node)
        ComponentType.BADGE -> RenderBadge(node = node)
        ComponentType.BADGED_BOX -> RenderBadgedBox(node = node, controller = controller)
        ComponentType.TOOLTIP -> RenderTooltip(node = node, controller = controller)
        ComponentType.PROGRESS_INDICATOR -> RenderProgressIndicator(node = node)
        ComponentType.LOADING_INDICATOR -> RenderLoadingIndicator(node = node)

        // Navigation
        ComponentType.TOP_APP_BAR -> RenderTopAppBar(node = node, controller = controller)
        ComponentType.NAVIGATION_BAR -> RenderNavigationBar(node = node, controller = controller)
        ComponentType.NAVIGATION_BAR_ITEM -> RenderNavigationBarItem(node = node)
        ComponentType.BOTTOM_APP_BAR -> RenderBottomAppBar(node = node, controller = controller)
        ComponentType.NAVIGATION_RAIL -> RenderNavigationRail(node = node, controller = controller)
        ComponentType.NAVIGATION_RAIL_ITEM -> RenderNavigationRailItem(node = node)
        ComponentType.NAVIGATION_DRAWER -> RenderNavigationDrawer(node = node, controller = controller)
        ComponentType.TABS -> RenderTabs(node = node, controller = controller)
        ComponentType.TAB -> RenderTab(node = node)
        ComponentType.SEARCH -> RenderSearch(node = node)

        // Selection
        ComponentType.CHECKBOX -> RenderCheckbox(node = node, controller = controller)
        ComponentType.RADIO_BUTTON -> RenderRadioButton(node = node, controller = controller)
        ComponentType.SWITCH -> RenderSwitch(node = node, controller = controller)
        ComponentType.SLIDER -> RenderSlider(node = node, controller = controller)
        ComponentType.RANGE_SLIDER -> RenderRangeSlider(node = node, controller = controller)
        ComponentType.CHIPS -> RenderChip(node = node, controller = controller)
        ComponentType.DATE_PICKER -> RenderDatePicker(node = node)
        ComponentType.TIME_PICKER -> RenderTimePicker(node = node)
        ComponentType.MENUS -> RenderMenu(node = node, controller = controller)
        ComponentType.DROPDOWN_MENU_ITEM -> RenderDropdownMenuItem(node = node)

        // Text input
        ComponentType.TEXT_FIELD -> RenderTextField(node = node, controller = controller)

        // Typography
        ComponentType.TEXT -> RenderText(node = node)

        // Graphics
        ComponentType.ICON -> RenderIcon(node = node)
        ComponentType.IMAGE -> RenderImage(node = node, controller = controller)
        ComponentType.HORIZONTAL_DIVIDER -> RenderHorizontalDivider(node = node)
        ComponentType.VERTICAL_DIVIDER -> RenderVerticalDivider(node = node)
    }
}

private fun handleInteractiveButtonClick(node: CanvasNode, controller: EditorController) {
    if (controller.state.isInteractiveMode) {
        var ancestor = controller.findParent(node.id)
        while (ancestor != null) {
            if (ancestor.type.isOverlay()) {
                controller.dismissOverlay(ancestor.id)
                break
            }
            ancestor = controller.findParent(ancestor.id)
        }
    }
}

@Composable
private fun RenderButton(node: CanvasNode, controller: EditorController) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Button
        ?: MaterialVariant.Button.FILLED
    val text = node.textOrDefault(default = "Button")
    val onClick = { handleInteractiveButtonClick(node, controller) }
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
private fun RenderCard(
    node: CanvasNode,
    controller: EditorController,
    forcedVariant: MaterialVariant.Card? = null
) {
    val variant = forcedVariant
        ?: (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Card
        ?: MaterialVariant.Card.FILLED
    val modifier = node.modifiers.toModifier().fillMaxSize()

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = node.layoutConfig.padding.dp),
            verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig),
            horizontalAlignment = resolveHorizontalAlignment(config = node.layoutConfig)
        ) {
            if (node.children.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Card (${node.name})\nDrop components here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                node.children.forEach { child ->
                    val weightSpec = child.modifiers.filterIsInstance<ModifierSpec.Weight>().firstOrNull()
                    val scopeModifier = if (weightSpec != null) {
                        Modifier.weight(weight = weightSpec.weight)
                    } else {
                        Modifier
                    }
                    ContainerChild(child = child, controller = controller, modifier = scopeModifier)
                }
            }
        }
    }

    when (variant) {
        MaterialVariant.Card.FILLED -> Card(modifier = modifier, content = cardContent)
        MaterialVariant.Card.ELEVATED -> ElevatedCard(modifier = modifier, content = cardContent)
        MaterialVariant.Card.OUTLINED -> OutlinedCard(modifier = modifier, content = cardContent)
    }
}

@Composable
private fun RenderIconButton(node: CanvasNode, controller: EditorController) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.IconButton
        ?: MaterialVariant.IconButton.STANDARD
    val iconName = node.iconProperty(key = "icon", default = "Add")
    val onClick = { handleInteractiveButtonClick(node, controller) }
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
private fun RenderChip(node: CanvasNode, controller: EditorController) {
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Chip
        ?: MaterialVariant.Chip.ASSIST
    val label = @Composable { Text(text = node.textOrDefault(default = "Chip")) }
    val initialSelected = node.booleanProperty(key = "selected", default = true)
    var selected by remember(node.id, controller.state.isInteractiveMode) { mutableStateOf(initialSelected) }
    val isInteractive = controller.state.isInteractiveMode
    val onClick = {
        if (isInteractive) {
            selected = !selected
        }
    }

    when (variant) {
        MaterialVariant.Chip.ASSIST -> AssistChip(onClick = onClick, label = label)
        MaterialVariant.Chip.FILTER -> FilterChip(selected = if (isInteractive) selected else false, onClick = onClick, label = label)
        MaterialVariant.Chip.INPUT -> InputChip(selected = if (isInteractive) selected else false, onClick = onClick, label = label)
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
private fun RenderBottomSheetScaffold(node: CanvasNode, controller: EditorController) {
    val topBarChild = node.childInSlot(SlotRole.TOP_BAR)
    val sheetChild = node.childInSlot(SlotRole.SHEET_CONTENT)
    val snackbarChild = node.childInSlot(SlotRole.SNACKBAR)
    val contentChildren = node.childrenInSlot(SlotRole.CONTENT).ifEmpty {
        node.children.filter { it.slot != SlotRole.TOP_BAR && it.slot != SlotRole.SHEET_CONTENT && it.slot != SlotRole.SNACKBAR }
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(),
        topBar = if (topBarChild != null) {
            { SlotContainer(child = topBarChild, controller = controller) }
        } else null,
        snackbarHost = if (snackbarChild != null) {
            { SlotContainer(child = snackbarChild, controller = controller) }
        } else {
            { SnackbarHost(hostState = remember { SnackbarHostState() }) }
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (sheetChild != null) {
                    SlotContainer(child = sheetChild, controller = controller)
                } else {
                    Text(
                        text = "Bottom Sheet Content",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sheet content slot preview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        sheetPeekHeight = 64.dp,
        modifier = node.modifiers.toModifier().fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            contentChildren.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderTopAppBar(node: CanvasNode, controller: EditorController) {
    val titleText = (node.property("title") as? ComponentProperty.Text)?.value
        ?: (node.property("text") as? ComponentProperty.Text)?.value
        ?: node.name

    val titleChild = node.childInSlot(SlotRole.TITLE)
    val navChild = node.childInSlot(SlotRole.NAVIGATION_ICON)
    val actionChildren = node.childrenInSlot(SlotRole.ACTIONS)

    TopAppBar(
        title = {
            if (titleChild != null) {
                ContainerChild(child = titleChild, controller = controller)
            } else {
                Text(text = titleText, maxLines = 1)
            }
        },
        navigationIcon = if (navChild != null) {
            { ContainerChild(child = navChild, controller = controller) }
        } else {
            val navIconName = (node.property("navigationIcon") as? ComponentProperty.Icon)?.iconName
            if (!navIconName.isNullOrBlank()) {
                {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = resolveMaterialIcon(navIconName),
                            contentDescription = "Navigation"
                        )
                    }
                }
            } else {
                {}
            }
        },
        actions = {
            if (actionChildren.isNotEmpty()) {
                actionChildren.forEach { child ->
                    ContainerChild(child = child, controller = controller)
                }
            }
        },
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderNavigationBar(node: CanvasNode, controller: EditorController) {
    val isInteractive = controller.state.isInteractiveMode
    var selectedIndex by remember(node.id, isInteractive) { mutableIntStateOf(0) }
    NavigationBar(
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        if (node.children.isNotEmpty()) {
            node.children.forEachIndexed { index, child ->
                if (child.type == ComponentType.NAVIGATION_BAR_ITEM) {
                    RenderNavigationBarItem(
                        node = child,
                        isSelected = if (isInteractive) index == selectedIndex else child.booleanProperty(key = "selected", default = index == 0),
                        onClick = { if (isInteractive) selectedIndex = index }
                    )
                } else {
                    ContainerChild(child = child, controller = controller)
                }
            }
        } else {
            EditorSlotAffordance(
                label = "NavigationBar: Add NavigationBarItem children",
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}

@Composable
private fun RowScope.RenderNavigationBarItem(
    node: CanvasNode,
    isSelected: Boolean = node.booleanProperty(key = "selected", default = true),
    onClick: () -> Unit = {}
) {
    val label = (node.property("label") as? ComponentProperty.Text)?.value
        ?: (node.property("text") as? ComponentProperty.Text)?.value
        ?: node.name
    val iconName = node.iconProperty(key = "icon", default = "Home")
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = { Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = label) },
        label = { Text(text = label) }
    )
}

@Composable
private fun RenderNavigationBarItem(node: CanvasNode) {
    Row {
        RenderNavigationBarItem(node = node)
    }
}

@Composable
private fun RenderFab(node: CanvasNode) {
    val iconName = node.iconProperty(key = "icon", default = "Add")
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.FloatingActionButton
        ?: MaterialVariant.FloatingActionButton.SECONDARY
    val fabSize = ((node.property(key = "fabSize") as? ComponentProperty.Variant)?.value as? MaterialVariant.FabSize)
        ?: ((node.property(key = "size") as? ComponentProperty.Variant)?.value as? MaterialVariant.FabSize)
        ?: MaterialVariant.FabSize.MEDIUM

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
    val iconContent: @Composable () -> Unit = {
        Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = "Add")
    }

    when (fabSize) {
        MaterialVariant.FabSize.SMALL -> SmallFloatingActionButton(
            onClick = {},
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = node.modifiers.toModifier(),
            content = iconContent
        )
        MaterialVariant.FabSize.MEDIUM -> FloatingActionButton(
            onClick = {},
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = node.modifiers.toModifier(),
            content = iconContent
        )
        MaterialVariant.FabSize.LARGE -> LargeFloatingActionButton(
            onClick = {},
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = node.modifiers.toModifier(),
            content = iconContent
        )
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
    val isInteractive = controller.state.isInteractiveMode
    val isSelected = !isInteractive && controller.state.isNodeSelected(nodeId = child.id)
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
            .then(
                if (!isInteractive) {
                    Modifier
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
                } else Modifier
            )
    ) {
        CanvasNodeRenderer(node = child, controller = controller)
        if (!isInteractive && !child.type.isContainer && child.children.isEmpty()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .selectOnPress(nodeId = child.id, controller = controller, focusRequester = focusRequester)
            )
        }
    }
}

@Composable
private fun ContainerChild(
    child: CanvasNode,
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    if (!child.isVisible) return
    val canvasCoords = LocalCanvasCoordinates.current
    val fillMaxSize = child.modifiers.filterIsInstance<ModifierSpec.FillMaxSize>().firstOrNull()
    val fillMaxWidth = child.modifiers.filterIsInstance<ModifierSpec.FillMaxWidth>().firstOrNull()
    val fillMaxHeight = child.modifiers.filterIsInstance<ModifierSpec.FillMaxHeight>().firstOrNull()
    val widthSpec = child.modifiers.filterIsInstance<ModifierSpec.Width>().firstOrNull()
    val heightSpec = child.modifiers.filterIsInstance<ModifierSpec.Height>().firstOrNull()
    val sizeSpec = child.modifiers.filterIsInstance<ModifierSpec.Size>().firstOrNull()
    val wrapContent = child.modifiers.filterIsInstance<ModifierSpec.WrapContentSize>().firstOrNull()

    // Compose layout semantics: intrinsic content-driven sizing unless explicitly overridden by modifier
    val sizeModifier = when {
        fillMaxSize != null -> Modifier.fillMaxSize(fraction = fillMaxSize.fraction)
        fillMaxWidth != null && fillMaxHeight != null -> Modifier.fillMaxWidth().fillMaxHeight()
        fillMaxWidth != null && heightSpec != null -> Modifier.fillMaxWidth().height(heightSpec.dp.dp)
        fillMaxWidth != null -> Modifier.fillMaxWidth()
        fillMaxHeight != null && widthSpec != null -> Modifier.fillMaxHeight().width(widthSpec.dp.dp)
        fillMaxHeight != null -> Modifier.fillMaxHeight()
        sizeSpec != null -> Modifier.size(width = sizeSpec.width.dp, height = sizeSpec.height.dp)
        widthSpec != null && heightSpec != null -> Modifier.size(width = widthSpec.dp.dp, height = heightSpec.dp.dp)
        widthSpec != null -> Modifier.width(widthSpec.dp.dp)
        heightSpec != null -> Modifier.height(heightSpec.dp.dp)
        wrapContent != null -> Modifier.wrapContentSize()
        else -> Modifier
    }

    DisposableEffect(child.id) {
        onDispose {
            controller.geometryStore.removeBounds(child.id)
        }
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    val root = canvasCoords
                    val boundsInCanvas = if (root != null && root.isAttached) {
                        root.localBoundingBoxOf(coords, clipBounds = false)
                    } else {
                        coords.boundsInRoot()
                    }
                    controller.geometryStore.updateBounds(
                        child.id,
                        RenderedBounds(
                            boundsInCanvas = boundsInCanvas,
                            boundsInWindow = coords.boundsInWindow(),
                            sizePx = coords.size
                        )
                    )
                }
            }
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
    if (controller.state.isInteractiveMode) return this
    val node = controller.state.project.findNode(nodeId)
    if (node == null || !node.isVisible || node.isLocked) return this
    val focusManager = LocalFocusManager.current
    return this.pointerInput(nodeId) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = true)
            down.consume()
            focusManager.clearFocus()
            val isMulti = currentEvent.keyboardModifiers.isShiftPressed ||
                    currentEvent.keyboardModifiers.isCtrlPressed ||
                    currentEvent.keyboardModifiers.isMetaPressed
            if (isMulti) {
                controller.toggleSelectNode(nodeId = nodeId)
            } else {
                controller.selectNode(nodeId = nodeId)
            }
            try {
                focusRequester?.requestFocus()
            } catch (_: Throwable) {}
        }
    }
}

@Composable
private fun RenderTextField(node: CanvasNode, controller: EditorController) {
    val initialText = node.textOrDefault(default = "")
    var text by remember(node.id, controller.state.isInteractiveMode) { mutableStateOf(initialText) }
    val isInteractive = controller.state.isInteractiveMode
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.TextField
        ?: MaterialVariant.TextField.OUTLINED
    val modifier = node.modifiers.toModifier().fillMaxSize()

    when (variant) {
        MaterialVariant.TextField.OUTLINED -> OutlinedTextField(
            value = if (isInteractive) text else initialText,
            onValueChange = { if (isInteractive) text = it },
            label = { Text(node.name) },
            placeholder = { Text("Enter text...") },
            readOnly = !isInteractive,
            modifier = modifier
        )
        MaterialVariant.TextField.FILLED -> TextField(
            value = if (isInteractive) text else initialText,
            onValueChange = { if (isInteractive) text = it },
            label = { Text(node.name) },
            placeholder = { Text("Enter text...") },
            readOnly = !isInteractive,
            modifier = modifier
        )
    }
}

@Composable
private fun RenderSegmentedButton(node: CanvasNode, controller: EditorController) {
    val isInteractive = controller.state.isInteractiveMode
    var selectedIndex by remember(node.id, isInteractive) { mutableIntStateOf(0) }
    SingleChoiceSegmentedButtonRow(
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        if (node.children.isNotEmpty()) {
            node.children.forEachIndexed { index, child ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = node.children.size),
                    onClick = { if (isInteractive) selectedIndex = index },
                    selected = if (isInteractive) index == selectedIndex else child.booleanProperty(key = "selected", default = index == 0)
                ) {
                    ContainerChild(child = child, controller = controller)
                }
            }
        } else {
            EditorSlotAffordance(
                label = "SegmentedButton: Add item children",
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )
        }
    }
}

@Composable
private fun RenderSplitButton(node: CanvasNode, controller: EditorController) {
    if (node.children.isNotEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = node.modifiers.toModifier().wrapContentSize()
        ) {
            node.children.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        }
    } else {
        EditorSlotAffordance(
            label = "SplitButton: Add Primary/Trailing Actions",
            modifier = node.modifiers.toModifier().wrapContentSize()
        )
    }
}

@Composable
private fun RenderButtonGroup(node: CanvasNode, controller: EditorController) {
    if (node.children.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = node.modifiers.toModifier().wrapContentSize()
        ) {
            node.children.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        }
    } else {
        EditorSlotAffordance(
            label = "ButtonGroup: Add Buttons",
            modifier = node.modifiers.toModifier().wrapContentSize()
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RenderToggleButton(node: CanvasNode) {
    val text = node.textOrDefault(default = "Toggle")
    val iconName = node.iconProperty(key = "icon", default = "")
    val checked = node.booleanProperty(key = "checked", default = true)
    val variant = (node.property(key = "variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.ToggleButton
        ?: MaterialVariant.ToggleButton.FILLED

    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconName.isNotEmpty()) {
                Icon(
                    imageVector = resolveMaterialIcon(iconName),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            }
            Text(text = text)
        }
    }

    when (variant) {
        MaterialVariant.ToggleButton.FILLED -> ToggleButton(
            checked = checked,
            onCheckedChange = {},
            modifier = node.modifiers.toModifier(),
            content = { content() }
        )
        MaterialVariant.ToggleButton.ELEVATED -> ElevatedToggleButton(
            checked = checked,
            onCheckedChange = {},
            modifier = node.modifiers.toModifier(),
            content = { content() }
        )
        MaterialVariant.ToggleButton.TONAL -> TonalToggleButton(
            checked = checked,
            onCheckedChange = {},
            modifier = node.modifiers.toModifier(),
            content = { content() }
        )
        MaterialVariant.ToggleButton.OUTLINED -> OutlinedToggleButton(
            checked = checked,
            onCheckedChange = {},
            modifier = node.modifiers.toModifier(),
            content = { content() }
        )
    }
}

@Composable
private fun RenderListItem(node: CanvasNode, controller: EditorController) {
    val headlineChild = node.childInSlot(SlotRole.HEADLINE)
    val supportingChild = node.childInSlot(SlotRole.SUPPORTING)
    val leadingChild = node.childInSlot(SlotRole.LEADING)
    val trailingChild = node.childInSlot(SlotRole.TRAILING)
    val overlineChild = node.childInSlot(SlotRole.OVERLINE)
    val headlineText = (node.property("text") as? ComponentProperty.Text)?.value
        ?: (node.property("headline") as? ComponentProperty.Text)?.value

    val headlineComposable: @Composable () -> Unit = {
        if (headlineChild != null) {
            ContainerChild(child = headlineChild, controller = controller)
        } else if (!headlineText.isNullOrBlank()) {
            Text(text = headlineText)
        } else {
            EditorSlotAffordance(label = "Headline")
        }
    }

    val supportingComposable: (@Composable () -> Unit)? = if (supportingChild != null) {
        { ContainerChild(child = supportingChild, controller = controller) }
    } else {
        val supportingText = (node.property("supporting") as? ComponentProperty.Text)?.value
        if (!supportingText.isNullOrBlank()) {
            { Text(text = supportingText) }
        } else null
    }

    val leadingComposable: (@Composable () -> Unit)? = if (leadingChild != null) {
        { ContainerChild(child = leadingChild, controller = controller) }
    } else {
        val leadingIcon = (node.property("leadingIcon") as? ComponentProperty.Icon)?.iconName
        if (!leadingIcon.isNullOrBlank()) {
            { Icon(imageVector = resolveMaterialIcon(leadingIcon), contentDescription = null) }
        } else null
    }

    val trailingComposable: (@Composable () -> Unit)? = if (trailingChild != null) {
        { ContainerChild(child = trailingChild, controller = controller) }
    } else {
        val trailingText = (node.property("trailing") as? ComponentProperty.Text)?.value
        if (!trailingText.isNullOrBlank()) {
            { Text(text = trailingText, style = MaterialTheme.typography.labelSmall) }
        } else null
    }

    val overlineComposable: (@Composable () -> Unit)? = if (overlineChild != null) {
        { ContainerChild(child = overlineChild, controller = controller) }
    } else {
        val overlineText = (node.property("overline") as? ComponentProperty.Text)?.value
        if (!overlineText.isNullOrBlank()) {
            { Text(text = overlineText) }
        } else null
    }

    ListItem(
        headlineContent = headlineComposable,
        supportingContent = supportingComposable,
        leadingContent = leadingComposable,
        trailingContent = trailingComposable,
        overlineContent = overlineComposable,
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = node.modifiers.toModifier().fillMaxWidth().clip(RoundedCornerShape(8.dp))
    )
}

@Composable
private fun RenderBottomSheet(node: CanvasNode, controller: EditorController) {
    ModalBottomSheet(
        onDismissRequest = {
            if (controller.state.isInteractiveMode) {
                controller.dismissOverlay(node.id)
            }
        },
        modifier = node.modifiers.toModifier()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (node.children.isNotEmpty()) {
                node.children.forEach { child ->
                    ContainerChild(child = child, controller = controller)
                }
            } else {
                EditorSlotAffordance(
                    label = "ModalBottomSheet: Add Content",
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        }
    }
}

@Composable
private fun RenderDialog(node: CanvasNode, controller: EditorController) {
    val iconChild = node.childInSlot(SlotRole.ICON)
    val titleChild = node.childInSlot(SlotRole.TITLE)
    val confirmChild = node.childInSlot(SlotRole.CONFIRM_BUTTON)
    val dismissChild = node.childInSlot(SlotRole.DISMISS_BUTTON)
    val contentChildren = node.children.filter {
        it.slot == null || it.slot == SlotRole.CONTENT || it.slot == SlotRole.SUPPORTING
    }
    val titleText = (node.property("title") as? ComponentProperty.Text)?.value
        ?: (node.property("text") as? ComponentProperty.Text)?.value

    AlertDialog(
        onDismissRequest = {
            if (controller.state.isInteractiveMode) {
                controller.dismissOverlay(node.id)
            }
        },
        confirmButton = {
            if (confirmChild != null) {
                ContainerChild(child = confirmChild, controller = controller)
            } else {
                EditorSlotAffordance(label = "Confirm Button")
            }
        },
        dismissButton = if (dismissChild != null) {
            { ContainerChild(child = dismissChild, controller = controller) }
        } else null,
        icon = if (iconChild != null) {
            { ContainerChild(child = iconChild, controller = controller) }
        } else null,
        title = if (titleChild != null) {
            { ContainerChild(child = titleChild, controller = controller) }
        } else if (!titleText.isNullOrBlank()) {
            { Text(text = titleText) }
        } else null,
        text = if (contentChildren.isNotEmpty()) {
            {
                Column {
                    contentChildren.forEach { child ->
                        ContainerChild(child = child, controller = controller)
                    }
                }
            }
        } else null,
        modifier = node.modifiers.toModifier()
    )
}

@Composable
private fun RenderBasicDialog(node: CanvasNode, controller: EditorController) {
    BasicAlertDialog(
        onDismissRequest = {
            if (controller.state.isInteractiveMode) {
                controller.dismissOverlay(node.id)
            }
        },
        modifier = node.modifiers.toModifier()
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        ContainerChild(child = child, controller = controller)
                    }
                } else {
                    EditorSlotAffordance(
                        label = "BasicAlertDialog: Add Content",
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderSnackbar(node: CanvasNode) {
    val message = (node.property("text") as? ComponentProperty.Text)?.value ?: "Snackbar notification alert."
    Snackbar(
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        Text(text = message)
    }
}

@Composable
private fun RenderSnackbarHost(node: CanvasNode) {
    val hostState = remember { SnackbarHostState() }
    SnackbarHost(
        hostState = hostState,
        modifier = node.modifiers.toModifier().wrapContentSize()
    )
}

@Composable
private fun RenderBadge(node: CanvasNode) {
    val text = (node.property("text") as? ComponentProperty.Text)?.value
    BadgedBox(
        badge = {
            if (!text.isNullOrBlank()) {
                Badge { Text(text = text) }
            } else {
                Badge()
            }
        },
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        EditorSlotAffordance(label = "Badge Anchor", modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun RenderBadgedBox(node: CanvasNode, controller: EditorController) {
    val badgeChild = node.childInSlot(SlotRole.BADGE)
    val contentChild = node.childInSlot(SlotRole.CONTENT) ?: node.children.firstOrNull { it.slot != SlotRole.BADGE }

    BadgedBox(
        badge = {
            if (badgeChild != null) {
                ContainerChild(child = badgeChild, controller = controller)
            } else {
                val badgeText = (node.property("badge") as? ComponentProperty.Text)?.value
                if (!badgeText.isNullOrBlank()) {
                    Badge { Text(text = badgeText) }
                } else {
                    Badge()
                }
            }
        },
        modifier = node.modifiers.toModifier()
    ) {
        if (contentChild != null) {
            ContainerChild(child = contentChild, controller = controller)
        } else {
            EditorSlotAffordance(label = "Anchor Item", modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun RenderTooltip(node: CanvasNode, controller: EditorController) {
    val tooltipText = (node.property("text") as? ComponentProperty.Text)?.value ?: ""
    val tooltipState = rememberTooltipState()
    val contentChild = node.children.firstOrNull()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                if (tooltipText.isNotEmpty()) {
                    Text(text = tooltipText)
                } else {
                    EditorSlotAffordance(label = "Tooltip Text")
                }
            }
        },
        state = tooltipState,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        if (contentChild != null) {
            ContainerChild(child = contentChild, controller = controller)
        } else {
            EditorSlotAffordance(label = "Anchor Item", modifier = Modifier.size(48.dp))
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
private fun RenderNavigationRail(node: CanvasNode, controller: EditorController) {
    NavigationRail(
        modifier = node.modifiers.toModifier().fillMaxHeight().width(80.dp)
    ) {
        if (node.children.isNotEmpty()) {
            node.children.forEach { child ->
                if (child.type == ComponentType.NAVIGATION_RAIL_ITEM) {
                    RenderNavigationRailItem(node = child)
                } else {
                    ContainerChild(child = child, controller = controller)
                }
            }
        } else {
            EditorSlotAffordance(
                label = "NavigationRail: Add Items",
                modifier = Modifier.width(64.dp).height(48.dp)
            )
        }
    }
}

@Composable
private fun RenderNavigationRailItem(node: CanvasNode) {
    val label = (node.property("label") as? ComponentProperty.Text)?.value
        ?: (node.property("text") as? ComponentProperty.Text)?.value
        ?: node.name
    val iconName = node.iconProperty(key = "icon", default = "Home")
    val selected = node.booleanProperty(key = "selected", default = true)
    NavigationRailItem(
        selected = selected,
        onClick = {},
        icon = { Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = label) },
        label = { Text(text = label) }
    )
}

@Composable
private fun RenderNavigationDrawer(node: CanvasNode, controller: EditorController) {
    ModalDrawerSheet(
        modifier = node.modifiers.toModifier().fillMaxHeight().width(300.dp)
    ) {
        val title = (node.property("title") as? ComponentProperty.Text)?.value
            ?: (node.property("text") as? ComponentProperty.Text)?.value
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        }
        if (node.children.isNotEmpty()) {
            node.children.forEach { child ->
                ContainerChild(child = child, controller = controller)
            }
        } else {
            EditorSlotAffordance(
                label = "Drawer: Add drawer content",
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
        }
    }
}

@Composable
private fun RenderTabs(node: CanvasNode, controller: EditorController) {
    val isInteractive = controller.state.isInteractiveMode
    var selectedTab by remember(node.id, isInteractive) { mutableIntStateOf(0) }
    PrimaryTabRow(
        selectedTabIndex = selectedTab,
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {
        if (node.children.isNotEmpty()) {
            node.children.forEachIndexed { index, child ->
                if (child.type == ComponentType.TAB) {
                    RenderTab(
                        node = child,
                        isSelected = if (isInteractive) index == selectedTab else child.booleanProperty(key = "selected", default = index == 0),
                        onClick = { if (isInteractive) selectedTab = index }
                    )
                } else {
                    ContainerChild(child = child, controller = controller)
                }
            }
        } else {
            EditorSlotAffordance(
                label = "Tabs: Add Tab children",
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )
        }
    }
}

@Composable
private fun RenderTab(
    node: CanvasNode,
    isSelected: Boolean = node.booleanProperty(key = "selected", default = false),
    onClick: () -> Unit = {}
) {
    val text = node.textOrDefault(default = "Tab")
    val iconName = node.iconProperty(key = "icon", default = "Dashboard")
    Tab(
        selected = isSelected,
        onClick = onClick,
        text = { Text(text) },
        icon = { Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = null) }
    )
}

@Composable
private fun RenderSearch(node: CanvasNode) {
    val query = node.textOrDefault(default = "")
    val placeholder = (node.property("placeholder") as? ComponentProperty.Text)?.value ?: "Search..."
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = {},
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(placeholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                }
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = node.modifiers.toModifier().fillMaxWidth()
    ) {}
}

@Composable
private fun RenderCheckbox(node: CanvasNode, controller: EditorController) {
    val initialChecked = node.booleanProperty(key = "checked", default = true)
    var checked by remember(node.id, controller.state.isInteractiveMode) { mutableStateOf(initialChecked) }
    val isInteractive = controller.state.isInteractiveMode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        Checkbox(
            checked = if (isInteractive) checked else initialChecked,
            onCheckedChange = { if (isInteractive) checked = it }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.textOrDefault(default = "Checkbox Option"),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RenderRadioButton(node: CanvasNode, controller: EditorController) {
    val initialSelected = node.booleanProperty(key = "selected", default = true)
    var selected by remember(node.id, controller.state.isInteractiveMode) { mutableStateOf(initialSelected) }
    val isInteractive = controller.state.isInteractiveMode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        RadioButton(
            selected = if (isInteractive) selected else initialSelected,
            onClick = { if (isInteractive) selected = !selected }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.textOrDefault(default = "Radio Option"),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RenderSwitch(node: CanvasNode, controller: EditorController) {
    val initialChecked = node.booleanProperty(key = "checked", default = true)
    var checked by remember(node.id, controller.state.isInteractiveMode) { mutableStateOf(initialChecked) }
    val isInteractive = controller.state.isInteractiveMode
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
        Switch(
            checked = if (isInteractive) checked else initialChecked,
            onCheckedChange = { if (isInteractive) checked = it }
        )
    }
}

@Composable
private fun RenderSlider(node: CanvasNode, controller: EditorController) {
    val initialValue = node.numericProperty(key = "value", default = 0.6f).coerceIn(0f, 1f)
    var sliderValue by remember(node.id, controller.state.isInteractiveMode) { mutableFloatStateOf(initialValue) }
    val isInteractive = controller.state.isInteractiveMode
    Slider(
        value = if (isInteractive) sliderValue else initialValue,
        onValueChange = { if (isInteractive) sliderValue = it },
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderDatePicker(node: CanvasNode) {
    val datePickerState = rememberDatePickerState()
    DatePicker(
        state = datePickerState,
        modifier = node.modifiers.toModifier()
    )
}

@Composable
private fun RenderTimePicker(node: CanvasNode) {
    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 30)
    TimePicker(
        state = timePickerState,
        modifier = node.modifiers.toModifier()
    )
}

@Composable
private fun RenderMenu(node: CanvasNode, controller: EditorController) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = node.modifiers.toModifier().wrapContentSize()
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = 4.dp)) {
            if (node.children.isNotEmpty()) {
                node.children.forEach { child ->
                    if (child.type == ComponentType.DROPDOWN_MENU_ITEM) {
                        RenderDropdownMenuItem(node = child)
                    } else {
                        ContainerChild(child = child, controller = controller)
                    }
                }
            } else {
                EditorSlotAffordance(
                    label = "Menu: Add DropdownMenuItem children",
                    modifier = Modifier.width(180.dp).height(48.dp)
                )
            }
        }
    }
}

@Composable
private fun RenderDropdownMenuItem(node: CanvasNode) {
    val text = node.textOrDefault(default = "Item")
    val iconName = node.iconProperty(key = "icon", default = "")
    DropdownMenuItem(
        text = { Text(text) },
        onClick = {},
        leadingIcon = if (iconName.isNotEmpty()) {
            { Icon(imageVector = resolveMaterialIcon(iconName), contentDescription = null) }
        } else null
    )
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
    val iconRef = node.iconReference("icon", default = IconReference.material("Favorite"))
    Icon(
        imageVector = resolveMaterialIcon(reference = iconRef),
        contentDescription = iconRef.name,
        tint = MaterialTheme.colorScheme.primary,
        modifier = node.modifiers.toModifier().fillMaxSize()
    )
}

@Composable
private fun RenderImage(node: CanvasNode, controller: EditorController) {
    val configuredAsset = node.assetReference()
    val rawAssetId = node.assetId()
    val assetId = configuredAsset?.assetId?.takeIf { it.isNotBlank() }
        ?: rawAssetId.takeIf { it.isNotBlank() }
        ?: "sample_hero_landscape"

    val projectAsset = controller.state.project.findAsset(assetId)
    val resolvedAsset = configuredAsset ?: projectAsset ?: DefaultAssetRepository.default.getAsset(assetId)

    // Section 42: Graceful recoverable UI when an asset is missing
    if (resolvedAsset == null && (assetId.startsWith("missing_") || rawAssetId.isNotBlank())) {
        RenderMissingAsset(assetId = assetId, node = node)
        return
    }

    val alignmentType = configuredAsset?.alignment ?: resolvedAsset?.alignment ?: AssetAlignment.CENTER
    val contentAlignment = when (alignmentType) {
        AssetAlignment.TOP_START -> Alignment.TopStart
        AssetAlignment.TOP_CENTER -> Alignment.TopCenter
        AssetAlignment.TOP_END -> Alignment.TopEnd
        AssetAlignment.CENTER_START -> Alignment.CenterStart
        AssetAlignment.CENTER -> Alignment.Center
        AssetAlignment.CENTER_END -> Alignment.CenterEnd
        AssetAlignment.BOTTOM_START -> Alignment.BottomStart
        AssetAlignment.BOTTOM_CENTER -> Alignment.BottomCenter
        AssetAlignment.BOTTOM_END -> Alignment.BottomEnd
    }

    Box(
        contentAlignment = contentAlignment,
        modifier = node.modifiers.toModifier()
            .clip(RoundedCornerShape(8.dp))
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawSampleAsset(resolvedAsset?.assetId ?: assetId)
        }
    }
}

@Composable
private fun RenderMissingAsset(assetId: String, node: CanvasNode) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = node.modifiers.toModifier()
            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = "Missing Asset",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Missing Asset: $assetId",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
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
private fun RenderBottomAppBar(node: CanvasNode, controller: EditorController) {
    val actionChildren = node.childrenInSlot(SlotRole.ACTIONS)
    val fabChild = node.childInSlot(SlotRole.FAB)

    BottomAppBar(
        actions = {
            if (actionChildren.isNotEmpty()) {
                actionChildren.forEach { child ->
                    ContainerChild(child = child, controller = controller)
                }
            } else {
                EditorSlotAffordance(label = "Actions Slot")
            }
        },
        floatingActionButton = fabChild?.let { child ->
            { ContainerChild(child = child, controller = controller) }
        },
        modifier = node.modifiers.toModifier().fillMaxWidth()
    )
}

@Composable
private fun RenderRangeSlider(node: CanvasNode, controller: EditorController) {
    val start = node.numericProperty("startValue", default = 0.2f).coerceIn(0f, 1f)
    val end = node.numericProperty("endValue", default = 0.8f).coerceIn(0f, 1f)
    val initialRange = if (start <= end) start..end else end..start
    var rangeValue by remember(node.id, controller.state.isInteractiveMode) { mutableStateOf(initialRange) }
    val isInteractive = controller.state.isInteractiveMode
    RangeSlider(
        value = if (isInteractive) rangeValue else initialRange,
        onValueChange = { if (isInteractive) rangeValue = it },
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
private fun RenderLazyHorizontalGrid(
    node: CanvasNode,
    controller: EditorController
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = node.modifiers.toModifier()
            .fillMaxSize()
            .padding(all = node.layoutConfig.padding.dp),
        horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig),
        verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
    ) {
        items(items = node.children, key = { child -> child.id }) { child ->
            ContainerChild(child = child, controller = controller)
        }
    }
}

@Composable
private fun RenderLazyVerticalStaggeredGrid(
    node: CanvasNode,
    controller: EditorController
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = node.modifiers.toModifier()
            .fillMaxSize()
            .padding(all = node.layoutConfig.padding.dp),
        verticalItemSpacing = node.layoutConfig.spacing.dp,
        horizontalArrangement = resolveHorizontalArrangement(config = node.layoutConfig)
    ) {
        items(items = node.children, key = { child -> child.id }) { child ->
            ContainerChild(child = child, controller = controller)
        }
    }
}

@Composable
private fun RenderLazyHorizontalStaggeredGrid(
    node: CanvasNode,
    controller: EditorController
) {
    LazyHorizontalStaggeredGrid(
        rows = StaggeredGridCells.Fixed(2),
        modifier = node.modifiers.toModifier()
            .fillMaxSize()
            .padding(all = node.layoutConfig.padding.dp),
        horizontalItemSpacing = node.layoutConfig.spacing.dp,
        verticalArrangement = resolveVerticalArrangement(config = node.layoutConfig)
    ) {
        items(items = node.children, key = { child -> child.id }) { child ->
            ContainerChild(child = child, controller = controller)
        }
    }
}

@Composable
private fun RenderHorizontalPager(
    node: CanvasNode,
    controller: EditorController
) {
    val pageCount = node.children.size.coerceAtLeast(1)
    val pagerState = rememberPagerState { pageCount }
    HorizontalPager(
        state = pagerState,
        modifier = node.modifiers.toModifier()
            .fillMaxSize()
            .padding(all = node.layoutConfig.padding.dp)
    ) { pageIndex ->
        if (node.children.isNotEmpty() && pageIndex < node.children.size) {
            ContainerChild(child = node.children[pageIndex], controller = controller)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Page ${pageIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderCarousel(
    node: CanvasNode,
    controller: EditorController
) {
    val itemCount = node.children.size.coerceAtLeast(3)
    val carouselState = rememberCarouselState { itemCount }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 186.dp,
        itemSpacing = node.layoutConfig.spacing.dp.coerceAtLeast(8.dp),
        modifier = node.modifiers.toModifier()
            .fillMaxWidth()
            .height(220.dp)
            .padding(all = node.layoutConfig.padding.dp)
    ) { index ->
        if (node.children.isNotEmpty() && index < node.children.size) {
            ContainerChild(child = node.children[index], controller = controller)
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Item ${index + 1}", style = MaterialTheme.typography.titleMedium)
                }
            }
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