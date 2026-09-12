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
        assertEquals(49, ComponentType.entries.size)
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
        assertEquals(49, ComponentType.entries.size)
        for (type in ComponentType.entries) {
            val size = EditorController.defaultSizeFor(type)
            assertTrue(size.width > 0f, "Width must be > 0 for ${type.name}")
            assertTrue(size.height > 0f, "Height must be > 0 for ${type.name}")
        }
    }

    @Test
    fun testEveryComponentTypeIsDeletableFromProject() {
        assertEquals(49, ComponentType.entries.size)
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
}