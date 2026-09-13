package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.service.DocumentEditorService
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlin.test.*

class GroupUngroupFidelityTest {

    private val documentEditor = DocumentEditorService()

    @Test
    fun testGroupTopLevelNodes() {
        val node1 = CanvasNode(
            id = "btn-1",
            type = ComponentType.BUTTON,
            name = "Button 1",
            position = CanvasPosition(20f, 30f),
            size = CanvasSize(100f, 40f)
        )
        val node2 = CanvasNode(
            id = "btn-2",
            type = ComponentType.BUTTON,
            name = "Button 2",
            position = CanvasPosition(150f, 80f),
            size = CanvasSize(80f, 50f)
        )
        val project = M3EProject(name = "Test", nodes = listOf(node1, node2))

        val (updated, groupId) = documentEditor.groupNodes(project, setOf("btn-1", "btn-2"), ComponentType.BOX)
        assertNotNull(groupId)
        assertEquals(1, updated.nodes.size, "Project should have 1 root node (the group)")

        val groupNode = updated.findNode(groupId)!!
        assertEquals(ComponentType.BOX, groupNode.type)
        // Bounding box: minX=20, minY=30, maxX=230 (150+80), maxY=130 (80+50)
        assertEquals(20f, groupNode.position.x)
        assertEquals(30f, groupNode.position.y)
        assertEquals(210f, groupNode.size.width)
        assertEquals(100f, groupNode.size.height)

        assertEquals(2, groupNode.children.size)
        val child1 = groupNode.children.find { it.id == "btn-1" }!!
        val child2 = groupNode.children.find { it.id == "btn-2" }!!

        // Coordinates should be adjusted relative to group container
        assertEquals(0f, child1.position.x)
        assertEquals(0f, child1.position.y)
        assertEquals(130f, child2.position.x) // 150 - 20
        assertEquals(50f, child2.position.y)  // 80 - 30
    }

    @Test
    fun testUngroupTopLevelContainer() {
        val child1 = CanvasNode(
            id = "child-1",
            type = ComponentType.BUTTON,
            name = "Child 1",
            position = CanvasPosition(10f, 15f),
            size = CanvasSize(80f, 40f)
        )
        val child2 = CanvasNode(
            id = "child-2",
            type = ComponentType.BUTTON,
            name = "Child 2",
            position = CanvasPosition(100f, 20f),
            size = CanvasSize(80f, 40f)
        )
        val groupContainer = CanvasNode(
            id = "group-1",
            type = ComponentType.BOX,
            name = "Group 1",
            position = CanvasPosition(50f, 60f),
            size = CanvasSize(200f, 100f),
            children = listOf(child1, child2)
        )
        val project = M3EProject(name = "Test", nodes = listOf(groupContainer))

        val (updated, releasedIds) = documentEditor.ungroupNode(project, "group-1")
        assertEquals(setOf("child-1", "child-2"), releasedIds.toSet())
        assertEquals(2, updated.nodes.size)
        assertNull(updated.findNode("group-1"))

        val released1 = updated.findNode("child-1")!!
        val released2 = updated.findNode("child-2")!!

        // Root coordinates should be restored: container.pos + child.pos
        assertEquals(60f, released1.position.x) // 50 + 10
        assertEquals(75f, released1.position.y) // 60 + 15
        assertEquals(150f, released2.position.x) // 50 + 100
        assertEquals(80f, released2.position.y)  // 60 + 20
    }

    @Test
    fun testGroupAndUngroupNestedSiblings() {
        val t1 = CanvasNode(id = "t1", type = ComponentType.TEXT, name = "T1", position = CanvasPosition.Zero, size = CanvasSize(50f, 20f))
        val t2 = CanvasNode(id = "t2", type = ComponentType.TEXT, name = "T2", position = CanvasPosition.Zero, size = CanvasSize(50f, 20f))
        val t3 = CanvasNode(id = "t3", type = ComponentType.TEXT, name = "T3", position = CanvasPosition.Zero, size = CanvasSize(50f, 20f))

        val column = CanvasNode(
            id = "col",
            type = ComponentType.COLUMN,
            name = "Col",
            position = CanvasPosition.Zero,
            size = CanvasSize(200f, 400f),
            children = listOf(t1, t2, t3)
        )
        val project = M3EProject(name = "Test", nodes = listOf(column))

        // Group t1 and t2 inside the column
        val (groupedProject, groupId) = documentEditor.groupNodes(project, setOf("t1", "t2"), ComponentType.BOX)
        assertNotNull(groupId)
        val updatedCol = groupedProject.findNode("col")!!
        assertEquals(2, updatedCol.children.size)
        assertEquals(groupId, updatedCol.children[0].id)
        assertEquals("t3", updatedCol.children[1].id)

        val nestedGroup = updatedCol.children[0]
        assertEquals(2, nestedGroup.children.size)
        assertEquals(setOf("t1", "t2"), nestedGroup.children.map { it.id }.toSet())

        // Ungroup the nested container
        val (ungroupedProject, releasedIds) = documentEditor.ungroupNode(groupedProject, groupId)
        assertEquals(setOf("t1", "t2"), releasedIds.toSet())
        val restoredCol = ungroupedProject.findNode("col")!!
        assertEquals(3, restoredCol.children.size)
        assertEquals(listOf("t1", "t2", "t3"), restoredCol.children.map { it.id })
    }

