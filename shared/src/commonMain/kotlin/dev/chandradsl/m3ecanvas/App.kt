package dev.chandradsl.m3ecanvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import dev.chandradsl.m3ecanvas.editor.state.EditorMode
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.domain.model.SlotRole
import dev.chandradsl.m3ecanvas.editor.canvas.CanvasScreen
import dev.chandradsl.m3ecanvas.editor.codegen.CodeExportPanel
import dev.chandradsl.m3ecanvas.editor.history.HistoryDialog
import dev.chandradsl.m3ecanvas.editor.inspector.LayersPanel
import dev.chandradsl.m3ecanvas.editor.inspector.PropertiesPanel
import dev.chandradsl.m3ecanvas.editor.palette.CommandPalette
import dev.chandradsl.m3ecanvas.editor.palette.ComponentPalette
import dev.chandradsl.m3ecanvas.editor.ui.HorizontalResizeSplitter
import dev.chandradsl.m3ecanvas.editor.ui.VerticalResizeSplitter
import dev.chandradsl.m3ecanvas.editor.persistence.M3EJson
import dev.chandradsl.m3ecanvas.editor.persistence.ProjectRepository
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.theme.EditorTheme

import dev.chandradsl.m3ecanvas.editor.persistence.LoadResult
import dev.chandradsl.m3ecanvas.editor.persistence.AutosaveManager
import dev.chandradsl.m3ecanvas.editor.persistence.getDefaultProjectLocation
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
    var showPalette by remember { mutableStateOf(value = true) }
    var showInspector by remember { mutableStateOf(value = true) }
    var layersExpanded by remember { mutableStateOf(value = true) }
    var propertiesExpanded by remember { mutableStateOf(value = true) }
    var showHistoryDialog by remember { mutableStateOf(value = false) }
    var paletteWidth by remember { mutableStateOf(260.dp) }
    var inspectorWidth by remember { mutableStateOf(280.dp) }
    var codeExportWidth by remember { mutableStateOf(440.dp) }
    var layersHeightRatio by remember { mutableStateOf(0.5f) }
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
                            event.key == Key.LeftBracket -> {
                                showPalette = !showPalette
                                statusMessage = if (showPalette) "Palette shown" else "Palette hidden"
                                return@onKeyEvent true
                            }
                            event.key == Key.RightBracket -> {
                                showInspector = !showInspector
                                statusMessage = if (showInspector) "Inspector shown" else "Inspector hidden"
                                return@onKeyEvent true
                            }
                            event.key == Key.H -> {
                                showHistoryDialog = !showHistoryDialog
                                return@onKeyEvent true
                            }
                            event.key == Key.E -> {
                                showCodeExport = !showCodeExport
                                return@onKeyEvent true
                            }
                            event.key == Key.P -> {
                                controller.toggleInteractiveMode()
                                statusMessage = if (controller.state.isInteractiveMode) "Interactive Preview Mode" else "Design Mode"
                                return@onKeyEvent true
                            }
                            event.key == Key.K -> {
                                controller.toggleCommandPalette()
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
                            if (controller.state.isInteractiveMode) {
                                controller.setMode(EditorMode.DESIGN)
                                statusMessage = "Design Mode"
                            } else {
                                controller.clearSelection()
                            }
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
                undoCount = controller.state.undoCount,
                redoCount = controller.state.redoCount,
                hasSelection = controller.state.selectedNodeIds.isNotEmpty(),
                onUndo = {
                    controller.undo()
                    statusMessage = "Undo"
                },
                onRedo = {
                    controller.redo()
                    statusMessage = "Redo"
                },
                onOpenHistory = {
                    showHistoryDialog = true
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
                                    path = repository.storageLocation.ifEmpty { getDefaultProjectLocation() }
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
                showPalette = showPalette,
                onTogglePalette = { showPalette = !showPalette },
                showInspector = showInspector,
                onToggleInspector = { showInspector = !showInspector },
                showCodeExport = showCodeExport,
                onToggleCodeExport = { showCodeExport = !showCodeExport },
                isInteractiveMode = controller.state.isInteractiveMode,
                onToggleInteractiveMode = {
                    controller.toggleInteractiveMode()
                    statusMessage = if (controller.state.isInteractiveMode) "Interactive Preview Mode" else "Design Mode"
                },
                onOpenCommandPalette = { controller.openCommandPalette() }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(modifier = Modifier.weight(weight = 1f)) {
                // Collapsible & Resizable Palette
                AnimatedVisibility(
                    visible = showPalette,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        ComponentPalette(
                            onAddComponent = { type ->
                                val selected = controller.state.selectedNodes.firstOrNull()
                                val targetContainer = controller.findValidTargetContainer(
                                    forType = type,
                                    startingFromNodeId = selected?.id
                                )

                                if (targetContainer != null) {
                                    val added = controller.addChildToContainer(
                                        containerId = targetContainer.id,
                                        type = type
                                    )
                                    if (added) {
                                        statusMessage = "Added ${type.displayName} into ${targetContainer.name}"
                                    } else {
                                        statusMessage = "Cannot add ${type.displayName} to ${targetContainer.name}"
                                    }
                                } else {
                                    val hasScaffold = controller.state.project.nodes.any { it.type == ComponentType.SCAFFOLD }
                                    if (type.isAllowedAtRoot(hasScaffold)) {
                                        val count = controller.state.project.nodes.size
                                        controller.addNode(
                                            type = type,
                                            position = CanvasPosition(
                                                x = 40f + count * 24f,
                                                y = 40f + count * 24f
                                            )
                                        )
                                        statusMessage = "Added ${type.displayName} to canvas"
                                    } else {
                                        statusMessage = if (type == ComponentType.SCAFFOLD && hasScaffold) {
                                            "Project already contains a Scaffold"
                                        } else {
                                            "Cannot place ${type.displayName} here"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.width(paletteWidth),
                            onCollapse = { showPalette = false },
                            favoriteTypes = controller.state.favoriteComponentTypes,
                            onToggleFavorite = { controller.toggleFavoriteComponent(it) },
                            recentTypes = controller.state.recentComponentTypes
                        )
                        VerticalResizeSplitter(
                            onResize = { deltaDp ->
                                val newWidth = paletteWidth + deltaDp.dp
                                if (newWidth < 120.dp) {
                                    showPalette = false
                                    statusMessage = "Palette collapsed"
                                } else {
                                    paletteWidth = newWidth.coerceIn(160.dp, 480.dp)
                                }
                            },
                            onResetToDefault = {
                                paletteWidth = 240.dp
                                statusMessage = "Palette reset to 240dp"
                            }
                        )
                    }
                }

                // Left Slim Expand Handle when Palette is collapsed
                if (!showPalette) {
                    Surface(
                        onClick = { showPalette = true },
                        modifier = Modifier.fillMaxHeight().width(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Expand Palette",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    VerticalDivider()
                }

                Box(modifier = Modifier.weight(weight = 1f)) {
                    CanvasScreen(controller = controller)
                }

                if (showCodeExport) {
                    VerticalResizeSplitter(
                        onResize = { deltaDp ->
                            val newWidth = codeExportWidth - deltaDp.dp
                            codeExportWidth = newWidth.coerceIn(320.dp, 760.dp)
                        },
                        onResetToDefault = {
                            codeExportWidth = 440.dp
                            statusMessage = "Code export reset to 440dp"
                        }
                    )
                    Box(modifier = Modifier.width(codeExportWidth)) {
                        CodeExportPanel(
                            project = controller.state.project,
                            onClose = { showCodeExport = false }
                        )
                    }
                }

                // Right Slim Expand Handle when Inspector is collapsed
                if (!showInspector) {
                    VerticalDivider()
                    Surface(
                        onClick = { showInspector = true },
                        modifier = Modifier.fillMaxHeight().width(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription = "Expand Inspector",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Collapsible & Resizable Inspector (Layers + Properties)
                AnimatedVisibility(
                    visible = showInspector,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        VerticalResizeSplitter(
                            onResize = { deltaDp ->
                                val newWidth = inspectorWidth - deltaDp.dp
                                if (newWidth < 150.dp) {
                                    showInspector = false
                                    statusMessage = "Inspector collapsed"
                                } else {
                                    inspectorWidth = newWidth.coerceIn(220.dp, 600.dp)
                                }
                            },
                            onResetToDefault = {
                                inspectorWidth = 280.dp
                                statusMessage = "Inspector reset to 280dp"
                            }
                        )
                        Column(modifier = Modifier.width(inspectorWidth).fillMaxHeight()) {
                            // Inspector Top Header Bar with collapse icon
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Inspector",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { showInspector = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = "Collapse Inspector",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()

                            // Layers and Properties collapsible & resizable sections
                            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                val totalHeightDp = maxHeight.value.coerceAtLeast(200f)
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (layersExpanded && propertiesExpanded) {
                                        LayersPanel(
                                            controller = controller,
                                            modifier = Modifier.weight(layersHeightRatio),
                                            isExpanded = true,
                                            onToggleExpand = { layersExpanded = false }
                                        )
                                        HorizontalResizeSplitter(
                                            onResize = { deltaDp ->
                                                val deltaRatio = deltaDp / totalHeightDp
                                                layersHeightRatio = (layersHeightRatio + deltaRatio).coerceIn(0.15f, 0.85f)
                                            },
                                            onResetToDefault = {
                                                layersHeightRatio = 0.5f
                                                statusMessage = "Layers / Properties reset to 50/50"
                                            }
                                        )
                                        PropertiesPanel(
                                            controller = controller,
                                            modifier = Modifier.weight(1f - layersHeightRatio),
                                            isExpanded = true,
                                            onToggleExpand = { propertiesExpanded = false }
                                        )
                                    } else if (layersExpanded && !propertiesExpanded) {
                                        LayersPanel(
                                            controller = controller,
                                            modifier = Modifier.weight(1f),
                                            isExpanded = true,
                                            onToggleExpand = { layersExpanded = false }
                                        )
                                        HorizontalDivider()
                                        PropertiesPanel(
                                            controller = controller,
                                            modifier = Modifier.wrapContentHeight(),
                                            isExpanded = false,
                                            onToggleExpand = { propertiesExpanded = true }
                                        )
                                    } else if (!layersExpanded && propertiesExpanded) {
                                        LayersPanel(
                                            controller = controller,
                                            modifier = Modifier.wrapContentHeight(),
                                            isExpanded = false,
                                            onToggleExpand = { layersExpanded = true }
                                        )
                                        HorizontalDivider()
                                        PropertiesPanel(
                                            controller = controller,
                                            modifier = Modifier.weight(1f),
                                            isExpanded = true,
                                            onToggleExpand = { propertiesExpanded = false }
                                        )
                                    } else {
                                        LayersPanel(
                                            controller = controller,
                                            modifier = Modifier.wrapContentHeight(),
                                            isExpanded = false,
                                            onToggleExpand = { layersExpanded = true }
                                        )
                                        HorizontalDivider()
                                        PropertiesPanel(
                                            controller = controller,
                                            modifier = Modifier.wrapContentHeight(),
                                            isExpanded = false,
                                            onToggleExpand = { propertiesExpanded = true }
                                        )
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showHistoryDialog) {
            HistoryDialog(
                controller = controller,
                onDismissRequest = { showHistoryDialog = false }
            )
        }

        if (controller.state.isCommandPaletteOpen) {
            CommandPalette(
                controller = controller,
                onDismissRequest = { controller.closeCommandPalette() }
            )
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
    undoCount: Int = 0,
    redoCount: Int = 0,
    hasSelection: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenHistory: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    isOperating: Boolean = false,
    statusMessage: String,
    showPalette: Boolean,
    onTogglePalette: () -> Unit,
    showInspector: Boolean,
    onToggleInspector: () -> Unit,
    showCodeExport: Boolean,
    onToggleCodeExport: () -> Unit,
    isInteractiveMode: Boolean = false,
    onToggleInteractiveMode: () -> Unit = {},
    onOpenCommandPalette: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group 1: Floating Brand Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onTogglePalette,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ViewSidebar,
                            contentDescription = if (showPalette) "Hide Palette (Ctrl+[)" else "Show Palette (Ctrl+[)",
                            tint = if (showPalette) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "M",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    Text(
                        text = "M3E Canvas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Group 2: History & Edit Operations Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo && !isInteractiveMode,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Undo (Ctrl+Z)",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo && !isInteractiveMode,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = "Redo (Ctrl+Shift+Z)",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenHistory,
                        enabled = !isInteractiveMode,
                        modifier = Modifier.size(28.dp)
                    ) {
                        val total = undoCount + redoCount
                        if (total > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Text("$undoCount", fontSize = 8.sp)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "History Timeline (Ctrl+H)",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "History Timeline (Ctrl+H)",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = hasSelection && !isInteractiveMode,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Selected (Del)",
                            tint = if (hasSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Group 3: Design | Preview Floating Segmented Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Design Mode Tab
                    Surface(
                        onClick = { if (isInteractiveMode) onToggleInteractiveMode() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (!isInteractiveMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (!isInteractiveMode) 1.dp else 0.dp,
                        border = if (!isInteractiveMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (!isInteractiveMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Design",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (!isInteractiveMode) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (!isInteractiveMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Preview Mode Tab
                    Surface(
                        onClick = { if (!isInteractiveMode) onToggleInteractiveMode() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isInteractiveMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shadowElevation = if (isInteractiveMode) 1.dp else 0.dp,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isInteractiveMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isInteractiveMode) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isInteractiveMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Group 4: Quick Command Jump Pill (Ctrl+K)
            Surface(
                onClick = onOpenCommandPalette,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search / Commands (Ctrl+K)",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Jump to...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Ctrl+K",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isOperating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Group 5: Primary Action "Export Code & AI" Pill
            Button(
                onClick = onToggleCodeExport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showCodeExport) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showCodeExport) "Hide Code" else "Export Code & AI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Group 6: Secondary Load & Save Actions Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onLoad,
                        enabled = !isOperating,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Load",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load", style = MaterialTheme.typography.labelSmall)
                    }

                    VerticalDivider(
                        modifier = Modifier.height(16.dp).padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    TextButton(
                        onClick = onSave,
                        enabled = !isOperating,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Group 7: Inspector Toggle Button Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (showInspector) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(
                    onClick = onToggleInspector,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = if (showInspector) "Hide Inspector (Ctrl+])" else "Show Inspector (Ctrl+])",
                        tint = if (showInspector) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}