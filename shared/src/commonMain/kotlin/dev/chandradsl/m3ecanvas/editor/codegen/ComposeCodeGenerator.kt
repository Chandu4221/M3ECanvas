package dev.chandradsl.m3ecanvas.editor.codegen

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry

/**
 * Deterministic compiler that translates an [M3EProject] AST into
 * clean, idiomatic, compile-ready Jetpack Compose / Compose Multiplatform code.
 */
open class ComposeCodeGenerator(
    val registry: ComponentRegistry = ComponentRegistry.default
) {

    /**
     * Generates a complete Kotlin file including imports, screen Composable,
     * and previews.
     */
    open fun generateFile(project: M3EProject, functionName: String = sanitizeName(project.name)): String {
        val body = buildString {
            appendLine("package com.example.app.ui.screens")
            appendLine()
            appendLine("import androidx.compose.foundation.background")
            appendLine("import androidx.compose.foundation.border")
            appendLine("import androidx.compose.foundation.clickable")
            appendLine("import androidx.compose.foundation.horizontalScroll")
            appendLine("import androidx.compose.foundation.layout.*")
            appendLine("import androidx.compose.foundation.lazy.LazyColumn")
            appendLine("import androidx.compose.foundation.lazy.LazyRow")
            appendLine("import androidx.compose.foundation.lazy.grid.GridCells")
            appendLine("import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid")
            appendLine("import androidx.compose.foundation.lazy.grid.LazyVerticalGrid")
            appendLine("import androidx.compose.foundation.lazy.grid.items")
            appendLine("import androidx.compose.foundation.lazy.items")
            appendLine("import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid")
            appendLine("import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid")
            appendLine("import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells")
            appendLine("import androidx.compose.foundation.lazy.staggeredgrid.items")
            appendLine("import androidx.compose.foundation.pager.HorizontalPager")
            appendLine("import androidx.compose.foundation.pager.rememberPagerState")
            appendLine("import androidx.compose.foundation.rememberScrollState")
            appendLine("import androidx.compose.foundation.shape.CircleShape")
            appendLine("import androidx.compose.foundation.shape.RoundedCornerShape")
            appendLine("import androidx.compose.foundation.verticalScroll")
            appendLine("import androidx.compose.material.icons.Icons")
            appendLine("import androidx.compose.material.icons.automirrored.outlined.*")
            appendLine("import androidx.compose.material.icons.filled.*")
            appendLine("import androidx.compose.material.icons.outlined.*")
            appendLine("import androidx.compose.material3.*")
            appendLine("import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel")
            appendLine("import androidx.compose.material3.carousel.rememberCarouselState")
            appendLine("import androidx.compose.runtime.*")
            appendLine("import androidx.compose.ui.Alignment")
            appendLine("import androidx.compose.ui.Modifier")
            appendLine("import androidx.compose.ui.draw.alpha")
            appendLine("import androidx.compose.ui.draw.clip")
            appendLine("import androidx.compose.ui.draw.rotate")
            appendLine("import androidx.compose.ui.draw.scale")
            appendLine("import androidx.compose.ui.draw.shadow")
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.ui.graphics.RectangleShape")
            appendLine("import androidx.compose.ui.text.font.FontWeight")
            appendLine("import androidx.compose.ui.unit.dp")
            appendLine("import androidx.compose.ui.zIndex")
            appendLine("import androidx.compose.ui.semantics.Role")
            appendLine("import androidx.compose.ui.semantics.contentDescription")
            appendLine("import androidx.compose.ui.semantics.role")
            appendLine("import androidx.compose.ui.semantics.semantics")
            appendLine()
            appendLine("@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)")
            appendLine("@Composable")
            appendLine("fun ${functionName}(modifier: Modifier = Modifier) {")
            if (project.nodes.isEmpty()) {
                appendLine("    // Empty canvas")
                appendLine("    Box(modifier = modifier.fillMaxSize())")
            } else {
                val scaffold = project.nodes.firstOrNull { it.type == ComponentType.SCAFFOLD || it.type == ComponentType.BOTTOM_SHEET_SCAFFOLD }
                if (scaffold != null) {
                    if (scaffold.type == ComponentType.BOTTOM_SHEET_SCAFFOLD) {
                        append(generateBottomSheetScaffoldCode(scaffold, indent = "    "))
                    } else {
                        append(generateScaffoldCode(scaffold, indent = "    "))
                    }
                    val overlays = project.nodes.filter { it.type.isOverlay() }
                    overlays.forEach { overlay ->
                        appendLine()
                        append(generateNodeCode(overlay, indent = "    ", isRootFloating = false))
                    }
                } else {
                    appendLine("    Box(modifier = modifier.fillMaxSize()) {")
                    val nonOverlays = project.nodes.filter { !it.type.isOverlay() }
                    val overlays = project.nodes.filter { it.type.isOverlay() }
                    nonOverlays.forEach { node ->
                        append(generateNodeCode(node, indent = "        ", isRootFloating = true))
                    }
                    overlays.forEach { overlay ->
                        append(generateNodeCode(overlay, indent = "        ", isRootFloating = false))
                    }
                    appendLine("    }")
                }
            }
            appendLine("}")
        }
        return body
    }

    private fun generateBottomSheetScaffoldCode(scaffold: CanvasNode, indent: String): String {
        val topBarChild = scaffold.childInSlot(SlotRole.TOP_BAR)
        val sheetChild = scaffold.childInSlot(SlotRole.SHEET_CONTENT)
        val snackbarChild = scaffold.childInSlot(SlotRole.SNACKBAR)
        val contentChildren = scaffold.childrenInSlot(SlotRole.CONTENT).ifEmpty {
            scaffold.children.filter { it.slot != SlotRole.TOP_BAR && it.slot != SlotRole.SHEET_CONTENT && it.slot != SlotRole.SNACKBAR }
        }

        return buildString {
            appendLine("${indent}val scaffoldState = rememberBottomSheetScaffoldState()")
            appendLine("${indent}BottomSheetScaffold(")
            appendLine("${indent}    scaffoldState = scaffoldState,")
            if (topBarChild != null) {
                appendLine("${indent}    topBar = {")
                append(generateNodeCode(topBarChild, indent = "$indent        "))
                appendLine("${indent}    },")
            }
            if (snackbarChild != null) {
                appendLine("${indent}    snackbarHost = {")
                append(generateNodeCode(snackbarChild, indent = "$indent        "))
                appendLine("${indent}    },")
            }
            appendLine("${indent}    sheetContent = {")
            if (sheetChild != null) {
                append(generateNodeCode(sheetChild, indent = "$indent        "))
            } else {
                appendLine("${indent}        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {")
                appendLine("${indent}            Text(text = \"Sheet content\", style = MaterialTheme.typography.titleMedium)")
                appendLine("${indent}        }")
            }
            appendLine("${indent}    },")
            appendLine("${indent}    sheetPeekHeight = 64.dp,")
            appendLine("${indent}    modifier = Modifier.fillMaxSize()")
            appendLine("${indent}) { innerPadding ->")
            appendLine("${indent}    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {")
            contentChildren.forEach { child ->
                append(generateNodeCode(child, indent = "$indent        "))
            }
            appendLine("${indent}    }")
            appendLine("${indent}}")
        }
    }

    private fun generateScaffoldCode(scaffold: CanvasNode, indent: String): String {
        val topBarChild = scaffold.childInSlot(SlotRole.TOP_BAR)
        val bottomBarChild = scaffold.childInSlot(SlotRole.BOTTOM_BAR)
        val railChild = scaffold.childInSlot(SlotRole.RAIL)
        val drawerChild = scaffold.childInSlot(SlotRole.DRAWER)
        val fabChild = scaffold.childInSlot(SlotRole.FAB)
        val snackbarChild = scaffold.childInSlot(SlotRole.SNACKBAR)
        val contentChildren = scaffold.children.filter { it.slot == null || it.slot == SlotRole.CONTENT }

        val scaffoldIndent = if (drawerChild != null) "$indent    " else indent
        val scaffoldString = buildString {
            appendLine("${scaffoldIndent}Scaffold(")
            appendLine("${scaffoldIndent}    modifier = Modifier.fillMaxSize(),")
            if (topBarChild != null) {
                appendLine("${scaffoldIndent}    topBar = {")
                append(generateNodeCode(topBarChild, indent = "$scaffoldIndent        "))
                appendLine("${scaffoldIndent}    },")
            }
            if (bottomBarChild != null) {
                appendLine("${scaffoldIndent}    bottomBar = {")
                append(generateNodeCode(bottomBarChild, indent = "$scaffoldIndent        "))
                appendLine("${scaffoldIndent}    },")
            }
            if (snackbarChild != null) {
                appendLine("${scaffoldIndent}    snackbarHost = {")
                append(generateNodeCode(snackbarChild, indent = "$scaffoldIndent        "))
                appendLine("${scaffoldIndent}    },")
            }
            if (fabChild != null) {
                appendLine("${scaffoldIndent}    floatingActionButton = {")
                append(generateNodeCode(fabChild, indent = "$scaffoldIndent        "))
                appendLine("${scaffoldIndent}    }")
            }
            appendLine("${scaffoldIndent}) { innerPadding ->")
            if (railChild != null) {
                appendLine("${scaffoldIndent}    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {")
                append(generateNodeCode(railChild, indent = "$scaffoldIndent        "))
                if (contentChildren.isEmpty()) {
                    appendLine("${scaffoldIndent}        Box(modifier = Modifier.weight(1f).fillMaxHeight())")
                } else if (contentChildren.size == 1 && contentChildren.first().isContainer) {
                    val singleContainer = contentChildren.first()
                    append(generateNodeCode(singleContainer, indent = "$scaffoldIndent        ", extraModifier = "weight(1f).fillMaxHeight()"))
                } else {
                    appendLine("${scaffoldIndent}        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {")
                    contentChildren.forEach { child ->
                        append(generateNodeCode(child, indent = "$scaffoldIndent            "))
                    }
                    appendLine("${scaffoldIndent}        }")
                }
                appendLine("${scaffoldIndent}    }")
            } else {
                if (contentChildren.isEmpty()) {
                    appendLine("${scaffoldIndent}    Box(modifier = Modifier.fillMaxSize().padding(innerPadding))")
                } else if (contentChildren.size == 1 && contentChildren.first().isContainer) {
                    val singleContainer = contentChildren.first()
                    append(generateNodeCode(singleContainer, indent = "$scaffoldIndent    ", extraModifier = "padding(innerPadding)"))
                } else {
                    appendLine("${scaffoldIndent}    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {")
                    contentChildren.forEach { child ->
                        append(generateNodeCode(child, indent = "$scaffoldIndent        "))
                    }
                    appendLine("${scaffoldIndent}    }")
                }
            }
            appendLine("${scaffoldIndent}}")
        }

        return if (drawerChild != null) {
            buildString {
                appendLine("${indent}ModalNavigationDrawer(")
                appendLine("${indent}    drawerContent = {")
                appendLine("${indent}        ModalDrawerSheet {")
                append(generateNodeCode(drawerChild, indent = "$indent            "))
                appendLine("${indent}        }")
                appendLine("${indent}    }")
                appendLine("${indent}) {")
                append(scaffoldString)
                appendLine("${indent}}")
            }
        } else {
            scaffoldString
        }
    }

    open fun generateNodeCode(
        node: CanvasNode,
        indent: String = "",
        isRootFloating: Boolean = false,
        extraModifier: String? = null
    ): String {
        return when (node.type) {
            ComponentType.SCAFFOLD -> generateScaffoldCode(node, indent)
            ComponentType.BOTTOM_SHEET_SCAFFOLD -> generateBottomSheetScaffoldCode(node, indent)

            ComponentType.COLUMN -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val arrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                val alignment = resolveHorizontalAlignmentString(node.layoutConfig.horizontalAlignment)
                val scroll = if (node.layoutConfig.scrollable) ".verticalScroll(rememberScrollState())" else ""
                val fullMod = if (scroll.isNotEmpty()) "$mod$scroll" else mod
                appendLine("${indent}Column(")
                appendLine("${indent}    modifier = $fullMod,")
                appendLine("${indent}    verticalArrangement = $arrangement,")
                appendLine("${indent}    horizontalAlignment = $alignment")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.ROW -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val arrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                val alignment = resolveVerticalAlignmentString(node.layoutConfig.verticalAlignment)
                val scroll = if (node.layoutConfig.scrollable) ".horizontalScroll(rememberScrollState())" else ""
                val fullMod = if (scroll.isNotEmpty()) "$mod$scroll" else mod
                appendLine("${indent}Row(")
                appendLine("${indent}    modifier = $fullMod,")
                appendLine("${indent}    horizontalArrangement = $arrangement,")
                appendLine("${indent}    verticalAlignment = $alignment")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.BOX -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val alignment = resolveBoxAlignmentString(node.layoutConfig.boxAlignment)
                appendLine("${indent}Box(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    contentAlignment = $alignment")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.BOX_WITH_CONSTRAINTS -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val alignment = resolveBoxAlignmentString(node.layoutConfig.boxAlignment)
                appendLine("${indent}BoxWithConstraints(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    contentAlignment = $alignment")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.LAZY_COLUMN -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val arrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                val alignment = resolveHorizontalAlignmentString(node.layoutConfig.horizontalAlignment)
                appendLine("${indent}LazyColumn(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    verticalArrangement = $arrangement,")
                appendLine("${indent}    horizontalAlignment = $alignment")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    appendLine("${indent}    item {")
                    append(generateNodeCode(child, indent = "$indent        "))
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.LAZY_ROW -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val arrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                val alignment = resolveVerticalAlignmentString(node.layoutConfig.verticalAlignment)
                appendLine("${indent}LazyRow(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    horizontalArrangement = $arrangement,")
                appendLine("${indent}    verticalAlignment = $alignment")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    appendLine("${indent}    item {")
                    append(generateNodeCode(child, indent = "$indent        "))
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.TOP_APP_BAR -> buildString {
                val titleChild = node.childInSlot(SlotRole.TITLE)
                val navChild = node.childInSlot(SlotRole.NAVIGATION_ICON)
                val actionChildren = node.childrenInSlot(SlotRole.ACTIONS)
                val title = (node.property("title") as? ComponentProperty.Text)?.value
                    ?: (node.property("text") as? ComponentProperty.Text)?.value
                    ?: "Dashboard"
                val mod = buildModifierString(node, isRootFloating = false, "fillMaxWidth()")
                appendLine("${indent}TopAppBar(")
                if (titleChild != null) {
                    appendLine("${indent}    title = {")
                    append(generateNodeCode(titleChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                } else {
                    appendLine("${indent}    title = { Text(text = \"$title\") },")
                }

                if (navChild != null) {
                    appendLine("${indent}    navigationIcon = {")
                    append(generateNodeCode(navChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                if (actionChildren.isNotEmpty()) {
                    appendLine("${indent}    actions = {")
                    actionChildren.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent        "))
                    }
                    appendLine("${indent}    },")
                }
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.NAVIGATION_BAR -> buildString {
                val mod = buildModifierString(node, isRootFloating = false, "fillMaxWidth()")
                appendLine("${indent}NavigationBar(modifier = $mod) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                }
                appendLine("${indent}}")
            }

            ComponentType.NAVIGATION_BAR_ITEM -> buildString {
                val label = (node.property("label") as? ComponentProperty.Text)?.value
                    ?: (node.property("text") as? ComponentProperty.Text)?.value
                    ?: "Item"
                val iconName = node.iconProperty("icon", default = "Home")
                val iconExpr = resolveIconCodeExpression(iconName)
                val selected = node.booleanProperty("selected", default = true)
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}NavigationBarItem(")
                appendLine("${indent}    selected = $selected,")
                appendLine("${indent}    onClick = {},")
                appendLine("${indent}    icon = { Icon($iconExpr, contentDescription = \"$label\") },")
                appendLine("${indent}    label = { Text(\"$label\") },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.BUTTON -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Button
                    ?: MaterialVariant.Button.FILLED
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: node.name.ifEmpty { "Button" }
                val composableName = when (variant) {
                    MaterialVariant.Button.FILLED -> "Button"
                    MaterialVariant.Button.TONAL -> "FilledTonalButton"
                    MaterialVariant.Button.OUTLINED -> "OutlinedButton"
                    MaterialVariant.Button.ELEVATED -> "ElevatedButton"
                    MaterialVariant.Button.TEXT -> "TextButton"
                }
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}$composableName(")
                appendLine("${indent}    onClick = {},")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Text(text = \"$text\")")
                appendLine("${indent}}")
            }

            ComponentType.TOGGLE_BUTTON -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.ToggleButton
                    ?: MaterialVariant.ToggleButton.FILLED
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Toggle"
                val checked = node.booleanProperty("checked", default = true)
                val composableName = when (variant) {
                    MaterialVariant.ToggleButton.FILLED -> "ToggleButton"
                    MaterialVariant.ToggleButton.ELEVATED -> "ElevatedToggleButton"
                    MaterialVariant.ToggleButton.TONAL -> "TonalToggleButton"
                    MaterialVariant.ToggleButton.OUTLINED -> "OutlinedToggleButton"
                }
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}var checked by remember { mutableStateOf($checked) }")
                appendLine("${indent}$composableName(")
                appendLine("${indent}    checked = checked,")
                appendLine("${indent}    onCheckedChange = { checked = it },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Text(text = \"$text\")")
                appendLine("${indent}}")
            }

            ComponentType.CARD, ComponentType.ELEVATED_CARD, ComponentType.OUTLINED_CARD -> buildString {
                val forcedVariant = when (node.type) {
                    ComponentType.ELEVATED_CARD -> MaterialVariant.Card.ELEVATED
                    ComponentType.OUTLINED_CARD -> MaterialVariant.Card.OUTLINED
                    else -> null
                }
                val variant = forcedVariant
                    ?: (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Card
                    ?: MaterialVariant.Card.FILLED
                val composableName = when (variant) {
                    MaterialVariant.Card.FILLED -> "Card"
                    MaterialVariant.Card.ELEVATED -> "ElevatedCard"
                    MaterialVariant.Card.OUTLINED -> "OutlinedCard"
                }
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}$composableName(")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                } else {
                    appendLine("${indent}    Column(modifier = Modifier.padding(16.dp)) {")
                    appendLine("${indent}        Text(text = \"${node.name}\", style = MaterialTheme.typography.titleMedium)")
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.FAB -> buildString {
                val iconName = node.iconProperty("icon", default = "Add")
                val iconExpr = resolveIconCodeExpression(iconName)
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.FloatingActionButton
                val fabSize = ((node.property("fabSize") as? ComponentProperty.Variant)?.value as? MaterialVariant.FabSize)
                    ?: ((node.property("size") as? ComponentProperty.Variant)?.value as? MaterialVariant.FabSize)
                    ?: MaterialVariant.FabSize.MEDIUM
                val composableName = when (fabSize) {
                    MaterialVariant.FabSize.SMALL -> "SmallFloatingActionButton"
                    MaterialVariant.FabSize.LARGE -> "LargeFloatingActionButton"
                    MaterialVariant.FabSize.MEDIUM -> "FloatingActionButton"
                }
                appendLine("${indent}$composableName(")
                appendLine("${indent}    onClick = {},")
                when (variant) {
                    MaterialVariant.FloatingActionButton.SURFACE -> {
                        appendLine("${indent}    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,")
                        appendLine("${indent}    contentColor = MaterialTheme.colorScheme.primary,")
                    }
                    MaterialVariant.FloatingActionButton.SECONDARY -> {
                        appendLine("${indent}    containerColor = MaterialTheme.colorScheme.secondaryContainer,")
                        appendLine("${indent}    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,")
                    }
                    MaterialVariant.FloatingActionButton.TERTIARY -> {
                        appendLine("${indent}    containerColor = MaterialTheme.colorScheme.tertiaryContainer,")
                        appendLine("${indent}    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,")
                    }
                    null -> {}
                }
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Icon($iconExpr, contentDescription = \"$iconName\")")
                appendLine("${indent}}")
            }

            ComponentType.EXTENDED_FAB -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Create"
                val iconName = node.iconProperty("icon", default = "Add")
                val iconExpr = resolveIconCodeExpression(iconName)
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.FloatingActionButton
                appendLine("${indent}ExtendedFloatingActionButton(")
                appendLine("${indent}    onClick = {},")
                when (variant) {
                    MaterialVariant.FloatingActionButton.SURFACE -> {
                        appendLine("${indent}    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,")
                        appendLine("${indent}    contentColor = MaterialTheme.colorScheme.primary,")
                    }
                    MaterialVariant.FloatingActionButton.SECONDARY -> {
                        appendLine("${indent}    containerColor = MaterialTheme.colorScheme.secondaryContainer,")
                        appendLine("${indent}    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,")
                    }
                    MaterialVariant.FloatingActionButton.TERTIARY -> {
                        appendLine("${indent}    containerColor = MaterialTheme.colorScheme.tertiaryContainer,")
                        appendLine("${indent}    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,")
                    }
                    null -> {}
                }
                appendLine("${indent}    icon = { Icon($iconExpr, contentDescription = null) },")
                appendLine("${indent}    text = { Text(text = \"$text\") },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.ICON_BUTTON -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.IconButton
                    ?: MaterialVariant.IconButton.STANDARD
                val iconName = node.iconProperty("icon", default = "Add")
                val iconExpr = resolveIconCodeExpression(iconName)
                val composableName = when (variant) {
                    MaterialVariant.IconButton.STANDARD -> "IconButton"
                    MaterialVariant.IconButton.FILLED -> "FilledIconButton"
                    MaterialVariant.IconButton.TONAL -> "FilledTonalIconButton"
                    MaterialVariant.IconButton.OUTLINED -> "OutlinedIconButton"
                }
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}$composableName(")
                appendLine("${indent}    onClick = {},")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Icon($iconExpr, contentDescription = null)")
                appendLine("${indent}}")
            }

            ComponentType.CHIPS -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Chip
                    ?: MaterialVariant.Chip.ASSIST
                val label = (node.property("text") as? ComponentProperty.Text)?.value ?: "Chip"
                val composableName = when (variant) {
                    MaterialVariant.Chip.ASSIST -> "AssistChip(onClick = {}, label = { Text(\"$label\") })"
                    MaterialVariant.Chip.FILTER -> "FilterChip(selected = true, onClick = {}, label = { Text(\"$label\") })"
                    MaterialVariant.Chip.INPUT -> "InputChip(selected = false, onClick = {}, label = { Text(\"$label\") })"
                    MaterialVariant.Chip.SUGGESTION -> "SuggestionChip(onClick = {}, label = { Text(\"$label\") })"
                }
                appendLine("${indent}$composableName")
            }

            ComponentType.TEXT_FIELD -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: ""
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.TextField
                    ?: MaterialVariant.TextField.OUTLINED
                val composable = if (variant == MaterialVariant.TextField.OUTLINED) "OutlinedTextField" else "TextField"
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}$composable(")
                appendLine("${indent}    value = \"$text\",")
                appendLine("${indent}    onValueChange = {},")
                appendLine("${indent}    label = { Text(\"${node.name}\") },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.SEGMENTED_BUTTON -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var selectedIndex by remember { mutableStateOf(0) }")
                appendLine("${indent}SingleChoiceSegmentedButtonRow(modifier = $mod) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEachIndexed { index, child ->
                        appendLine("${indent}    SegmentedButton(")
                        appendLine("${indent}        shape = SegmentedButtonDefaults.itemShape(index = $index, count = ${node.children.size}),")
                        appendLine("${indent}        onClick = { selectedIndex = $index },")
                        appendLine("${indent}        selected = selectedIndex == $index")
                        appendLine("${indent}    ) {")
                        append(generateNodeCode(child, indent = "$indent        "))
                        appendLine("${indent}    }")
                    }
                }
                appendLine("${indent}}")
            }

            ComponentType.SPLIT_BUTTON -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Action"
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}SplitButton(")
                appendLine("${indent}    leadingButton = {")
                appendLine("${indent}        SplitButtonDefaults.LeadingButton(onClick = {}) {")
                appendLine("${indent}            Text(\"$text\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    trailingButton = {")
                appendLine("${indent}        SplitButtonDefaults.TrailingButton(")
                appendLine("${indent}            checked = false,")
                appendLine("${indent}            onCheckedChange = {}")
                appendLine("${indent}        ) {")
                appendLine("${indent}            Icon(Icons.Default.ArrowDropDown, contentDescription = \"More options\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.BUTTON_GROUP -> buildString {
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}ButtonGroup(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    horizontalArrangement = Arrangement.spacedBy(8.dp)")
                appendLine("${indent}) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                }
                appendLine("${indent}}")
            }

            ComponentType.LIST_ITEM -> buildString {
                val headlineChild = node.childInSlot(SlotRole.HEADLINE)
                val supportingChild = node.childInSlot(SlotRole.SUPPORTING)
                val leadingChild = node.childInSlot(SlotRole.LEADING)
                val trailingChild = node.childInSlot(SlotRole.TRAILING)
                val overlineChild = node.childInSlot(SlotRole.OVERLINE)
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "List Item Headline"
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}ListItem(")
                if (headlineChild != null) {
                    appendLine("${indent}    headlineContent = {")
                    append(generateNodeCode(headlineChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                } else {
                    appendLine("${indent}    headlineContent = { Text(\"$text\") },")
                }

                if (supportingChild != null) {
                    appendLine("${indent}    supportingContent = {")
                    append(generateNodeCode(supportingChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                if (leadingChild != null) {
                    appendLine("${indent}    leadingContent = {")
                    append(generateNodeCode(leadingChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                if (trailingChild != null) {
                    appendLine("${indent}    trailingContent = {")
                    append(generateNodeCode(trailingChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                if (overlineChild != null) {
                    appendLine("${indent}    overlineContent = {")
                    append(generateNodeCode(overlineChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.MODAL_BOTTOM_SHEET -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}ModalBottomSheet(")
                appendLine("${indent}    onDismissRequest = {},")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                if (node.children.isNotEmpty()) {
                    appendLine("${indent}    Column(modifier = Modifier.padding(16.dp)) {")
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent        "))
                    }
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.ALERT_DIALOG -> buildString {
                val iconChild = node.childInSlot(SlotRole.ICON)
                val titleChild = node.childInSlot(SlotRole.TITLE)
                val confirmChild = node.childInSlot(SlotRole.CONFIRM_BUTTON)
                val dismissChild = node.childInSlot(SlotRole.DISMISS_BUTTON)
                val contentChildren = node.children.filter {
                    it.slot == null || it.slot == SlotRole.CONTENT || it.slot == SlotRole.SUPPORTING
                }
                val title = (node.property("text") as? ComponentProperty.Text)?.value ?: "Dialog Title"
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}AlertDialog(")
                appendLine("${indent}    onDismissRequest = {},")
                if (iconChild != null) {
                    appendLine("${indent}    icon = {")
                    append(generateNodeCode(iconChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                if (titleChild != null) {
                    appendLine("${indent}    title = {")
                    append(generateNodeCode(titleChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                if (contentChildren.isNotEmpty()) {
                    appendLine("${indent}    text = {")
                    appendLine("${indent}        Column {")
                    contentChildren.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent            "))
                    }
                    appendLine("${indent}        }")
                    appendLine("${indent}    },")
                }

                if (confirmChild != null) {
                    appendLine("${indent}    confirmButton = {")
                    append(generateNodeCode(confirmChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                if (dismissChild != null) {
                    appendLine("${indent}    dismissButton = {")
                    append(generateNodeCode(dismissChild, indent = "$indent        "))
                    appendLine("${indent}    },")
                }

                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.BASIC_ALERT_DIALOG -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}BasicAlertDialog(")
                appendLine("${indent}    onDismissRequest = {},")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Surface(")
                appendLine("${indent}        shape = RoundedCornerShape(28.dp),")
                appendLine("${indent}        color = MaterialTheme.colorScheme.surfaceContainerHigh,")
                appendLine("${indent}        tonalElevation = 6.dp")
                appendLine("${indent}    ) {")
                if (node.children.isNotEmpty()) {
                    appendLine("${indent}        Column(modifier = Modifier.padding(24.dp)) {")
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent            "))
                    }
                    appendLine("${indent}        }")
                }
                appendLine("${indent}    }")
                appendLine("${indent}}")
            }

            ComponentType.SNACKBAR -> buildString {
                val message = (node.property("text") as? ComponentProperty.Text)?.value ?: "Snackbar notification alert."
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}Snackbar(")
                appendLine("${indent}    action = {")
                appendLine("${indent}        TextButton(onClick = {}) {")
                appendLine("${indent}            Text(\"Dismiss\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Text(\"$message\")")
                appendLine("${indent}}")
            }

            ComponentType.SNACKBAR_HOST -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}val snackbarHostState = remember { SnackbarHostState() }")
                appendLine("${indent}SnackbarHost(hostState = snackbarHostState, modifier = $mod)")
            }

            ComponentType.BADGE -> buildString {
                val count = (node.property("text") as? ComponentProperty.Text)?.value ?: ""
                val mod = buildModifierString(node, isRootFloating)
                if (count.isNotEmpty()) {
                    appendLine("${indent}Badge(modifier = $mod) { Text(\"$count\") }")
                } else {
                    appendLine("${indent}Badge(modifier = $mod)")
                }
            }

            ComponentType.BADGED_BOX -> buildString {
                val badgeChild = node.childInSlot(SlotRole.BADGE)
                val contentChild = node.childInSlot(SlotRole.CONTENT) ?: node.children.firstOrNull { it.slot != SlotRole.BADGE }
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}BadgedBox(")
                appendLine("${indent}    badge = {")
                if (badgeChild != null) {
                    append(generateNodeCode(badgeChild, indent = "$indent        "))
                } else {
                    appendLine("${indent}        Badge()")
                }
                appendLine("${indent}    },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                if (contentChild != null) {
                    append(generateNodeCode(contentChild, indent = "$indent        "))
                } else {
                    appendLine("${indent}        Icon(Icons.Outlined.Notifications, contentDescription = null)")
                }
                appendLine("${indent}}")
            }

            ComponentType.TOOLTIP -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Tooltip label"
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}val tooltipState = rememberTooltipState()")
                appendLine("${indent}TooltipBox(")
                appendLine("${indent}    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),")
                appendLine("${indent}    tooltip = {")
                appendLine("${indent}        PlainTooltip {")
                appendLine("${indent}            Text(\"$text\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    state = tooltipState,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                } else {
                    appendLine("${indent}    IconButton(onClick = {}) {")
                    appendLine("${indent}        Icon(Icons.Outlined.Info, contentDescription = \"Info\")")
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.PROGRESS_INDICATOR -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}LinearProgressIndicator(")
                appendLine("${indent}    progress = { 0.7f },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.LOADING_INDICATOR -> buildString {
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}CircularProgressIndicator(")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.NAVIGATION_RAIL -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxHeight()")
                appendLine("${indent}NavigationRail(modifier = $mod) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                }
                appendLine("${indent}}")
            }

            ComponentType.NAVIGATION_RAIL_ITEM -> buildString {
                val label = (node.property("label") as? ComponentProperty.Text)?.value
                    ?: (node.property("text") as? ComponentProperty.Text)?.value
                    ?: "Item"
                val iconName = node.iconProperty("icon", default = "Home")
                val iconExpr = resolveIconCodeExpression(iconName)
                val selected = node.booleanProperty("selected", default = true)
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}NavigationRailItem(")
                appendLine("${indent}    selected = $selected,")
                appendLine("${indent}    onClick = {},")
                appendLine("${indent}    icon = { Icon($iconExpr, contentDescription = \"$label\") },")
                appendLine("${indent}    label = { Text(\"$label\") },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.NAVIGATION_DRAWER -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxHeight()")
                appendLine("${indent}ModalDrawerSheet(modifier = $mod) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                }
                appendLine("${indent}}")
            }

            ComponentType.TABS -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var selectedTab by remember { mutableStateOf(0) }")
                appendLine("${indent}PrimaryTabRow(selectedTabIndex = selectedTab, modifier = $mod) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                }
                appendLine("${indent}}")
            }

            ComponentType.TAB -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Tab"
                val iconName = node.iconProperty("icon", default = "")
                val selected = node.booleanProperty("selected", default = false)
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}Tab(")
                appendLine("${indent}    selected = $selected,")
                appendLine("${indent}    onClick = {},")
                appendLine("${indent}    text = { Text(\"$text\") },")
                if (iconName.isNotEmpty()) {
                    val iconExpr = resolveIconCodeExpression(iconName)
                    appendLine("${indent}    icon = { Icon($iconExpr, contentDescription = null) },")
                }
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.SEARCH -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var searchQuery by remember { mutableStateOf(\"\") }")
                appendLine("${indent}SearchBar(")
                appendLine("${indent}    inputField = {")
                appendLine("${indent}        SearchBarDefaults.InputField(")
                appendLine("${indent}            query = searchQuery,")
                appendLine("${indent}            onQueryChange = { searchQuery = it },")
                appendLine("${indent}            onSearch = {},")
                appendLine("${indent}            expanded = false,")
                appendLine("${indent}            onExpandedChange = {},")
                appendLine("${indent}            placeholder = { Text(\"Search components...\") },")
                appendLine("${indent}            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = \"Search\") },")
                appendLine("${indent}            trailingIcon = { Icon(Icons.Outlined.Mic, contentDescription = \"Voice\") }")
                appendLine("${indent}        )")
                appendLine("${indent}    },")
                appendLine("${indent}    expanded = false,")
                appendLine("${indent}    onExpandedChange = {},")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    // Suggestions list")
                appendLine("${indent}}")
            }

            ComponentType.CHECKBOX -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Checkbox Option"
                val checked = node.booleanProperty("checked", default = true)
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}var checked by remember { mutableStateOf($checked) }")
                appendLine("${indent}Row(modifier = $mod, verticalAlignment = Alignment.CenterVertically) {")
                appendLine("${indent}    Checkbox(checked = checked, onCheckedChange = { checked = it })")
                appendLine("${indent}    Spacer(modifier = Modifier.width(8.dp))")
                appendLine("${indent}    Text(\"$text\")")
                appendLine("${indent}}")
            }

            ComponentType.RADIO_BUTTON -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Radio Option"
                val selected = node.booleanProperty("selected", default = true)
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}var selected by remember { mutableStateOf($selected) }")
                appendLine("${indent}Row(modifier = $mod, verticalAlignment = Alignment.CenterVertically) {")
                appendLine("${indent}    RadioButton(selected = selected, onClick = { selected = !selected })")
                appendLine("${indent}    Spacer(modifier = Modifier.width(8.dp))")
                appendLine("${indent}    Text(\"$text\")")
                appendLine("${indent}}")
            }

            ComponentType.SWITCH -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Enable feature"
                val checked = node.booleanProperty("checked", default = true)
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var checked by remember { mutableStateOf($checked) }")
                appendLine("${indent}Row(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    verticalAlignment = Alignment.CenterVertically,")
                appendLine("${indent}    horizontalArrangement = Arrangement.SpaceBetween")
                appendLine("${indent}) {")
                appendLine("${indent}    Text(\"$text\")")
                appendLine("${indent}    Switch(checked = checked, onCheckedChange = { checked = it })")
                appendLine("${indent}}")
            }

            ComponentType.SLIDER -> buildString {
                val value = node.numericProperty("value", default = 0.6f)
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var sliderPosition by remember { mutableStateOf(${value}f) }")
                appendLine("${indent}Slider(")
                appendLine("${indent}    value = sliderPosition,")
                appendLine("${indent}    onValueChange = { sliderPosition = it },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.RANGE_SLIDER -> buildString {
                val start = node.numericProperty("startValue", default = 0.2f)
                val end = node.numericProperty("endValue", default = 0.8f)
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var rangeSliderPosition by remember { mutableStateOf(${start}f..${end}f) }")
                appendLine("${indent}RangeSlider(")
                appendLine("${indent}    value = rangeSliderPosition,")
                appendLine("${indent}    onValueChange = { rangeSliderPosition = it },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.DATE_PICKER -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}val datePickerState = rememberDatePickerState()")
                appendLine("${indent}DatePicker(")
                appendLine("${indent}    state = datePickerState,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.TIME_PICKER -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 30)")
                appendLine("${indent}TimePicker(")
                appendLine("${indent}    state = timePickerState,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.MENUS -> buildString {
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}var menuExpanded by remember { mutableStateOf(true) }")
                appendLine("${indent}DropdownMenu(")
                appendLine("${indent}    expanded = menuExpanded,")
                appendLine("${indent}    onDismissRequest = { menuExpanded = false },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                if (node.children.isNotEmpty()) {
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent    "))
                    }
                }
                appendLine("${indent}}")
            }

            ComponentType.DROPDOWN_MENU_ITEM -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Item"
                val iconName = node.iconProperty("icon", default = "")
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}DropdownMenuItem(")
                appendLine("${indent}    text = { Text(\"$text\") },")
                appendLine("${indent}    onClick = {},")
                if (iconName.isNotEmpty()) {
                    val iconExpr = resolveIconCodeExpression(iconName)
                    appendLine("${indent}    leadingIcon = { Icon($iconExpr, contentDescription = null) },")
                }
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.BOTTOM_APP_BAR -> buildString {
                val actions = node.childrenInSlot(SlotRole.ACTIONS)
                val fab = node.childInSlot(SlotRole.FAB)
                val mod = buildModifierString(node, isRootFloating = false, "fillMaxWidth()")
                appendLine("${indent}BottomAppBar(")
                if (actions.isNotEmpty()) {
                    appendLine("${indent}    actions = {")
                    actions.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent        "))
                    }
                    appendLine("${indent}    },")
                }
                if (fab != null) {
                    appendLine("${indent}    floatingActionButton = {")
                    append(generateNodeCode(fab, indent = "$indent        "))
                    appendLine("${indent}    },")
                }
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.TEXT -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: node.name
                val typography = (node.property("typography") as? ComponentProperty.Text)?.value ?: "bodyLarge"
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}Text(")
                appendLine("${indent}    text = \"$text\",")
                appendLine("${indent}    style = MaterialTheme.typography.$typography,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.ICON -> buildString {
                val iconName = node.iconProperty("icon", default = "Favorite")
                val iconExpr = resolveIconCodeExpression(iconName)
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}Icon(")
                appendLine("${indent}    imageVector = $iconExpr,")
                appendLine("${indent}    contentDescription = \"$iconName\",")
                appendLine("${indent}    tint = MaterialTheme.colorScheme.primary,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.IMAGE -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}Surface(")
                appendLine("${indent}    shape = RoundedCornerShape(8.dp),")
                appendLine("${indent}    color = MaterialTheme.colorScheme.surfaceVariant,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {")
                appendLine("${indent}        Icon(Icons.Outlined.Image, contentDescription = \"Image\", tint = MaterialTheme.colorScheme.onSurfaceVariant)")
                appendLine("${indent}    }")
                appendLine("${indent}}")
            }

            ComponentType.HORIZONTAL_DIVIDER -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}HorizontalDivider(modifier = $mod)")
            }

            ComponentType.VERTICAL_DIVIDER -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}VerticalDivider(modifier = $mod)")
            }

            ComponentType.SPACER -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}Spacer(modifier = $mod)")
            }

            ComponentType.SURFACE -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Surface
                    ?: MaterialVariant.Surface.ROUNDED
                val shapeCode = when (variant) {
                    MaterialVariant.Surface.ROUNDED -> "RoundedCornerShape(12.dp)"
                    MaterialVariant.Surface.RECTANGLE -> "RectangleShape"
                    MaterialVariant.Surface.CIRCLE -> "CircleShape"
                }
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val alignment = resolveBoxAlignmentString(node.layoutConfig.boxAlignment)
                appendLine("${indent}Surface(")
                appendLine("${indent}    shape = $shapeCode,")
                appendLine("${indent}    color = MaterialTheme.colorScheme.surfaceContainer,")
                appendLine("${indent}    tonalElevation = 1.dp,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                if (node.children.isEmpty()) {
                    appendLine("${indent}    Box(modifier = Modifier.fillMaxSize(), contentAlignment = $alignment)")
                } else {
                    appendLine("${indent}    Box(modifier = Modifier.fillMaxSize(), contentAlignment = $alignment) {")
                    node.children.forEach { child ->
                        append(generateNodeCode(child, indent = "$indent        "))
                    }
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.LAZY_VERTICAL_GRID -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val vArrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                val hArrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                appendLine("${indent}LazyVerticalGrid(")
                appendLine("${indent}    columns = GridCells.Fixed(2),")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    verticalArrangement = $vArrangement,")
                appendLine("${indent}    horizontalArrangement = $hArrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    appendLine("${indent}    item {")
                    append(generateNodeCode(child, indent = "$indent        "))
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.LAZY_HORIZONTAL_GRID -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val hArrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                val vArrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                appendLine("${indent}LazyHorizontalGrid(")
                appendLine("${indent}    rows = GridCells.Fixed(2),")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    horizontalArrangement = $hArrangement,")
                appendLine("${indent}    verticalArrangement = $vArrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    appendLine("${indent}    item {")
                    append(generateNodeCode(child, indent = "$indent        "))
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.LAZY_VERTICAL_STAGGERED_GRID -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val hArrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                val spacing = node.layoutConfig.spacing.toInt()
                appendLine("${indent}LazyVerticalStaggeredGrid(")
                appendLine("${indent}    columns = StaggeredGridCells.Fixed(2),")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    verticalItemSpacing = ${spacing}.dp,")
                appendLine("${indent}    horizontalArrangement = $hArrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    appendLine("${indent}    item {")
                    append(generateNodeCode(child, indent = "$indent        "))
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val vArrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                val spacing = node.layoutConfig.spacing.toInt()
                appendLine("${indent}LazyHorizontalStaggeredGrid(")
                appendLine("${indent}    rows = StaggeredGridCells.Fixed(2),")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    horizontalItemSpacing = ${spacing}.dp,")
                appendLine("${indent}    verticalArrangement = $vArrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    appendLine("${indent}    item {")
                    append(generateNodeCode(child, indent = "$indent        "))
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.HORIZONTAL_PAGER -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val pageCount = node.children.size.coerceAtLeast(1)
                appendLine("${indent}val pagerState = rememberPagerState { $pageCount }")
                appendLine("${indent}HorizontalPager(")
                appendLine("${indent}    state = pagerState,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) { page ->")
                if (node.children.isNotEmpty()) {
                    appendLine("${indent}    when (page) {")
                    node.children.forEachIndexed { index, child ->
                        appendLine("${indent}        $index -> {")
                        append(generateNodeCode(child, indent = "$indent            "))
                        appendLine("${indent}        }")
                    }
                    appendLine("${indent}    }")
                } else {
                    appendLine("${indent}    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {")
                    appendLine("${indent}        Text(text = \"Page \${page + 1}\")")
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.CAROUSEL -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val itemCount = node.children.size.coerceAtLeast(3)
                appendLine("${indent}val carouselState = rememberCarouselState { $itemCount }")
                appendLine("${indent}HorizontalMultiBrowseCarousel(")
                appendLine("${indent}    state = carouselState,")
                appendLine("${indent}    preferredItemWidth = 186.dp,")
                appendLine("${indent}    itemSpacing = 8.dp,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) { index ->")
                if (node.children.isNotEmpty()) {
                    appendLine("${indent}    when (index) {")
                    node.children.forEachIndexed { idx, child ->
                        appendLine("${indent}        $idx -> {")
                        append(generateNodeCode(child, indent = "$indent            "))
                        appendLine("${indent}        }")
                    }
                    appendLine("${indent}    }")
                } else {
                    appendLine("${indent}    Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {")
                    appendLine("${indent}        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {")
                    appendLine("${indent}            Text(text = \"Item \${index + 1}\")")
                    appendLine("${indent}        }")
                    appendLine("${indent}    }")
                }
                appendLine("${indent}}")
            }

            ComponentType.FLOW_ROW -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val hArrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                val vArrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                appendLine("${indent}FlowRow(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    horizontalArrangement = $hArrangement,")
                appendLine("${indent}    verticalArrangement = $vArrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.FLOW_COLUMN -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val vArrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                val hArrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                appendLine("${indent}FlowColumn(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    verticalArrangement = $vArrangement,")
                appendLine("${indent}    horizontalArrangement = $hArrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }
        }
    }

    private fun buildModifierString(
        node: CanvasNode,
        isRootFloating: Boolean,
        extraModifier: String? = null
    ): String {
        val parts = mutableListOf<String>()

        if (isRootFloating && !node.type.isOverlay()) {
            parts.add("offset(x = ${node.position.x.toInt()}.dp, y = ${node.position.y.toInt()}.dp)")
            if (!node.hasExplicitWidth() && !node.hasExplicitHeight()) {
                parts.add("size(width = ${node.size.width.toInt()}.dp, height = ${node.size.height.toInt()}.dp)")
            } else {
                if (!node.hasExplicitWidth()) {
                    parts.add("width(${node.size.width.toInt()}.dp)")
                }
                if (!node.hasExplicitHeight()) {
                    parts.add("height(${node.size.height.toInt()}.dp)")
                }
            }
        }

        if (extraModifier != null) {
            parts.add(extraModifier)
        }

        if (node.layoutConfig.padding > 0f) {
            parts.add("padding(${node.layoutConfig.padding.toInt()}.dp)")
        }

        for (spec in node.modifiers) {
            when (spec) {
                is ModifierSpec.Padding -> {
                    if (spec.horizontal > 0f || spec.vertical > 0f) {
                        if (spec.horizontal > 0f && spec.vertical > 0f) {
                            parts.add("padding(horizontal = ${spec.horizontal.toInt()}.dp, vertical = ${spec.vertical.toInt()}.dp)")
                        } else if (spec.horizontal > 0f) {
                            parts.add("padding(horizontal = ${spec.horizontal.toInt()}.dp)")
                        } else {
                            parts.add("padding(vertical = ${spec.vertical.toInt()}.dp)")
                        }
                    } else {
                        parts.add("padding(${spec.all.toInt()}.dp)")
                    }
                }
                is ModifierSpec.Background -> {
                    if (spec.cornerRadius > 0f) {
                        parts.add("background(Color(0x${spec.colorHex.removePrefix("#")}), RoundedCornerShape(${spec.cornerRadius.toInt()}.dp))")
                    } else {
                        parts.add("background(Color(0x${spec.colorHex.removePrefix("#")}))")
                    }
                }
                is ModifierSpec.Border -> {
                    parts.add("border(${spec.width.toInt()}.dp, Color(0x${spec.colorHex.removePrefix("#")}), RoundedCornerShape(${spec.cornerRadius.toInt()}.dp))")
                }
                is ModifierSpec.Clip -> parts.add("clip(RoundedCornerShape(${spec.cornerRadius.toInt()}.dp))")
                is ModifierSpec.Shadow -> parts.add("shadow(elevation = ${spec.elevation.toInt()}.dp, shape = RoundedCornerShape(${spec.cornerRadius.toInt()}.dp))")
                is ModifierSpec.Alpha -> parts.add("alpha(${spec.value}f)")
                is ModifierSpec.Rotation -> parts.add("rotate(${spec.degrees}f)")
                is ModifierSpec.Scale -> parts.add("scale(${spec.value}f)")
                is ModifierSpec.FillMaxSize -> {
                    if (spec.fraction == 1f) {
                        parts.add("fillMaxSize()")
                    } else {
                        parts.add("fillMaxSize(${spec.fraction}f)")
                    }
                }
                is ModifierSpec.FillMaxWidth -> parts.add("fillMaxWidth()")
                is ModifierSpec.FillMaxHeight -> parts.add("fillMaxHeight()")
                is ModifierSpec.WrapContentSize -> parts.add("wrapContentSize()")
                is ModifierSpec.Width -> parts.add("width(${spec.dp.toInt()}.dp)")
                is ModifierSpec.Height -> parts.add("height(${spec.dp.toInt()}.dp)")
                is ModifierSpec.Size -> parts.add("size(width = ${spec.width.toInt()}.dp, height = ${spec.height.toInt()}.dp)")
                is ModifierSpec.AspectRatio -> parts.add("aspectRatio(${spec.ratio}f)")
                is ModifierSpec.Offset -> parts.add("offset(x = ${spec.x.toInt()}.dp, y = ${spec.y.toInt()}.dp)")
                is ModifierSpec.Weight -> {
                    if (spec.fill) {
                        parts.add("weight(${spec.weight}f)")
                    } else {
                        parts.add("weight(${spec.weight}f, fill = false)")
                    }
                }
                is ModifierSpec.Align -> parts.add("align(${resolveAlignTargetCode(spec.alignment)})")
                is ModifierSpec.ZIndex -> parts.add("zIndex(${spec.value}f)")
                is ModifierSpec.Clickable -> parts.add("clickable(enabled = ${spec.enabled}) {}")
            }
        }

        val contentDesc = (node.property("contentDescription") as? ComponentProperty.Text)?.value
        val semanticsRole = (node.property("semanticsRole") as? ComponentProperty.Text)?.value
        if (!contentDesc.isNullOrBlank() || (!semanticsRole.isNullOrBlank() && semanticsRole != "None")) {
            val semParts = mutableListOf<String>()
            if (!contentDesc.isNullOrBlank()) {
                semParts.add("contentDescription = \"${contentDesc.replace("\"", "\\\"")}\"")
            }
            if (!semanticsRole.isNullOrBlank() && semanticsRole != "None") {
                semParts.add("role = Role.$semanticsRole")
            }
            parts.add("semantics { ${semParts.joinToString("; ")} }")
        }

        return if (parts.isEmpty()) {
            "Modifier"
        } else {
            "Modifier." + parts.joinToString(".")
        }
    }

    private fun resolveHorizontalAlignmentString(align: HorizontalAlignment): String {
        return when (align) {
            HorizontalAlignment.START -> "Alignment.Start"
            HorizontalAlignment.CENTER_HORIZONTALLY -> "Alignment.CenterHorizontally"
            HorizontalAlignment.END -> "Alignment.End"
        }
    }

    private fun resolveVerticalAlignmentString(align: VerticalAlignment): String {
        return when (align) {
            VerticalAlignment.TOP -> "Alignment.Top"
            VerticalAlignment.CENTER_VERTICALLY -> "Alignment.CenterVertically"
            VerticalAlignment.BOTTOM -> "Alignment.Bottom"
        }
    }

    private fun resolveBoxAlignmentString(align: BoxAlignment): String {
        return when (align) {
            BoxAlignment.TOP_START -> "Alignment.TopStart"
            BoxAlignment.TOP_CENTER -> "Alignment.TopCenter"
            BoxAlignment.TOP_END -> "Alignment.TopEnd"
            BoxAlignment.CENTER_START -> "Alignment.CenterStart"
            BoxAlignment.CENTER -> "Alignment.Center"
            BoxAlignment.CENTER_END -> "Alignment.CenterEnd"
            BoxAlignment.BOTTOM_START -> "Alignment.BottomStart"
            BoxAlignment.BOTTOM_CENTER -> "Alignment.BottomCenter"
            BoxAlignment.BOTTOM_END -> "Alignment.BottomEnd"
        }
    }

    private fun resolveAlignTargetCode(target: AlignTarget): String {
        return when (target) {
            AlignTarget.TOP_START -> "Alignment.TopStart"
            AlignTarget.TOP_CENTER -> "Alignment.TopCenter"
            AlignTarget.TOP_END -> "Alignment.TopEnd"
            AlignTarget.CENTER_START -> "Alignment.CenterStart"
            AlignTarget.CENTER -> "Alignment.Center"
            AlignTarget.CENTER_END -> "Alignment.CenterEnd"
            AlignTarget.BOTTOM_START -> "Alignment.BottomStart"
            AlignTarget.BOTTOM_CENTER -> "Alignment.BottomCenter"
            AlignTarget.BOTTOM_END -> "Alignment.BottomEnd"
            AlignTarget.START -> "Alignment.Start"
            AlignTarget.CENTER_HORIZONTALLY -> "Alignment.CenterHorizontally"
            AlignTarget.END -> "Alignment.End"
            AlignTarget.TOP -> "Alignment.Top"
            AlignTarget.CENTER_VERTICALLY -> "Alignment.CenterVertically"
            AlignTarget.BOTTOM -> "Alignment.Bottom"
        }
    }

    private fun resolveArrangementString(config: LayoutConfig, isVertical: Boolean): String {
        if (config.spacing > 0f) {
            return "Arrangement.spacedBy(${config.spacing.toInt()}.dp)"
        }
        return if (isVertical) {
            when (config.arrangement) {
                LayoutArrangement.START -> "Arrangement.Top"
                LayoutArrangement.CENTER -> "Arrangement.Center"
                LayoutArrangement.END -> "Arrangement.Bottom"
                LayoutArrangement.SPACE_BETWEEN -> "Arrangement.SpaceBetween"
                LayoutArrangement.SPACE_AROUND -> "Arrangement.SpaceAround"
                LayoutArrangement.SPACE_EVENLY -> "Arrangement.SpaceEvenly"
            }
        } else {
            when (config.arrangement) {
                LayoutArrangement.START -> "Arrangement.Start"
                LayoutArrangement.CENTER -> "Arrangement.Center"
                LayoutArrangement.END -> "Arrangement.End"
                LayoutArrangement.SPACE_BETWEEN -> "Arrangement.SpaceBetween"
                LayoutArrangement.SPACE_AROUND -> "Arrangement.SpaceAround"
                LayoutArrangement.SPACE_EVENLY -> "Arrangement.SpaceEvenly"
            }
        }
    }

    private fun resolveIconCodeExpression(iconName: String): String {
        return when (iconName.trim().lowercase()) {
            "favorite" -> "Icons.Outlined.Favorite"
            "favoriteborder" -> "Icons.Outlined.FavoriteBorder"
            "home" -> "Icons.Outlined.Home"
            "search" -> "Icons.Outlined.Search"
            "settings" -> "Icons.Outlined.Settings"
            "add" -> "Icons.Outlined.Add"
            "close" -> "Icons.Outlined.Close"
            "star" -> "Icons.Outlined.Star"
            "notifications" -> "Icons.Outlined.Notifications"
            "share" -> "Icons.Outlined.Share"
            "delete" -> "Icons.Outlined.Delete"
            "edit" -> "Icons.Outlined.Edit"
            "info" -> "Icons.Outlined.Info"
            "menu" -> "Icons.Outlined.Menu"
            "check" -> "Icons.Outlined.Check"
            "arrowback", "arrow_back" -> "Icons.AutoMirrored.Outlined.ArrowBack"
            "person" -> "Icons.Outlined.Person"
            "refresh" -> "Icons.Outlined.Refresh"
            "lock" -> "Icons.Outlined.Lock"
            "email", "mail" -> "Icons.Outlined.Email"
            "phone" -> "Icons.Outlined.Phone"
            "send" -> "Icons.AutoMirrored.Outlined.Send"
            "thumbup", "thumb_up" -> "Icons.Outlined.ThumbUp"
            "playarrow", "play_arrow" -> "Icons.Outlined.PlayArrow"
            "warning" -> "Icons.Outlined.Warning"
            else -> "Icons.Outlined.Favorite"
        }
    }

    companion object {
        fun sanitizeName(raw: String): String {
            val clean = raw.replace(Regex("[^A-Za-z0-9]"), "")
            return if (clean.isEmpty()) "M3EScreen" else "${clean.replaceFirstChar { it.uppercase() }}Screen"
        }

        val default: ComposeCodeGenerator by lazy { ComposeCodeGenerator() }

        fun generateFile(project: M3EProject, functionName: String = sanitizeName(project.name)): String =
            default.generateFile(project, functionName)

        fun generateNodeCode(
            node: CanvasNode,
            indent: String = "",
            isRootFloating: Boolean = false,
            extraModifier: String? = null
        ): String = default.generateNodeCode(node, indent, isRootFloating, extraModifier)
    }
}
