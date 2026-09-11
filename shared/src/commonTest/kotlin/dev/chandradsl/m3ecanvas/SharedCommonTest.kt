package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.codegen.AIPromptGenerator
import dev.chandradsl.m3ecanvas.editor.codegen.ComposeCodeGenerator
import dev.chandradsl.m3ecanvas.editor.persistence.M3EJson
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlin.test.*

class SharedCommonTest {

    @Test
    fun testSlotRoleCanonicalMapping() {
        assertEquals(SlotRole.TOP_BAR, ComponentType.TOP_APP_BAR.canonicalSlot())
        assertEquals(SlotRole.BOTTOM_BAR, ComponentType.NAVIGATION_BAR.canonicalSlot())
        assertEquals(SlotRole.BOTTOM_BAR, ComponentType.NAVIGATION_RAIL.canonicalSlot())
        assertEquals(SlotRole.FAB, ComponentType.FAB.canonicalSlot())
        assertEquals(SlotRole.FAB, ComponentType.EXTENDED_FAB.canonicalSlot())
        assertEquals(SlotRole.SNACKBAR, ComponentType.SNACKBAR.canonicalSlot())
        assertEquals(SlotRole.CONTENT, ComponentType.COLUMN.canonicalSlot())
        assertEquals(SlotRole.CONTENT, ComponentType.BUTTON.canonicalSlot())
    }

    @Test
    fun testIsLockedInSlot() {
        val topBarNode = CanvasNode(
            type = ComponentType.TOP_APP_BAR,
            name = "TopBar",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 64f),
            slot = SlotRole.TOP_BAR
        )
        assertTrue(topBarNode.isLockedInSlot)

        val fabNode = CanvasNode(
            type = ComponentType.FAB,
            name = "FAB",
            position = CanvasPosition.Zero,
            size = CanvasSize(56f, 56f),
            slot = SlotRole.FAB
        )
        assertTrue(fabNode.isLockedInSlot)

        val contentNode = CanvasNode(
            type = ComponentType.COLUMN,
            name = "Content",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 700f),
            slot = SlotRole.CONTENT
        )
        assertFalse(contentNode.isLockedInSlot)

        val freeNode = CanvasNode(
            type = ComponentType.BUTTON,
            name = "Button",
            position = CanvasPosition(20f, 20f),
            size = CanvasSize(120f, 40f),
            slot = null
        )
        assertFalse(freeNode.isLockedInSlot)
    }

    @Test
    fun testScaffoldWithChildInSlotReplacesSingleSlots() {
        var scaffold = CanvasNode(
            type = ComponentType.SCAFFOLD,
            name = "Scaffold",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 915f)
        )

        val topBar1 = CanvasNode(
            type = ComponentType.TOP_APP_BAR,
            name = "TopBar 1",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 64f),
            slot = SlotRole.TOP_BAR
        )
        scaffold = scaffold.withChildInSlot(child = topBar1, slotRole = SlotRole.TOP_BAR)
        assertEquals("TopBar 1", scaffold.childInSlot(SlotRole.TOP_BAR)?.name)
        assertEquals(1, scaffold.children.size)

        val topBar2 = CanvasNode(
            type = ComponentType.TOP_APP_BAR,
            name = "TopBar 2",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 64f),
            slot = SlotRole.TOP_BAR
        )
        scaffold = scaffold.withChildInSlot(child = topBar2, slotRole = SlotRole.TOP_BAR)
        assertEquals("TopBar 2", scaffold.childInSlot(SlotRole.TOP_BAR)?.name)
        assertEquals(1, scaffold.children.size)
    }

    @Test
    fun testEditorControllerScaffoldAutomaticSlotSnapping() {
        val project = EditorController.newProject(name = "TestProject", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        // Add a Navigation Bar to Scaffold
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.NAVIGATION_BAR)
        val updatedScaffold = controller.state.project.findNode(scaffold.id)!!
        val bottomBar = updatedScaffold.childInSlot(SlotRole.BOTTOM_BAR)
        assertNotNull(bottomBar)
        assertEquals(ComponentType.NAVIGATION_BAR, bottomBar.type)
        assertTrue(bottomBar.isLockedInSlot)

        // Attempt to drag the locked BottomBar - should NOT start drag
        controller.startDrag(nodeId = bottomBar.id)
        assertNull(controller.state.drag)

        // Attempt to nudge the locked node
        val originalPos = bottomBar.position
        controller.selectNode(bottomBar.id)
        controller.nudgeSelected(dx = 10f, dy = 10f)
        val unnudged = controller.state.project.findNode(bottomBar.id)!!
        assertEquals(originalPos, unnudged.position)
    }

    @Test
    fun testScaffoldSerializationRoundTrip() {
        val project = EditorController.newProject(name = "SavedProject", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.FAB)

        val jsonString = M3EJson.encodeToString(controller.state.project)
        val decoded = M3EJson.decodeFromString<M3EProject>(jsonString)

        assertEquals(controller.state.project.id, decoded.id)
        assertEquals(controller.state.project.name, decoded.name)
        val decodedScaffold = decoded.nodes.first { it.type == ComponentType.SCAFFOLD }
        assertNotNull(decodedScaffold.childInSlot(SlotRole.TOP_BAR))
        assertNotNull(decodedScaffold.childInSlot(SlotRole.FAB))
        assertNotNull(decodedScaffold.childInSlot(SlotRole.CONTENT))
    }

    @Test
    fun testComposeCodeGeneratorEmitsValidScaffoldCode() {
        val project = EditorController.newProject(name = "Dashboard", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        // Add a bottom navigation bar
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.NAVIGATION_BAR)

        // Add a button with TONAL variant inside the content column
        val contentCol = scaffold.children.first { it.slot == SlotRole.CONTENT }
        controller.addChildToContainer(containerId = contentCol.id, type = ComponentType.BUTTON)
        val buttonNode = controller.state.project.findNode(contentCol.id)!!.children.first { it.type == ComponentType.BUTTON }
        controller.updateNodeProperty(
            nodeId = buttonNode.id,
            property = ComponentProperty.Variant(key = "variant", value = MaterialVariant.Button.TONAL)
        )
        controller.addModifier(
            nodeId = buttonNode.id,
            spec = ModifierSpec.Clip(cornerRadius = 16f)
        )

        val code = ComposeCodeGenerator.generateFile(controller.state.project)

        assertTrue(code.contains("@Composable"))
        assertTrue(code.contains("fun DashboardScreen("))
        assertTrue(code.contains("Scaffold("))
        assertTrue(code.contains("topBar = {"))
        assertTrue(code.contains("TopAppBar("))
        assertTrue(code.contains("bottomBar = {"))
        assertTrue(code.contains("NavigationBar("))
        assertTrue(code.contains("FilledTonalButton("))
        assertTrue(code.contains("clip(RoundedCornerShape(16.dp))"))
        assertTrue(code.contains("innerPadding ->"))
    }

    @Test
    fun testAIPromptGeneratorIncludesArchitectureAndSpecs() {
        val project = EditorController.newProject(name = "Analytics", withDefaultScaffold = true)
        val prompt = AIPromptGenerator.generate(project)

        assertTrue(prompt.contains("# Material 3 Expressive Screen Specification: Analytics"))
        assertTrue(prompt.contains("Target Device Specifications"))
        assertTrue(prompt.contains("Pixel 8"))
        assertTrue(prompt.contains("412dp × 915dp"))
        assertTrue(prompt.contains("[Slot: Top App Bar]"))
        assertTrue(prompt.contains("Implementation Instructions for AI"))
        assertTrue(prompt.contains("State Hoisting"))
    }
}