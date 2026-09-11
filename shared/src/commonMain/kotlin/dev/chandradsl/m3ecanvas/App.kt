package dev.chandradsl.m3ecanvas

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.SlotRole
import dev.chandradsl.m3ecanvas.editor.canvas.CanvasScreen
import dev.chandradsl.m3ecanvas.editor.inspector.LayersPanel
import dev.chandradsl.m3ecanvas.editor.inspector.PropertiesPanel
import dev.chandradsl.m3ecanvas.editor.palette.ComponentPalette
import dev.chandradsl.m3ecanvas.editor.persistence.ProjectRepository
import dev.chandradsl.m3ecanvas.editor.state.EditorController

@Composable
fun App(repository: ProjectRepository) {
    val controller = remember {
        EditorController(
            initialProject = repository.load() ?: EditorController.newProject()
        )
    }
    val focusRequester = remember { FocusRequester() }
    var statusMessage by remember { mutableStateOf(value = "") }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
        Column(
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
            TopBar(
                onSave = {
                    repository.save(project = controller.state.project)
                    statusMessage = "Saved"
                },
                onLoad = {
                    val loaded = repository.load()
                    if (loaded != null) {
                        controller.replaceProject(project = loaded)
                        statusMessage = "Loaded"
                    } else {
                        statusMessage = "No saved project found"
                    }
                },
                statusMessage = statusMessage
            )
            HorizontalDivider()
            Row(modifier = Modifier.weight(weight = 1f)) {
                ComponentPalette(
                    onAddComponent = { type ->
                        val selected = controller.state.selectedNodes.firstOrNull()
                        if (selected != null && selected.isContainer) {
                            controller.addChildToContainer(
                                containerId = selected.id,
                                type = type
                            )
                        } else {
                            val rootScaffold = controller.state.project.nodes.firstOrNull { it.type == ComponentType.SCAFFOLD }
                            if (rootScaffold != null && type != ComponentType.SCAFFOLD) {
                                val contentContainer = rootScaffold.children.firstOrNull {
                                    it.slot == SlotRole.CONTENT && it.isContainer
                                }
                                val targetContainerId = if (type.canonicalSlot() == SlotRole.CONTENT && contentContainer != null) {
                                    contentContainer.id
                                } else {
                                    rootScaffold.id
                                }
                                controller.addChildToContainer(
                                    containerId = targetContainerId,
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
}

@Composable
private fun TopBar(
    onSave: () -> Unit,
    onLoad: () -> Unit,
    statusMessage: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "M3E Canvas",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.weight(weight = 1f))
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        TextButton(onClick = onLoad) {
            Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = "Load")
            Text(text = "Load", modifier = Modifier.padding(start = 6.dp))
        }
        TextButton(onClick = onSave) {
            Icon(imageVector = Icons.Outlined.Save, contentDescription = "Save")
            Text(text = "Save", modifier = Modifier.padding(start = 6.dp))
        }
    }
}