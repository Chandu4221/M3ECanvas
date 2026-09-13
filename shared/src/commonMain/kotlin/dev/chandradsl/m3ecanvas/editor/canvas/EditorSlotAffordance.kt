package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual insertion affordance and empty-state for component slots in the editor.
 *
 * Rather than inventing hardcoded demo content (e.g. fake text, fake icons, fake tabs),
 * this composable clearly indicates to the designer that a slot is currently unoccupied
 * and ready to accept content.
 */
@Composable
fun EditorSlotAffordance(
    label: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 32.dp,
    minWidth: Dp = 48.dp,
    cornerRadius: Dp = 6.dp,
    outlineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f),
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val stroke = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
                drawRoundRect(
                    color = outlineColor,
                    style = stroke,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1
        )
    }
}
