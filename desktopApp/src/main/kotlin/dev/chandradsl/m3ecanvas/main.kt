package dev.chandradsl.m3ecanvas

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.chandradsl.m3ecanvas.editor.persistence.FileProjectRepository


fun main() = application {
    val repository = FileProjectRepository()
    Window(onCloseRequest = ::exitApplication, title = "M3E Canvas") {
        App(repository = repository)
    }
}