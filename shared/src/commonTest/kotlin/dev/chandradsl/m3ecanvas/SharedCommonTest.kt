package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.canvas.AVAILABLE_MATERIAL_ICONS
import dev.chandradsl.m3ecanvas.editor.canvas.resolveMaterialIcon
import dev.chandradsl.m3ecanvas.editor.codegen.AIPromptGenerator
import dev.chandradsl.m3ecanvas.editor.codegen.ComposeCodeGenerator
import dev.chandradsl.m3ecanvas.editor.component.ComponentDefinition
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry
import dev.chandradsl.m3ecanvas.editor.persistence.M3EJson
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.state.GuideOrientation
import dev.chandradsl.m3ecanvas.editor.theme.CanvasThemeGenerator
import dev.chandradsl.m3ecanvas.editor.canvas.AlignmentSnapper
import dev.chandradsl.m3ecanvas.editor.canvas.SnapResult
import dev.chandradsl.m3ecanvas.editor.codegen.ProjectCodeExporter
import dev.chandradsl.m3ecanvas.editor.inspector.ContrastCalculator
import dev.chandradsl.m3ecanvas.editor.inspector.ContrastLevel
import dev.chandradsl.m3ecanvas.editor.history.HistoryStack
import dev.chandradsl.m3ecanvas.editor.persistence.AutosaveManager
import dev.chandradsl.m3ecanvas.editor.persistence.LoadResult
import dev.chandradsl.m3ecanvas.util.AppLogger
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SharedCommonTest {

    @Test
    fun testMaterialIconResolver() {
        val icon = resolveMaterialIcon("Favorite")
        assertNotNull(icon)
        AVAILABLE_MATERIAL_ICONS.forEach { name ->
            val resolved = resolveMaterialIcon(name)
            assertNotNull(resolved, "Failed to resolve icon $name")
        }
    }

    @Test
    fun testSlotRoleCanonicalMapping() {
        assertEquals(SlotRole.TOP_BAR, ComponentType.TOP_APP_BAR.canonicalSlot())
        assertEquals(SlotRole.BOTTOM_BAR, ComponentType.NAVIGATION_BAR.canonicalSlot())
        assertEquals(SlotRole.BOTTOM_BAR, ComponentType.BOTTOM_APP_BAR.canonicalSlot())
        assertEquals(SlotRole.RAIL, ComponentType.NAVIGATION_RAIL.canonicalSlot())
        assertEquals(SlotRole.DRAWER, ComponentType.NAVIGATION_DRAWER.canonicalSlot())
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
        assertEquals(66, ComponentType.entries.size)
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
        assertEquals(66, ComponentType.entries.size)
        for (type in ComponentType.entries) {
            val size = EditorController.defaultSizeFor(type)
            assertTrue(size.width > 0f, "Width must be > 0 for ${type.name}")
            assertTrue(size.height > 0f, "Height must be > 0 for ${type.name}")
        }
    }

    @Test
    fun testEveryComponentTypeIsDeletableFromProject() {
        assertEquals(66, ComponentType.entries.size)
        for (type in ComponentType.entries) {
            val project = EditorController.newProject(name = "DeleteTest_${type.name}", withDefaultScaffold = false)
            val controller = EditorController(initialProject = project)

            // Add node
            controller.addNode(type = type, position = CanvasPosition.Zero)
            val addedNode = controller.state.project.nodes.firstOrNull { it.type == type }
            assertNotNull(addedNode, "Failed to add component ${type.name}")

            // Select and delete
            controller.selectNode(addedNode.id)
            assertTrue(controller.state.selectedNodeIds.contains(addedNode.id))
            controller.deleteSelected()

            // Verify completely deleted
            assertNull(
                controller.state.project.findNode(addedNode.id),
                "Component ${type.name} was not deleted from project"
            )
            assertFalse(
                controller.state.selectedNodeIds.contains(addedNode.id),
                "Selection was not cleared after deleting ${type.name}"
            )
        }
    }

    @Test
    fun testNewComponentPropertiesAndVariants() {
        var node = CanvasNode(
            type = ComponentType.TEXT,
            name = "Text",
            position = CanvasPosition.Zero,
            size = CanvasSize(100f, 30f)
        )
        node = node.withProperty(ComponentProperty.Text(key = "text", value = "Custom Text"))
            .withProperty(ComponentProperty.Text(key = "typography", value = "headlineMedium"))
            .withProperty(ComponentProperty.Icon(key = "icon", iconName = "Search"))
            .withProperty(ComponentProperty.BooleanFlag(key = "checked", value = true))
            .withProperty(ComponentProperty.Numeric(key = "value", value = 0.75f))
            .withProperty(ComponentProperty.Variant(key = "variant", value = MaterialVariant.Surface.CIRCLE))

        assertEquals("Custom Text", node.textProperty("text"))
        assertEquals("headlineMedium", node.textProperty("typography"))
        assertEquals("Search", node.iconProperty("icon"))
        assertTrue(node.booleanProperty("checked"))
        assertEquals(0.75f, node.numericProperty("value"))

        // Test serialization of new variants
        val tfVariant = MaterialVariant.TextField.OUTLINED
        val tfSerial = tfVariant.toSerialString()
        assertEquals("TextField.OUTLINED", tfSerial)
        assertEquals(tfVariant, materialVariantFromSerialString(tfSerial))

        val surfVariant = MaterialVariant.Surface.CIRCLE
        val surfSerial = surfVariant.toSerialString()
        assertEquals("Surface.CIRCLE", surfSerial)
        assertEquals(surfVariant, materialVariantFromSerialString(surfSerial))
    }

    @Test
    fun testScaffoldSlotsAreDeletableIndividually() {
        val project = EditorController.newProject(name = "SlotsDeleteTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        // Add bottom bar, fab, and snackbar
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.NAVIGATION_BAR)
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.FAB)
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.SNACKBAR)

        val scaffoldWithAll = controller.state.project.findNode(scaffold.id)!!
        val topBar = scaffoldWithAll.childInSlot(SlotRole.TOP_BAR)
        val bottomBar = scaffoldWithAll.childInSlot(SlotRole.BOTTOM_BAR)
        val fab = scaffoldWithAll.childInSlot(SlotRole.FAB)
        val snackbar = scaffoldWithAll.childInSlot(SlotRole.SNACKBAR)

        assertNotNull(topBar, "Missing TopBar")
        assertNotNull(bottomBar, "Missing BottomBar")
        assertNotNull(fab, "Missing FAB")
        assertNotNull(snackbar, "Missing Snackbar")

        // 1. Delete TopBar
        controller.removeNode(nodeId = topBar.id)
        var currentScaffold = controller.state.project.findNode(scaffold.id)!!
        assertNull(currentScaffold.childInSlot(SlotRole.TOP_BAR))
        assertNotNull(currentScaffold.childInSlot(SlotRole.BOTTOM_BAR))
        assertNotNull(currentScaffold.childInSlot(SlotRole.FAB))
        assertNotNull(currentScaffold.childInSlot(SlotRole.SNACKBAR))

        // 2. Delete BottomBar
        controller.removeNode(nodeId = bottomBar.id)
        currentScaffold = controller.state.project.findNode(scaffold.id)!!
        assertNull(currentScaffold.childInSlot(SlotRole.BOTTOM_BAR))
        assertNotNull(currentScaffold.childInSlot(SlotRole.FAB))
        assertNotNull(currentScaffold.childInSlot(SlotRole.SNACKBAR))

        // 3. Delete FAB
        controller.removeNode(nodeId = fab.id)
        currentScaffold = controller.state.project.findNode(scaffold.id)!!
        assertNull(currentScaffold.childInSlot(SlotRole.FAB))
        assertNotNull(currentScaffold.childInSlot(SlotRole.SNACKBAR))

        // 4. Delete Snackbar
        controller.removeNode(nodeId = snackbar.id)
        currentScaffold = controller.state.project.findNode(scaffold.id)!!
        assertNull(currentScaffold.childInSlot(SlotRole.SNACKBAR))
    }

    @Test
    fun testRootScaffoldIsDeletable() {
        val project = EditorController.newProject(name = "ScaffoldDeleteTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        controller.selectNode(scaffold.id)
        controller.deleteSelected()

        assertTrue(controller.state.project.nodes.isEmpty(), "Scaffold was not deleted from project root")
        assertNull(controller.state.project.findNode(scaffold.id))

        // Ensure we can add a new node to the empty project
        controller.addNode(type = ComponentType.BUTTON, position = CanvasPosition(10f, 10f))
        assertEquals(1, controller.state.project.nodes.size)
        assertEquals(ComponentType.BUTTON, controller.state.project.nodes.first().type)
    }

    @Test
    fun testDeleteSelectedWhenChildIsSelectedOnlyDeletesChildNotParentScaffold() {
        val project = EditorController.newProject(name = "ChildDeleteTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        // Add Extended FAB to Scaffold
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.EXTENDED_FAB)
        val scaffoldWithFab = controller.state.project.findNode(scaffold.id)!!
        val fab = scaffoldWithFab.childInSlot(SlotRole.FAB)
        assertNotNull(fab, "Extended FAB should be slotted in Scaffold")

        // 1. Select the child (Extended FAB)
        controller.selectNode(fab.id)
        assertEquals(setOf(fab.id), controller.state.selectedNodeIds)

        // 2. Trigger deleteSelected
        controller.deleteSelected()

        // 3. Verify Scaffold is still present and intact
        val scaffoldAfterFabDelete = controller.state.project.findNode(scaffold.id)
        assertNotNull(scaffoldAfterFabDelete, "Scaffold must NOT be deleted when child is selected")
        assertNull(scaffoldAfterFabDelete.childInSlot(SlotRole.FAB), "Extended FAB should be deleted")
        assertNotNull(scaffoldAfterFabDelete.childInSlot(SlotRole.TOP_BAR), "TopAppBar must remain intact")
        assertNotNull(scaffoldAfterFabDelete.childInSlot(SlotRole.CONTENT), "Content Column must remain intact")

        // 4. Test nested container child deletion (Column -> Button)
        val contentCol = scaffoldAfterFabDelete.children.first { it.slot == SlotRole.CONTENT }
        controller.addChildToContainer(containerId = contentCol.id, type = ComponentType.BUTTON)
        val updatedCol = controller.state.project.findNode(contentCol.id)!!
        val button = updatedCol.children.first { it.type == ComponentType.BUTTON }
        assertNotNull(button)

        controller.selectNode(button.id)
        controller.deleteSelected()

        val scaffoldAfterButtonDelete = controller.state.project.findNode(scaffold.id)!!
        val colAfterButtonDelete = controller.state.project.findNode(contentCol.id)!!
        assertNotNull(scaffoldAfterButtonDelete, "Scaffold must remain intact")
        assertNotNull(colAfterButtonDelete, "Content column must remain intact")
        assertTrue(colAfterButtonDelete.children.none { it.id == button.id }, "Only button should be deleted")
    }

    @Test
    fun testAll21ModifiersCreationAndSerialization() {
        assertEquals(21, ModifierKind.entries.size)

        val allSpecs: List<ModifierSpec> = listOf(
            ModifierSpec.Padding(all = 12f, horizontal = 8f, vertical = 4f),
            ModifierSpec.Background(colorHex = "#FF123456", cornerRadius = 16f),
            ModifierSpec.Border(width = 3f, colorHex = "#FF654321", cornerRadius = 8f),
            ModifierSpec.Clip(cornerRadius = 10f),
            ModifierSpec.Shadow(elevation = 6f, cornerRadius = 12f),
            ModifierSpec.Alpha(value = 0.75f),
            ModifierSpec.Rotation(degrees = 90f),
            ModifierSpec.Scale(value = 1.5f),
            ModifierSpec.FillMaxSize(fraction = 0.8f),
            ModifierSpec.FillMaxWidth(),
            ModifierSpec.FillMaxHeight(),
            ModifierSpec.WrapContentSize(),
            ModifierSpec.Width(dp = 150f),
            ModifierSpec.Height(dp = 200f),
            ModifierSpec.Size(width = 80f, height = 90f),
            ModifierSpec.AspectRatio(ratio = 1.77f),
            ModifierSpec.Offset(x = 10f, y = 20f),
            ModifierSpec.Weight(weight = 2f, fill = false),
            ModifierSpec.Align(alignment = AlignTarget.BOTTOM_END),
            ModifierSpec.ZIndex(value = 5f),
            ModifierSpec.Clickable(enabled = true)
        )

        assertEquals(21, allSpecs.size)

        // Verify factory method creates correct instance for every kind
        ModifierKind.entries.forEach { kind ->
            val created = kind.create()
            assertNotNull(created)
            assertNotEquals(created.id, created.withNewId().id)
        }

        val testNode = CanvasNode(
            type = ComponentType.BOX,
            name = "TestBox",
            position = CanvasPosition(10f, 10f),
            size = CanvasSize(200f, 200f),
            modifiers = allSpecs
        )

        val json = M3EJson.encodeToString(testNode)
        val decoded = M3EJson.decodeFromString<CanvasNode>(json)

        assertEquals(21, decoded.modifiers.size)
        val decodedAlign = decoded.modifiers.filterIsInstance<ModifierSpec.Align>().first()
        assertEquals(AlignTarget.BOTTOM_END, decodedAlign.alignment)

        val decodedWeight = decoded.modifiers.filterIsInstance<ModifierSpec.Weight>().first()
        assertEquals(2f, decodedWeight.weight)
        assertFalse(decodedWeight.fill)

        val decodedPadding = decoded.modifiers.filterIsInstance<ModifierSpec.Padding>().first()
        assertEquals(8f, decodedPadding.horizontal)
        assertEquals(4f, decodedPadding.vertical)
    }

    @Test
    fun testContainerAlignmentSerializationAndCodegen() {
        val colConfig = LayoutConfig(
            horizontalAlignment = HorizontalAlignment.CENTER_HORIZONTALLY,
            arrangement = LayoutArrangement.CENTER
        )
        val rowConfig = LayoutConfig(
            verticalAlignment = VerticalAlignment.BOTTOM,
            arrangement = LayoutArrangement.SPACE_BETWEEN
        )
        val boxConfig = LayoutConfig(
            boxAlignment = BoxAlignment.TOP_END
        )

        val colJson = M3EJson.encodeToString(colConfig)
        val decodedCol = M3EJson.decodeFromString<LayoutConfig>(colJson)
        assertEquals(HorizontalAlignment.CENTER_HORIZONTALLY, decodedCol.horizontalAlignment)

        val rowJson = M3EJson.encodeToString(rowConfig)
        val decodedRow = M3EJson.decodeFromString<LayoutConfig>(rowJson)
        assertEquals(VerticalAlignment.BOTTOM, decodedRow.verticalAlignment)

        val boxJson = M3EJson.encodeToString(boxConfig)
        val decodedBox = M3EJson.decodeFromString<LayoutConfig>(boxJson)
        assertEquals(BoxAlignment.TOP_END, decodedBox.boxAlignment)

        // Test ComposeCodeGenerator output for alignments
        val colNode = CanvasNode(
            type = ComponentType.COLUMN,
            name = "TestColumn",
            position = CanvasPosition.Zero,
            size = CanvasSize(200f, 200f),
            layoutConfig = colConfig
        )
        val rowNode = CanvasNode(
            type = ComponentType.ROW,
            name = "TestRow",
            position = CanvasPosition.Zero,
            size = CanvasSize(200f, 200f),
            layoutConfig = rowConfig
        )
        val boxNode = CanvasNode(
            type = ComponentType.BOX,
            name = "TestBox",
            position = CanvasPosition.Zero,
            size = CanvasSize(200f, 200f),
            layoutConfig = boxConfig
        )

        val project = M3EProject(
            id = "test-project",
            name = "TestScreen",
            nodes = listOf(colNode, rowNode, boxNode)
        )

        val code = ComposeCodeGenerator.generateFile(project)
        assertTrue(code.contains("horizontalAlignment = Alignment.CenterHorizontally"))
        assertTrue(code.contains("verticalAlignment = Alignment.Bottom"))
        assertTrue(code.contains("contentAlignment = Alignment.TopEnd"))
    }

    @Test
    fun testAll21ModifiersComposeCodeGeneration() {
        val allSpecs: List<ModifierSpec> = listOf(
            ModifierSpec.Padding(all = 12f, horizontal = 8f, vertical = 4f),
            ModifierSpec.Background(colorHex = "#FF123456", cornerRadius = 16f),
            ModifierSpec.Border(width = 3f, colorHex = "#FF654321", cornerRadius = 8f),
            ModifierSpec.Clip(cornerRadius = 10f),
            ModifierSpec.Shadow(elevation = 6f, cornerRadius = 12f),
            ModifierSpec.Alpha(value = 0.75f),
            ModifierSpec.Rotation(degrees = 90f),
            ModifierSpec.Scale(value = 1.5f),
            ModifierSpec.FillMaxSize(fraction = 0.8f),
            ModifierSpec.FillMaxWidth(),
            ModifierSpec.FillMaxHeight(),
            ModifierSpec.WrapContentSize(),
            ModifierSpec.Width(dp = 150f),
            ModifierSpec.Height(dp = 200f),
            ModifierSpec.Size(width = 80f, height = 90f),
            ModifierSpec.AspectRatio(ratio = 1.77f),
            ModifierSpec.Offset(x = 10f, y = 20f),
            ModifierSpec.Weight(weight = 2f, fill = false),
            ModifierSpec.Align(alignment = AlignTarget.BOTTOM_END),
            ModifierSpec.ZIndex(value = 5f),
            ModifierSpec.Clickable(enabled = true)
        )

        val node = CanvasNode(
            type = ComponentType.BUTTON,
            name = "MyButton",
            position = CanvasPosition.Zero,
            size = CanvasSize(100f, 50f),
            modifiers = allSpecs
        )

        val project = M3EProject(
            id = "proj",
            name = "Screen",
            nodes = listOf(node)
        )

        val code = ComposeCodeGenerator.generateFile(project)
        assertTrue(code.contains("padding(horizontal = 8.dp, vertical = 4.dp)"))
        assertTrue(code.contains("background(Color(0xFF123456), RoundedCornerShape(16.dp))"))
        assertTrue(code.contains("border(3.dp, Color(0xFF654321), RoundedCornerShape(8.dp))"))
        assertTrue(code.contains("clip(RoundedCornerShape(10.dp))"))
        assertTrue(code.contains("shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))"))
        assertTrue(code.contains("alpha(0.75f)"))
        assertTrue(code.contains("rotate(90.0f)"))
        assertTrue(code.contains("scale(1.5f)"))
        assertTrue(code.contains("fillMaxSize(0.8f)"))
        assertTrue(code.contains("fillMaxWidth()"))
        assertTrue(code.contains("fillMaxHeight()"))
        assertTrue(code.contains("wrapContentSize()"))
        assertTrue(code.contains("width(150.dp)"))
        assertTrue(code.contains("height(200.dp)"))
        assertTrue(code.contains("size(width = 80.dp, height = 90.dp)"))
        assertTrue(code.contains("aspectRatio(1.77f)"))
        assertTrue(code.contains("offset(x = 10.dp, y = 20.dp)"))
        assertTrue(code.contains("weight(2.0f, fill = false)"))
        assertTrue(code.contains("align(Alignment.BottomEnd)"))
        assertTrue(code.contains("zIndex(5.0f)"))
        assertTrue(code.contains("clickable(enabled = true)"))
    }

    @Test
    fun testSetDeviceProfileDoesNotAffectUndoRedoHistory() {
        val initialProject = EditorController.newProject(name = "DeviceSwitchTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = initialProject)

        val defaultProfile = controller.state.project.deviceProfile
        val rootScaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        assertEquals(defaultProfile.size, rootScaffold.size)
        assertFalse(controller.canUndo)

        // 1. Switch device profile: changes viewport and root scaffold, but does NOT push to undo history
        val tabletProfile = DeviceProfile.presets.first { it.category == DeviceCategory.TABLET }
        controller.setDeviceProfile(tabletProfile)

        assertEquals(tabletProfile, controller.state.project.deviceProfile)
        val updatedScaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        assertEquals(tabletProfile.size, updatedScaffold.size)

        val updatedContent = updatedScaffold.childInSlot(SlotRole.CONTENT)
        assertNotNull(updatedContent)
        assertEquals(tabletProfile.size.width, updatedContent.size.width)
        assertEquals(tabletProfile.size.height - 144f, updatedContent.size.height)

        // Changing device is viewport configuration, NOT an undoable action
        assertFalse(controller.canUndo)
        assertFalse(controller.canRedo)

        // 2. Perform an actual canvas edit (add a Button)
        controller.addNode(type = ComponentType.BUTTON, position = CanvasPosition(10f, 10f))
        assertTrue(controller.canUndo)
        val addedButton = controller.state.project.nodes.first { it.type == ComponentType.BUTTON }

        // 3. Switch device again while undo history is active
        val foldProfile = DeviceProfile.presets.first { it.category == DeviceCategory.FOLDABLE }
        controller.setDeviceProfile(foldProfile)

        assertEquals(foldProfile, controller.state.project.deviceProfile)
        // Undo is STILL available for the Button addition, not erased by device switch
        assertTrue(controller.canUndo)

        // 4. Undo the button addition: Button is removed, but active device profile stays Fold!
        controller.undo()
        assertEquals(foldProfile, controller.state.project.deviceProfile)
        val foldScaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        assertEquals(foldProfile.size, foldScaffold.size)
        assertNull(controller.state.project.findNode(addedButton.id))
        assertTrue(controller.canRedo)

        // 5. Redo the button addition: Button is restored, and active device profile still stays Fold!
        controller.redo()
        assertEquals(foldProfile, controller.state.project.deviceProfile)
        assertNotNull(controller.state.project.findNode(addedButton.id))
    }

    @Test
    fun testViewportZoomClamping() {
        val controller = EditorController()
        assertEquals(1f, controller.state.viewport.zoom)

        // Zoom In
        controller.zoomIn(0.5f)
        assertEquals(1.5f, controller.state.viewport.zoom)

        // Upper clamp at 3.0f
        controller.setZoom(5.0f)
        assertEquals(3.0f, controller.state.viewport.zoom)

        // Lower clamp at 0.25f
        controller.setZoom(0.1f)
        assertEquals(0.25f, controller.state.viewport.zoom)

        // Zoom out smoothly
        controller.setZoom(1.0f)
        controller.zoomOut(0.2f)
        assertEquals(0.8f, controller.state.viewport.zoom, 0.001f)

        // Reset
        controller.resetZoom()
        assertEquals(1f, controller.state.viewport.zoom)
        assertEquals(CanvasPosition.Zero, controller.state.viewport.panOffset)
    }

    @Test
    fun testViewportPanAndDragGuard() {
        val controller = EditorController()
        assertEquals(CanvasPosition.Zero, controller.state.viewport.panOffset)

        // Pan accumulates delta
        controller.pan(50f, 100f)
        assertEquals(CanvasPosition(50f, 100f), controller.state.viewport.panOffset)

        controller.pan(-20f, 30f)
        assertEquals(CanvasPosition(30f, 130f), controller.state.viewport.panOffset)

        // Reset pan
        controller.resetPan()
        assertEquals(CanvasPosition.Zero, controller.state.viewport.panOffset)

        // When a node drag is active, pan does not execute
        val button = CanvasNode(
            type = ComponentType.BUTTON,
            name = "Btn",
            position = CanvasPosition(10f, 10f),
            size = CanvasSize(100f, 40f)
        )
        controller.replaceProject(controller.state.project.withNode(button))
        controller.startDrag(button.id)
        assertNotNull(controller.state.drag)

        // Attempting to pan canvas while dragging component is blocked
        controller.pan(50f, 50f)
        assertEquals(CanvasPosition.Zero, controller.state.viewport.panOffset)

        controller.endDrag()
        assertNull(controller.state.drag)
        controller.pan(50f, 50f)
        assertEquals(CanvasPosition(50f, 50f), controller.state.viewport.panOffset)
    }

    @Test
    fun testFitToScreenCalculatesProportionalScale() {
        val tabletProfile = DeviceProfile.presets.first { it.displayName == "Pixel Tablet" } // 1280 x 800
        val controller = EditorController()
        controller.setDeviceProfile(tabletProfile)

        // Available viewport is 800 x 600 (smaller than the 1280x800 tablet)
        controller.fitToScreen(availableWidth = 800f, availableHeight = 600f)

        val zoom = controller.state.viewport.zoom
        // Tablet width + margin = 1280 + 48 = 1328; 800 / 1328 ≈ 0.602
        // Tablet height + margin = 800 + 96 = 896; 600 / 896 ≈ 0.669
        // Min scale ≈ 0.602
        assertTrue(zoom < 1.0f, "Tablet should scale down to fit smaller window")
        assertTrue(zoom in 0.55f..0.65f, "Expected zoom around 0.60, was $zoom")
        assertEquals(CanvasPosition.Zero, controller.state.viewport.panOffset)

        // Now test small phone on large 1920 x 1080 display - should NOT scale above 100%
        val phoneProfile = DeviceProfile.presets.first { it.displayName == "Pixel 8" } // 412 x 915
        controller.setDeviceProfile(phoneProfile)
        controller.fitToScreen(availableWidth = 1920f, availableHeight = 1080f)
        assertEquals(1.0f, controller.state.viewport.zoom, "Small device in large screen should cap at 1.0 (100%)")
    }

    @Test
    fun testViewportOperationsDoNotPolluteUndoHistory() {
        val controller = EditorController()
        assertFalse(controller.canUndo)

        // Viewport transformations
        controller.setZoom(1.5f)
        controller.pan(100f, 200f)
        controller.fitToScreen(800f, 600f)
        controller.resetZoom()

        // None of these should touch undo/redo
        assertFalse(controller.canUndo)
        assertFalse(controller.canRedo)
    }

    @Test
    fun testFabVariantRenderingAndCodegen() {
        val project = EditorController.newProject(name = "FabTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.FAB)
        val fab = controller.state.project.findNode(scaffold.id)!!.childInSlot(SlotRole.FAB)!!

        // SURFACE variant
        controller.updateNodeProperty(
            nodeId = fab.id,
            property = ComponentProperty.Variant(key = "variant", value = MaterialVariant.FloatingActionButton.SURFACE)
        )
        var code = ComposeCodeGenerator.generateFile(controller.state.project)
        assertTrue(code.contains("surfaceContainerHigh"), "Surface variant should emit surfaceContainerHigh")
        assertTrue(code.contains("primary"), "Surface variant should emit primary contentColor")

        // SECONDARY variant
        controller.updateNodeProperty(
            nodeId = fab.id,
            property = ComponentProperty.Variant(key = "variant", value = MaterialVariant.FloatingActionButton.SECONDARY)
        )
        code = ComposeCodeGenerator.generateFile(controller.state.project)
        assertTrue(code.contains("secondaryContainer"), "Secondary variant should emit secondaryContainer")
        assertTrue(code.contains("onSecondaryContainer"), "Secondary variant should emit onSecondaryContainer")

        // TERTIARY variant
        controller.updateNodeProperty(
            nodeId = fab.id,
            property = ComponentProperty.Variant(key = "variant", value = MaterialVariant.FloatingActionButton.TERTIARY)
        )
        code = ComposeCodeGenerator.generateFile(controller.state.project)
        assertTrue(code.contains("tertiaryContainer"), "Tertiary variant should emit tertiaryContainer")
        assertTrue(code.contains("onTertiaryContainer"), "Tertiary variant should emit onTertiaryContainer")
    }

    @Test
    fun testScaffoldNavigationRailAndDrawerSlots() {
        val project = EditorController.newProject(name = "NavTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        // Test Rail slot
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.NAVIGATION_RAIL)
        val updatedScaffold = controller.state.project.findNode(scaffold.id)!!
        val railNode = updatedScaffold.childInSlot(SlotRole.RAIL)
        assertNotNull(railNode, "Navigation Rail should snap to SlotRole.RAIL")
        assertEquals(SlotRole.RAIL, railNode.slot)

        // Test Drawer slot
        controller.addChildToContainer(containerId = scaffold.id, type = ComponentType.NAVIGATION_DRAWER)
        val scaffoldWithDrawer = controller.state.project.findNode(scaffold.id)!!
        val drawerNode = scaffoldWithDrawer.childInSlot(SlotRole.DRAWER)
        assertNotNull(drawerNode, "Navigation Drawer should snap to SlotRole.DRAWER")
        assertEquals(SlotRole.DRAWER, drawerNode.slot)

        // Test code generation emits Row + NavigationRail and ModalNavigationDrawer
        val code = ComposeCodeGenerator.generateFile(controller.state.project)
        assertTrue(code.contains("ModalNavigationDrawer("), "Should emit ModalNavigationDrawer when DRAWER slot exists")
        assertTrue(code.contains("NavigationRail("), "Should emit NavigationRail when RAIL slot exists")
        assertTrue(code.contains("Row(modifier = Modifier.fillMaxSize().padding(innerPadding))"), "Should place Rail and content in a Row with innerPadding")
    }

    @Test
    fun testPasteSiblingSafeWithoutNullAssertion() {
        val project = EditorController.newProject(name = "PasteTest", withDefaultScaffold = false)
        val controller = EditorController(initialProject = project)

        // Create a column with a button
        controller.addNode(type = ComponentType.COLUMN, position = CanvasPosition(20f, 20f))
        val column = controller.state.project.nodes.first { it.type == ComponentType.COLUMN }
        controller.addChildToContainer(containerId = column.id, type = ComponentType.BUTTON)

        val button = controller.state.project.findNode(column.id)!!.children.first { it.type == ComponentType.BUTTON }
        controller.selectNode(button.id)
        controller.copySelected()
        controller.paste()

        val updatedColumn = controller.state.project.findNode(column.id)!!
        assertEquals(2, updatedColumn.children.size, "Pasting sibling should insert into parent container safely")
    }

    @Test
    fun testTopLevelNodeSizingConsistency() {
        val node = CanvasNode(
            type = ComponentType.CARD,
            name = "FullWidthCard",
            position = CanvasPosition.Zero,
            size = CanvasSize(300f, 200f),
            modifiers = listOf(ModifierSpec.FillMaxWidth(), ModifierSpec.FillMaxHeight())
        )

        assertTrue(node.hasFillMaxWidth())
        assertTrue(node.hasFillMaxHeight())
        assertTrue(node.hasExplicitWidth())
        assertTrue(node.hasExplicitHeight())

        val bareNode = CanvasNode(
            type = ComponentType.BUTTON,
            name = "BareButton",
            position = CanvasPosition.Zero,
            size = CanvasSize(100f, 40f)
        )
        assertFalse(bareNode.hasExplicitWidth())
        assertFalse(bareNode.hasExplicitHeight())

        val project = M3EProject(
            name = "SizingTest",
            nodes = listOf(node)
        )
        val code = ComposeCodeGenerator.generateFile(project)
        assertTrue(code.contains("fillMaxWidth()"), "Should include fillMaxWidth")
        assertTrue(code.contains("fillMaxHeight()"), "Should include fillMaxHeight")
        assertFalse(code.contains("size(width = 300.dp"), "Should not generate fixed width when fillMaxWidth is present")
    }

    @Test
    fun testComponentRegistryCompleteness() {
        val allDefs = ComponentRegistry.default.all()
        assertEquals(ComponentType.entries.size, allDefs.size, "Every ComponentType must have a ComponentDefinition")

        ComponentType.entries.forEach { type ->
            val def = ComponentRegistry.default.get(type)
            assertNotNull(def, "ComponentDefinition missing for $type")
            assertEquals(type, def.type)
            assertTrue(def.displayName.isNotEmpty(), "Display name must not be empty for $type")
            assertNotNull(def.icon(), "Icon must not be null for $type")

            val size = def.defaultSize(412f, 915f)
            assertTrue(size.width > 0f, "Default width must be positive for $type")
            assertTrue(size.height > 0f, "Default height must be positive for $type")
        }
    }

    @Test
    fun testComponentRegistryVariantsMapping() {
        assertEquals(MaterialVariant.Button.entries, ComponentRegistry.default.variantsFor(ComponentType.BUTTON))
        assertEquals(MaterialVariant.IconButton.entries, ComponentRegistry.default.variantsFor(ComponentType.ICON_BUTTON))
        assertEquals(MaterialVariant.FloatingActionButton.entries, ComponentRegistry.default.variantsFor(ComponentType.FAB))
        assertEquals(MaterialVariant.FloatingActionButton.entries, ComponentRegistry.default.variantsFor(ComponentType.EXTENDED_FAB))
        assertEquals(MaterialVariant.Card.entries, ComponentRegistry.default.variantsFor(ComponentType.CARD))
        assertEquals(MaterialVariant.Chip.entries, ComponentRegistry.default.variantsFor(ComponentType.CHIPS))
        assertEquals(MaterialVariant.TextField.entries, ComponentRegistry.default.variantsFor(ComponentType.TEXT_FIELD))
        assertEquals(MaterialVariant.Surface.entries, ComponentRegistry.default.variantsFor(ComponentType.SURFACE))

        // Non-variant component returns empty
        assertEquals(emptyList(), ComponentRegistry.default.variantsFor(ComponentType.TEXT))
    }

    @Test
    fun testCustomComponentRegistryInjection() {
        val customDefs = ComponentRegistry.defaultDefinitions().toMutableMap()
        customDefs[ComponentType.BUTTON] = ComponentDefinition(
            type = ComponentType.BUTTON,
            defaultSize = { _, _ -> CanvasSize(999f, 888f) }
        )
        val customRegistry = ComponentRegistry(definitions = customDefs)
        val controller = EditorController(
            initialProject = EditorController.newProject(withDefaultScaffold = false),
            componentRegistry = customRegistry
        )

        val size = controller.defaultSizeFor(ComponentType.BUTTON)
        assertEquals(CanvasSize(999f, 888f), size, "EditorController should use injected ComponentRegistry size")

        controller.addNode(type = ComponentType.BUTTON, position = CanvasPosition(10f, 10f))
        val addedButton = controller.state.project.nodes.first { it.type == ComponentType.BUTTON }
        assertEquals(CanvasSize(999f, 888f), addedButton.size, "Added button node should take injected registry size")
    }

    @Test
    fun testCustomCodeGeneratorInjectedIntoAIPromptGenerator() {
        val customGenerator = object : ComposeCodeGenerator() {
            override fun generateFile(project: M3EProject, functionName: String): String {
                return "// Custom Generated Code for ${project.name}"
            }
        }
        val promptGenerator = AIPromptGenerator(codeGenerator = customGenerator)
        val project = M3EProject(name = "CustomPromptTest")
        val prompt = promptGenerator.generate(project)

        assertTrue(prompt.contains("// Custom Generated Code for CustomPromptTest"))
        assertFalse(prompt.contains("Theme Preset"), "themeId should no longer appear in prompt output")
    }

    @Test
    fun testProjectSerializationWithoutThemeId() {
        val project = M3EProject(name = "NoThemeProject")
        val json = M3EJson.encodeToString(project)

        assertFalse(json.contains("\"themeId\""), "Serialized JSON must not contain dead themeId")
        val decoded = M3EJson.decodeFromString<M3EProject>(json)
        assertEquals("NoThemeProject", decoded.name)
    }

    // --- Phase 2 Tests ---

    @Test
    fun testMultiSelectToggleAndSelectAll() {
        val controller = EditorController(initialProject = M3EProject(name = "MultiSelectTest"))
        controller.addNode(ComponentType.BUTTON, CanvasPosition(10f, 10f))
        controller.addNode(ComponentType.CARD, CanvasPosition(20f, 20f))
        controller.addNode(ComponentType.FAB, CanvasPosition(30f, 30f))

        val nodes = controller.state.project.nodes
        assertEquals(3, nodes.size)

        // Select first node
        controller.selectNode(nodes[0].id)
        assertEquals(setOf(nodes[0].id), controller.state.selectedNodeIds)

        // Toggle second node (extends selection)
        controller.toggleSelectNode(nodes[1].id)
        assertEquals(setOf(nodes[0].id, nodes[1].id), controller.state.selectedNodeIds)

        // Toggle first node off
        controller.toggleSelectNode(nodes[0].id)
        assertEquals(setOf(nodes[1].id), controller.state.selectedNodeIds)

        // Select all
        controller.selectAll()
        assertEquals(3, controller.state.selectedNodeIds.size)
        assertTrue(nodes.all { it.id in controller.state.selectedNodeIds })

        // Clear selection
        controller.clearSelection()
        assertTrue(controller.state.selectedNodeIds.isEmpty())
    }

    @Test
    fun testMultiSelectGroupDelete() {
        val controller = EditorController(initialProject = M3EProject(name = "GroupDeleteTest"))
        controller.addNode(ComponentType.BUTTON, CanvasPosition(10f, 10f))
        controller.addNode(ComponentType.CARD, CanvasPosition(20f, 20f))
        controller.addNode(ComponentType.FAB, CanvasPosition(30f, 30f))

        val nodes = controller.state.project.nodes
        // Multi-select BUTTON and CARD
        controller.selectNodes(setOf(nodes[0].id, nodes[1].id))
        assertEquals(2, controller.state.selectedNodeIds.size)

        controller.deleteSelected()
        assertEquals(1, controller.state.project.nodes.size)
        assertEquals(ComponentType.FAB, controller.state.project.nodes.first().type)
        assertTrue(controller.state.selectedNodeIds.isEmpty(), "Selection must be cleared after delete")
    }

    @Test
    fun testMultiSelectGroupNudge() {
        val controller = EditorController(initialProject = M3EProject(name = "GroupNudgeTest"))
        controller.addNode(ComponentType.BUTTON, CanvasPosition(10f, 10f))
        controller.addNode(ComponentType.CARD, CanvasPosition(50f, 50f))

        val nodes = controller.state.project.nodes
        controller.selectAll()

        controller.nudgeSelected(dx = 5f, dy = 10f)

        val updatedNodes = controller.state.project.nodes
        assertEquals(CanvasPosition(15f, 20f), updatedNodes[0].position)
        assertEquals(CanvasPosition(55f, 60f), updatedNodes[1].position)
    }

    @Test
    fun testMultiSelectGroupDrag() {
        val controller = EditorController(initialProject = M3EProject(name = "GroupDragTest"))
        controller.addNode(ComponentType.BUTTON, CanvasPosition(10f, 10f))
        controller.addNode(ComponentType.CARD, CanvasPosition(50f, 50f))

        val nodes = controller.state.project.nodes
        controller.selectAll()

        // Drag started on the first node
        controller.startDrag(nodeId = nodes[0].id)
        assertNotNull(controller.state.drag)
        assertEquals(2, controller.state.drag?.nodeIds?.size)

        // Drag movement
        controller.updateDrag(deltaX = 20f, deltaY = 30f)
        val draggedNodes = controller.state.project.nodes
        assertEquals(CanvasPosition(30f, 40f), draggedNodes[0].position)
        assertEquals(CanvasPosition(70f, 80f), draggedNodes[1].position)

        controller.endDrag()
        assertNull(controller.state.drag)
        assertTrue(controller.state.canUndo)

        // Undo group drag
        controller.undo()
        val undoneNodes = controller.state.project.nodes
        assertEquals(CanvasPosition(10f, 10f), undoneNodes[0].position)
        assertEquals(CanvasPosition(50f, 50f), undoneNodes[1].position)
    }

    @Test
    fun testCanvasThemeConfigSerializationAndDefaults() {
        val projectWithDefaultTheme = M3EProject(name = "DefaultThemeProject")
        assertEquals(CanvasThemeConfig.DEFAULT_SEED_HEX, projectWithDefaultTheme.themeConfig.seedColorHex)
        assertFalse(projectWithDefaultTheme.themeConfig.isDark)

        val customTheme = CanvasThemeConfig(seedColorHex = "#006A60", isDark = true)
        val projectWithCustomTheme = projectWithDefaultTheme.withTheme(customTheme)
        assertEquals("#006A60", projectWithCustomTheme.themeConfig.seedColorHex)
        assertTrue(projectWithCustomTheme.themeConfig.isDark)

        val json = M3EJson.encodeToString(projectWithCustomTheme)
        assertTrue(json.contains("\"seedColorHex\": \"#006A60\"") || json.contains("\"seedColorHex\":\"#006A60\""))
        assertTrue(json.contains("\"isDark\": true") || json.contains("\"isDark\":true"))

        val decoded = M3EJson.decodeFromString<M3EProject>(json)
        assertEquals("#006A60", decoded.themeConfig.seedColorHex)
        assertTrue(decoded.themeConfig.isDark)
    }

    @Test
    fun testLegacyProjectDeserializationWithDefaultTheme() {
        // Simulates project JSON saved before Phase 2 without themeConfig field
        val legacyJson = """
            {
                "id": "11111111-1111-1111-1111-111111111111",
                "name": "LegacyProject",
                "schemaVersion": 1,
                "deviceProfile": {
                    "id": "pixel_7",
                    "displayName": "Pixel 7",
                    "category": "PHONE",
                    "size": { "width": 412.0, "height": 915.0 }
                },
                "nodes": [],
                "createdAt": 0,
                "updatedAt": 0
            }
        """.trimIndent()

        val project = M3EJson.decodeFromString<M3EProject>(legacyJson)
        assertEquals("LegacyProject", project.name)
        assertNotNull(project.themeConfig, "Legacy JSON should automatically receive default themeConfig")
        assertEquals(CanvasThemeConfig.DEFAULT_SEED_HEX, project.themeConfig.seedColorHex)
        assertFalse(project.themeConfig.isDark)
    }

    @Test
    fun testCanvasThemeGeneratorColorSchemes() {
        // Baseline purple
        val lightPurple = CanvasThemeGenerator.generateColorScheme(CanvasThemeConfig("#6750A4", isDark = false))
        assertNotNull(lightPurple)

        val darkPurple = CanvasThemeGenerator.generateColorScheme(CanvasThemeConfig("#6750A4", isDark = true))
        assertNotNull(darkPurple)

        // Custom Teal
        val lightTeal = CanvasThemeGenerator.generateColorScheme(CanvasThemeConfig("#006A60", isDark = false))
        assertNotNull(lightTeal)

        val darkTeal = CanvasThemeGenerator.generateColorScheme(CanvasThemeConfig("#006A60", isDark = true))
        assertNotNull(darkTeal)
    }

    @Test
    fun testCanvasThemeHexParsing() {
        assertTrue(CanvasThemeGenerator.isValidHexColor("#FF0000"))
        assertTrue(CanvasThemeGenerator.isValidHexColor("00FF00"))
        assertTrue(CanvasThemeGenerator.isValidHexColor("#FFF"))
        assertFalse(CanvasThemeGenerator.isValidHexColor("invalid-color"))

        val parsedRed = CanvasThemeGenerator.parseHexColor("#FF0000")
        assertEquals(1f, parsedRed.red, 0.05f)
        assertEquals(0f, parsedRed.green, 0.05f)
        assertEquals(0f, parsedRed.blue, 0.05f)
    }

    @Test
    fun testEditorControllerThemeUndoRedo() {
        val controller = EditorController(initialProject = M3EProject(name = "ThemeUndoTest"))
        assertEquals("#6750A4", controller.state.project.themeConfig.seedColorHex)
        assertFalse(controller.state.project.themeConfig.isDark)

        controller.setThemeSeed("#0061A4")
        assertEquals("#0061A4", controller.state.project.themeConfig.seedColorHex)
        assertTrue(controller.state.canUndo)

        controller.toggleThemeDarkMode()
        assertTrue(controller.state.project.themeConfig.isDark)

        // Undo dark mode toggle
        controller.undo()
        assertFalse(controller.state.project.themeConfig.isDark)
        assertEquals("#0061A4", controller.state.project.themeConfig.seedColorHex)

        // Undo seed change
        controller.undo()
        assertEquals("#6750A4", controller.state.project.themeConfig.seedColorHex)

        // Redo
        controller.redo()
        assertEquals("#0061A4", controller.state.project.themeConfig.seedColorHex)
    }

    @Test
    fun testMaterialIconResolverFallbackLogging() {
        val resolved = resolveMaterialIcon("non_existent_icon_name_xyz")
        assertNotNull(resolved)
        // Resolves to Favorite gracefully with diagnostic warning
    }

    @Test
    fun testAppLogger() {
        // AppLogger should execute cleanly without throwing
        AppLogger.info("TestTag", "Info message")
        AppLogger.warn("TestTag", "Warning message", RuntimeException("Test warning"))
        AppLogger.error("TestTag", "Error message", RuntimeException("Test error"))
    }

    @Test
    fun testDualStateFlowSynchronization() {
        val controller = EditorController(EditorController.newProject(withDefaultScaffold = false))
        assertEquals(0, controller.stateFlow.value.project.nodes.size)
        assertEquals(controller.state, controller.stateFlow.value)

        controller.addNode(ComponentType.BUTTON, CanvasPosition(50f, 50f))
        assertEquals(1, controller.stateFlow.value.project.nodes.size)
        assertEquals(controller.state, controller.stateFlow.value)

        controller.deleteSelected()
        assertEquals(0, controller.stateFlow.value.project.nodes.size)
        assertEquals(controller.state, controller.stateFlow.value)
    }

    @Test
    fun testAlignmentSnapperEdgesAndCenter() {
        val snapper = AlignmentSnapper(snapThreshold = 6f)
        val fixedNode = CanvasNode(
            type = ComponentType.BUTTON,
            name = "Fixed",
            position = CanvasPosition(100f, 100f),
            size = CanvasSize(100f, 40f)
        )
        val movingNode = CanvasNode(
            type = ComponentType.BUTTON,
            name = "Moving",
            position = CanvasPosition(20f, 20f),
            size = CanvasSize(100f, 40f)
        )

        // Test Left-to-Left snap (candidate at 103f should snap to 100f)
        val snapLeft = snapper.computeSnap(
            node = movingNode,
            candidatePos = CanvasPosition(103f, 50f),
            otherNodes = listOf(fixedNode),
            deviceWidth = 412f,
            deviceHeight = 915f
        )
        assertEquals(100f, snapLeft.snappedPosition.x)
        assertTrue(snapLeft.guides.any { it.orientation == GuideOrientation.VERTICAL && it.position == 100f })

        // Test Canvas Center X snap (deviceWidth = 412, center = 206, node width = 100 -> x = 156)
        val snapCenter = snapper.computeSnap(
            node = movingNode,
            candidatePos = CanvasPosition(158f, 50f),
            otherNodes = emptyList(),
            deviceWidth = 412f,
            deviceHeight = 915f
        )
        assertEquals(156f, snapCenter.snappedPosition.x)
        assertTrue(snapCenter.guides.any { it.orientation == GuideOrientation.VERTICAL && it.position == 206f })
    }

    @Test
    fun testContrastCalculator() {
        // Black on White
        val black = Color(0xFF000000)
        val white = Color(0xFFFFFFFF)
        val maxContrast = ContrastCalculator.contrastRatio(black, white)
        assertTrue(maxContrast >= 20.9f, "Black on white contrast should be ~21.0")

        val evalMax = ContrastCalculator.evaluate(black, white)
        assertEquals(ContrastLevel.AAA_PASS, evalMax.level)

        // Same color on same color
        val minContrast = ContrastCalculator.contrastRatio(white, white)
        assertEquals(1.0f, minContrast)
        val evalMin = ContrastCalculator.evaluate(white, white)
        assertEquals(ContrastLevel.FAIL, evalMin.level)

        // Hex parsing
        val parsed = ContrastCalculator.parseColorHex("#FF0000")
        assertEquals(1f, parsed.red)
        assertEquals(0f, parsed.green)
        assertEquals(0f, parsed.blue)
    }

    @Test
    fun testProjectCodeExporter() {
        val project = EditorController.newProject(name = "TestApp", withDefaultScaffold = true)
        val files = ProjectCodeExporter.exportProject(project)

        assertEquals(5, files.size)
        val filenames = files.map { it.filename }.toSet()
        assertTrue(filenames.contains("TestAppScreen.kt"))
        assertTrue(filenames.contains("Theme.kt"))
        assertTrue(filenames.contains("MainActivity.kt"))
        assertTrue(filenames.contains("Main.kt"))
        assertTrue(filenames.contains("build.gradle.kts"))

        val screenFile = files.first { it.filename == "TestAppScreen.kt" }
        assertTrue(screenFile.content.contains("fun TestAppScreen"))

        val themeFile = files.first { it.filename == "Theme.kt" }
        assertTrue(themeFile.content.contains("AppTheme"))
        assertTrue(themeFile.content.contains("LightColorScheme"))
        assertTrue(themeFile.content.contains("DarkColorScheme"))
    }

    @Test
    fun testA11yCodeGeneration() {
        val button = CanvasNode(
            type = ComponentType.BUTTON,
            name = "Accessible Button",
            position = CanvasPosition(20f, 20f),
            size = CanvasSize(120f, 40f),
            properties = listOf(
                ComponentProperty.Text(key = "contentDescription", value = "Submit Form Button"),
                ComponentProperty.Text(key = "semanticsRole", value = "Button")
            )
        )
        val project = M3EProject(
            name = "A11yProject",
            nodes = listOf(button)
        )
        val code = ComposeCodeGenerator.generateFile(project)

        assertTrue(code.contains("contentDescription = \"Submit Form Button\""))
        assertTrue(code.contains("role = Role.Button"))
        assertTrue(code.contains("import androidx.compose.ui.semantics.semantics"))
    }

    @Test
    fun testCustomComponentRegistrationDynamically() {
        val registry = ComponentRegistry()
        val customDef = ComponentDefinition(
            type = ComponentType.BUTTON,
            defaultSize = { _, _ -> CanvasSize(333f, 444f) }
        )
        registry.registerComponent(customDef)
        assertEquals(CanvasSize(333f, 444f), registry.defaultSizeFor(ComponentType.BUTTON))
    }

    @Test
    fun testAutosaveManagerOperations() = runBlocking {
        val tempDir = java.io.File.createTempFile("m3e_autosave_dir", "").apply { delete(); mkdirs() }
        val autosaveFile = java.io.File(tempDir, "autosave.json")
        val recentsFile = java.io.File(tempDir, "recents.json")

        val manager = AutosaveManager(autosaveFile = autosaveFile, recentsFile = recentsFile)
        assertFalse(manager.hasAutosave())

        val project = EditorController.newProject("AutosaveTest")
        manager.saveAutosave(project)
        assertTrue(manager.hasAutosave())

        val loaded = manager.loadAutosave()
        assertTrue(loaded is LoadResult.Success)
        assertEquals("AutosaveTest", loaded.project.name)

        manager.clearAutosave()
        assertFalse(manager.hasAutosave())

        // Recents
        manager.addRecentProject("Recent 1", "/path/1")
        manager.addRecentProject("Recent 2", "/path/2")
        val recents = manager.getRecentProjects()
        assertEquals(2, recents.size)
        assertEquals("Recent 2", recents[0].name)

        tempDir.deleteRecursively()
        Unit
    }

    @Test
    fun testAllNodesTreeFlattening() {
        val project = EditorController.newProject(name = "FlattenTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)

        // Scaffold + topBar + Screen Content Column
        val initialNodes = controller.state.project.allNodes()
        assertEquals(3, initialNodes.size, "Scaffold, topBar, and contentColumn should make 3 nodes")

        // Add Button into Screen Content Column
        val column = controller.state.project.allNodes().first { it.type == ComponentType.COLUMN }
        controller.addChildToContainer(containerId = column.id, type = ComponentType.BUTTON)

        val updatedNodes = controller.state.project.allNodes()
        assertEquals(4, updatedNodes.size, "Should now contain 4 nodes including the button")
        assertTrue(updatedNodes.any { it.type == ComponentType.BUTTON }, "Button should be in allNodes()")
    }

    @Test
    fun testSelectionStateBehavior() {
        val project = EditorController.newProject(name = "SelectionTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)
        val allNodes = controller.state.project.allNodes()
        val scaffold = allNodes.first { it.type == ComponentType.SCAFFOLD }
        val column = allNodes.first { it.type == ComponentType.COLUMN }

        // Initial state has no selection (project level)
        assertTrue(controller.state.selectedNodeIds.isEmpty())
        assertFalse(controller.state.isNodeSelected(column.id))

        // Select column (Screen Content)
        controller.selectNode(column.id)
        assertEquals(setOf(column.id), controller.state.selectedNodeIds)
        assertTrue(controller.state.isNodeSelected(column.id))
        assertFalse(controller.state.isNodeSelected(scaffold.id))

        // Select scaffold
        controller.selectNode(scaffold.id)
        assertEquals(setOf(scaffold.id), controller.state.selectedNodeIds)
        assertTrue(controller.state.isNodeSelected(scaffold.id))
        assertFalse(controller.state.isNodeSelected(column.id))

        // Toggle multi-selection (Shift/Ctrl-click)
        controller.toggleSelectNode(column.id)
        assertEquals(setOf(scaffold.id, column.id), controller.state.selectedNodeIds)

        // Toggle again to deselect scaffold
        controller.toggleSelectNode(scaffold.id)
        assertEquals(setOf(column.id), controller.state.selectedNodeIds)

        // Clear selection (Project level)
        controller.clearSelection()
        assertTrue(controller.state.selectedNodeIds.isEmpty())
    }

    @Test
    fun testHistoryStackMultiStepUndoRedo() {
        val stack = HistoryStack<String>(maxDepth = 10)
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)
        assertEquals(0, stack.getUndoList().size)
        assertEquals(0, stack.getRedoList().size)

        stack.push("State 1")
        stack.push("State 2")
        stack.push("State 3")

        assertTrue(stack.canUndo)
        assertFalse(stack.canRedo)
        assertEquals(3, stack.getUndoList().size)
        assertEquals(listOf("State 1", "State 2", "State 3"), stack.getUndoList())

        // Multi-step undo by 2 steps from "State 4" (active)
        val afterUndo = stack.undoSteps(steps = 2, currentState = "State 4")
        assertEquals("State 2", afterUndo)
        assertEquals(1, stack.getUndoList().size)
        assertEquals(2, stack.getRedoList().size)
        assertEquals(listOf("State 3", "State 4"), stack.getRedoList())

        // Multi-step redo by 1 step from "State 2" (active)
        val afterRedo = stack.redoSteps(steps = 1, currentState = "State 2")
        assertEquals("State 3", afterRedo)
        assertEquals(2, stack.getUndoList().size)
        assertEquals(1, stack.getRedoList().size)
        assertEquals(listOf("State 4"), stack.getRedoList())
    }

    @Test
    fun testEditorControllerHistoryTimelineAndCounts() {
        val project = EditorController.newProject(name = "TimelineTest", withDefaultScaffold = true)
        val controller = EditorController(initialProject = project)

        assertEquals(0, controller.state.undoCount)
        assertEquals(0, controller.state.redoCount)
        assertFalse(controller.state.canUndo)
        assertFalse(controller.state.canRedo)

        // Action 1: Add a button
        controller.addNode(ComponentType.BUTTON, CanvasPosition(100f, 100f))
        assertEquals(1, controller.state.undoCount)
        assertEquals(0, controller.state.redoCount)
        assertTrue(controller.state.canUndo)
        assertFalse(controller.state.canRedo)
        assertEquals(1, controller.getUndoSnapshots().size)

        // Action 2: Add a card
        controller.addNode(ComponentType.CARD, CanvasPosition(200f, 200f))
        assertEquals(2, controller.state.undoCount)
        assertEquals(0, controller.state.redoCount)
        assertEquals(2, controller.getUndoSnapshots().size)

        // Undo 1 step
        controller.undo()
        assertEquals(1, controller.state.undoCount)
        assertEquals(1, controller.state.redoCount)
        assertTrue(controller.state.canUndo)
        assertTrue(controller.state.canRedo)
        assertEquals(1, controller.getRedoSnapshots().size)

        // Multi-step jump via undoSteps
        controller.undoSteps(1)
        assertEquals(0, controller.state.undoCount)
        assertEquals(2, controller.state.redoCount)
        assertFalse(controller.state.canUndo)
        assertTrue(controller.state.canRedo)

        // Multi-step jump via redoSteps
        controller.redoSteps(2)
        assertEquals(2, controller.state.undoCount)
        assertEquals(0, controller.state.redoCount)
        assertTrue(controller.state.canUndo)
        assertFalse(controller.state.canRedo)

        // Clear history
        controller.clearHistory()
        assertEquals(0, controller.state.undoCount)
        assertEquals(0, controller.state.redoCount)
        assertFalse(controller.state.canUndo)
        assertFalse(controller.state.canRedo)
        assertTrue(controller.getUndoSnapshots().isEmpty())
        assertTrue(controller.getRedoSnapshots().isEmpty())
    }

    @Test
    fun testPanelResizeClampingAndRatioBounds() {
        // Palette width clamping: 160f .. 480f with collapse threshold < 120f
        val defaultPalette = 240f
        val minPalette = 160f
        val maxPalette = 480f

        var paletteWidth = defaultPalette + 50f
        assertEquals(290f, paletteWidth.coerceIn(minPalette, maxPalette))

        paletteWidth = defaultPalette - 100f // 140f
        assertEquals(160f, paletteWidth.coerceIn(minPalette, maxPalette))

        paletteWidth = defaultPalette - 130f // 110f < 120f -> auto-collapse condition
        assertTrue(paletteWidth < 120f, "Width under 120f should trigger collapse")

        paletteWidth = defaultPalette + 300f // 540f
        assertEquals(480f, paletteWidth.coerceIn(minPalette, maxPalette))

        // Inspector width clamping: 220f .. 600f with collapse threshold < 150f
        val defaultInspector = 280f
        val minInspector = 220f
        val maxInspector = 600f

        var inspectorWidth = defaultInspector - (-50f) // 330f (dragging left increases width)
        assertEquals(330f, inspectorWidth.coerceIn(minInspector, maxInspector))

        inspectorWidth = defaultInspector - 100f // 180f
        assertEquals(220f, inspectorWidth.coerceIn(minInspector, maxInspector))

        inspectorWidth = defaultInspector - 150f // 130f < 150f -> auto-collapse condition
        assertTrue(inspectorWidth < 150f, "Width under 150f should trigger collapse")

        inspectorWidth = defaultInspector - (-400f) // 680f
        assertEquals(600f, inspectorWidth.coerceIn(minInspector, maxInspector))

        // Code Export width clamping: 320f .. 760f
        val defaultCodeExport = 440f
        val minCodeExport = 320f
        val maxCodeExport = 760f

        var codeExportWidth = defaultCodeExport - (-100f) // 540f
        assertEquals(540f, codeExportWidth.coerceIn(minCodeExport, maxCodeExport))

        codeExportWidth = defaultCodeExport - 200f // 240f
        assertEquals(320f, codeExportWidth.coerceIn(minCodeExport, maxCodeExport))

        codeExportWidth = defaultCodeExport - (-400f) // 840f
        assertEquals(760f, codeExportWidth.coerceIn(minCodeExport, maxCodeExport))

        // Layers vs Properties ratio clamping: 0.15f .. 0.85f
        var layersRatio = 0.5f
        layersRatio = (layersRatio + 0.2f).coerceIn(0.15f, 0.85f)
        assertEquals(0.7f, layersRatio, 0.001f)

        layersRatio = (0.5f - 0.5f).coerceIn(0.15f, 0.85f)
        assertEquals(0.15f, layersRatio, 0.001f)

        layersRatio = (0.5f + 0.6f).coerceIn(0.15f, 0.85f)
        assertEquals(0.85f, layersRatio, 0.001f)
    }

    @Test
    fun testCardIsContainerAndAcceptsChildren() {
        val controller = EditorController(initialProject = EditorController.newProject("CardProject", withDefaultScaffold = false))
        controller.addNode(ComponentType.CARD, CanvasPosition(20f, 20f))
        val card = controller.state.project.nodes.first { it.type == ComponentType.CARD }
        assertTrue(card.isContainer, "Card should be a container")

        controller.addChildToContainer(card.id, ComponentType.IMAGE)
        controller.addChildToContainer(card.id, ComponentType.TEXT)

        val updatedCard = controller.state.project.findNode(card.id)!!
        assertEquals(2, updatedCard.children.size)
        assertEquals(ComponentType.IMAGE, updatedCard.children[0].type)
        assertEquals(ComponentType.TEXT, updatedCard.children[1].type)
        assertEquals(updatedCard.id, controller.findParent(updatedCard.children[0].id)?.id)
    }

    @Test
    fun testSlotRoleMultiOccupancy() {
        var topBar = CanvasNode(
            type = ComponentType.TOP_APP_BAR,
            name = "TopBar",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 64f)
        )

        val action1 = CanvasNode(type = ComponentType.ICON_BUTTON, name = "Action 1", position = CanvasPosition.Zero, size = CanvasSize(48f, 48f))
        val action2 = CanvasNode(type = ComponentType.ICON_BUTTON, name = "Action 2", position = CanvasPosition.Zero, size = CanvasSize(48f, 48f))

        topBar = topBar.withChildInSlot(action1, SlotRole.ACTIONS)
        topBar = topBar.withChildInSlot(action2, SlotRole.ACTIONS)
        assertEquals(2, topBar.childrenInSlot(SlotRole.ACTIONS).size, "ACTIONS should allow multiple occupants")

        val title1 = CanvasNode(type = ComponentType.TEXT, name = "Title 1", position = CanvasPosition.Zero, size = CanvasSize(100f, 24f))
        val title2 = CanvasNode(type = ComponentType.TEXT, name = "Title 2", position = CanvasPosition.Zero, size = CanvasSize(100f, 24f))

        topBar = topBar.withChildInSlot(title1, SlotRole.TITLE)
        assertEquals("Title 1", topBar.childInSlot(SlotRole.TITLE)?.name)

        topBar = topBar.withChildInSlot(title2, SlotRole.TITLE)
        assertEquals("Title 2", topBar.childInSlot(SlotRole.TITLE)?.name)
        assertEquals(1, topBar.childrenInSlot(SlotRole.TITLE).size, "TITLE should replace single occupant")
    }

    @Test
    fun testTopAppBarSlots() {
        val controller = EditorController(initialProject = EditorController.newProject("TopBarProject", withDefaultScaffold = false))
        controller.addNode(ComponentType.TOP_APP_BAR, CanvasPosition.Zero)
        val topBar = controller.state.project.nodes.first { it.type == ComponentType.TOP_APP_BAR }

        controller.addChildToContainer(topBar.id, ComponentType.TEXT)
        controller.addChildToContainer(topBar.id, ComponentType.ICON_BUTTON)

        val updatedTopBar = controller.state.project.findNode(topBar.id)!!
        assertEquals(SlotRole.TITLE, updatedTopBar.children[0].slot)
        assertEquals(SlotRole.ACTIONS, updatedTopBar.children[1].slot)
    }

    @Test
    fun testListItemSlots() {
        val controller = EditorController(initialProject = EditorController.newProject("ListProject", withDefaultScaffold = false))
        controller.addNode(ComponentType.LIST_ITEM, CanvasPosition.Zero)
        val listItem = controller.state.project.nodes.first { it.type == ComponentType.LIST_ITEM }

        controller.addChildToContainer(listItem.id, ComponentType.ICON)
        controller.addChildToContainer(listItem.id, ComponentType.TEXT)
        controller.addChildToContainer(listItem.id, ComponentType.CHECKBOX)

        val updatedListItem = controller.state.project.findNode(listItem.id)!!
        assertEquals(SlotRole.LEADING, updatedListItem.children[0].slot)
        assertEquals(SlotRole.HEADLINE, updatedListItem.children[1].slot)
        assertEquals(SlotRole.TRAILING, updatedListItem.children[2].slot)
    }

    @Test
    fun testSetNodeSlot() {
        val controller = EditorController(initialProject = EditorController.newProject("SlotSwitchProject", withDefaultScaffold = false))
        controller.addNode(ComponentType.TOP_APP_BAR, CanvasPosition.Zero)
        val topBar = controller.state.project.nodes.first { it.type == ComponentType.TOP_APP_BAR }

        controller.addChildToContainer(topBar.id, ComponentType.ICON_BUTTON)
        val actionButton = controller.state.project.findNode(topBar.id)!!.children.first()
        assertEquals(SlotRole.ACTIONS, actionButton.slot)

        // Switch to NAVIGATION_ICON
        controller.setNodeSlot(actionButton.id, SlotRole.NAVIGATION_ICON)
        val switchedButton = controller.state.project.findNode(actionButton.id)!!
        assertEquals(SlotRole.NAVIGATION_ICON, switchedButton.slot)
    }

    @Test
    fun testComposeCodeGeneratorSlottedTopAppBarAndCard() {
        val generator = ComposeCodeGenerator.default
        val cardNode = CanvasNode(
            type = ComponentType.CARD,
            name = "MyCard",
            position = CanvasPosition.Zero,
            size = CanvasSize(300f, 200f),
            children = listOf(
                CanvasNode(type = ComponentType.TEXT, name = "Card Title", position = CanvasPosition.Zero, size = CanvasSize(100f, 20f))
            )
        )
        val cardCode = generator.generateNodeCode(cardNode)
        assertTrue(cardCode.contains("Card("))
        assertTrue(cardCode.contains("Text("))

        val topBarNode = CanvasNode(
            type = ComponentType.TOP_APP_BAR,
            name = "MyTopBar",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 64f),
            children = listOf(
                CanvasNode(type = ComponentType.TEXT, name = "App Header", position = CanvasPosition.Zero, size = CanvasSize(120f, 24f), slot = SlotRole.TITLE),
                CanvasNode(type = ComponentType.ICON_BUTTON, name = "SearchBtn", position = CanvasPosition.Zero, size = CanvasSize(48f, 48f), slot = SlotRole.ACTIONS)
            )
        )
        val topBarCode = generator.generateNodeCode(topBarNode)
        assertTrue(topBarCode.contains("TopAppBar("))
        assertTrue(topBarCode.contains("title = {"))
        assertTrue(topBarCode.contains("actions = {"))
    }

    @Test
    fun testFabAllowedSlotsInScaffoldOnlyHasFab() {
        assertEquals(listOf(SlotRole.FAB), ComponentType.FAB.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.FAB), ComponentType.EXTENDED_FAB.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.TOP_BAR), ComponentType.TOP_APP_BAR.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.BOTTOM_BAR), ComponentType.NAVIGATION_BAR.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.BOTTOM_BAR), ComponentType.BOTTOM_APP_BAR.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.RAIL), ComponentType.NAVIGATION_RAIL.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.DRAWER), ComponentType.NAVIGATION_DRAWER.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.SNACKBAR), ComponentType.SNACKBAR.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.CONTENT), ComponentType.CARD.allowedSlotsIn(ComponentType.SCAFFOLD))
        assertEquals(listOf(SlotRole.CONTENT), ComponentType.BUTTON.allowedSlotsIn(ComponentType.SCAFFOLD))
    }

    @Test
    fun testSetNodeSlotRejectsInvalidSlot() {
        val controller = EditorController(initialProject = EditorController.newProject("ScaffoldFabTest", withDefaultScaffold = true))
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        controller.addChildToContainer(scaffold.id, ComponentType.FAB)

        val fab = controller.state.project.findNode(scaffold.id)!!.children.first { it.type == ComponentType.FAB }
        assertEquals(SlotRole.FAB, fab.slot)

        // Attempting to move FAB to TOP_BAR should be rejected
        controller.setNodeSlot(fab.id, SlotRole.TOP_BAR)
        val unChangedFab = controller.state.project.findNode(fab.id)!!
        assertEquals(SlotRole.FAB, unChangedFab.slot, "FAB must not be permitted in TOP_BAR slot")
    }

    @Test
    fun testIconButtonAllowedSlotsInTopAppBar() {
        val allowed = ComponentType.ICON_BUTTON.allowedSlotsIn(ComponentType.TOP_APP_BAR)
        assertEquals(listOf(SlotRole.ACTIONS, SlotRole.NAVIGATION_ICON), allowed)
    }

    @Test
    fun testScaffoldRejectsNestedScaffoldAndDialog() {
        assertFalse(ComponentType.SCAFFOLD.canAcceptChild(ComponentType.SCAFFOLD))
        assertFalse(ComponentType.SCAFFOLD.canAcceptChild(ComponentType.ALERT_DIALOG))

        val controller = EditorController(initialProject = EditorController.newProject("ScaffoldTest", withDefaultScaffold = true))
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }

        val addedScaffold = controller.addChildToContainer(scaffold.id, ComponentType.SCAFFOLD)
        assertFalse(addedScaffold, "Scaffold must reject adding another Scaffold as child")

        val addedDialog = controller.addChildToContainer(scaffold.id, ComponentType.ALERT_DIALOG)
        assertFalse(addedDialog, "Scaffold must reject Dialog as in-flow child")
    }

    @Test
    fun testTopAppBarOnlyAcceptsTextIconAndIconButton() {
        assertTrue(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.TEXT))
        assertTrue(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.ICON))
        assertTrue(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.ICON_BUTTON))

        assertFalse(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.CARD))
        assertFalse(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.BUTTON))
        assertFalse(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.COLUMN))
        assertFalse(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.LAZY_COLUMN))
        assertFalse(ComponentType.TOP_APP_BAR.canAcceptChild(ComponentType.SCAFFOLD))

        val controller = EditorController(initialProject = EditorController.newProject("TopBarTest", withDefaultScaffold = true))
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val topBar = scaffold.children.first { it.type == ComponentType.TOP_APP_BAR }

        val addedCard = controller.addChildToContainer(topBar.id, ComponentType.CARD)
        assertFalse(addedCard, "TopAppBar must reject Card child")

        val addedText = controller.addChildToContainer(topBar.id, ComponentType.TEXT)
        assertTrue(addedText, "TopAppBar must accept Text child for Title")
    }

    @Test
    fun testListItemAcceptsUniversalSlotChildren() {
        assertTrue(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.TEXT))
        assertTrue(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.ICON))
        assertTrue(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.CHECKBOX))
        assertTrue(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.SWITCH))
        assertTrue(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.CARD))
        assertTrue(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.COLUMN))
        assertTrue(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.BUTTON))

        assertFalse(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.SCAFFOLD))
        assertFalse(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.BOTTOM_SHEET_SCAFFOLD))
        assertFalse(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.TOP_APP_BAR))
        assertFalse(ComponentType.LIST_ITEM.canAcceptChild(ComponentType.ALERT_DIALOG))

        val controller = EditorController(initialProject = EditorController.newProject("ListItemTest", withDefaultScaffold = false))
        controller.addNode(ComponentType.LIST_ITEM, CanvasPosition.Zero)
        val listItem = controller.state.project.nodes.first { it.type == ComponentType.LIST_ITEM }

        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.CARD))
        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.COLUMN))
        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.TEXT))
    }

    @Test
    fun testLazyColumnRejectsNestedLazyColumnAndGrid() {
        assertFalse(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LAZY_COLUMN))
        assertFalse(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LAZY_VERTICAL_GRID))
        assertFalse(ComponentType.LAZY_VERTICAL_GRID.canAcceptChild(ComponentType.LAZY_COLUMN))
        assertFalse(ComponentType.LAZY_VERTICAL_GRID.canAcceptChild(ComponentType.LAZY_VERTICAL_GRID))

        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.CARD))
        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LIST_ITEM))
        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.ROW))
        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LAZY_ROW)) // Cross-axis lazy layout is allowed

        val controller = EditorController(initialProject = EditorController.newProject("LazyTest", withDefaultScaffold = false))
        controller.addNode(ComponentType.LAZY_COLUMN, CanvasPosition.Zero)
        val lazyCol = controller.state.project.nodes.first { it.type == ComponentType.LAZY_COLUMN }

        assertFalse(controller.addChildToContainer(lazyCol.id, ComponentType.LAZY_COLUMN))
        assertFalse(controller.addChildToContainer(lazyCol.id, ComponentType.LAZY_VERTICAL_GRID))
        assertTrue(controller.addChildToContainer(lazyCol.id, ComponentType.CARD))
    }

    @Test
    fun testLazyRowRejectsNestedLazyRow() {
        assertFalse(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.LAZY_ROW))
        assertTrue(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.CARD))
        assertTrue(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.LAZY_COLUMN)) // Cross-axis is allowed
    }

    @Test
    fun testCardRejectsScaffoldAndStructuralSlots() {
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.SCAFFOLD))
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.TOP_APP_BAR))
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.NAVIGATION_BAR))
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.BOTTOM_APP_BAR))
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.NAVIGATION_RAIL))
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.NAVIGATION_DRAWER))
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.ALERT_DIALOG))
        assertFalse(ComponentType.CARD.canAcceptChild(ComponentType.SNACKBAR))

        assertTrue(ComponentType.CARD.canAcceptChild(ComponentType.BUTTON))
        assertTrue(ComponentType.CARD.canAcceptChild(ComponentType.TEXT))
        assertTrue(ComponentType.CARD.canAcceptChild(ComponentType.IMAGE))
        assertTrue(ComponentType.CARD.canAcceptChild(ComponentType.COLUMN))
    }

    @Test
    fun testSmartAncestorContainerTargeting() {
        val controller = EditorController(initialProject = EditorController.newProject("AncestorTest", withDefaultScaffold = true))
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val contentColumn = scaffold.children.first { it.slot == SlotRole.CONTENT }

        // Add a LazyColumn to contentColumn
        assertTrue(controller.addChildToContainer(contentColumn.id, ComponentType.LAZY_COLUMN))
        val lazyCol = controller.state.project.findNode(contentColumn.id)!!.children.first { it.type == ComponentType.LAZY_COLUMN }

        // When LazyColumn is selected and user wants to add a LazyColumn, LazyColumn cannot accept same-axis LazyColumn,
        // so findValidTargetContainer must climb to contentColumn!
        val target = controller.findValidTargetContainer(forType = ComponentType.LAZY_COLUMN, startingFromNodeId = lazyCol.id)
        assertNotNull(target)
        assertEquals(contentColumn.id, target.id)
    }

    @Test
    fun testSmartAncestorRoutingForScaffoldStructuralSlots() {
        val controller = EditorController(initialProject = EditorController.newProject("ScaffoldRoutingTest", withDefaultScaffold = true))
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val contentColumn = scaffold.children.first { it.slot == SlotRole.CONTENT }

        // Even with contentColumn selected, FAB and TopAppBar route to root Scaffold
        val fabTarget = controller.findValidTargetContainer(forType = ComponentType.FAB, startingFromNodeId = contentColumn.id)
        assertNotNull(fabTarget)
        assertEquals(scaffold.id, fabTarget.id)

        val navBarTarget = controller.findValidTargetContainer(forType = ComponentType.NAVIGATION_BAR, startingFromNodeId = contentColumn.id)
        assertNotNull(navBarTarget)
        assertEquals(scaffold.id, navBarTarget.id)
    }

    @Test
    fun testPasteHierarchyValidationAndClimbing() {
        val controller = EditorController(initialProject = EditorController.newProject("PasteHierarchyTest", withDefaultScaffold = true))
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val contentColumn = scaffold.children.first { it.slot == SlotRole.CONTENT }

        // Add a Button (non-container) to contentColumn
        assertTrue(controller.addChildToContainer(contentColumn.id, ComponentType.BUTTON))
        val button = controller.state.project.findNode(contentColumn.id)!!.children.first { it.type == ComponentType.BUTTON }

        // Select the Button
        controller.selectNode(button.id)

        // Copy a Card
        val cardNode = CanvasNode(
            type = ComponentType.CARD,
            name = "Copied Card",
            position = CanvasPosition.Zero,
            size = CanvasSize(200f, 100f)
        )

        // Paste while Button is selected: paste must climb to contentColumn and not corrupt Button
        val pasted = controller.paste(sourceNode = cardNode)
        assertNotNull(pasted)

        val updatedButton = controller.state.project.findNode(button.id)!!
        assertEquals(0, updatedButton.children.size, "Button must not have the pasted Card as child")

        val updatedContent = controller.state.project.findNode(contentColumn.id)!!
        assertTrue(updatedContent.children.any { it.type == ComponentType.CARD }, "Content Column must contain the pasted Card")
    }

    @Test
    fun testDuplicateSingleOccupantBlocked() {
        val controller = EditorController(initialProject = EditorController.newProject("DuplicateTest", withDefaultScaffold = true))
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val topBar = scaffold.children.first { it.type == ComponentType.TOP_APP_BAR }

        controller.selectNode(topBar.id)
        val duplicate = controller.duplicateSelected()
        assertNull(duplicate, "Duplicating single-occupant TopAppBar must be blocked")
    }

    @Test
    fun testProjectValidateHierarchyDetectsViolations() {
        val controller = EditorController(initialProject = EditorController.newProject("ValidationTest", withDefaultScaffold = true))
        val cleanViolations = controller.validateHierarchy()
        assertTrue(cleanViolations.isEmpty(), "Initial standard project must have 0 hierarchy violations")

        // Manually introduce an illegal child: Card inside TopAppBar
        val scaffold = controller.state.project.nodes.first { it.type == ComponentType.SCAFFOLD }
        val topBar = scaffold.children.first { it.type == ComponentType.TOP_APP_BAR }
        val illegalCard = CanvasNode(
            type = ComponentType.CARD,
            name = "Illegal Card",
            position = CanvasPosition.Zero,
            size = CanvasSize(100f, 50f)
        )
        val corruptedTopBar = topBar.copy(children = listOf(illegalCard))
        controller.replaceProject(controller.state.project.updateNode(corruptedTopBar))

        val violations = controller.validateHierarchy()
        assertEquals(1, violations.size, "validateHierarchy must detect 1 violation")
        assertEquals(illegalCard.id, violations.first().nodeId)
        assertTrue(violations.first().description.contains("Top App Bar cannot contain Card"))
    }

    @Test
    fun testListItemConsecutiveAddsPopulatesDifferentSlots() {
        val controller = EditorController(initialProject = EditorController.newProject("MultiItemTest", withDefaultScaffold = false))
        controller.addNode(ComponentType.LIST_ITEM, CanvasPosition.Zero)
        val listItem = controller.state.project.nodes.first { it.type == ComponentType.LIST_ITEM }

        // 1st text -> HEADLINE
        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.TEXT))
        var updated = controller.state.project.findNode(listItem.id)!!
        assertEquals(1, updated.children.size)
        assertEquals(SlotRole.HEADLINE, updated.children[0].slot)

        // 2nd text -> SUPPORTING
        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.TEXT))
        updated = controller.state.project.findNode(listItem.id)!!
        assertEquals(2, updated.children.size)
        assertEquals(SlotRole.SUPPORTING, updated.children[1].slot)

        // 3rd text -> OVERLINE
        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.TEXT))
        updated = controller.state.project.findNode(listItem.id)!!
        assertEquals(3, updated.children.size)
        assertEquals(SlotRole.OVERLINE, updated.children[2].slot)

        // 1st icon -> LEADING
        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.ICON))
        updated = controller.state.project.findNode(listItem.id)!!
        assertEquals(4, updated.children.size)
        assertEquals(SlotRole.LEADING, updated.children.first { it.type == ComponentType.ICON }.slot)

        // Checkbox -> TRAILING
        assertTrue(controller.addChildToContainer(listItem.id, ComponentType.CHECKBOX))
        updated = controller.state.project.findNode(listItem.id)!!
        assertEquals(5, updated.children.size)
        assertEquals(SlotRole.TRAILING, updated.children.first { it.type == ComponentType.CHECKBOX }.slot)
    }

    @Test
    fun testDialogConsecutiveButtonsPopulatesConfirmAndDismiss() {
        val controller = EditorController(initialProject = EditorController.newProject("DialogTest", withDefaultScaffold = false))
        controller.addNode(ComponentType.ALERT_DIALOG, CanvasPosition.Zero)
        val dialog = controller.state.project.nodes.first { it.type == ComponentType.ALERT_DIALOG }

        // 1st button -> CONFIRM_BUTTON
        assertTrue(controller.addChildToContainer(dialog.id, ComponentType.BUTTON))
        var updated = controller.state.project.findNode(dialog.id)!!
        assertEquals(1, updated.children.size)
        assertEquals(SlotRole.CONFIRM_BUTTON, updated.children[0].slot)

        // 2nd button -> DISMISS_BUTTON
        assertTrue(controller.addChildToContainer(dialog.id, ComponentType.BUTTON))
        updated = controller.state.project.findNode(dialog.id)!!
        assertEquals(2, updated.children.size)
        assertEquals(SlotRole.DISMISS_BUTTON, updated.children[1].slot)
    }

    @Test
    fun testLegacySerializationBackwardsCompatibility() {
        val legacyJson = """
            {
              "id": "node-1",
              "type": "LISTS",
              "name": "Legacy List Item",
              "position": {"x": 0.0, "y": 0.0},
              "size": {"width": 360.0, "height": 56.0},
              "properties": [],
              "children": [
                {
                  "id": "node-2",
                  "type": "DIALOG",
                  "name": "Legacy Dialog",
                  "position": {"x": 0.0, "y": 0.0},
                  "size": {"width": 280.0, "height": 200.0},
                  "properties": [],
                  "children": []
                },
                {
                  "id": "node-3",
                  "type": "SHEETS",
                  "name": "Legacy Sheet",
                  "position": {"x": 0.0, "y": 0.0},
                  "size": {"width": 360.0, "height": 300.0},
                  "properties": [],
                  "children": []
                }
              ]
            }
        """.trimIndent()
        val node = M3EJson.decodeFromString<CanvasNode>(legacyJson)
        assertEquals(ComponentType.LIST_ITEM, node.type)
        assertEquals(ComponentType.ALERT_DIALOG, node.children[0].type)
        assertEquals(ComponentType.MODAL_BOTTOM_SHEET, node.children[1].type)
    }

    @Test
    fun testNewComponentsRegisteredInComponentRegistry() {
        val registeredTypes = ComponentRegistry.all().map { it.type }.toSet()
        val expectedNewTypes = listOf(
            ComponentType.BOX_WITH_CONSTRAINTS,
            ComponentType.BOTTOM_SHEET_SCAFFOLD,
            ComponentType.LAZY_HORIZONTAL_GRID,
            ComponentType.LAZY_VERTICAL_STAGGERED_GRID,
            ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID,
            ComponentType.HORIZONTAL_PAGER,
            ComponentType.CAROUSEL,
            ComponentType.ELEVATED_CARD,
            ComponentType.OUTLINED_CARD,
            ComponentType.BASIC_ALERT_DIALOG,
            ComponentType.TOGGLE_BUTTON,
            ComponentType.NAVIGATION_BAR_ITEM,
            ComponentType.NAVIGATION_RAIL_ITEM,
            ComponentType.TAB,
            ComponentType.DROPDOWN_MENU_ITEM,
            ComponentType.BADGED_BOX,
            ComponentType.SNACKBAR_HOST
        )
        for (type in expectedNewTypes) {
            assertTrue(registeredTypes.contains(type), "Missing registration for component type $type")
            assertNotNull(ComponentRegistry.get(type), "Definition missing for $type")
        }
    }

    @Test
    fun testSameAxisLazyLayoutNestingPrevention() {
        // Vertical lazy layouts reject vertical lazy layouts
        assertFalse(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LAZY_VERTICAL_GRID))
        assertFalse(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LAZY_VERTICAL_STAGGERED_GRID))
        assertFalse(ComponentType.LAZY_VERTICAL_GRID.canAcceptChild(ComponentType.LAZY_COLUMN))
        assertFalse(ComponentType.LAZY_VERTICAL_STAGGERED_GRID.canAcceptChild(ComponentType.LAZY_COLUMN))

        // Horizontal lazy layouts reject horizontal lazy layouts
        assertFalse(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.LAZY_HORIZONTAL_GRID))
        assertFalse(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID))
        assertFalse(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.HORIZONTAL_PAGER))
        assertFalse(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.CAROUSEL))
        assertFalse(ComponentType.HORIZONTAL_PAGER.canAcceptChild(ComponentType.LAZY_ROW))
        assertFalse(ComponentType.CAROUSEL.canAcceptChild(ComponentType.HORIZONTAL_PAGER))

        // Cross-axis is explicitly permitted
        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LAZY_ROW))
        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.LAZY_HORIZONTAL_GRID))
        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.CAROUSEL))
        assertTrue(ComponentType.LAZY_COLUMN.canAcceptChild(ComponentType.HORIZONTAL_PAGER))
        assertTrue(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.LAZY_COLUMN))
        assertTrue(ComponentType.LAZY_ROW.canAcceptChild(ComponentType.LAZY_VERTICAL_GRID))
    }

    @Test
    fun testComposeCodeGeneratorNewComponentsAndFabSizes() {
        val generator = ComposeCodeGenerator.default

        // BottomSheetScaffold code generation
        val sheetScaffoldNode = CanvasNode(
            type = ComponentType.BOTTOM_SHEET_SCAFFOLD,
            name = "MyBottomSheetScaffold",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 917f),
            children = listOf(
                CanvasNode(
                    type = ComponentType.TEXT,
                    name = "Sheet Header",
                    position = CanvasPosition.Zero,
                    size = CanvasSize(200f, 24f),
                    slot = SlotRole.SHEET_CONTENT
                ),
                CanvasNode(
                    type = ComponentType.BUTTON,
                    name = "Main Content Button",
                    position = CanvasPosition.Zero,
                    size = CanvasSize(150f, 48f),
                    properties = listOf(ComponentProperty.Text("text", "Main Content Button")),
                    slot = SlotRole.CONTENT
                )
            )
        )
        val testProject = M3EProject(
            name = "TestScaffold",
            nodes = listOf(sheetScaffoldNode)
        )
        val scaffoldFile = generator.generateFile(testProject, "TestScaffold")
        assertTrue(scaffoldFile.contains("BottomSheetScaffold("))
        assertTrue(scaffoldFile.contains("sheetContent = {"))
        assertTrue(scaffoldFile.contains("Sheet Header"))
        assertTrue(scaffoldFile.contains("Main Content Button"))

        // BoxWithConstraints
        val bwcNode = CanvasNode(
            type = ComponentType.BOX_WITH_CONSTRAINTS,
            name = "MyBwc",
            position = CanvasPosition.Zero,
            size = CanvasSize(300f, 200f)
        )
        val bwcCode = generator.generateNodeCode(bwcNode)
        assertTrue(bwcCode.contains("BoxWithConstraints("))

        // LazyHorizontalGrid
        val lhgNode = CanvasNode(
            type = ComponentType.LAZY_HORIZONTAL_GRID,
            name = "MyLhg",
            position = CanvasPosition.Zero,
            size = CanvasSize(400f, 200f)
        )
        assertTrue(generator.generateNodeCode(lhgNode).contains("LazyHorizontalGrid("))

        // Staggered Grids
        val lvsgNode = CanvasNode(
            type = ComponentType.LAZY_VERTICAL_STAGGERED_GRID,
            name = "MyLvsg",
            position = CanvasPosition.Zero,
            size = CanvasSize(400f, 600f)
        )
        assertTrue(generator.generateNodeCode(lvsgNode).contains("LazyVerticalStaggeredGrid("))

        val lhsgNode = CanvasNode(
            type = ComponentType.LAZY_HORIZONTAL_STAGGERED_GRID,
            name = "MyLhsg",
            position = CanvasPosition.Zero,
            size = CanvasSize(600f, 400f)
        )
        assertTrue(generator.generateNodeCode(lhsgNode).contains("LazyHorizontalStaggeredGrid("))

        // Pager & Carousel
        val pagerNode = CanvasNode(
            type = ComponentType.HORIZONTAL_PAGER,
            name = "MyPager",
            position = CanvasPosition.Zero,
            size = CanvasSize(400f, 300f)
        )
        assertTrue(generator.generateNodeCode(pagerNode).contains("HorizontalPager("))

        val carouselNode = CanvasNode(
            type = ComponentType.CAROUSEL,
            name = "MyCarousel",
            position = CanvasPosition.Zero,
            size = CanvasSize(400f, 220f)
        )
        assertTrue(generator.generateNodeCode(carouselNode).contains("HorizontalMultiBrowseCarousel("))

        // Small and Large FAB
        val smallFab = CanvasNode(
            type = ComponentType.FAB,
            name = "SmallFab",
            position = CanvasPosition.Zero,
            size = CanvasSize(40f, 40f),
            properties = listOf(ComponentProperty.Variant("fabSize", MaterialVariant.FabSize.SMALL))
        )
        assertTrue(generator.generateNodeCode(smallFab).contains("SmallFloatingActionButton("))

        val largeFab = CanvasNode(
            type = ComponentType.FAB,
            name = "LargeFab",
            position = CanvasPosition.Zero,
            size = CanvasSize(96f, 96f),
            properties = listOf(ComponentProperty.Variant("fabSize", MaterialVariant.FabSize.LARGE))
        )
        assertTrue(generator.generateNodeCode(largeFab).contains("LargeFloatingActionButton("))

        // ToggleButton
        val toggleBtn = CanvasNode(
            type = ComponentType.TOGGLE_BUTTON,
            name = "MyToggle",
            position = CanvasPosition.Zero,
            size = CanvasSize(100f, 40f),
            properties = listOf(ComponentProperty.Variant("variant", MaterialVariant.ToggleButton.ELEVATED))
        )
        assertTrue(generator.generateNodeCode(toggleBtn).contains("ElevatedToggleButton("))
    }
}