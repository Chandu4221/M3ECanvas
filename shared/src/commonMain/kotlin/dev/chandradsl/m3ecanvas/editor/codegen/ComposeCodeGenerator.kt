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
            appendLine("import androidx.compose.foundation.layout.*")
            appendLine("import androidx.compose.foundation.lazy.LazyColumn")
            appendLine("import androidx.compose.foundation.lazy.LazyRow")
            appendLine("import androidx.compose.foundation.lazy.items")
            appendLine("import androidx.compose.foundation.rememberScrollState")
            appendLine("import androidx.compose.foundation.shape.RoundedCornerShape")
            appendLine("import androidx.compose.foundation.verticalScroll")
            appendLine("import androidx.compose.foundation.horizontalScroll")
            appendLine("import androidx.compose.material.icons.Icons")
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
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.ui.text.font.FontWeight")
            appendLine("import androidx.compose.ui.unit.dp")
            appendLine()
            appendLine("@OptIn(ExperimentalMaterial3Api::class)")
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
                val scroll = if (node.layoutConfig.scrollable) ".verticalScroll(rememberScrollState())" else ""
                val fullMod = if (scroll.isNotEmpty()) "$mod$scroll" else mod
                appendLine("${indent}Column(")
                appendLine("${indent}    modifier = $fullMod,")
                appendLine("${indent}    verticalArrangement = $arrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.ROW -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val arrangement = resolveArrangementString(node.layoutConfig, isVertical = false)
                val scroll = if (node.layoutConfig.scrollable) ".horizontalScroll(rememberScrollState())" else ""
                val fullMod = if (scroll.isNotEmpty()) "$mod$scroll" else mod
                appendLine("${indent}Row(")
                appendLine("${indent}    modifier = $fullMod,")
                appendLine("${indent}    horizontalArrangement = $arrangement")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.BOX -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}Box(")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                node.children.forEach { child ->
                    append(generateNodeCode(child, indent = "$indent    "))
                }
                appendLine("${indent}}")
            }

            ComponentType.LAZY_COLUMN -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                val arrangement = resolveArrangementString(node.layoutConfig, isVertical = true)
                appendLine("${indent}LazyColumn(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    verticalArrangement = $arrangement")
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
                appendLine("${indent}LazyRow(")
                appendLine("${indent}    modifier = $mod,")
                appendLine("${indent}    horizontalArrangement = $arrangement")
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
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}FloatingActionButton(")
                appendLine("${indent}    onClick = { /* TODO: FAB Action */ },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent}) {")
                appendLine("${indent}    Icon(Icons.Filled.Add, contentDescription = \"Add\")")
                appendLine("${indent}}")
            }

            ComponentType.EXTENDED_FAB -> buildString {
                val text = (node.property("text") as? ComponentProperty.Text)?.value ?: "Create"
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}ExtendedFloatingActionButton(")
                appendLine("${indent}    onClick = { /* TODO: Action */ },")
                appendLine("${indent}    icon = { Icon(Icons.Filled.Add, contentDescription = null) },")
                appendLine("${indent}    text = { Text(text = \"$text\") },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            ComponentType.ICON_BUTTON -> buildString {
                val variant = (node.property("variant") as? ComponentProperty.Variant)?.value as? MaterialVariant.IconButton
                    ?: MaterialVariant.IconButton.STANDARD
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
                appendLine("${indent}    Icon(Icons.Filled.Add, contentDescription = null)")
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
                val mod = buildModifierString(node, isRootFloating, "fillMaxWidth()")
                appendLine("${indent}OutlinedTextField(")
                appendLine("${indent}    value = \"$text\",")
                appendLine("${indent}    onValueChange = { /* TODO: Update state */ },")
                appendLine("${indent}    label = { Text(\"${node.name}\") },")
                appendLine("${indent}    modifier = $mod")
                appendLine("${indent})")
            }

            else -> buildString {
                val mod = buildModifierString(node, isRootFloating, extraModifier)
                appendLine("${indent}// Placeholder for ${node.type.displayName}")
                appendLine("${indent}Box(modifier = $mod) {")
                appendLine("${indent}    Text(\"${node.type.displayName}\")")
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
                is ModifierSpec.Padding -> parts.add("padding(${spec.all.toInt()}.dp)")
                is ModifierSpec.Background -> parts.add("background(Color(0x${spec.colorHex.removePrefix("#")}))")
                is ModifierSpec.Border -> parts.add("border(${spec.width.toInt()}.dp, Color(0x${spec.colorHex.removePrefix("#")}), RoundedCornerShape(8.dp))")
                is ModifierSpec.Clip -> parts.add("clip(RoundedCornerShape(${spec.cornerRadius.toInt()}.dp))")
                is ModifierSpec.Alpha -> parts.add("alpha(${spec.value}f)")
                is ModifierSpec.Rotation -> parts.add("rotate(${spec.degrees}f)")
                is ModifierSpec.Scale -> parts.add("scale(${spec.value}f)")
                is ModifierSpec.FillMaxWidth -> parts.add("fillMaxWidth()")
                is ModifierSpec.FillMaxHeight -> parts.add("fillMaxHeight()")
            }
        }

        return if (parts.isEmpty()) {
            "Modifier"
        } else {
            "Modifier." + parts.joinToString(".")
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
}
