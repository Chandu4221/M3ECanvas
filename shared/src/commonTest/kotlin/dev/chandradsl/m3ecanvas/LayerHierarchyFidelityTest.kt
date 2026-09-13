package dev.chandradsl.m3ecanvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.canvas.InMemoryLayoutGeometryStore
import dev.chandradsl.m3ecanvas.editor.canvas.RenderedBounds
import dev.chandradsl.m3ecanvas.editor.persistence.M3EJson
import dev.chandradsl.m3ecanvas.editor.service.AlignmentType
import dev.chandradsl.m3ecanvas.editor.service.DistributionType
import dev.chandradsl.m3ecanvas.editor.service.DocumentEditorService
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlin.test.*

class LayerHierarchyFidelityTest {

    private val documentEditor = DocumentEditorService()

    @Test
    fun testDefaultNodeVisibilityAndLocking() {
        val node = CanvasNode(
            type = ComponentType.BUTTON,
            name = "TestButton",
            position = CanvasPosition(10f, 20f),
            size = CanvasSize(100f, 40f)
        )
        assertTrue(node.isVisible, "Default node should be visible")
        assertFalse(node.isLocked, "Default node should not be locked")

        val hidden = node.withVisibility(false)
        assertFalse(hidden.isVisible)
        assertTrue(hidden.withVisibility(true).isVisible)

        val locked = node.withLocked(true)
        assertTrue(locked.isLocked)
        assertFalse(locked.withLocked(false).isLocked)
    }

    @Test
    fun testToggleVisibilityAndLockInDocumentEditor() {
        val node1 = CanvasNode(
            id = "btn-1",
            type = ComponentType.BUTTON,
            name = "Button 1",
            position = CanvasPosition(10f, 20f),
            size = CanvasSize(100f, 40f)
        )
        val childNode = CanvasNode(
            id = "child-text",
            type = ComponentType.TEXT,
            name = "Child Text",
            position = CanvasPosition.Zero,
            size = CanvasSize(60f, 20f)
        )
        val colNode = CanvasNode(
            id = "col-1",
            type = ComponentType.COLUMN,
            name = "Column",
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(200f, 200f),
            children = listOf(childNode)
        )
        var project = M3EProject(name = "Test", nodes = listOf(node1, colNode))

        // Toggle visibility on top-level node
        project = documentEditor.toggleNodeVisibility(project, "btn-1")
        assertFalse(project.findNode("btn-1")!!.isVisible)
        project = documentEditor.toggleNodeVisibility(project, "btn-1")
        assertTrue(project.findNode("btn-1")!!.isVisible)

        // Toggle visibility on nested child node
        project = documentEditor.toggleNodeVisibility(project, "child-text")
        assertFalse(project.findNode("child-text")!!.isVisible)
        project = documentEditor.toggleNodeVisibility(project, "child-text")
        assertTrue(project.findNode("child-text")!!.isVisible)

        // Toggle lock on top-level node
        project = documentEditor.toggleNodeLock(project, "col-1")
        assertTrue(project.findNode("col-1")!!.isLocked)
        project = documentEditor.toggleNodeLock(project, "col-1")
        assertFalse(project.findNode("col-1")!!.isLocked)

        // Toggle lock on nested child node
        project = documentEditor.toggleNodeLock(project, "child-text")
        assertTrue(project.findNode("child-text")!!.isLocked)
        project = documentEditor.toggleNodeLock(project, "child-text")
        assertFalse(project.findNode("child-text")!!.isLocked)
    }

    @Test
    fun testLockedNodesBlockDeletion() {
        val unlockedNode = CanvasNode(
            id = "unlocked-1",
            type = ComponentType.BUTTON,
            name = "Unlocked",
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(100f, 40f),
            isLocked = false
        )
        val lockedNode = CanvasNode(
            id = "locked-1",
            type = ComponentType.BUTTON,
            name = "Locked",
            position = CanvasPosition(0f, 50f),
            size = CanvasSize(100f, 40f),
            isLocked = true
        )
        var project = M3EProject(name = "Test", nodes = listOf(unlockedNode, lockedNode))

        // Attempt to delete both nodes
        project = documentEditor.deleteNodes(project, setOf("unlocked-1", "locked-1"))
        assertNull(project.findNode("unlocked-1"), "Unlocked node should be deleted")
        assertNotNull(project.findNode("locked-1"), "Locked node MUST NOT be deleted")
    }

    @Test
    fun testLockedNodesBlockNudge() {
        val lockedNode = CanvasNode(
            id = "locked-1",
            type = ComponentType.CARD,
            name = "Locked Card",
            position = CanvasPosition(50f, 50f),
            size = CanvasSize(200f, 150f),
            isLocked = true
        )
        val project = M3EProject(name = "Test", nodes = listOf(lockedNode))

        val (updated, movedAny) = documentEditor.nudgeNodes(project, setOf("locked-1"), dx = 10f, dy = 10f)
        assertFalse(movedAny, "Nudge should report movedAny = false for locked node")
        assertEquals(50f, updated.findNode("locked-1")!!.position.x, "Position X must not change for locked node")
        assertEquals(50f, updated.findNode("locked-1")!!.position.y, "Position Y must not change for locked node")
    }

