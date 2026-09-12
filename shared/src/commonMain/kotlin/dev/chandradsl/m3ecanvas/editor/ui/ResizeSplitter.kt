package dev.chandradsl.m3ecanvas.editor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Vertical splitter bar placed between horizontally adjacent panels (e.g. Palette and Canvas).
 *
 * Provides:
 * - 8dp hit area with centered visual divider line
 * - Native `<->` horizontal resize mouse cursor
 * - Hover & active drag highlighting with [MaterialTheme.colorScheme.primary]
 * - Horizontal drag delta reporting via [onResize] (in Dp units)
 * - Optional double-tap reset via [onResetToDefault]
 */
@Composable
fun VerticalResizeSplitter(
    onResize: (deltaDp: Float) -> Unit,
    modifier: Modifier = Modifier,
    onResetToDefault: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val isActive = isHovered || isDragging
    val dividerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }
    )
    val dividerWidth by animateDpAsState(
        targetValue = if (isActive) 2.dp else 1.dp
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(8.dp)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .pointerHoverIcon(horizontalResizeCursor())
            .pointerInput(onResetToDefault) {
                if (onResetToDefault != null) {
                    detectTapGestures(
                        onDoubleTap = { onResetToDefault() }
                    )
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { _, dragAmount ->
                        val deltaDp = with(density) { dragAmount.toDp() }
                        onResize(deltaDp.value)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .width(dividerWidth)
                .fillMaxHeight()
                .background(dividerColor)
        )
    }
}

/**
 * Horizontal splitter bar placed between vertically stacked panels (e.g. Layers and Properties).
 *
 * Provides:
 * - 8dp hit area with centered visual divider line
 * - Native `^v` vertical resize mouse cursor
 * - Hover & active drag highlighting with [MaterialTheme.colorScheme.primary]
 * - Vertical drag delta reporting via [onResize] (in Dp units)
 * - Optional double-tap reset via [onResetToDefault]
 */
@Composable
fun HorizontalResizeSplitter(
    onResize: (deltaDp: Float) -> Unit,
    modifier: Modifier = Modifier,
    onResetToDefault: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val isActive = isHovered || isDragging
    val dividerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }
    )
    val dividerHeight by animateDpAsState(
        targetValue = if (isActive) 2.dp else 1.dp
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(8.dp)
            .fillMaxWidth()
            .hoverable(interactionSource)
            .pointerHoverIcon(verticalResizeCursor())
            .pointerInput(onResetToDefault) {
                if (onResetToDefault != null) {
                    detectTapGestures(
                        onDoubleTap = { onResetToDefault() }
                    )
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { _, dragAmount ->
                        val deltaDp = with(density) { dragAmount.toDp() }
                        onResize(deltaDp.value)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .height(dividerHeight)
                .fillMaxWidth()
                .background(dividerColor)
        )
    }
}
