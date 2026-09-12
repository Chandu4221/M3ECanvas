package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.chandradsl.m3ecanvas.domain.model.DeviceCategory
import dev.chandradsl.m3ecanvas.domain.model.DeviceProfile
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.CanvasPosition
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.theme.CanvasThemeGenerator
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CanvasScreen(controller: EditorController) {
    val state = controller.state
    val deviceProfile = state.project.deviceProfile
    val viewport = state.viewport

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val availableWidth = maxWidth.value
        val availableHeight = maxHeight.value

        LaunchedEffect(availableWidth, availableHeight) {
            controller.updateCanvasViewportSize(availableWidth, availableHeight)
        }

        // Auto-fit to screen on device profile change if it exceeds available bounds
        LaunchedEffect(deviceProfile) {
            val deviceWidth = deviceProfile.size.width + 48f
            val deviceHeight = deviceProfile.size.height + 96f
            if (deviceWidth > availableWidth || deviceHeight > availableHeight) {
                controller.fitToScreen(availableWidth, availableHeight)
            }
        }

        val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        val backgroundColor = Color(0xFFE2E4E8)

        // Outer Interactive Canvas Viewport (Background + Pan Drag + Scroll Zoom/Pan)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(color = backgroundColor)
                    val dotSpacing = 24.dp.toPx()
                    val dotRadius = 1.25.dp.toPx()
                    val offsetX = (viewport.panOffset.x % dotSpacing + dotSpacing) % dotSpacing
                    val offsetY = (viewport.panOffset.y % dotSpacing + dotSpacing) % dotSpacing

                    var x = offsetX
                    while (x < size.width) {
                        var y = offsetY
                        while (y < size.height) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        controller.pan(dragAmount.x, dragAmount.y)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.firstOrNull() ?: continue
                                val scrollDelta = change.scrollDelta
                                val isCtrl = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                                if (isCtrl) {
                                    if (scrollDelta.y < 0f) {
                                        controller.zoomIn(0.10f)
                                    } else if (scrollDelta.y > 0f) {
                                        controller.zoomOut(0.10f)
                                    }
                                } else {
                                    controller.pan(-scrollDelta.x * 24f, -scrollDelta.y * 24f)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Transformed Device Chassis Column (Scaled and Translated via graphicsLayer)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = viewport.zoom
                        scaleY = viewport.zoom
                        translationX = viewport.panOffset.x
                        translationY = viewport.panOffset.y
                    }
                    .padding(16.dp)
            ) {
                // Device Information Header Badge (Click to select Project & display Project properties)
                DeviceHeaderBadge(
                    currentProfile = deviceProfile,
                    isSelected = state.selectedNodeIds.isEmpty(),
                    onClick = { controller.clearSelection() }
                )

                // Outer Device Hardware Frame with Bezel & Drop Shadow
                Surface(
                    modifier = Modifier
                        .size(
                            width = (deviceProfile.size.width + 16f).dp,
                            height = (deviceProfile.size.height + 16f).dp
                        )
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(36.dp),
                            spotColor = Color.Black.copy(alpha = 0.35f)
                        ),
                    shape = RoundedCornerShape(36.dp),
                    color = Color(0xFF1C1D22),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF383A42))
                ) {
                    val canvasColorScheme = remember(state.project.themeConfig) {
                        CanvasThemeGenerator.generateColorScheme(state.project.themeConfig)
                    }

                    // Inner Screen Display
                    MaterialTheme(
                        colorScheme = canvasColorScheme,
                        typography = Typography()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(all = 8.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .focusable()
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Simulated Android Status Bar
                                SimulatedStatusBar()

                                // Screen Content Area with Marquee Drag Selection
                                val density = LocalDensity.current
                                var marqueeStart by remember { mutableStateOf<Offset?>(null) }
                                var marqueeCurrent by remember { mutableStateOf<Offset?>(null) }
                                var isModifierActive by remember { mutableStateOf(false) }

                                val marqueeRect = remember(marqueeStart, marqueeCurrent) {
                                    val s = marqueeStart
                                    val c = marqueeCurrent
                                    if (s != null && c != null) {
                                        Rect(
                                            left = min(s.x, c.x),
                                            top = min(s.y, c.y),
                                            right = max(s.x, c.x),
                                            bottom = max(s.y, c.y)
                                        )
                                    } else null
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                                val isModifier = currentEvent.keyboardModifiers.isShiftPressed ||
                                                        currentEvent.keyboardModifiers.isCtrlPressed ||
                                                        currentEvent.keyboardModifiers.isMetaPressed
                                                var start = down.position
                                                var current = down.position
                                                marqueeStart = start
                                                marqueeCurrent = current

                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                    if (change.changedToUp()) {
                                                        change.consume()
                                                        break
                                                    }
                                                    if (change.positionChange() != Offset.Zero) {
                                                        change.consume()
                                                        current = change.position
                                                        marqueeCurrent = current
                                                    }
                                                }

                                                val finalStart = marqueeStart
                                                val finalCurrent = marqueeCurrent
                                                if (finalStart != null && finalCurrent != null) {
                                                    val rect = Rect(
                                                        left = min(finalStart.x, finalCurrent.x),
                                                        top = min(finalStart.y, finalCurrent.y),
                                                        right = max(finalStart.x, finalCurrent.x),
                                                        bottom = max(finalStart.y, finalCurrent.y)
                                                    )
                                                    if (rect.width > 5f || rect.height > 5f) {
                                                        val hits = state.project.nodes.filter { node ->
                                                            if (node.type == ComponentType.SCAFFOLD) return@filter false
                                                            val leftPx = with(density) { node.position.x.dp.toPx() }
                                                            val topPx = with(density) { node.position.y.dp.toPx() }
                                                            val widthPx = with(density) { node.size.width.dp.toPx() }
                                                            val heightPx = with(density) { node.size.height.dp.toPx() }
                                                            val nodeRect = Rect(leftPx, topPx, leftPx + widthPx, topPx + heightPx)
                                                            !(rect.right < nodeRect.left || rect.left > nodeRect.right ||
                                                                    rect.bottom < nodeRect.top || rect.top > nodeRect.bottom)
                                                        }.map { it.id }.toSet()

                                                        if (isModifier) {
                                                            controller.selectNodes(state.selectedNodeIds + hits)
                                                        } else {
                                                            controller.selectNodes(hits)
                                                        }
                                                    } else {
                                                        controller.clearSelection()
                                                    }
                                                }
                                                marqueeStart = null
                                                marqueeCurrent = null
                                            }
                                        }
                                        .drawBehind {
                                            val rect = marqueeRect
                                            if (rect != null && (rect.width > 2f || rect.height > 2f)) {
                                                drawRect(
                                                    color = canvasColorScheme.primary.copy(alpha = 0.15f),
                                                    topLeft = rect.topLeft,
                                                    size = rect.size
                                                )
                                                drawRect(
                                                    color = canvasColorScheme.primary,
                                                    topLeft = rect.topLeft,
                                                    size = rect.size,
                                                    style = Stroke(
                                                        width = 1.5.dp.toPx(),
                                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                                    )
                                                )
                                            }
                                        }
                                ) {
                                    state.project.nodes.forEach { node ->
                                        CanvasNodePlacement(node = node, controller = controller)
                                    }
                                }

                                // Simulated Android Gesture Navigation Bar
                                SimulatedNavigationBar()
                            }
                        }
                    }
                }
            }
        }

        // Floating Viewport Controls Toolbar
        ViewportControlBar(
            zoom = viewport.zoom,
            panOffset = viewport.panOffset,
            onZoomIn = { controller.zoomIn() },
            onZoomOut = { controller.zoomOut() },
            onResetZoom = { controller.resetZoom() },
            onFitToScreen = { controller.fitToScreen(availableWidth, availableHeight) },
            onResetPan = { controller.resetPan() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}

@Composable
private fun ViewportControlBar(
    zoom: Float,
    panOffset: CanvasPosition,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onFitToScreen: () -> Unit,
    onResetPan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Zoom Out
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Current Zoom Level (Click to Reset to 100%)
            TextButton(
                onClick = onResetZoom,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${(zoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Zoom In
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(18.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp)
            )

            // Fit to Screen
            IconButton(
                onClick = onFitToScreen,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitScreen,
                    contentDescription = "Fit to Screen",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Reset Pan button (visible when canvas is panned away from center)
            if (panOffset.x != 0f || panOffset.y != 0f) {
                IconButton(
                    onClick = onResetPan,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = "Center Viewport",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceHeaderBadge(
    currentProfile: DeviceProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getDeviceCategoryIcon(currentProfile.category),
                contentDescription = "Project Settings",
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${currentProfile.displayName} • ${currentProfile.size.width.toInt()} × ${currentProfile.size.height.toInt()} dp",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

private fun getDeviceCategoryIcon(category: DeviceCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        DeviceCategory.PHONE -> Icons.Outlined.Smartphone
        DeviceCategory.FOLDABLE -> Icons.Outlined.DevicesFold
        DeviceCategory.TABLET -> Icons.Outlined.Tablet
        DeviceCategory.DESKTOP -> Icons.Outlined.Computer
        DeviceCategory.CUSTOM -> Icons.Outlined.AspectRatio
    }
}

@Composable
private fun SimulatedStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clock
        Text(
            text = "09:41",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Center Camera Punch-hole
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color(0xFF0F0F12), shape = CircleShape)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Status Icons (Cellular, Wifi, Battery)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.SignalCellular4Bar,
                contentDescription = "Signal",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Outlined.Wifi,
                contentDescription = "Wi-Fi",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Outlined.BatteryFull,
                contentDescription = "Battery",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimulatedNavigationBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

@Composable
private fun CanvasNodePlacement(node: CanvasNode, controller: EditorController) {
    val isSelected = controller.state.isNodeSelected(nodeId = node.id)
    val isScaffold = node.type == ComponentType.SCAFFOLD
    val isLocked = node.isLockedInSlot || isScaffold
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            try {
                focusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    val baseModifier = if (isScaffold) {
        Modifier.fillMaxSize()
    } else {
        var mod = Modifier.offset(x = node.position.x.dp, y = node.position.y.dp)
        mod = if (node.hasFillMaxWidth()) mod.fillMaxWidth() else mod.width(node.size.width.dp)
        mod = if (node.hasFillMaxHeight()) mod.fillMaxHeight() else mod.height(node.size.height.dp)
        mod
    }

    Box(
        modifier = baseModifier
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = if (isScaffold) RoundedCornerShape(36.dp) else RoundedCornerShape(4.dp)
            )
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (isSelected && event.type == KeyEventType.KeyDown && (event.key == Key.Delete || event.key == Key.Backspace)) {
                    controller.deleteSelected()
                    true
                } else {
                    false
                }
            }
            .selectOnPress(nodeId = node.id, controller = controller, focusRequester = focusRequester)
            .then(
                if (!isLocked) {
                    Modifier.pointerInput(node.id) {
                        detectDragGestures(
                            onDragStart = {
                                controller.startDrag(nodeId = node.id)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                controller.updateDrag(
                                    deltaX = dragAmount.x,
                                    deltaY = dragAmount.y
                                )
                            },
                            onDragEnd = { controller.endDrag() },
                            onDragCancel = { controller.endDrag() }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        CanvasNodeRenderer(node = node, controller = controller)
    }
}