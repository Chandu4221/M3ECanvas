package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.codegen.ComposeCodeGenerator
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Formal WYSIWYG Fidelity test suite enforcing Rule 48 of the M3ECanvas specification:
 * Preview and Code Generation must represent the EXACT same document semantics.
 * Every component must map to a real Compose Material 3 API with zero placeholders or fake fallbacks.
 */
class WysiwygFidelityTest {

    private val registry = ComponentRegistry.default

    @Test
    fun testEveryComponentDefinitionMatchesCategoryAndSemantics() {
        for (type in ComponentType.entries) {
            val def = registry.get(type)
            assertNotNull(def, "Missing ComponentDefinition in registry for $type")
            assertEquals(type.category, def.category, "Category mismatch for $type")
            assertEquals(type.isContainer, def.isContainer, "Container flag mismatch for $type")
        }
    }

    @Test
    fun testCoreActionComponentsFidelity() {
        val actionTypes = listOf(
            ComponentType.BUTTON,
            ComponentType.ICON_BUTTON,
            ComponentType.FAB,
            ComponentType.EXTENDED_FAB,
            ComponentType.SEGMENTED_BUTTON
        )
        for (type in actionTypes) {
            val node = CanvasNode(
                type = type,
                name = "ActionNode",
                position = CanvasPosition(10f, 10f),
                size = EditorController.defaultSizeFor(type)
            )
            val project = M3EProject(name = "ActionTest", nodes = listOf(node))
            val code = ComposeCodeGenerator.generateFile(project)

            assertFalse(code.contains("TODO"), "Code for $type contains TODO")
            assertFalse(code.contains("Placeholder"), "Code for $type contains placeholder")
            assertTrue(code.contains("import androidx.compose.material3."), "Missing M3 import for $type")
        }
    }

    @Test
    fun testDialogAndSheetFidelity() {
        val dialogNode = CanvasNode(
            type = ComponentType.ALERT_DIALOG,
            name = "MyDialog",
            position = CanvasPosition.Zero,
            size = CanvasSize(320f, 240f),
            properties = listOf(ComponentProperty.Text(key = "text", value = "Confirm Action"))
        )
        val project = M3EProject(name = "DialogTest", nodes = listOf(dialogNode))
        val code = ComposeCodeGenerator.generateFile(project)

        assertTrue(code.contains("AlertDialog("), "Expected real AlertDialog in generated code")
        assertTrue(code.contains("onDismissRequest = {}"), "Expected clean onDismissRequest lambda")
        assertFalse(code.contains("TODO"), "Generated dialog code must not have TODOs")
    }

    @Test
    fun testPickerFidelity() {
        val datePickerNode = CanvasNode(
            type = ComponentType.DATE_PICKER,
            name = "MyDatePicker",
            position = CanvasPosition.Zero,
            size = CanvasSize(360f, 480f)
        )
        val timePickerNode = CanvasNode(
            type = ComponentType.TIME_PICKER,
            name = "MyTimePicker",
            position = CanvasPosition.Zero,
            size = CanvasSize(360f, 400f)
        )
        val project = M3EProject(name = "PickerTest", nodes = listOf(datePickerNode, timePickerNode))
        val code = ComposeCodeGenerator.generateFile(project)

        assertTrue(code.contains("rememberDatePickerState("), "Expected DatePickerState in codegen")
        assertTrue(code.contains("DatePicker("), "Expected real DatePicker in codegen")
        assertTrue(code.contains("rememberTimePickerState("), "Expected TimePickerState in codegen")
        assertTrue(code.contains("TimePicker("), "Expected real TimePicker in codegen")
        assertFalse(code.contains("TODO"), "Pickers must not have TODO comments")
    }

    @Test
    fun testNavigationComponentsFidelity() {
        val navBar = CanvasNode(
            type = ComponentType.NAVIGATION_BAR,
            name = "NavBar",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 80f),
            slot = SlotRole.BOTTOM_BAR
        )
        val scaffold = CanvasNode(
            type = ComponentType.SCAFFOLD,
            name = "Scaffold",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 915f),
            children = listOf(navBar)
        )
        val project = M3EProject(name = "NavTest", nodes = listOf(scaffold))
        val code = ComposeCodeGenerator.generateFile(project)

        assertTrue(code.contains("Scaffold("), "Expected Scaffold")
        assertTrue(code.contains("NavigationBar("), "Expected real NavigationBar")
        assertTrue(code.contains("bottomBar = {"), "Expected slotted bottomBar")
    }

    @Test
    fun testIntrinsicHugContentMeasurementContract() {
        // Container children must hug intrinsic content when no size modifier is explicitly specified
        val button = CanvasNode(
            type = ComponentType.BUTTON,
            name = "HugButton",
            position = CanvasPosition.Zero,
            size = CanvasSize(100f, 40f) // default bounding box hint
        )
        val column = CanvasNode(
            type = ComponentType.COLUMN,
            name = "ContainerCol",
            position = CanvasPosition.Zero,
            size = CanvasSize(200f, 300f),
            children = listOf(button)
        )
        val project = M3EProject(name = "HugTest", nodes = listOf(column))
        val code = ComposeCodeGenerator.generateFile(project)

        // The button has no explicit width/height modifier, so in code it does not generate modifier.size
        assertFalse(
            code.contains("Modifier.size(width = 100.dp, height = 40.dp)"),
            "Container child without explicit size modifier must not force fixed dimensions"
        )
    }
}
