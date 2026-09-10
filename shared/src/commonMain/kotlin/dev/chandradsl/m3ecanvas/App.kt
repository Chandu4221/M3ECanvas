package dev.chandradsl.m3ecanvas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.editor.canvas.CanvasScreen
import dev.chandradsl.m3ecanvas.editor.inspector.LayersPanel
import dev.chandradsl.m3ecanvas.editor.inspector.PropertiesPanel
import dev.chandradsl.m3ecanvas.editor.palette.ComponentPalette
import dev.chandradsl.m3ecanvas.editor.state.EditorController

@Composable
fun App() {
    val controller = remember {
        EditorController(initialProject = EditorController.newProject())
    }

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            ComponentPalette(
                onAddComponent = { type ->
                    val selected = controller.state.selectedNodes.firstOrNull()
                    if (selected != null && selected.isContainer) {
                        controller.addChildToContainer(
                            containerId = selected.id,
                            type = type
                        )
                    } else {
                        val count = controller.state.project.nodes.size
                        controller.addNode(
                            type = type,
                            position = CanvasPosition(
                                x = 40f + count * 24f,
                                y = 40f + count * 24f
                            )
                        )
                    }
                },
                modifier = Modifier.width(width = 240.dp)
            )
            Box(modifier = Modifier.weight(weight = 1f)) {
                CanvasScreen(controller = controller)
            }
            Column(modifier = Modifier.width(width = 280.dp)) {
                LayersPanel(
                    controller = controller,
                    modifier = Modifier.weight(weight = 1f)
                )
                HorizontalDivider()
                PropertiesPanel(
                    controller = controller,
                    modifier = Modifier.weight(weight = 1f)
                )
            }
        }
    }
}