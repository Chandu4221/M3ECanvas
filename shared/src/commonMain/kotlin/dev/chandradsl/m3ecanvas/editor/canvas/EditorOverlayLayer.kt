package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.editor.state.AlignmentGuide
import dev.chandradsl.m3ecanvas.editor.state.EditorController
import dev.chandradsl.m3ecanvas.editor.state.GuideOrientation
import kotlin.math.max
import kotlin.math.min

/**
 * Decoupled Layer 2 Editor Overlay.
 * Renders selection boxes, resize handles, alignment guides, and marquee rectangles
 * reading coordinates directly from [LayoutGeometryStore].
 *
 * This layer does not alter component measurement, constraints, or intrinsic size.
 */
@Composable
fun EditorOverlayLayer(
    controller: EditorController,
    geometryStore: LayoutGeometryStore,
    alignmentGuides: List<AlignmentGuide>,
    marqueeRect: Rect?,
    onMarqueeChange: (Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = controller.state
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val focusRequester = remember { FocusRequester() }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Throwable) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val isCtrlOrCmd = event.isCtrlPressed || event.isMetaPressed
                    when {
                        event.key == Key.Delete || event.key == Key.Backspace -> {
                            if (state.selectedNodeIds.isNotEmpty()) {
                                controller.deleteSelected()
                                true
                            } else false
                        }
                        event.key == Key.Escape -> {
                            controller.clearSelection()
                            true
                        }
                        isCtrlOrCmd && event.key == Key.G && event.isShiftPressed -> {
                            controller.ungroupSelected()
                            true
                        }
                        isCtrlOrCmd && event.key == Key.G -> {
                            controller.groupSelected()
                            true
                        }
                        isCtrlOrCmd && event.key == Key.K -> {
                            controller.toggleCommandPalette()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .pointerInput(controller, geometryStore) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = false)
                    val isModifier = currentEvent.keyboardModifiers.isShiftPressed ||
                            currentEvent.keyboardModifiers.isCtrlPressed ||
                            currentEvent.keyboardModifiers.isMetaPressed

                    val hitNode = geometryStore.findNodeAt(down.position, state.project)

                    if (hitNode != null) {
                        // User pressed on a node
                        val isAlreadySelected = hitNode.id in state.selectedNodeIds
                        if (isModifier) {
                            controller.toggleNodeSelection(hitNode.id)
                        } else if (!isAlreadySelected) {
                            controller.selectNode(hitNode.id)
                        }

                        // Check if node is movable (floating root node)
                        val isFloating = !hitNode.isLockedInSlot &&
                                !hitNode.isLocked &&
                                hitNode.type != ComponentType.SCAFFOLD &&
                                !hitNode.type.isOverlay() &&
                                state.project.nodes.any { it.id == hitNode.id }

                        var dragStarted = false
                        var lastPosition = down.position

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                if (dragStarted) {
                                    controller.endDrag()
                                }
                                break
                            }
                            val changeDelta = change.positionChange()
                            if (changeDelta != Offset.Zero) {
                                change.consume()
                                if (isFloating) {
                                    if (!dragStarted) {
                                        controller.startDrag(hitNode.id)
                                        dragStarted = true
                                    }
                                    val deltaX = with(density) { (change.position.x - lastPosition.x).toDp().value }
                                    val deltaY = with(density) { (change.position.y - lastPosition.y).toDp().value }
                                    controller.updateDrag(deltaX, deltaY)
                                    lastPosition = change.position
                                }
                            }
                        }
                    } else {
                        // User pressed on empty canvas -> Marquee selection or clear
                        var start = down.position
                        var current = down.position
                        onMarqueeChange(Rect(start, current))

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                break
                            }
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                                current = change.position
                                val currentRect = Rect(
                                    left = min(start.x, current.x),
                                    top = min(start.y, current.y),
                                    right = max(start.x, current.x),
                                    bottom = max(start.y, current.y)
                                )
                                onMarqueeChange(currentRect)
                            }
                        }

                        val finalRect = Rect(
                            left = min(start.x, current.x),
                            top = min(start.y, current.y),
                            right = max(start.x, current.x),
                            bottom = max(start.y, current.y)
                        )

                        if (finalRect.width > 4f || finalRect.height > 4f) {
                            val hits = geometryStore.findNodesIn(finalRect, state.project)
                                .filter { it.type != ComponentType.SCAFFOLD && !it.type.isOverlay() && !it.isLocked }
                                .map { it.id }
                                .toSet()

                            if (isModifier) {
                                controller.selectNodes(state.selectedNodeIds + hits)
                            } else {
                                controller.selectNodes(hits)
                            }
                        } else {
                            // Click on empty space with no drag clears selection
                            controller.clearSelection()
                        }
                        onMarqueeChange(null)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val handleSize = 7.dp.toPx()
            val handleOffset = handleSize / 2f

            // 1. Draw Selection Outlines & Resize Handles
            for (selectedId in state.selectedNodeIds) {
                val bounds = geometryStore.getBounds(selectedId) ?: continue
                val rect = bounds.boundsInCanvas
                val node = state.project.findNode(selectedId)
                val isLocked = node?.isLocked == true

                // Selection border
                val borderColor = if (isLocked) Color(0xFFFF9800) else primaryColor
                drawRect(
                    color = borderColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = if (isLocked) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
                    )
                )

                if (isLocked) {
                    val lockText = "LOCKED"
                    val lockLayout = textMeasurer.measure(
                        text = lockText,
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    val badgeTop = Offset(rect.right - lockLayout.size.width - 10f, rect.top + 4f)
                    drawRect(
                        color = Color(0xFFFF9800),
                        topLeft = Offset(badgeTop.x - 4f, badgeTop.y - 2f),
                        size = Size(lockLayout.size.width + 8f, lockLayout.size.height + 4f)
                    )
                    drawText(textLayoutResult = lockLayout, topLeft = badgeTop)
                }

                // If single floating node is selected and not locked, draw corner resize handles
                val isSingle = state.selectedNodeIds.size == 1
                val isFloating = node != null && !node.isLockedInSlot && !isLocked &&
                        node.type != ComponentType.SCAFFOLD &&
                        !node.type.isOverlay() &&
                        state.project.nodes.any { it.id == node.id }

                if (isSingle && isFloating) {
                    val corners = listOf(
                        rect.topLeft,
                        Offset(rect.right, rect.top),
                        Offset(rect.left, rect.bottom),
                        rect.bottomRight
                    )
                    for (corner in corners) {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(corner.x - handleOffset, corner.y - handleOffset),
                            size = Size(handleSize, handleSize)
                        )
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(corner.x - handleOffset, corner.y - handleOffset),
                            size = Size(handleSize, handleSize),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            // 2. Draw Marquee Rectangle
            if (marqueeRect != null && (marqueeRect.width > 2f || marqueeRect.height > 2f)) {
                drawRect(
                    color = primaryColor.copy(alpha = 0.12f),
                    topLeft = marqueeRect.topLeft,
                    size = marqueeRect.size
                )
                drawRect(
                    color = primaryColor,
                    topLeft = marqueeRect.topLeft,
                    size = marqueeRect.size,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                )
            }

            // 3. Draw Magnetic Alignment Guides
            for (guide in alignmentGuides) {
                val guidePx = guide.position.dp.toPx()
                if (guide.orientation == GuideOrientation.VERTICAL) {
                    drawLine(
                        color = tertiaryColor,
                        start = Offset(guidePx, 0f),
                        end = Offset(guidePx, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                } else {
                    drawLine(
                        color = tertiaryColor,
                        start = Offset(0f, guidePx),
                        end = Offset(size.width, guidePx),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }
            }

            // 4. Draw Distance & Spacing Measurements when a single node is selected
            if (state.showMeasurements && state.selectedNodeIds.size == 1) {
                val selectedId = state.selectedNodeIds.first()
                val selectedBounds = geometryStore.getBounds(selectedId)
                if (selectedBounds != null) {
                    val rect = selectedBounds.boundsInCanvas
                    val measureColor = Color(0xFFE91E63)
                    val isNearTop = rect.top > 8f
                    val isNearLeft = rect.left > 8f

                    // Top Distance to Canvas / Container Edge
                    if (isNearTop) {
                        val distanceDp = with(density) { rect.top.toDp().value }
                        drawLine(
                            color = measureColor,
                            start = Offset(rect.center.x, 0f),
                            end = Offset(rect.center.x, rect.top),
                            strokeWidth = 1.dp.toPx()
                        )
                        val text = "${distanceDp.toInt()} dp"
                        val layout = textMeasurer.measure(
                            text = text,
                            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        val badgeTop = Offset(rect.center.x - layout.size.width / 2f, rect.top / 2f - layout.size.height / 2f)
                        drawRect(
                            color = measureColor,
                            topLeft = Offset(badgeTop.x - 3f, badgeTop.y - 2f),
                            size = Size(layout.size.width + 6f, layout.size.height + 4f)
                        )
                        drawText(textLayoutResult = layout, topLeft = badgeTop)
                    }

                    // Left Distance to Canvas / Container Edge
                    if (isNearLeft) {
                        val distanceDp = with(density) { rect.left.toDp().value }
                        drawLine(
                            color = measureColor,
                            start = Offset(0f, rect.center.y),
                            end = Offset(rect.left, rect.center.y),
                            strokeWidth = 1.dp.toPx()
                        )
                        val text = "${distanceDp.toInt()} dp"
                        val layout = textMeasurer.measure(
                            text = text,
                            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        val badgeLeft = Offset(rect.left / 2f - layout.size.width / 2f, rect.center.y - layout.size.height / 2f)
                        drawRect(
                            color = measureColor,
                            topLeft = Offset(badgeLeft.x - 3f, badgeLeft.y - 2f),
                            size = Size(layout.size.width + 6f, layout.size.height + 4f)
                        )
                        drawText(textLayoutResult = layout, topLeft = badgeLeft)
                    }
                }
            }
        }
    }
}
