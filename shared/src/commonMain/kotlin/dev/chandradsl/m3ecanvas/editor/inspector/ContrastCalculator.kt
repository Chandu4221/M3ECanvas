package dev.chandradsl.m3ecanvas.editor.inspector

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

enum class ContrastLevel(val label: String, val isPassing: Boolean) {
    AAA_PASS("AAA Pass", true),
    AA_PASS("AA Pass", true),
    AA_LARGE_ONLY("AA Large Only", true),
    FAIL("Low Contrast", false)
}

data class ContrastResult(
    val ratio: Float,
    val formattedRatio: String,
    val level: ContrastLevel
)

/**
 * Calculates WCAG 2.1 relative luminance and contrast ratio between foreground and background colors.
 */
object ContrastCalculator {

    /**
     * Converts an sRGB color component in [0, 1] to linear RGB space.
     */
    private fun srgbToLinear(c: Float): Float {
        return if (c <= 0.04045f) {
            c / 12.92f
        } else {
            ((c + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    /**
     * Computes the relative luminance of a color according to WCAG 2.1.
     * Returned value is in the range [0.0, 1.0], where 0 is pure black and 1 is pure white.
     */
    fun relativeLuminance(color: Color): Float {
        val r = srgbToLinear(color.red)
        val g = srgbToLinear(color.green)
        val b = srgbToLinear(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    /**
     * Computes the contrast ratio between two colors (ranging from 1.0 to 21.0).
     */
    fun contrastRatio(foreground: Color, background: Color): Float {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Evaluates contrast ratio compliance under WCAG 2.1.
     */
    fun evaluate(foreground: Color, background: Color): ContrastResult {
        val ratio = contrastRatio(foreground, background)
        val roundedRatio = (ratio * 10f).roundToInt() / 10f
        val formatted = "${roundedRatio}:1"

        val level = when {
            ratio >= 7.0f -> ContrastLevel.AAA_PASS
            ratio >= 4.5f -> ContrastLevel.AA_PASS
            ratio >= 3.0f -> ContrastLevel.AA_LARGE_ONLY
            else -> ContrastLevel.FAIL
        }

        return ContrastResult(
            ratio = ratio,
            formattedRatio = formatted,
            level = level
        )
    }

    /**
     * Helper to parse a hex string like "#RRGGBB" or "#AARRGGBB" to Compose Color.
     */
    fun parseColorHex(hex: String, fallback: Color = Color.Black): Color {
        val clean = hex.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> {
                    val rgb = clean.toLong(16)
                    Color(0xFF000000 or rgb)
                }
                8 -> {
                    val argb = clean.toLong(16)
                    Color(argb)
                }
                else -> fallback
            }
        } catch (_: Throwable) {
            fallback
        }
    }
}
