package dev.chandradsl.m3ecanvas.editor.codegen

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import dev.chandradsl.m3ecanvas.editor.theme.CanvasThemeGenerator

/**
 * A generated project file ready for export.
 */
data class ExportedProjectFile(
    val filename: String,
    val path: String,
    val language: String,
    val content: String
)

/**
 * Exports a full, multi-file, ready-to-build project structure from an M3EProject.
 */
object ProjectCodeExporter {

    fun exportProject(project: M3EProject): List<ExportedProjectFile> {
        val sanitizedName = project.name.filter { it.isLetterOrDigit() }.ifEmpty { "Generated" }
        val screenName = "${sanitizedName}Screen"

        val screenContent = ComposeCodeGenerator.generateFile(project)
        val themeContent = generateThemeFile(project)
        val mainActivityContent = generateMainActivity(screenName, sanitizedName)
        val mainDesktopContent = generateMainDesktop(project.name, screenName)
        val gradleContent = generateBuildGradle()

        return listOf(
            ExportedProjectFile(
                filename = "$screenName.kt",
                path = "src/commonMain/kotlin/$screenName.kt",
                language = "kotlin",
                content = screenContent
            ),
            ExportedProjectFile(
                filename = "Theme.kt",
                path = "src/commonMain/kotlin/Theme.kt",
                language = "kotlin",
                content = themeContent
            ),
            ExportedProjectFile(
                filename = "MainActivity.kt",
                path = "src/androidMain/kotlin/MainActivity.kt",
                language = "kotlin",
                content = mainActivityContent
            ),
            ExportedProjectFile(
                filename = "Main.kt",
                path = "src/jvmMain/kotlin/Main.kt",
                language = "kotlin",
                content = mainDesktopContent
            ),
            ExportedProjectFile(
                filename = "build.gradle.kts",
                path = "build.gradle.kts",
                language = "kotlin",
                content = gradleContent
            )
        )
    }

    private fun generateThemeFile(project: M3EProject): String {
        val theme = project.themeConfig
        val scheme = CanvasThemeGenerator.generateColorScheme(theme)

        fun toHex(c: androidx.compose.ui.graphics.Color): String {
            val argb = c.value.toLong()
            return "Color(0x%08X)".format(argb)
        }

        return """
package com.example.m3ecanvas.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Seed Color: ${theme.seedColorHex} (Default Mode: ${if (theme.isDark) "Dark" else "Light"})
private val LightColorScheme = lightColorScheme(
    primary = ${toHex(scheme.primary)},
    onPrimary = ${toHex(scheme.onPrimary)},
    primaryContainer = ${toHex(scheme.primaryContainer)},
    onPrimaryContainer = ${toHex(scheme.onPrimaryContainer)},
    secondary = ${toHex(scheme.secondary)},
    onSecondary = ${toHex(scheme.onSecondary)},
    secondaryContainer = ${toHex(scheme.secondaryContainer)},
    onSecondaryContainer = ${toHex(scheme.onSecondaryContainer)},
    tertiary = ${toHex(scheme.tertiary)},
    onTertiary = ${toHex(scheme.onTertiary)},
    tertiaryContainer = ${toHex(scheme.tertiaryContainer)},
    onTertiaryContainer = ${toHex(scheme.onTertiaryContainer)},
    background = ${toHex(scheme.background)},
    onBackground = ${toHex(scheme.onBackground)},
    surface = ${toHex(scheme.surface)},
    onSurface = ${toHex(scheme.onSurface)},
    surfaceVariant = ${toHex(scheme.surfaceVariant)},
    onSurfaceVariant = ${toHex(scheme.onSurfaceVariant)}
)

private val DarkColorScheme = darkColorScheme(
    primary = ${toHex(scheme.primary)},
    onPrimary = ${toHex(scheme.onPrimary)},
    primaryContainer = ${toHex(scheme.primaryContainer)},
    onPrimaryContainer = ${toHex(scheme.onPrimaryContainer)},
    secondary = ${toHex(scheme.secondary)},
    onSecondary = ${toHex(scheme.onSecondary)},
    secondaryContainer = ${toHex(scheme.secondaryContainer)},
    onSecondaryContainer = ${toHex(scheme.onSecondaryContainer)},
    tertiary = ${toHex(scheme.tertiary)},
    onTertiary = ${toHex(scheme.onTertiary)},
    tertiaryContainer = ${toHex(scheme.tertiaryContainer)},
    onTertiaryContainer = ${toHex(scheme.onTertiaryContainer)},
    background = ${toHex(scheme.background)},
    onBackground = ${toHex(scheme.onBackground)},
    surface = ${toHex(scheme.surface)},
    onSurface = ${toHex(scheme.onSurface)},
    surfaceVariant = ${toHex(scheme.surfaceVariant)},
    onSurfaceVariant = ${toHex(scheme.onSurfaceVariant)}
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
""".trimIndent()
    }

    private fun generateMainActivity(screenName: String, title: String): String {
        return """
package com.example.m3ecanvas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.m3ecanvas.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                $screenName()
            }
        }
    }
}
""".trimIndent()
    }

    private fun generateMainDesktop(appName: String, screenName: String): String {
        return """
package com.example.m3ecanvas

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.m3ecanvas.theme.AppTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "$appName"
    ) {
        AppTheme {
            $screenName()
        }
    }
}
""".trimIndent()
    }

    private fun generateBuildGradle(): String {
        return """
plugins {
    kotlin("multiplatform") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
        }
    }
}
""".trimIndent()
    }
}
