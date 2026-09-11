package dev.chandradsl.m3ecanvas.editor.codegen

import dev.chandradsl.m3ecanvas.domain.model.*

/**
 * Deterministic compiler that translates an [M3EProject] AST into
 * clean, idiomatic, compile-ready Jetpack Compose / Compose Multiplatform code.
 */
object ComposeCodeGenerator {

    /**
     * Generates a complete Kotlin file including imports, screen Composable,
     * and previews.
     */
    fun generateFile(project: M3EProject, functionName: String = sanitizeName(project.name)): String {
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
            appendLine("import androidx.compose.foundation.lazy.grid.LazyVerticalGrid")
            appendLine("import androidx.compose.foundation.lazy.grid.items")
            appendLine("import androidx.compose.foundation.lazy.items")
            appendLine("import androidx.compose.foundation.rememberScrollState")
            appendLine("import androidx.compose.foundation.shape.CircleShape")
            appendLine("import androidx.compose.foundation.shape.RoundedCornerShape")
            appendLine("import androidx.compose.foundation.verticalScroll")
            appendLine("import androidx.compose.material.icons.Icons")
            appendLine("import androidx.compose.material.icons.automirrored.outlined.*")
            appendLine("import androidx.compose.material.icons.filled.*")
            appendLine("import androidx.compose.material.icons.outlined.*")
            appendLine("import androidx.compose.material3.*")
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
            appendLine()
            appendLine("@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)")
            appendLine("@Composable")
            appendLine("fun ${functionName}(modifier: Modifier = Modifier) {")
            if (project.nodes.isEmpty()) {
                appendLine("    // Empty canvas")
                appendLine("    Box(modifier = modifier.fillMaxSize())")
            } else {
                val scaffold = project.nodes.firstOrNull { it.type == ComponentType.SCAFFOLD }
                if (scaffold != null) {
                    append(generateScaffoldCode(scaffold, indent = "    "))
                } else {
                    appendLine("    Box(modifier = modifier.fillMaxSize()) {")
                    project.nodes.forEach { node ->
                        append(generateNodeCode(node, indent = "        ", isRootFloating = true))
                    }
                    appendLine("    }")
                }
            }
            appendLine("}")
        }
        return body
    }

    private fun generateScaffoldCode(scaffold: CanvasNode, indent: String): String {
        val topBarChild = scaffold.childInSlot(SlotRole.TOP_BAR)
        val bottomBarChild = scaffold.childInSlot(SlotRole.BOTTOM_BAR)
        val fabChild = scaffold.childInSlot(SlotRole.FAB)
        val contentChildren = scaffold.children.filter { it.slot == null || it.slot == SlotRole.CONTENT }

        return buildString {
            appendLine("${indent}Scaffold(")
            appendLine("${indent}    modifier = Modifier.fillMaxSize(),")
            if (topBarChild != null) {
                appendLine("${indent}    topBar = {")
                append(generateNodeCode(topBarChild, indent = "$indent        "))
                appendLine("${indent}    },")
            }
            if (bottomBarChild != null) {
                appendLine("${indent}    bottomBar = {")
                append(generateNodeCode(bottomBarChild, indent = "$indent        "))
                appendLine("${indent}    },")
            }
            if (fabChild != null) {
                appendLine("${indent}    floatingActionButton = {")
                append(generateNodeCode(fabChild, indent = "$indent        "))
                appendLine("${indent}    }")
            }
            appendLine("${indent}) { innerPadding ->")
            if (contentChildren.isEmpty()) {
                appendLine("${indent}    Box(modifier = Modifier.fillMaxSize().padding(innerPadding))")
            } else if (contentChildren.size == 1 && contentChildren.first().isContainer) {
                val singleContainer = contentChildren.first()
                append(generateNodeCode(singleContainer, indent = "$indent    ", extraModifier = "padding(innerPadding)"))
            } else {
                appendLine("${indent}    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {")
                contentChildren.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent        "))
                }
                appendLine("${indent}    }")
            }
            appendLine("${indent}}")
        }
    }

    fun generateNodeCode(
        node: CanvasNode,
        indent: String,
        isRootFloating: Boolean = false,
        extraModifier: String? = null
    ): String {
        return when (node.type) {
            ComponentType.SCAFFOLD -> generateScaffoldCode(node, indent)

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
                val title = (node.property("title") as? ComponentProperty.Text)?.value
                    ?: (node.property("text") as? ComponentProperty.Text)?.value
                    ?: "Dashboard"
                val mod = buildModifierString(node, isRootFloating = false, "fillMaxWidth()")
                appendLine("${indent}TopAppBar(")
                appendLine("${indent}    title = { Text(text = \"$title\") },")
                appendLine("${indent}    navigationIcon = {")
                appendLine("${indent}        IconButton(onClick = { /* TODO: Open navigation */ }) {")
                appendLine("${indent}            Icon(Icons.Outlined.Menu, contentDescription = \"Menu\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    actions = {")
                appendLine("${indent}        IconButton(onClick = { /* TODO: Search */ }) {")
                appendLine("${indent}            Icon(Icons.Outlined.Search, contentDescription = \"Search\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.NAVIGATION_BAR -> buildString {
                val mod = buildModifierString(node, isRootFloating = false, "fillMaxWidth()")
                appendLine("${indent}NavigationBar(modifier = $mod) {")
                appendLine("${indent}    NavigationBarItem(")
                appendLine("${indent}        selected = true,")
                appendLine("${indent}        onClick = { /* TODO: Navigate Home */ },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Home, contentDescription = \"Home\") },")
                appendLine("${indent}        label = { Text(\"Home\") }")
                appendLine("${indent}    )")
                appendLine("${indent}    NavigationBarItem(")
                appendLine("${indent}        selected = false,")
                appendLine("${indent}        onClick = { /* TODO: Navigate Explore */ },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Search, contentDescription = \"Explore\") },")
                appendLine("${indent}        label = { Text(\"Explore\") }")
                appendLine("${indent}    )")
                appendLine("${indent}    NavigationBarItem(")
                appendLine("${indent}        selected = false,")
                appendLine("${indent}        onClick = { /* TODO: Navigate Profile */ },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Person, contentDescription = \"Profile\") },")
                appendLine("${indent}        label = { Text(\"Profile\") }")
                appendLine("${indent}    )")
                appendLine("${indent}}")
            }

            ComponentType.BUTTON -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Button
                    ?: MaterialVariant.Button.FILLED
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Button"
                val composableName = when (variant) {
                    MaterialVariant.Button.FILLED -> "Button"
                    MaterialVariant.Button.TONAL -> "FilledTonalButton"
                    MaterialVariant.Button.OUTLINED -> "OutlinedButton"
                    MaterialVariant.Button.ELEVATED -> "ElevatedButton"
                    MaterialVariant.Button.TEXT -> "TextButton"
                }
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}$composableName(")
                appendLine("${indent}    onClick = { /* TODO: Button click action */ },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Text(text = \"$text\")")
                appendLine("${indent}}")
            }

            ComponentType.CARD -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.Card
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
                appendLine("${indent}FloatingActionButton(")
                appendLine("${indent}    onClick = { /* TODO: FAB Action */ },")
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
                appendLine("${indent}ExtendedFloatingActionButton(")
                appendLine("${indent}    onClick = { /* TODO: Action */ },")
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
                appendLine("${indent}    onClick = { /* TODO */ },")
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
                    MaterialVariant.Chip.ASSIST -> "AssistChip(onClick = { /* TODO */ }, label = { Text(\"$label\") })"
                    MaterialVariant.Chip.FILTER -> "FilterChip(selected = true, onClick = { /* TODO */ }, label = { Text(\"$label\") })"
                    MaterialVariant.Chip.INPUT -> "InputChip(selected = false, onClick = { /* TODO */ }, label = { Text(\"$label\") })"
                    MaterialVariant.Chip.SUGGESTION -> "SuggestionChip(onClick = { /* TODO */ }, label = { Text(\"$label\") })"
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
                appendLine("${indent}    onValueChange = { /* TODO: Update state */ },")
                appendLine("${indent}    label = { Text(\"${node.name}\") },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.SEGMENTED_BUTTON -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var selectedIndex by remember { mutableStateOf(0) }")
                appendLine("${indent}val options = listOf(\"Day\", \"Week\", \"Month\")")
                appendLine("${indent}SingleChoiceSegmentedButtonRow(modifier = $mod) {")
                appendLine("${indent}    options.forEachIndexed { index, label ->")
                appendLine("${indent}        SegmentedButton(")
                appendLine("${indent}            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),")
                appendLine("${indent}            onClick = { selectedIndex = index },")
                appendLine("${indent}            selected = index == selectedIndex")
                appendLine("${indent}        ) {")
                appendLine("${indent}            Text(label)")
                appendLine("${indent}        }")
                appendLine("${indent}    }")
                appendLine("${indent}}")
            }

            ComponentType.SPLIT_BUTTON -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Action"
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}Row(modifier = $mod, verticalAlignment = Alignment.CenterVertically) {")
                appendLine("${indent}    FilledTonalButton(")
                appendLine("${indent}        onClick = { /* TODO */ },")
                appendLine("${indent}        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)")
                appendLine("${indent}    ) {")
                appendLine("${indent}        Text(\"$text\")")
                appendLine("${indent}    }")
                appendLine("${indent}    Spacer(modifier = Modifier.width(2.dp))")
                appendLine("${indent}    FilledTonalButton(")
                appendLine("${indent}        onClick = { /* TODO */ },")
                appendLine("${indent}        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp),")
                appendLine("${indent}        contentPadding = PaddingValues(horizontal = 8.dp)")
                appendLine("${indent}    ) {")
                appendLine("${indent}        Icon(Icons.Outlined.ArrowDropDown, contentDescription = \"More\")")
                appendLine("${indent}    }")
                appendLine("${indent}}")
            }

            ComponentType.BUTTON_GROUP -> buildString {
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}Row(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    horizontalArrangement = Arrangement.spacedBy(8.dp),")
                appendLine("${indent}    verticalAlignment = Alignment.CenterVertically")
                appendLine("${indent}) {")
                appendLine("${indent}    Button(onClick = { /* TODO */ }) { Text(\"Primary\") }")
                appendLine("${indent}    FilledTonalButton(onClick = { /* TODO */ }) { Text(\"Tonal\") }")
                appendLine("${indent}    OutlinedButton(onClick = { /* TODO */ }) { Text(\"Cancel\") }")
                appendLine("${indent}}")
            }

            ComponentType.LISTS -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "List Item Headline"
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}ListItem(")
                appendLine("${indent}    headlineContent = { Text(\"$text\") },")
                appendLine("${indent}    supportingContent = { Text(\"Supporting secondary description\") },")
                appendLine("${indent}    leadingContent = { Icon(Icons.Outlined.Star, contentDescription = null) },")
                appendLine("${indent}    trailingContent = { Text(\"10:30\", style = MaterialTheme.typography.labelSmall) },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.SHEETS -> buildString {
                val title = (node.property("text") as? ComponentProperty.Text)?.value ?: "Modal Bottom Sheet"
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}Surface(")
                appendLine("${indent}    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),")
                appendLine("${indent}    color = MaterialTheme.colorScheme.surfaceContainerLow,")
                appendLine("${indent}    tonalElevation = 2.dp,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Column(modifier = Modifier.padding(16.dp)) {")
                appendLine("${indent}        Text(\"$title\", style = MaterialTheme.typography.titleMedium)")
                appendLine("${indent}        Spacer(modifier = Modifier.height(8.dp))")
                appendLine("${indent}        Text(\"Supplementary modal bottom sheet content.\", style = MaterialTheme.typography.bodyMedium)")
                appendLine("${indent}        Spacer(modifier = Modifier.height(16.dp))")
                appendLine("${indent}        Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {")
                appendLine("${indent}            Text(\"Confirm\")")
                appendLine("${indent}        }")
                appendLine("${indent}    }")
                appendLine("${indent}}")
            }

            ComponentType.DIALOG -> buildString {
                val title = (node.property("text") as? ComponentProperty.Text)?.value ?: "Dialog Title"
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}AlertDialog(")
                appendLine("${indent}    onDismissRequest = { /* TODO */ },")
                appendLine("${indent}    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },")
                appendLine("${indent}    title = { Text(\"$title\") },")
                appendLine("${indent}    text = { Text(\"Dialogs inform users about a task and can contain critical information.\") },")
                appendLine("${indent}    confirmButton = {")
                appendLine("${indent}        Button(onClick = { /* TODO */ }) { Text(\"Confirm\") }")
                appendLine("${indent}    },")
                appendLine("${indent}    dismissButton = {")
                appendLine("${indent}        TextButton(onClick = { /* TODO */ }) { Text(\"Cancel\") }")
                appendLine("${indent}    },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.SNACKBAR -> buildString {
                val message = (node.property("text") as? ComponentProperty.Text)?.value ?: "Snackbar notification alert."
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}Snackbar(")
                appendLine("${indent}    action = {")
                appendLine("${indent}        TextButton(onClick = { /* TODO */ }) {")
                appendLine("${indent}            Text(\"Dismiss\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Text(\"$message\")")
                appendLine("${indent}}")
            }

            ComponentType.BADGE -> buildString {
                val count = (node.property("text") as? ComponentProperty.Text)?.value ?: "3"
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}BadgedBox(")
                appendLine("${indent}    badge = { Badge { Text(\"$count\") } },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Icon(Icons.Outlined.Notifications, contentDescription = \"Notifications\")")
                appendLine("${indent}}")
            }

            ComponentType.TOOLTIP -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Helpful tooltip label"
                val mod = buildModifierString(node, isRootFloating)
                appendLine("${indent}Surface(")
                appendLine("${indent}    shape = RoundedCornerShape(4.dp),")
                appendLine("${indent}    color = MaterialTheme.colorScheme.inverseSurface,")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Text(")
                appendLine("${indent}        text = \"$text\",")
                appendLine("${indent}        style = MaterialTheme.typography.labelSmall,")
                appendLine("${indent}        color = MaterialTheme.colorScheme.inverseOnSurface,")
                appendLine("${indent}        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)")
                appendLine("${indent}    )")
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
                appendLine("${indent}var selectedItem by remember { mutableStateOf(0) }")
                appendLine("${indent}NavigationRail(modifier = $mod) {")
                appendLine("${indent}    NavigationRailItem(")
                appendLine("${indent}        selected = selectedItem == 0,")
                appendLine("${indent}        onClick = { selectedItem = 0 },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Home, contentDescription = \"Home\") },")
                appendLine("${indent}        label = { Text(\"Home\") }")
                appendLine("${indent}    )")
                appendLine("${indent}    NavigationRailItem(")
                appendLine("${indent}        selected = selectedItem == 1,")
                appendLine("${indent}        onClick = { selectedItem = 1 },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Search, contentDescription = \"Search\") },")
                appendLine("${indent}        label = { Text(\"Search\") }")
                appendLine("${indent}    )")
                appendLine("${indent}    NavigationRailItem(")
                appendLine("${indent}        selected = selectedItem == 2,")
                appendLine("${indent}        onClick = { selectedItem = 2 },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Settings, contentDescription = \"Settings\") },")
                appendLine("${indent}        label = { Text(\"Settings\") }")
                appendLine("${indent}    )")
                appendLine("${indent}}")
            }

            ComponentType.NAVIGATION_DRAWER -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxHeight()")
                appendLine("${indent}var selectedItem by remember { mutableStateOf(0) }")
                appendLine("${indent}ModalDrawerSheet(modifier = $mod) {")
                appendLine("${indent}    Text(\"${node.name}\", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)")
                appendLine("${indent}    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))")
                appendLine("${indent}    NavigationDrawerItem(")
                appendLine("${indent}        label = { Text(\"Inbox\") },")
                appendLine("${indent}        selected = selectedItem == 0,")
                appendLine("${indent}        onClick = { selectedItem = 0 },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Inbox, contentDescription = null) },")
                appendLine("${indent}        badge = { Text(\"12\") }")
                appendLine("${indent}    )")
                appendLine("${indent}    NavigationDrawerItem(")
                appendLine("${indent}        label = { Text(\"Outbox\") },")
                appendLine("${indent}        selected = selectedItem == 1,")
                appendLine("${indent}        onClick = { selectedItem = 1 },")
                appendLine("${indent}        icon = { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}    NavigationDrawerItem(")
                appendLine("${indent}        label = { Text(\"Favorites\") },")
                appendLine("${indent}        selected = selectedItem == 2,")
                appendLine("${indent}        onClick = { selectedItem = 2 },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}}")
            }

            ComponentType.TABS -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var selectedTab by remember { mutableStateOf(0) }")
                appendLine("${indent}PrimaryTabRow(selectedTabIndex = selectedTab, modifier = $mod) {")
                appendLine("${indent}    Tab(")
                appendLine("${indent}        selected = selectedTab == 0,")
                appendLine("${indent}        onClick = { selectedTab = 0 },")
                appendLine("${indent}        text = { Text(\"Overview\") },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Dashboard, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}    Tab(")
                appendLine("${indent}        selected = selectedTab == 1,")
                appendLine("${indent}        onClick = { selectedTab = 1 },")
                appendLine("${indent}        text = { Text(\"Analytics\") },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Analytics, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}    Tab(")
                appendLine("${indent}        selected = selectedTab == 2,")
                appendLine("${indent}        onClick = { selectedTab = 2 },")
                appendLine("${indent}        text = { Text(\"Settings\") },")
                appendLine("${indent}        icon = { Icon(Icons.Outlined.Tune, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}}")
            }

            ComponentType.SEARCH -> buildString {
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}var searchQuery by remember { mutableStateOf(\"\") }")
                appendLine("${indent}SearchBar(")
                appendLine("${indent}    inputField = {")
                appendLine("${indent}        SearchBarDefaults.InputField(")
                appendLine("${indent}            query = searchQuery,")
                appendLine("${indent}            onQueryChange = { searchQuery = it },")
                appendLine("${indent}            onSearch = { /* TODO */ },")
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
                appendLine("${indent}    DropdownMenuItem(")
                appendLine("${indent}        text = { Text(\"Edit\") },")
                appendLine("${indent}        onClick = { /* TODO */ },")
                appendLine("${indent}        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}    DropdownMenuItem(")
                appendLine("${indent}        text = { Text(\"Duplicate\") },")
                appendLine("${indent}        onClick = { /* TODO */ },")
                appendLine("${indent}        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}    HorizontalDivider()")
                appendLine("${indent}    DropdownMenuItem(")
                appendLine("${indent}        text = { Text(\"Delete\") },")
                appendLine("${indent}        onClick = { /* TODO */ },")
                appendLine("${indent}        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }")
                appendLine("${indent}    )")
                appendLine("${indent}}")
            }

            ComponentType.BOTTOM_APP_BAR -> buildString {
                val mod = buildModifierString(node, isRootFloating = false, "fillMaxWidth()")
                appendLine("${indent}BottomAppBar(")
                appendLine("${indent}    actions = {")
                appendLine("${indent}        IconButton(onClick = { /* TODO */ }) {")
                appendLine("${indent}            Icon(Icons.Outlined.Menu, contentDescription = \"Menu\")")
                appendLine("${indent}        }")
                appendLine("${indent}        IconButton(onClick = { /* TODO */ }) {")
                appendLine("${indent}            Icon(Icons.Outlined.Search, contentDescription = \"Search\")")
                appendLine("${indent}        }")
                appendLine("${indent}        IconButton(onClick = { /* TODO */ }) {")
                appendLine("${indent}            Icon(Icons.Outlined.FavoriteBorder, contentDescription = \"Favorite\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
                appendLine("${indent}    floatingActionButton = {")
                appendLine("${indent}        FloatingActionButton(")
                appendLine("${indent}            onClick = { /* TODO */ },")
                appendLine("${indent}            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()")
                appendLine("${indent}        ) {")
                appendLine("${indent}            Icon(Icons.Filled.Add, contentDescription = \"Add\")")
                appendLine("${indent}        }")
                appendLine("${indent}    },")
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

        if (isRootFloating) {
            parts.add("offset(x = ${node.position.x.toInt()}.dp, y = ${node.position.y.toInt()}.dp)")
            parts.add("size(width = ${node.size.width.toInt()}.dp, height = ${node.size.height.toInt()}.dp)")
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
                is ModifierSpec.Clickable -> parts.add("clickable(enabled = ${spec.enabled}) { /* TODO */ }")
            }
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

    private fun sanitizeName(raw: String): String {
        val clean = raw.replace(Regex("[^A-Za-z0-9]"), "")
        return if (clean.isEmpty()) "M3EScreen" else "${clean.replaceFirstChar { it.uppercase() }}Screen"
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
}
