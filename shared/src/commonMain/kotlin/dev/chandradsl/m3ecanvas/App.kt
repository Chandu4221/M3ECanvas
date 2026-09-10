package dev.chandradsl.m3ecanvas

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val step = if (event.isShiftPressed) 10f else 1f
                    when (event.key) {
                        Key.Delete -> {
                            controller.deleteSelected()
                            true
                        }
                        Key.Escape -> {
                            controller.clearSelection()
                            true
                        }
                        Key.DirectionUp -> {
                            controller.nudgeSelected(dx = 0f, dy = -step)
                            true
                        }
                        Key.DirectionDown -> {
                            controller.nudgeSelected(dx = 0f, dy = step)
                            true
                        }
                        Key.DirectionLeft -> {
                            controller.nudgeSelected(dx = -step, dy = 0f)
                            true
                        }
                        Key.DirectionRight -> {
                            controller.nudgeSelected(dx = step, dy = 0f)
                            true
                        }
                        else -> false
                    }
                }
        ) {
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