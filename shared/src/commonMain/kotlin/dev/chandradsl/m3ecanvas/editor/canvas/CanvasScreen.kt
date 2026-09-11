package dev.chandradsl.m3ecanvas.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.CanvasNode
import dev.chandradsl.m3ecanvas.domain.model.ComponentType
import dev.chandradsl.m3ecanvas.editor.state.EditorController

@Composable
fun CanvasScreen(controller: EditorController) {
    val state = controller.state
    val deviceProfile = state.project.deviceProfile

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E4E8)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Device Information Header Badge
            DeviceHeaderBadge(
                displayName = deviceProfile.displayName,
                sizeWidth = deviceProfile.size.width,
                sizeHeight = deviceProfile.size.height
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
                // Inner Screen Display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 8.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .focusable()
                ) {
                    MaterialTheme(
                        typography = Typography()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Simulated Android Status Bar
                            SimulatedStatusBar()

                            // Screen Content Area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
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
}

@Composable
private fun DeviceHeaderBadge(displayName: String, sizeWidth: Float, sizeHeight: Float) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$displayName • ${sizeWidth.toInt()} × ${sizeHeight.toInt()} dp",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
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
        Modifier
            .offset(x = node.position.x.dp, y = node.position.y.dp)
            .size(width = node.size.width.dp, height = node.size.height.dp)
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