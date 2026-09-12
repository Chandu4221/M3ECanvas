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

import dev.chandradsl.m3ecanvas.editor.persistence.LoadResult
import dev.chandradsl.m3ecanvas.editor.persistence.AutosaveManager
import dev.chandradsl.m3ecanvas.editor.persistence.FileProjectRepository
import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import dev.chandradsl.m3ecanvas.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun App(repository: ProjectRepository) {
    val controller = remember {
        EditorController(
            initialProject = EditorController.newProject()
        )
    }
    val autosaveManager = remember { AutosaveManager.default }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var statusMessage by remember { mutableStateOf(value = "") }
    var showCodeExport by remember { mutableStateOf(value = false) }
    var isOperating by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(value = null) }
    var recoveryProject by remember { mutableStateOf<M3EProject?>(value = null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        isOperating = true
        when (val result = repository.load()) {
            is LoadResult.Success -> {
                controller.replaceProject(project = result.project)
                statusMessage = "Loaded saved project"
            }
            is LoadResult.NotFound -> {
                if (autosaveManager.hasAutosave()) {
                    when (val auto = autosaveManager.loadAutosave()) {
                        is LoadResult.Success -> {
                            recoveryProject = auto.project
                        }
                        else -> {}
                    }
                }
            }
            is LoadResult.Corrupted -> {
                errorMessage = "Failed to load project: ${result.reason}"
                statusMessage = "Project file corrupted"
            }
        }
        isOperating = false
    }

    LaunchedEffect(controller.state.project) {
        kotlinx.coroutines.delay(4000)
        try {
            autosaveManager.saveAutosave(controller.state.project)
        } catch (_: Throwable) {}
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
                            event.key == Key.A -> {
                                controller.selectAll()
                                val count = controller.state.selectedNodeIds.size
                                statusMessage = if (count > 0) "Selected all ($count components)" else "No components to select"
                                return@onKeyEvent true
                            }
                            event.key == Key.C -> {
                                val copied = controller.copySelected()
                                if (copied != null) {
                                    try {
                                        val json = M3EJson.encodeToString(copied)
                                        clipboardManager.setText(AnnotatedString(json))
                                        statusMessage = "Copied ${copied.name}"
                                    } catch (e: Exception) {
                                        AppLogger.error("Clipboard", "Failed to serialize node '${copied.name}' to JSON", e)
                                        statusMessage = "Failed to copy to clipboard"
                                    }
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
                                    } catch (e: Exception) {
                                        AppLogger.warn("Clipboard", "Failed to parse system clipboard as CanvasNode: ${e.message}", e)
                                        null
                                    }
                                    if (text != null && parsed == null) {
                                        statusMessage = "Clipboard does not contain a valid M3E component"
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
                            event.key == Key.Equals || event.key == Key.Plus -> {
                                controller.zoomIn()
                                statusMessage = "Zoom: ${(controller.state.viewport.zoom * 100).toInt()}%"
                                return@onKeyEvent true
                            }
                            event.key == Key.Minus -> {
                                controller.zoomOut()
                                statusMessage = "Zoom: ${(controller.state.viewport.zoom * 100).toInt()}%"
                                return@onKeyEvent true
                            }
                            event.key == Key.Zero -> {
                                controller.fitToScreen()
                                statusMessage = "Fit to Screen"
                                return@onKeyEvent true
                            }
                            event.key == Key.One -> {
                                controller.resetZoom()
                                statusMessage = "Zoom: 100%"
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
                    coroutineScope.launch {
                        isOperating = true
                        statusMessage = "Saving..."
                        try {
                            repository.save(project = controller.state.project)
                            autosaveManager.clearAutosave()
                            try {
                                autosaveManager.addRecentProject(
                                    name = controller.state.project.name,
                                    path = FileProjectRepository.defaultFile().absolutePath
                                )
                            } catch (_: Throwable) {}
                            statusMessage = "Saved"
                        } catch (e: Throwable) {
                            statusMessage = "Save failed"
                            errorMessage = "Failed to save project: ${e.message}"
                        } finally {
                            isOperating = false
                        }
                    }
                },
                onLoad = {
                    coroutineScope.launch {
                        isOperating = true
                        statusMessage = "Loading..."
                        when (val result = repository.load()) {
                            is LoadResult.Success -> {
                                controller.replaceProject(project = result.project)
                                statusMessage = "Loaded"
                            }
                            is LoadResult.NotFound -> {
                                statusMessage = "No saved project found"
                            }
                            is LoadResult.Corrupted -> {
                                errorMessage = "Failed to load project: ${result.reason}"
                                statusMessage = "Project file corrupted"
                            }
                        }
                        isOperating = false
                    }
                },
                isOperating = isOperating,
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

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                title = { Text(text = "Project Error") },
                text = { Text(text = errorMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        if (recoveryProject != null) {
            AlertDialog(
                onDismissRequest = { recoveryProject = null },
                title = { Text(text = "Recover Unsaved Work") },
                text = { Text(text = "An unsaved session was found from a previous edit (${recoveryProject?.name ?: "Untitled"}). Would you like to restore it?") },
                confirmButton = {
                    TextButton(onClick = {
                        val toRestore = recoveryProject
                        recoveryProject = null
                        if (toRestore != null) {
                            controller.replaceProject(toRestore)
                            statusMessage = "Restored unsaved session"
                        }
                    }) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        recoveryProject = null
                        coroutineScope.launch {
                            autosaveManager.clearAutosave()
                        }
                    }) {
                        Text("Discard")
                    }
                }
            )
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
    isOperating: Boolean = false,
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
        if (isOperating) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
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
        TextButton(onClick = onLoad, enabled = !isOperating) {
            Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = "Load")
            Text(text = "Load", modifier = Modifier.padding(start = 6.dp))
        }
        TextButton(onClick = onSave, enabled = !isOperating) {
            Icon(imageVector = Icons.Outlined.Save, contentDescription = "Save")
            Text(text = "Save", modifier = Modifier.padding(start = 6.dp))
        }
    }
}