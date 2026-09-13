@file:OptIn(ExperimentalUuidApi::class)

package dev.chandradsl.m3ecanvas.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Configuration for a single text style in the document typography scale.
 */
@Serializable
data class TextStyleConfig(
    val fontFamily: String = "Default",
    val fontSizeSp: Float? = null,
    val fontWeight: String? = null,
    val letterSpacingSp: Float? = null,
    val lineHeightSp: Float? = null
) {
    companion object {
        val FONT_FAMILIES = listOf("Default", "SansSerif", "Serif", "Monospace", "Cursive")
        val FONT_WEIGHTS = listOf("Thin", "Light", "Normal", "Medium", "SemiBold", "Bold", "ExtraBold", "Black")
    }
}

/**
 * Full Material 3 typography scale configuration for the document.
 * Matches all 15 official M3 typography roles.
 */
@Serializable
data class DocumentTypographyConfig(
    val displayLarge: TextStyleConfig? = null,
    val displayMedium: TextStyleConfig? = null,
    val displaySmall: TextStyleConfig? = null,
    val headlineLarge: TextStyleConfig? = null,
    val headlineMedium: TextStyleConfig? = null,
    val headlineSmall: TextStyleConfig? = null,
    val titleLarge: TextStyleConfig? = null,
    val titleMedium: TextStyleConfig? = null,
    val titleSmall: TextStyleConfig? = null,
    val bodyLarge: TextStyleConfig? = null,
    val bodyMedium: TextStyleConfig? = null,
    val bodySmall: TextStyleConfig? = null,
    val labelLarge: TextStyleConfig? = null,
    val labelMedium: TextStyleConfig? = null,
    val labelSmall: TextStyleConfig? = null
)

/**
 * Material 3 shapes configuration for the document.
 * Defines corner radii for the 5 M3 shape scales.
 */
@Serializable
data class DocumentShapesConfig(
    val extraSmallCornerDp: Float = 4f,
    val smallCornerDp: Float = 8f,
    val mediumCornerDp: Float = 12f,
    val largeCornerDp: Float = 16f,
    val extraLargeCornerDp: Float = 28f
)

/**
 * Category of design token.
 */
@Serializable
enum class DesignTokenType(val displayName: String) {
    COLOR("Color"),
    SPACING("Spacing"),
    DIMENSION("Dimension"),
    SHAPE("Shape"),
    ELEVATION("Elevation")
}

/**
 * A reusable design token defined within the document.
 */
@Serializable
data class DesignToken(
    val id: String = Uuid.random().toString(),
    val name: String,
    val type: DesignTokenType,
    val value: String
)
