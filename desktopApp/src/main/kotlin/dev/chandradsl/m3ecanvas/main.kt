package dev.chandradsl.m3ecanvas

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "M3E Canvas",
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        App()
    }
}