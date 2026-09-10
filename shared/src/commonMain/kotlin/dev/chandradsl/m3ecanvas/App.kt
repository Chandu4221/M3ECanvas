package dev.chandradsl.m3ecanvas

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.editor.canvas.CanvasScreen
import dev.chandradsl.m3ecanvas.editor.state.EditorController

@Composable
fun App() {
    val controller = remember {
        EditorController(initialProject = EditorController.newProject()).also {
            it.addNode(type = ComponentType.LAZY_COLUMN, position = CanvasPosition(x = 40f, y = 40f))
        }
    }

    LaunchedEffect(Unit) {
        controller.addNode(
            type = ComponentType.BUTTON,
            position = CanvasPosition(x = 100f, y = 100f)
        )
    }

    MaterialTheme {
        CanvasScreen(controller = controller)
    }
}