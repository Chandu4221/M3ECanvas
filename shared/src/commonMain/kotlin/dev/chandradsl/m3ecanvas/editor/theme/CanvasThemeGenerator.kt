package dev.chandradsl.m3ecanvas.editor.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.chandradsl.m3ecanvas.domain.model.CanvasThemeConfig
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CanvasThemePreset(
    val name: String,
    val hexColor: String,
    val color: Color
)

/**
 * Generator and manager for Canvas Material 3 themes.
 * Dynamically derives high-fidelity, contrast-compliant ColorScheme instances
 * from any seed color or preset.
 */
object CanvasThemeGenerator {

    val PRESETS = listOf(
        CanvasThemePreset("Purple", "#6750A4", Color(0xFF6750A4)),
        CanvasThemePreset("Indigo", "#3F51B5", Color(0xFF3F51B5)),
        CanvasThemePreset("Blue", "#0061A4", Color(0xFF0061A4)),
        CanvasThemePreset("Teal", "#006A60", Color(0xFF006A60)),
        CanvasThemePreset("Green", "#386A20", Color(0xFF386A20)),
        CanvasThemePreset("Orange", "#9C4330", Color(0xFF9C4330)),
        CanvasThemePreset("Pink", "#984061", Color(0xFF984061)),
        CanvasThemePreset("Monochrome", "#5E5E62", Color(0xFF5E5E62))
    )

    fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF6750A4)): Color {
        val clean = hex.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> Color((0xFF000000 or clean.toLong(16)))
                8 -> Color(clean.toLong(16))
                3 -> {
                    val r = clean.substring(0, 1).repeat(2)
                    val g = clean.substring(1, 2).repeat(2)
                    val b = clean.substring(2, 3).repeat(2)
                    Color((0xFF000000 or "$r$g$b".toLong(16)))
                }
                else -> defaultColor
            }
        } catch (_: Throwable) {
            defaultColor
        }
    }

    fun isValidHexColor(hex: String): Boolean {
        val clean = hex.trim().removePrefix("#")
        return (clean.length == 6 || clean.length == 8 || clean.length == 3) &&
                clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Converts RGB values (0f..1f) to HSL [hue (0..360), saturation (0..1), lightness (0..1)].
     */
    fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val maxVal = max(r, max(g, b))
        val minVal = min(r, min(g, b))
        var h = 0f
        var s: Float
        val l = (maxVal + minVal) / 2f

        if (maxVal == minVal) {
            h = 0f
            s = 0f
        } else {
            val d = maxVal - minVal
            s = if (l > 0.5f) d / (2f - maxVal - minVal) else d / (maxVal + minVal)
            when (maxVal) {
                r -> h = (g - b) / d + (if (g < b) 6f else 0f)
                g -> h = (b - r) / d + 2f
                b -> h = (r - g) / d + 4f
            }
            h *= 60f
        }
        return floatArrayOf(h, s, l)
    }

    /**
     * Converts HSL [hue (0..360), saturation (0..1), lightness (0..1)] to [Color].
     */
    fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        var r = 0f
        var g = 0f
        var b = 0f

        when {
            h < 60f -> { r = c; g = x; b = 0f }
            h < 120f -> { r = x; g = c; b = 0f }
            h < 180f -> { r = 0f; g = c; b = x }
            h < 240f -> { r = 0f; g = x; b = c }
            h < 300f -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }

        return Color(
            red = (r + m).coerceIn(0f, 1f),
            green = (g + m).coerceIn(0f, 1f),
            blue = (b + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    /**
     * Generates a complete, authentic Material 3 [ColorScheme] matching the given [CanvasThemeConfig].
     */
    fun generateColorScheme(config: CanvasThemeConfig): ColorScheme {
        val isDefaultPurple = config.seedColorHex.trim().equals("#6750A4", ignoreCase = true)
        if (isDefaultPurple) {
            return if (config.isDark) darkColorScheme() else lightColorScheme()
        }

        val seedColor = parseHexColor(config.seedColorHex)
        val hsl = rgbToHsl(seedColor.red, seedColor.green, seedColor.blue)
        val h = hsl[0]
        val s = hsl[1].coerceIn(0.15f, 0.95f)

        val tertiaryHue = (h + 60f) % 360f

        return if (!config.isDark) {
            // Light ColorScheme
            lightColorScheme(
                primary = hslToColor(h, s, 0.40f),
                onPrimary = Color.White,
                primaryContainer = hslToColor(h, s * 0.7f, 0.90f),
                onPrimaryContainer = hslToColor(h, s, 0.15f),
                inversePrimary = hslToColor(h, s, 0.80f),

                secondary = hslToColor(h, s * 0.35f, 0.40f),
                onSecondary = Color.White,
                secondaryContainer = hslToColor(h, s * 0.35f, 0.90f),
                onSecondaryContainer = hslToColor(h, s * 0.40f, 0.15f),

                tertiary = hslToColor(tertiaryHue, s * 0.50f, 0.40f),
                onTertiary = Color.White,
                tertiaryContainer = hslToColor(tertiaryHue, s * 0.50f, 0.90f),
                onTertiaryContainer = hslToColor(tertiaryHue, s * 0.50f, 0.15f),

                background = hslToColor(h, 0.04f, 0.98f),
                onBackground = hslToColor(h, 0.04f, 0.10f),
                surface = hslToColor(h, 0.04f, 0.98f),
                onSurface = hslToColor(h, 0.04f, 0.10f),
                surfaceVariant = hslToColor(h, 0.08f, 0.90f),
                onSurfaceVariant = hslToColor(h, 0.08f, 0.30f),
                surfaceTint = hslToColor(h, s, 0.40f),

                surfaceContainerLowest = Color.White,
                surfaceContainerLow = hslToColor(h, 0.04f, 0.96f),
                surfaceContainer = hslToColor(h, 0.05f, 0.94f),
                surfaceContainerHigh = hslToColor(h, 0.06f, 0.92f),
                surfaceContainerHighest = hslToColor(h, 0.07f, 0.90f),

                outline = hslToColor(h, 0.08f, 0.50f),
                outlineVariant = hslToColor(h, 0.08f, 0.80f),
                inverseSurface = hslToColor(h, 0.04f, 0.20f),
                inverseOnSurface = hslToColor(h, 0.04f, 0.95f)
            )
        } else {
            // Dark ColorScheme
            darkColorScheme(
                primary = hslToColor(h, s, 0.80f),
                onPrimary = hslToColor(h, s, 0.20f),
                primaryContainer = hslToColor(h, s * 0.8f, 0.30f),
                onPrimaryContainer = hslToColor(h, s * 0.8f, 0.90f),
                inversePrimary = hslToColor(h, s, 0.40f),

                secondary = hslToColor(h, s * 0.35f, 0.80f),
                onSecondary = hslToColor(h, s * 0.35f, 0.20f),
                secondaryContainer = hslToColor(h, s * 0.35f, 0.30f),
                onSecondaryContainer = hslToColor(h, s * 0.35f, 0.90f),

                tertiary = hslToColor(tertiaryHue, s * 0.50f, 0.80f),
                onTertiary = hslToColor(tertiaryHue, s * 0.50f, 0.20f),
                tertiaryContainer = hslToColor(tertiaryHue, s * 0.50f, 0.30f),
                onTertiaryContainer = hslToColor(tertiaryHue, s * 0.50f, 0.90f),

                background = hslToColor(h, 0.04f, 0.08f),
                onBackground = hslToColor(h, 0.04f, 0.90f),
                surface = hslToColor(h, 0.04f, 0.08f),
                onSurface = hslToColor(h, 0.04f, 0.90f),
                surfaceVariant = hslToColor(h, 0.08f, 0.25f),
                onSurfaceVariant = hslToColor(h, 0.08f, 0.80f),
                surfaceTint = hslToColor(h, s, 0.80f),

                surfaceContainerLowest = hslToColor(h, 0.04f, 0.05f),
                surfaceContainerLow = hslToColor(h, 0.04f, 0.10f),
                surfaceContainer = hslToColor(h, 0.05f, 0.14f),
                surfaceContainerHigh = hslToColor(h, 0.06f, 0.18f),
                surfaceContainerHighest = hslToColor(h, 0.07f, 0.22f),

                outline = hslToColor(h, 0.08f, 0.60f),
                outlineVariant = hslToColor(h, 0.08f, 0.30f),
                inverseSurface = hslToColor(h, 0.04f, 0.90f),
                inverseOnSurface = hslToColor(h, 0.04f, 0.20f)
            )
        }
    }
}
