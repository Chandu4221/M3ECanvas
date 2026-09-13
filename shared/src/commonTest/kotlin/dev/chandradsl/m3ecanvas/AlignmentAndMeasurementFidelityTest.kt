package dev.chandradsl.m3ecanvas

import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.service.AlignmentType
import dev.chandradsl.m3ecanvas.editor.service.DistributionType
import dev.chandradsl.m3ecanvas.editor.service.DocumentEditorService
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlignmentAndMeasurementFidelityTest {

    private val editorService = DocumentEditorService()

    @Test
    fun testTopLevelNodeAlignment() {
        val nodeA = CanvasNode(
            id = "node-a",
            type = ComponentType.BUTTON,
            name = "Button A",
            position = CanvasPosition(10f, 20f),
            size = CanvasSize(100f, 50f)
        )
        val nodeB = CanvasNode(
            id = "node-b",
            type = ComponentType.CARD,
            name = "Card B",
            position = CanvasPosition(50f, 80f),
            size = CanvasSize(80f, 40f)
        )
        val project = M3EProject(name = "Test", nodes = listOf(nodeA, nodeB))
        val nodeIds = setOf("node-a", "node-b")

        // Align Left (minX = 10)
        val alignedLeft = editorService.alignNodes(project, nodeIds, AlignmentType.LEFT)
        assertEquals(10f, alignedLeft.findNode("node-a")?.position?.x)
        assertEquals(10f, alignedLeft.findNode("node-b")?.position?.x)

        // Align Right (maxX = 50 + 80 = 130)
        val alignedRight = editorService.alignNodes(project, nodeIds, AlignmentType.RIGHT)
        assertEquals(30f, alignedRight.findNode("node-a")?.position?.x)
        assertEquals(50f, alignedRight.findNode("node-b")?.position?.x)

        // Align Center Horizontally (minX=10, maxX=130, centerX=70)
        val alignedCenterH = editorService.alignNodes(project, nodeIds, AlignmentType.CENTER_HORIZONTALLY)
        assertEquals(20f, alignedCenterH.findNode("node-a")?.position?.x)
        assertEquals(30f, alignedCenterH.findNode("node-b")?.position?.x)

        // Align Top (minY = 20)
        val alignedTop = editorService.alignNodes(project, nodeIds, AlignmentType.TOP)
        assertEquals(20f, alignedTop.findNode("node-a")?.position?.y)
        assertEquals(20f, alignedTop.findNode("node-b")?.position?.y)

        // Align Bottom (maxY = 80 + 40 = 120)
        val alignedBottom = editorService.alignNodes(project, nodeIds, AlignmentType.BOTTOM)
        assertEquals(70f, alignedBottom.findNode("node-a")?.position?.y)
        assertEquals(80f, alignedBottom.findNode("node-b")?.position?.y)

        // Align Center Vertically (minY=20, maxY=120, centerY=70)
        val alignedCenterV = editorService.alignNodes(project, nodeIds, AlignmentType.CENTER_VERTICALLY)
        assertEquals(45f, alignedCenterV.findNode("node-a")?.position?.y)
        assertEquals(50f, alignedCenterV.findNode("node-b")?.position?.y)
    }

    @Test
    fun testContainerLayoutAwareAlignment() {
        val child1 = CanvasNode(id = "c1", type = ComponentType.BUTTON, name = "Btn1", position = CanvasPosition.Zero, size = CanvasSize(100f, 40f))
        val child2 = CanvasNode(id = "c2", type = ComponentType.BUTTON, name = "Btn2", position = CanvasPosition.Zero, size = CanvasSize(100f, 40f))
        val column = CanvasNode(
            id = "col",
            type = ComponentType.COLUMN,
            name = "Column",
            position = CanvasPosition.Zero,
            size = CanvasSize(300f, 400f),
            children = listOf(child1, child2)
        )
        val project = M3EProject(name = "ColTest", nodes = listOf(column))
        val childIds = setOf("c1", "c2")

        // In Column, horizontal alignment applies ModifierSpec.Align
        val colAlignedLeft = editorService.alignNodes(project, childIds, AlignmentType.LEFT)
        val c1Align = colAlignedLeft.findNode("c1")?.modifiers?.filterIsInstance<ModifierSpec.Align>()?.firstOrNull()
        val c2Align = colAlignedLeft.findNode("c2")?.modifiers?.filterIsInstance<ModifierSpec.Align>()?.firstOrNull()
        assertEquals(AlignTarget.START, c1Align?.alignment)
        assertEquals(AlignTarget.START, c2Align?.alignment)

        // In Row, vertical alignment applies ModifierSpec.Align
        val row = CanvasNode(
            id = "row",
            type = ComponentType.ROW,
            name = "Row",
            position = CanvasPosition.Zero,
            size = CanvasSize(400f, 100f),
            children = listOf(child1, child2)
        )
        val rowProject = M3EProject(name = "RowTest", nodes = listOf(row))
        val rowAlignedBottom = editorService.alignNodes(rowProject, childIds, AlignmentType.BOTTOM)
        val r1Align = rowAlignedBottom.findNode("c1")?.modifiers?.filterIsInstance<ModifierSpec.Align>()?.firstOrNull()
        val r2Align = rowAlignedBottom.findNode("c2")?.modifiers?.filterIsInstance<ModifierSpec.Align>()?.firstOrNull()
        assertEquals(AlignTarget.BOTTOM, r1Align?.alignment)
        assertEquals(AlignTarget.BOTTOM, r2Align?.alignment)
    }

    @Test
    fun testTopLevelNodeDistribution() {
        val node1 = CanvasNode(id = "n1", type = ComponentType.BUTTON, name = "N1", position = CanvasPosition(0f, 0f), size = CanvasSize(50f, 50f))
        val node2 = CanvasNode(id = "n2", type = ComponentType.BUTTON, name = "N2", position = CanvasPosition(80f, 0f), size = CanvasSize(50f, 50f))
        val node3 = CanvasNode(id = "n3", type = ComponentType.BUTTON, name = "N3", position = CanvasPosition(200f, 0f), size = CanvasSize(50f, 50f))
        val project = M3EProject(name = "DistTest", nodes = listOf(node1, node2, node3))
        val nodeIds = setOf("n1", "n2", "n3")

        val distributedH = editorService.distributeNodes(project, nodeIds, DistributionType.HORIZONTALLY)
        // Total span: 250 - 0 = 250. Items: 3 * 50 = 150. Available: 100. Gap: 50.
        assertEquals(0f, distributedH.findNode("n1")?.position?.x)
        assertEquals(100f, distributedH.findNode("n2")?.position?.x)
        assertEquals(200f, distributedH.findNode("n3")?.position?.x)

        val nodeV1 = CanvasNode(id = "v1", type = ComponentType.BUTTON, name = "V1", position = CanvasPosition(0f, 0f), size = CanvasSize(50f, 40f))
        val nodeV2 = CanvasNode(id = "v2", type = ComponentType.BUTTON, name = "V2", position = CanvasPosition(0f, 70f), size = CanvasSize(50f, 40f))
        val nodeV3 = CanvasNode(id = "v3", type = ComponentType.BUTTON, name = "V3", position = CanvasPosition(0f, 160f), size = CanvasSize(50f, 40f))
        val vProject = M3EProject(name = "VTest", nodes = listOf(nodeV1, nodeV2, nodeV3))
        val vIds = setOf("v1", "v2", "v3")

        val distributedV = editorService.distributeNodes(vProject, vIds, DistributionType.VERTICALLY)
        // Total span: 160 + 40 - 0 = 200. Items: 3 * 40 = 120. Available: 80. Gap: 40.
        assertEquals(0f, distributedV.findNode("v1")?.position?.y)
        assertEquals(80f, distributedV.findNode("v2")?.position?.y)
        assertEquals(160f, distributedV.findNode("v3")?.position?.y)
    }

    @Test
    fun testEditorControllerRulerAndMeasurementToggles() {
        val controller = EditorController()
        assertTrue(controller.state.showRulers)
        assertTrue(controller.state.showMeasurements)

        controller.toggleRulers()
        assertFalse(controller.state.showRulers)

        controller.toggleMeasurements()
        assertFalse(controller.state.showMeasurements)

        controller.toggleRulers()
        assertTrue(controller.state.showRulers)

        controller.toggleMeasurements()
        assertTrue(controller.state.showMeasurements)
    }
}
