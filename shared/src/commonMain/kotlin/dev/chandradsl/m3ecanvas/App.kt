package dev.chandradsl.m3ecanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // This Box represents our main design canvas
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)),
                contentAlignment = Alignment.Center
            ) {
                // Phase 1: We will build our draggable component engine here next.
            }
        }
    }
}