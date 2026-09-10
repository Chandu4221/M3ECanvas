package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.editor.state.EditorController

@Composable
fun CanvasScreen(controller: EditorController) {
    val state = controller.state
    val deviceProfile = state.project.deviceProfile

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE8E8E8)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = deviceProfile.size.width.dp, height = deviceProfile.size.height.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            state.project.nodes.forEach { node ->
                CanvasNodePlacement(node = node, controller = controller)
            }
        }
    }
}

@Composable
private fun CanvasNodePlacement(node: CanvasNode, controller: EditorController) {
    val isSelected = controller.state.isNodeSelected(nodeId = node.id)

    Box(
        modifier = Modifier
            .offset(x = node.position.x.dp, y = node.position.y.dp)
            .size(width = node.size.width.dp, height = node.size.height.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .selectOnPress(nodeId = node.id, controller = controller)
            .pointerInput(node.id) {
                detectDragGestures(
                    onDragStart = { controller.startDrag(nodeId = node.id) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        controller.updateDrag(deltaX = dragAmount.x, deltaY = dragAmount.y)
                    },
                    onDragEnd = { controller.endDrag() },
                    onDragCancel = { controller.endDrag() }
                )
            }
    ) {
        CanvasNodeRenderer(node = node, controller = controller)
    }
}