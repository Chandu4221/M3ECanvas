package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.component.ComponentRegistry
import dev.chandradsl.m3ecanvas.editor.palette.CommandCategory
import dev.chandradsl.m3ecanvas.editor.palette.CommandItem
import dev.chandradsl.m3ecanvas.editor.palette.buildAllCommands
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.state.EditorMode
import kotlin.test.*

class CommandPaletteAndLibraryTest {

    @Test
    fun testFavoritesTrackingAndToggling() {
        val controller = EditorController(initialProject = M3EProject(name = "Test"))
        assertEquals(emptySet(), controller.state.favoriteComponentTypes)

        // Add Button to favorites
        controller.toggleFavoriteComponent(ComponentType.BUTTON)
        assertTrue(ComponentType.BUTTON in controller.state.favoriteComponentTypes)

        // Add Card to favorites
        controller.toggleFavoriteComponent(ComponentType.CARD)
        assertEquals(setOf(ComponentType.BUTTON, ComponentType.CARD), controller.state.favoriteComponentTypes)

        // Toggle Button again to remove
        controller.toggleFavoriteComponent(ComponentType.BUTTON)
        assertFalse(ComponentType.BUTTON in controller.state.favoriteComponentTypes)
        assertEquals(setOf(ComponentType.CARD), controller.state.favoriteComponentTypes)
    }

    @Test
    fun testRecentComponentsTracking() {
        val controller = EditorController(initialProject = M3EProject(name = "Test"))
        assertTrue(controller.state.recentComponentTypes.isEmpty())

        // Insert Button
        controller.insertComponent(ComponentType.BUTTON)
        assertEquals(listOf(ComponentType.BUTTON), controller.state.recentComponentTypes)

        // Insert Text
        controller.insertComponent(ComponentType.TEXT)
        assertEquals(listOf(ComponentType.TEXT, ComponentType.BUTTON), controller.state.recentComponentTypes)

        // Insert Button again (should move to front, no duplicate)
        controller.insertComponent(ComponentType.BUTTON)
        assertEquals(listOf(ComponentType.BUTTON, ComponentType.TEXT), controller.state.recentComponentTypes)

        // Verify cap at 10 items
        val allTypes = ComponentType.entries.take(15)
        allTypes.forEach { type ->
            controller.recordRecentComponent(type)
        }
        assertEquals(10, controller.state.recentComponentTypes.size)
    }

    @Test
    fun testInsertComponentSmartPlacement() {
        val contentColumn = CanvasNode(
            id = "content-col",
            type = ComponentType.COLUMN,
            name = "Content",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 700f),
            slot = SlotRole.CONTENT
        )
        val scaffold = CanvasNode(
            id = "scaffold",
            type = ComponentType.SCAFFOLD,
            name = "Scaffold",
            position = CanvasPosition.Zero,
            size = CanvasSize(412f, 915f),
            children = listOf(contentColumn)
        )
        val project = M3EProject(name = "Test", nodes = listOf(scaffold))
        val controller = EditorController(initialProject = project)

        // Select the Column
        controller.selectNode("content-col")

        // Insert Button -> should be placed inside content-col
        val inserted = controller.insertComponent(ComponentType.BUTTON)
        assertTrue(inserted, "Button should be successfully inserted")

