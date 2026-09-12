package dev.chandradsl.m3ecanvas.editor.ui

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * Returns the platform cursor used for horizontal resizing (<->).
 */
expect fun horizontalResizeCursor(): PointerIcon

/**
 * Returns the platform cursor used for vertical resizing (^v).
 */
expect fun verticalResizeCursor(): PointerIcon
