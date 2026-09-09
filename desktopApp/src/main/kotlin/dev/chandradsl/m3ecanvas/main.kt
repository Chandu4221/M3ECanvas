package dev.chandradsl.m3ecanvas

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "M3ECanvas",
    ) {
        App()
    }
}