        val updatedCol = controller.state.project.findNode("content-col")!!
        assertEquals(1, updatedCol.children.size, "Button should be added as child of selected Column")
        assertEquals(ComponentType.BUTTON, updatedCol.children[0].type)
        assertTrue(controller.state.isNodeSelected(updatedCol.children[0].id), "Newly inserted child should be selected")
    }

    @Test
    fun testBuildAllCommandsContainsRequiredCategories() {
        val node1 = CanvasNode(
            id = "test-node",
            type = ComponentType.CARD,
            name = "User Profile Card",
            position = CanvasPosition(20f, 40f),
            size = CanvasSize(200f, 150f)
        )
        val controller = EditorController(initialProject = M3EProject(name = "Test", nodes = listOf(node1)))

        val commands = buildAllCommands(controller)
        assertTrue(commands.isNotEmpty())

        val actionCommands = commands.filterIsInstance<CommandItem.ActionCommand>()
        val componentCommands = commands.filterIsInstance<CommandItem.ComponentCommand>()
        val deviceCommands = commands.filterIsInstance<CommandItem.DeviceCommand>()
        val layerCommands = commands.filterIsInstance<CommandItem.LayerCommand>()

        // Check Action Commands
        assertTrue(actionCommands.any { it.id == "toggle_mode" })
        assertTrue(actionCommands.any { it.id == "undo" })
        assertTrue(actionCommands.any { it.id == "redo" })
        assertTrue(actionCommands.any { it.id == "zoom_in" })
        assertTrue(actionCommands.any { it.id == "zoom_fit" })
        assertTrue(actionCommands.any { it.id == "group_selection" })
        assertTrue(actionCommands.any { it.id == "ungroup_selection" })
        assertTrue(actionCommands.any { it.id == "align_center" })
        assertTrue(actionCommands.any { it.id == "distribute_horizontally" })

        // Check Component Commands covers all ComponentTypes
        assertEquals(ComponentType.entries.size, componentCommands.size)
        assertTrue(componentCommands.any { it.type == ComponentType.BUTTON })
        assertTrue(componentCommands.any { it.type == ComponentType.ALERT_DIALOG })

        // Check Device Commands covers presets
        assertTrue(deviceCommands.any { it.id.startsWith("device_") })
        assertTrue(deviceCommands.any { it.id == "toggle_orientation" })

        // Check Layer Commands contains our node
        assertEquals(1, layerCommands.size)
        assertEquals("test-node", layerCommands[0].node.id)
        assertTrue(layerCommands[0].title.contains("User Profile Card"))
    }

    @Test
    fun testCommandExecution() {
        val node1 = CanvasNode(
            id = "layer-1",
            type = ComponentType.BUTTON,
            name = "My Button",
            position = CanvasPosition(10f, 10f),
            size = CanvasSize(100f, 40f)
        )
        val controller = EditorController(initialProject = M3EProject(name = "Test", nodes = listOf(node1)))

        val commands = buildAllCommands(controller)

        // 1. Execute Mode Toggle Command
        val toggleModeCmd = commands.filterIsInstance<CommandItem.ActionCommand>().first { it.id == "toggle_mode" }
        assertEquals(EditorMode.DESIGN, controller.state.mode)
        toggleModeCmd.execute(controller)
        assertEquals(EditorMode.INTERACTIVE, controller.state.mode)

        // 2. Execute Layer Select Command
        val layerCmd = commands.filterIsInstance<CommandItem.LayerCommand>().first { it.node.id == "layer-1" }
        controller.clearSelection()
        assertFalse(controller.state.isNodeSelected("layer-1"))
        layerCmd.execute(controller)
        assertTrue(controller.state.isNodeSelected("layer-1"))

        // 3. Execute Device Switch Command
        val tabletProfile = DeviceProfile.presets.first { it.category == DeviceCategory.TABLET }
        val deviceCmd = commands.filterIsInstance<CommandItem.DeviceCommand>().first { it.id == "device_${tabletProfile.id}" }
        deviceCmd.execute(controller)
        assertEquals(tabletProfile.id, controller.state.project.deviceProfile.id)

        // 4. Execute Component Command
        val insertTextCmd = commands.filterIsInstance<CommandItem.ComponentCommand>().first { it.type == ComponentType.TEXT }
        insertTextCmd.execute(controller)
        assertTrue(controller.state.project.allNodes().any { it.type == ComponentType.TEXT })
    }

    @Test
    fun testCommandPaletteOpenCloseToggle() {
        val controller = EditorController(initialProject = M3EProject(name = "Test"))
        assertFalse(controller.state.isCommandPaletteOpen)

        controller.openCommandPalette()
        assertTrue(controller.state.isCommandPaletteOpen)

        controller.closeCommandPalette()
        assertFalse(controller.state.isCommandPaletteOpen)

        controller.toggleCommandPalette()
        assertTrue(controller.state.isCommandPaletteOpen)

        controller.toggleCommandPalette()
        assertFalse(controller.state.isCommandPaletteOpen)
    }

    @Test
    fun testComponentRegistryDescriptionsAndParameters() {
        val buttonDef = ComponentRegistry.get(ComponentType.BUTTON)
        assertNotNull(buttonDef)
        assertTrue(buttonDef.description.isNotEmpty(), "Button definition should have description")
        assertTrue(buttonDef.keyParameters.isNotEmpty(), "Button definition should have key parameters")
        assertTrue(buttonDef.keyParameters.contains("text"))

        val scaffoldDef = ComponentRegistry.get(ComponentType.SCAFFOLD)
        assertNotNull(scaffoldDef)
        assertTrue(scaffoldDef.description.isNotEmpty())
        assertTrue(scaffoldDef.keyParameters.contains("topBar"))

        val dialogDef = ComponentRegistry.get(ComponentType.ALERT_DIALOG)
        assertNotNull(dialogDef)
        assertTrue(dialogDef.description.isNotEmpty())
        assertTrue(dialogDef.keyParameters.contains("confirmButton"))
    }
}
