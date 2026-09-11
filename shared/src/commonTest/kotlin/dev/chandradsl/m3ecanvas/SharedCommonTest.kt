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

    @Test
    fun testHistoryStackBoundedPushAndUndoRedo() {
        val stack = dev.chandradsl.m3ecanvas.editor.history.HistoryStack<String>(maxDepth = 3)
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)

        stack.push("State 1")
        stack.push("State 2")
        stack.push("State 3")
        stack.push("State 4") // Overflows capacity, State 1 dropped

        assertEquals(3, stack.undoCount)
        assertTrue(stack.canUndo)

        // Undo 1
        val u1 = stack.undo("State Current")
        assertEquals("State 4", u1)
        assertTrue(stack.canRedo)

        // Undo 2
        val u2 = stack.undo(u1!!)
        assertEquals("State 3", u2)

        // Undo 3
        val u3 = stack.undo(u2!!)
        assertEquals("State 2", u3)

        // Oldest (State 1) was evicted by maxDepth = 3
        assertNull(stack.undo(u3!!))

        // Redo 1
        val r1 = stack.redo(u3)
        assertEquals("State 3", r1)

        // New action clears redo
        stack.push("New State")
        assertFalse(stack.canRedo)
        assertEquals(2, stack.undoCount)
    }

    @Test
    fun testCanvasNodeDeepCloneWithNewIds() {
        val originalChild = CanvasNode(
            type = ComponentType.BUTTON,
            name = "Submit Button",
            position = CanvasPosition(10f, 10f),
            size = CanvasSize(100f, 40f),
            modifiers = listOf(ModifierSpec.Padding(all = 12f))
        )
        val originalParent = CanvasNode(
            type = ComponentType.COLUMN,
            name = "Form",
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(200f, 200f),
            children = listOf(originalChild),
            modifiers = listOf(ModifierSpec.Border(width = 1f, colorHex = "#FF000000"))
        )

        val clone = originalParent.deepCloneWithNewIds(offsetPosition = true, dx = 15f, dy = 15f)

        // Root checks
        assertNotEquals(originalParent.id, clone.id)
        assertEquals(CanvasPosition(15f, 15f), clone.position)
        assertEquals(1, clone.modifiers.size)
        assertNotEquals(originalParent.modifiers.first().id, clone.modifiers.first().id)

        // Child checks
        assertEquals(1, clone.children.size)
        val clonedChild = clone.children.first()
        assertNotEquals(originalChild.id, clonedChild.id)
        assertEquals("Submit Button", clonedChild.name)
        assertNotEquals(originalChild.modifiers.first().id, clonedChild.modifiers.first().id)
    }

    @Test
    fun testEditorControllerUndoRedoLifecycle() {
        val project = EditorController.newProject(name = "UndoTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val contentCol = scaffold.children.first { it.slot == SlotRole.CONTENT }

        assertFalse(controller.state.canUndo)
        assertFalse(controller.state.canRedo)

        // 1. Add a button inside content column
        controller.addChildToContainer(containerId = contentCol.id, type = ComponentType.BUTTON)
        assertTrue(controller.state.canUndo)
        assertFalse(controller.state.canRedo)

        val buttonId = controller.state.selectedNodeIds.first()
        assertNotNull(controller.state.project.findNode(buttonId))

        // 2. Undo the addition
        controller.undo()
        assertNull(controller.state.project.findNode(buttonId))
        assertTrue(controller.state.canRedo)
        assertFalse(controller.state.canUndo)

        // 3. Redo the addition
        controller.redo()
        assertNotNull(controller.state.project.findNode(buttonId))
        assertFalse(controller.state.canRedo)
        assertTrue(controller.state.canUndo)
    }

    @Test
    fun testEditorControllerDuplicateAndCopyPaste() {
        val project = EditorController.newProject(name = "ClipboardTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val contentCol = scaffold.children.first { it.slot == SlotRole.CONTENT }

        // Add initial button
        controller.addChildToContainer(containerId = contentCol.id, type = ComponentType.BUTTON)
        val originalButton = controller.state.selectedNodes.first()

        // Test Duplicate
        val duplicated = controller.duplicateSelected()
        assertNotNull(duplicated)
        assertNotEquals(originalButton.id, duplicated.id)
        val updatedContentCol = controller.state.project.findNode(contentCol.id)!!
        assertEquals(2, updatedContentCol.children.size)

        // Test Copy and Paste
        controller.selectNode(originalButton.id)
        val copied = controller.copySelected()
        assertNotNull(copied)
        assertEquals(originalButton.id, copied.id)

        val pasted = controller.paste()
        assertNotNull(pasted)
        assertNotEquals(originalButton.id, pasted.id)
        val colAfterPaste = controller.state.project.findNode(contentCol.id)!!
        assertEquals(3, colAfterPaste.children.size)

        // Test Undo removes the pasted node
        controller.undo()
        val colAfterUndo = controller.state.project.findNode(contentCol.id)!!
        assertEquals(2, colAfterUndo.children.size)
        assertNull(controller.state.project.findNode(pasted.id))
    }

    @Test
    fun testAllComponentTypesEmitValidCodeWithoutPlaceholders() {
        assertEquals(37, ComponentType.entries.size)
        for (type in ComponentType.entries) {
            val node = CanvasNode(
                type = type,
                name = type.displayName,
                position = CanvasPosition.Zero,
                size = EditorController.defaultSizeFor(type)
            )
            val project = M3EProject(
                name = "Test_${type.name}",
                nodes = listOf(node)
            )
            val code = ComposeCodeGenerator.generateFile(project)
            assertTrue(code.contains("@Composable"), "Missing @Composable for ${type.name}")
            assertFalse(
                code.contains("// Placeholder for"),
                "Found placeholder comment for ${type.name} in generated code:\n$code"
            )
        }
    }

    @Test
    fun testAllComponentTypesHaveNonZeroDefaultSize() {
        assertEquals(37, ComponentType.entries.size)
        for (type in ComponentType.entries) {
            val size = EditorController.defaultSizeFor(type)
            assertTrue(size.width > 0f, "Width must be > 0 for ${type.name}")
            assertTrue(size.height > 0f, "Height must be > 0 for ${type.name}")
        }
    }
}