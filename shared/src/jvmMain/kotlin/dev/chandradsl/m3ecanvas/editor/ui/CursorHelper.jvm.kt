package dev.chandradsl.m3ecanvas.editor.ui

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

actual fun horizontalResizeCursor(): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))

actual fun verticalResizeCursor(): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR))