    @Test
    fun testGroupGuards() {
        val lockedNode = CanvasNode(
            id = "locked",
            type = ComponentType.BUTTON,
            name = "Locked",
            position = CanvasPosition.Zero,
            size = CanvasSize(50f, 20f),
            isLocked = true
        )
        val unlockedNode = CanvasNode(
            id = "unlocked",
            type = ComponentType.BUTTON,
            name = "Unlocked",
            position = CanvasPosition.Zero,
            size = CanvasSize(50f, 20f),
            isLocked = false
        )
        val project = M3EProject(name = "Test", nodes = listOf(lockedNode, unlockedNode))

        // Group with locked node will only consider unlocked node
        val (updated, groupId) = documentEditor.groupNodes(project, setOf("locked", "unlocked"), ComponentType.BOX)
        assertNotNull(groupId)
        // Locked node remains root, unlocked was grouped
        assertNotNull(updated.nodes.find { it.id == "locked" })
    }

    @Test
    fun testReparentNodeWithCyclePrevention() {
        val child = CanvasNode(id = "child", type = ComponentType.TEXT, name = "Child", position = CanvasPosition.Zero, size = CanvasSize(50f, 20f))
        val innerBox = CanvasNode(id = "innerBox", type = ComponentType.BOX, name = "Inner", position = CanvasPosition.Zero, size = CanvasSize(100f, 100f), children = listOf(child))
        val outerBox = CanvasNode(id = "outerBox", type = ComponentType.BOX, name = "Outer", position = CanvasPosition.Zero, size = CanvasSize(200f, 200f), children = listOf(innerBox))
        val project = M3EProject(name = "Test", nodes = listOf(outerBox))

        // Reparent child to outerBox
        val reparented1 = documentEditor.reparentNode(project, nodeId = "child", newParentId = "outerBox")
        val updatedOuter = reparented1.findNode("outerBox")!!
        assertEquals(2, updatedOuter.children.size)
        assertTrue(updatedOuter.children.any { it.id == "child" })

        // Cycle Prevention: Try to reparent outerBox into child (which is a descendant)
        val cycleAttempt = documentEditor.reparentNode(project, nodeId = "outerBox", newParentId = "child")
        assertEquals(project, cycleAttempt, "Reparenting an ancestor into its own descendant must be blocked")

        // Move child to root canvas
        val movedToRoot = documentEditor.reparentNode(project, nodeId = "child", newParentId = null)
        assertNotNull(movedToRoot.nodes.find { it.id == "child" }, "Child should now be a root node")
    }

    @Test
    fun testEditorControllerGroupUngroupUndoRedo() {
        val n1 = CanvasNode(id = "n1", type = ComponentType.BUTTON, name = "N1", position = CanvasPosition(10f, 10f), size = CanvasSize(50f, 30f))
        val n2 = CanvasNode(id = "n2", type = ComponentType.BUTTON, name = "N2", position = CanvasPosition(70f, 10f), size = CanvasSize(50f, 30f))
        val controller = EditorController(initialProject = M3EProject(name = "Test", nodes = listOf(n1, n2)))

        controller.selectNodes(setOf("n1", "n2"))
        controller.groupSelected()

        assertEquals(1, controller.state.project.nodes.size)
        val groupId = controller.state.project.nodes.first().id
        assertEquals(setOf(groupId), controller.state.selectedNodeIds)

        // Undo grouping
        controller.undo()
        assertEquals(2, controller.state.project.nodes.size)
        assertNotNull(controller.state.project.findNode("n1"))
        assertNotNull(controller.state.project.findNode("n2"))

        // Redo grouping
        controller.redo()
        assertEquals(1, controller.state.project.nodes.size)

        // Ungroup
        controller.selectNode(groupId)
        controller.ungroupSelected()
        assertEquals(2, controller.state.project.nodes.size)
        assertEquals(setOf("n1", "n2"), controller.state.selectedNodeIds)

        // Undo ungroup
        controller.undo()
        assertEquals(1, controller.state.project.nodes.size)
        assertEquals(groupId, controller.state.project.nodes.first().id)
    }
}
