package dev.chandradsl.m3ecanvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.editor.canvas.CanvasScreen
import dev.chandradsl.m3ecanvas.editor.inspector.LayersPanel
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
                    val count = controller.state.project.nodes.size
                    controller.addNode(
                        type = type,
                        position = CanvasPosition(
                            x = 40f + count * 24f,
                            y = 40f + count * 24f
                        )
                    )
                },
                modifier = Modifier.width(240.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                CanvasScreen(controller = controller)
            }
            LayersPanel(
                controller = controller,
                modifier = Modifier.width(240.dp)
            )
        }
    }
}