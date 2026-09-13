package dev.chandradsl.m3ecanvas.editor.codegen

import dev.chandradsl.m3ecanvas.domain.model.*

/**
 * Generates high-context, structured prompts designed for AI coding assistants
 * (Cursor, Claude Code, Gemini CLI) to build complete working features around
 * the visual M3E Canvas screen.
 */
open class AIPromptGenerator(
    private val codeGenerator: ComposeCodeGenerator = ComposeCodeGenerator.default
) {

    fun generate(project: M3EProject): String {
        return buildString {
            appendLine("# Material 3 Expressive Screen Specification: ${project.name}")
            appendLine()
            appendLine("You are an expert Android and Kotlin Multiplatform developer. Implement the following screen specification in Jetpack Compose using Material 3 Expressive components and best architectural practices (MVI/MVVM with state hoisting).")
            appendLine()
            appendLine("## Target Device Specifications")
            appendLine("- **Device Profile**: ${project.deviceProfile.displayName} (${project.deviceProfile.category.displayName})")
            appendLine("- **Viewport Resolution**: ${project.deviceProfile.size.width.toInt()}dp × ${project.deviceProfile.size.height.toInt()}dp")
            appendLine()
            appendLine("## UI Hierarchy & Structural Slots")
            appendLine()
            if (project.nodes.isEmpty()) {
                appendLine("- Empty screen canvas.")
            } else {
                project.nodes.forEach { root ->
                    append(describeNodeTree(root, depth = 0))
                }
            }
            appendLine()
            appendLine("## Generated Compose Scaffold & Layout Code")
            appendLine("```kotlin")
            append(codeGenerator.generateFile(project))
            appendLine("```")
            appendLine()
            appendLine("## Implementation Instructions for AI")
            appendLine("1. **State Hoisting**: Define a strongly typed `StateFlow` and UI event sealed interface (`UiEvent`) in a corresponding ViewModel.")
            appendLine("2. **Slot Architecture**: Preserve the Scaffold slots (`topBar`, `bottomBar`, `floatingActionButton`) as specified above.")
            appendLine("3. **Material 3 Expressive Tokens**: Utilize `MaterialTheme.colorScheme` and `MaterialTheme.typography` rather than hardcoded colors or text styles.")
            appendLine("4. **Interactivity**: Hook up click handlers for all buttons, chips, and navigation items to emit events to the ViewModel.")
        }
    }

    private fun describeNodeTree(node: CanvasNode, depth: Int): String {
        val indent = "  ".repeat(depth)
        val slotInfo = if (node.slot != null) " [Slot: ${node.slot.displayName}]" else ""
        val props = if (node.properties.isNotEmpty()) {
            " (${node.properties.joinToString(", ") { "${it.key}: ${propertyValue(it)}" }})"
        } else {
            ""
        }
        val modifiers = if (node.modifiers.isNotEmpty()) {
            " | Modifiers: [${node.modifiers.joinToString(", ") { it.label() }}]"
        } else {
            ""
        }

        return buildString {
            appendLine("$indent- **${node.type.displayName}** (\"${node.name}\")$slotInfo$props$modifiers")
            node.children.forEach { child ->
                append(describeNodeTree(child, depth = depth + 1))
            }
        }
    }

    private fun propertyValue(prop: ComponentProperty): String {
        return when (prop) {
            is ComponentProperty.Text -> "\"${prop.value}\""
            is ComponentProperty.BooleanFlag -> prop.value.toString()
            is ComponentProperty.Numeric -> prop.value.toString()
            is ComponentProperty.ColorHex -> prop.value
            is ComponentProperty.Variant -> prop.value.displayName
            is ComponentProperty.Icon -> prop.iconName
            is ComponentProperty.Asset -> prop.assetId
        }
    }

    companion object {
        val default: AIPromptGenerator by lazy { AIPromptGenerator() }

        fun generate(project: M3EProject): String = default.generate(project)
    }
}
