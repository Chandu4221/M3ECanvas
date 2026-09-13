package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val RULER_THICKNESS = 20.dp

/**
 * Renders horizontal and vertical canvas rulers showing dp coordinates
 * tracking viewport pan and zoom.
 */
@Composable
fun CanvasRulers(
    zoom: Float,
    panOffset: Offset,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Horizontal Ruler (Top)
        CanvasHorizontalRuler(
            zoom = zoom,
            panX = panOffset.x,
            modifier = Modifier
                .fillMaxWidth()
                .height(RULER_THICKNESS)
                .padding(start = RULER_THICKNESS)
        )

        // Vertical Ruler (Left)
        CanvasVerticalRuler(
            zoom = zoom,
            panY = panOffset.y,
            modifier = Modifier
                .fillMaxHeight()
                .width(RULER_THICKNESS)
                .padding(top = RULER_THICKNESS)
        )

        // Corner Box (0, 0 origin indicator)
        Box(
            modifier = Modifier
                .size(RULER_THICKNESS)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "dp",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CanvasHorizontalRuler(
    zoom: Float,
    panX: Float,
    modifier: Modifier = Modifier
) {
    val rulerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.background(rulerBg)) {
        val width = size.width
        val height = size.height

        // Bottom border line
        drawLine(
            color = tickColor,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1f
        )

        // Determine step based on zoom level so labels do not collide
        val step = when {
            zoom < 0.5f -> 200
            zoom < 1.0f -> 100
            zoom < 2.0f -> 50
            else -> 20
        }

        // Calculate visible start and end in canvas dp coordinates
        val minDp = ((-panX) / zoom).toInt() - 100
        val maxDp = ((-panX + width) / zoom).toInt() + 100
        val startDp = (minDp / step) * step

        var currentDp = startDp
        while (currentDp <= maxDp) {
            val screenX = panX + currentDp * zoom
            if (screenX in 0f..width) {
                val isMajor = currentDp % (step * 2) == 0
                val tickHeight = if (isMajor) height * 0.45f else height * 0.25f

                drawLine(
                    color = tickColor,
                    start = Offset(screenX, height - tickHeight),
                    end = Offset(screenX, height),
                    strokeWidth = 1f
                )

                if (isMajor) {
                    val label = currentDp.toString()
                    val textLayout = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 8.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(screenX + 2f, 2f)
                    )
                }
            }
            currentDp += step
        }
    }
}

@Composable
fun CanvasVerticalRuler(
    zoom: Float,
    panY: Float,
    modifier: Modifier = Modifier
) {
    val rulerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.background(rulerBg)) {
        val width = size.width
        val height = size.height

        // Right border line
        drawLine(
            color = tickColor,
            start = Offset(width, 0f),
            end = Offset(width, height),
            strokeWidth = 1f
        )

        val step = when {
            zoom < 0.5f -> 200
            zoom < 1.0f -> 100
            zoom < 2.0f -> 50
            else -> 20
        }

        val minDp = ((-panY) / zoom).toInt() - 100
        val maxDp = ((-panY + height) / zoom).toInt() + 100
        val startDp = (minDp / step) * step

        var currentDp = startDp
        while (currentDp <= maxDp) {
            val screenY = panY + currentDp * zoom
            if (screenY in 0f..height) {
                val isMajor = currentDp % (step * 2) == 0
                val tickWidth = if (isMajor) width * 0.45f else width * 0.25f

                drawLine(
                    color = tickColor,
                    start = Offset(width - tickWidth, screenY),
                    end = Offset(width, screenY),
                    strokeWidth = 1f
                )

                if (isMajor) {
                    val label = currentDp.toString()
                    val textLayout = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 8.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(2f, screenY + 2f)
                    )
                }
            }
            currentDp += step
        }
    }
}