    @Test
    fun testLockedNodesExcludedFromAlignmentAndDistribution() {
        val node1 = CanvasNode(
            id = "n1",
            type = ComponentType.BUTTON,
            name = "N1",
            position = CanvasPosition(0f, 10f),
            size = CanvasSize(50f, 30f),
            isLocked = false
        )
        val node2 = CanvasNode(
            id = "n2",
            type = ComponentType.BUTTON,
            name = "N2",
            position = CanvasPosition(100f, 10f),
            size = CanvasSize(50f, 30f),
            isLocked = true // Locked
        )
        val node3 = CanvasNode(
            id = "n3",
            type = ComponentType.BUTTON,
            name = "N3",
            position = CanvasPosition(200f, 50f),
            size = CanvasSize(50f, 30f),
            isLocked = false
        )
        val project = M3EProject(name = "Test", nodes = listOf(node1, node2, node3))

        // Align top on all 3 nodes: node2 is locked so it shouldn't align or move
        val aligned = documentEditor.alignNodes(project, setOf("n1", "n2", "n3"), AlignmentType.TOP)
        assertEquals(10f, aligned.findNode("n2")!!.position.y, "Locked node2 position must remain intact")

        // Distribution requires at least 3 non-locked nodes; with node2 locked, only 2 remain
        val distributed = documentEditor.distributeNodes(project, setOf("n1", "n2", "n3"), DistributionType.HORIZONTALLY)
        assertEquals(project, distributed, "Distribute must do nothing when less than 3 non-locked nodes are selected")
    }

    @Test
    fun testEditorControllerLockAndVisibilityActions() {
        val node = CanvasNode(
            id = "node-1",
            type = ComponentType.BUTTON,
            name = "Button",
            position = CanvasPosition(10f, 20f),
            size = CanvasSize(100f, 40f)
        )
        val controller = EditorController(initialProject = M3EProject(name = "Test", nodes = listOf(node)))

        // Toggle visibility via controller
        controller.toggleNodeVisibility("node-1")
        assertFalse(controller.state.project.findNode("node-1")!!.isVisible)
        controller.undo()
        assertTrue(controller.state.project.findNode("node-1")!!.isVisible)

        // Toggle lock via controller
        controller.toggleNodeLock("node-1")
        assertTrue(controller.state.project.findNode("node-1")!!.isLocked)
        controller.undo()
        assertFalse(controller.state.project.findNode("node-1")!!.isLocked)

        // Delete selected when locked
        controller.toggleNodeLock("node-1")
        controller.selectNode("node-1")
        controller.deleteSelected()
        assertNotNull(controller.state.project.findNode("node-1"), "Locked node cannot be deleted via deleteSelected()")
    }

    @Test
    fun testCollapsedContainerState() {
        val controller = EditorController()
        assertTrue(controller.state.collapsedNodeIds.isEmpty())

        controller.toggleCollapseNode("container-1")
        assertTrue("container-1" in controller.state.collapsedNodeIds)

        controller.toggleCollapseNode("container-2")
        assertEquals(setOf("container-1", "container-2"), controller.state.collapsedNodeIds)

        controller.toggleCollapseNode("container-1")
        assertEquals(setOf("container-2"), controller.state.collapsedNodeIds)
    }

    @Test
    fun testRenameNode() {
        val node = CanvasNode(
            id = "node-1",
            type = ComponentType.BUTTON,
            name = "InitialName",
            position = CanvasPosition.Zero,
            size = CanvasSize(100f, 40f)
        )
        val controller = EditorController(initialProject = M3EProject(name = "Test", nodes = listOf(node)))

        controller.renameNode("node-1", "RenamedButton")
        assertEquals("RenamedButton", controller.state.project.findNode("node-1")!!.name)

        controller.undo()
        assertEquals("InitialName", controller.state.project.findNode("node-1")!!.name)
    }

    @Test
    fun testLayoutGeometryStoreIgnoresHiddenAndLockedNodes() {
        val visibleUnlocked = CanvasNode(
            id = "n-visible",
            type = ComponentType.BUTTON,
            name = "Visible",
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(100f, 50f),
            isVisible = true,
            isLocked = false
        )
        val hiddenNode = CanvasNode(
            id = "n-hidden",
            type = ComponentType.BUTTON,
            name = "Hidden",
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(100f, 50f),
            isVisible = false,
            isLocked = false
        )
        val lockedNode = CanvasNode(
            id = "n-locked",
            type = ComponentType.BUTTON,
            name = "Locked",
            position = CanvasPosition(0f, 0f),
            size = CanvasSize(100f, 50f),
            isVisible = true,
            isLocked = true
        )

        val project = M3EProject(name = "Test", nodes = listOf(visibleUnlocked, hiddenNode, lockedNode))
        val store = InMemoryLayoutGeometryStore()

        val bounds = RenderedBounds(
            boundsInCanvas = Rect(0f, 0f, 100f, 50f),
            sizePx = IntSize(100, 50)
        )
        store.updateBounds("n-visible", bounds)
        store.updateBounds("n-hidden", bounds)
        store.updateBounds("n-locked", bounds)

        // Hit testing at (50, 25)
        val hit = store.findNodeAt(Offset(50f, 25f), project)
        assertEquals("n-visible", hit?.id, "Hit testing must only return visible, unlocked nodes")

        // Marquee selection
        val inRect = store.findNodesIn(Rect(-10f, -10f, 150f, 150f), project)
        assertEquals(listOf("n-visible"), inRect.map { it.id }, "Marquee selection must only select visible, unlocked nodes")
    }

    @Test
    fun testSerializationRoundtripWithVisibilityAndLocking() {
        val original = CanvasNode(
            id = "test-node",
            type = ComponentType.CARD,
            name = "LockedHiddenCard",
            position = CanvasPosition(15f, 25f),
            size = CanvasSize(250f, 180f),
            isVisible = false,
            isLocked = true
        )
        val project = M3EProject(name = "Test", nodes = listOf(original))

        val json = M3EJson.encodeToString(project)
        val restored = M3EJson.decodeFromString<M3EProject>(json)

        val restoredNode = restored.findNode("test-node")!!
        assertFalse(restoredNode.isVisible, "restored node should have isVisible = false")
        assertTrue(restoredNode.isLocked, "restored node should have isLocked = true")
    }
}
