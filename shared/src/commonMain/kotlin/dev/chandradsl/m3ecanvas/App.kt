package dev.chandradsl.m3ecanvas

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.SlotRole
import dev.chandradsl.m3ecanvas.editor.canvas.CanvasScreen
import dev.chandradsl.m3ecanvas.editor.codegen.CodeExportPanel
import dev.chandradsl.m3ecanvas.editor.inspector.LayersPanel
import dev.chandradsl.m3ecanvas.editor.inspector.PropertiesPanel
import dev.chandradsl.m3ecanvas.editor.palette.ComponentPalette
import dev.chandradsl.m3ecanvas.editor.persistence.M3EJson
import dev.chandradsl.m3ecanvas.editor.persistence.ProjectRepository
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.theme.EditorTheme

@Composable
fun App(repository: ProjectRepository) {
    val controller = remember {
        EditorController(
            initialProject = repository.load() ?: EditorController.newProject()
        )
    }
    val focusRequester = remember { FocusRequester() }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var statusMessage by remember { mutableStateOf(value = "") }
    var showCodeExport by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    EditorTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val isCtrlOrCmd = event.isCtrlPressed || event.isMetaPressed
                    if (isCtrlOrCmd) {
                        when {
                            (event.key == Key.Z && event.isShiftPressed) || event.key == Key.Y -> {
                                controller.redo()
                                statusMessage = "Redo"
                                return@onKeyEvent true
                            }
                            event.key == Key.Z -> {
                                controller.undo()
                                statusMessage = "Undo"
                                return@onKeyEvent true
                            }
                            event.key == Key.C -> {
                                val copied = controller.copySelected()
                                if (copied != null) {
                                    try {
                                        val json = M3EJson.encodeToString(copied)
                                        clipboardManager.setText(AnnotatedString(json))
                                    } catch (_: Exception) {}
                                    statusMessage = "Copied ${copied.name}"
                                }
                                return@onKeyEvent true
                            }
                            event.key == Key.V -> {
                                val pasted = if (controller.state.clipboard != null) {
                                    controller.paste()
                                } else {
                                    val text = clipboardManager.getText()?.text
                                    val parsed = try {
                                        if (text != null) M3EJson.decodeFromString<CanvasNode>(text) else null
                                    } catch (_: Exception) {
                                        null
                                    }
                                    controller.paste(sourceNode = parsed)
                                }
                                if (pasted != null) {
                                    statusMessage = "Pasted ${pasted.name}"
                                }
                                return@onKeyEvent true
                            }
                            event.key == Key.D -> {
                                val duplicated = controller.duplicateSelected()
                                if (duplicated != null) {
                                    statusMessage = "Duplicated ${duplicated.name}"
                                }
                                return@onKeyEvent true
                            }
                        }
                    }

                    val step = if (event.isShiftPressed) 10f else 1f
                    when (event.key) {
                        Key.Delete, Key.Backspace -> {
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
                canUndo = controller.state.canUndo,
                canRedo = controller.state.canRedo,
                hasSelection = controller.state.selectedNodeIds.isNotEmpty(),
                onUndo = {
                    controller.undo()
                    statusMessage = "Undo"
                },
                onRedo = {
                    controller.redo()
                    statusMessage = "Redo"
                },
                onDelete = {
                    controller.deleteSelected()
                    statusMessage = "Deleted"
                },
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
                statusMessage = statusMessage,
                showCodeExport = showCodeExport,
                onToggleCodeExport = { showCodeExport = !showCodeExport }
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
                if (showCodeExport) {
                    VerticalDivider()
                    Box(modifier = Modifier.width(440.dp)) {
                        CodeExportPanel(
                            project = controller.state.project,
                            onClose = { showCodeExport = false }
                        )
                    }
                }
                VerticalDivider()
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
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    statusMessage: String,
    showCodeExport: Boolean,
    onToggleCodeExport: () -> Unit
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
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = onUndo,
            enabled = canUndo
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Undo,
                contentDescription = "Undo (Ctrl+Z)"
            )
        }
        IconButton(
            onClick = onRedo,
            enabled = canRedo
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Redo,
                contentDescription = "Redo (Ctrl+Shift+Z)"
            )
        }
        IconButton(
            onClick = onDelete,
            enabled = hasSelection
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete Selected (Del / Backspace)"
            )
        }
        Spacer(modifier = Modifier.weight(weight = 1f))
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        FilledTonalButton(
            onClick = onToggleCodeExport,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (showCodeExport) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(imageVector = Icons.Outlined.Code, contentDescription = "Export Code & AI")
            Text(
                text = if (showCodeExport) "Hide Code" else "Export Code & AI",
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
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