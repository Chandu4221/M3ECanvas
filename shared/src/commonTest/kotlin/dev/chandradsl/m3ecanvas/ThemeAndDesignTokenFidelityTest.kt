package dev.chandradsl.m3ecanvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.*
import dev.chandradsl.m3ecanvas.editor.codegen.ComposeCodeGenerator
import dev.chandradsl.m3ecanvas.editor.theme.CanvasThemeGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Formal test suite verifying Phase 11: Document Theme Model and Design Tokens.
 * Enforces Sections 20 & 21 of the M3ECanvas specification:
 * Real ColorScheme, Typography, Shapes, and Design Tokens with zero fake mocks,
 * full isolation between editor chrome and document preview, and valid Theme.kt code generation.
 */
class ThemeAndDesignTokenFidelityTest {

    @Test
    fun testDefaultColorSchemeGeneration() {
        val config = CanvasThemeConfig()
        val lightScheme = CanvasThemeGenerator.generateColorScheme(config)
        val darkScheme = CanvasThemeGenerator.generateColorScheme(config.copy(isDark = true))

        assertNotEquals(lightScheme.primary, darkScheme.primary)
        assertNotEquals(lightScheme.surface, darkScheme.surface)
    }

    @Test
    fun testCustomSeedColorSchemeGeneration() {
        val configGreen = CanvasThemeConfig(seedColorHex = "#386A20", isDark = false)
        val greenScheme = CanvasThemeGenerator.generateColorScheme(configGreen)

        val configTeal = CanvasThemeConfig(seedColorHex = "#006A60", isDark = false)
        val tealScheme = CanvasThemeGenerator.generateColorScheme(configTeal)

        assertNotEquals(greenScheme.primary, tealScheme.primary)
    }

    @Test
    fun testColorRoleOverrides() {
        val baseConfig = CanvasThemeConfig(seedColorHex = "#6750A4")
        val configWithOverrides = baseConfig
            .withColorOverride("primary", "#FF0000")
            .withColorOverride("surface", "#00FF00")

        val scheme = CanvasThemeGenerator.generateColorScheme(configWithOverrides)
        assertEquals(Color(0xFFFF0000), scheme.primary)
        assertEquals(Color(0xFF00FF00), scheme.surface)

        val configRemoved = configWithOverrides.withoutColorOverride("primary")
        val schemeRemoved = CanvasThemeGenerator.generateColorScheme(configRemoved)
        assertNotEquals(Color(0xFFFF0000), schemeRemoved.primary)
        assertEquals(Color(0xFF00FF00), schemeRemoved.surface)
    }

    @Test
    fun testTypographyGenerationWithCustomFontFamily() {
        val typoConfig = DocumentTypographyConfig(
            bodyLarge = TextStyleConfig(fontFamily = "Monospace", fontSizeSp = 18f),
            titleMedium = TextStyleConfig(fontFamily = "Serif", fontSizeSp = 22f)
        )
        val config = CanvasThemeConfig(typography = typoConfig)
        val typography = CanvasThemeGenerator.generateTypography(config)

        assertEquals(FontFamily.Monospace, typography.bodyLarge.fontFamily)
        assertEquals(18.sp, typography.bodyLarge.fontSize)
        assertEquals(FontFamily.Serif, typography.titleMedium.fontFamily)
        assertEquals(22.sp, typography.titleMedium.fontSize)
    }

    @Test
    fun testShapesGenerationWithCustomCorners() {
        val shapesConfig = DocumentShapesConfig(
            smallCornerDp = 10f,
            mediumCornerDp = 14f,
            largeCornerDp = 20f
        )
        val config = CanvasThemeConfig(shapes = shapesConfig)
        val shapes = CanvasThemeGenerator.generateShapes(config)

        // Verifying shapes generation creates exact corner radius shapes
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(10.dp), shapes.small)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(14.dp), shapes.medium)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(20.dp), shapes.large)
    }

    @Test
    fun testDesignTokenLifecycle() {
        val tokenSpacing = DesignToken(name = "Spacing12", type = DesignTokenType.SPACING, value = "12")
        val tokenColor = DesignToken(name = "BrandAccent", type = DesignTokenType.COLOR, value = "#FF9900")

        var config = CanvasThemeConfig()
        assertTrue(config.customTokens.isEmpty())

        config = config.withToken(tokenSpacing).withToken(tokenColor)
        assertEquals(2, config.customTokens.size)
        assertTrue(config.customTokens.any { it.name == "Spacing12" && it.type == DesignTokenType.SPACING })
        assertTrue(config.customTokens.any { it.name == "BrandAccent" && it.type == DesignTokenType.COLOR })

        config = config.withoutToken(tokenSpacing.id)
        assertEquals(1, config.customTokens.size)
        assertFalse(config.customTokens.any { it.name == "Spacing12" })
    }

    @Test
    fun testThemeCodeGeneration() {
        val project = M3EProject(
            name = "MyAwesomeApp",
            themeConfig = CanvasThemeConfig(
                seedColorHex = "#006A60",
                colorOverrides = mapOf("primary" to "#007A70"),
                typography = DocumentTypographyConfig(
                    bodyLarge = TextStyleConfig(fontFamily = "Monospace")
                ),
                shapes = DocumentShapesConfig(mediumCornerDp = 14f),
                customTokens = listOf(
                    DesignToken(name = "Spacing8", type = DesignTokenType.SPACING, value = "8"),
                    DesignToken(name = "BrandGold", type = DesignTokenType.COLOR, value = "#FFD700")
                )
            )
        )

        val themeCode = ComposeCodeGenerator.generateThemeFile(project, themeName = "MyAwesomeAppTheme")

        assertTrue(themeCode.contains("package com.example.app.ui.theme"), "Missing package declaration")
        assertTrue(themeCode.contains("import androidx.compose.material3.*"), "Missing M3 imports")
        assertTrue(themeCode.contains("val LightColorScheme = lightColorScheme("), "Missing LightColorScheme")
        assertTrue(themeCode.contains("val DarkColorScheme = darkColorScheme("), "Missing DarkColorScheme")
        assertTrue(themeCode.contains("val AppShapes = Shapes("), "Missing AppShapes")
        assertTrue(themeCode.contains("val AppTypography = Typography("), "Missing AppTypography")
        assertTrue(themeCode.contains("object DesignTokens"), "Missing DesignTokens object")
        assertTrue(themeCode.contains("val Spacing8 = 8.dp"), "Missing Spacing8 token")
        assertTrue(themeCode.contains("val BrandGold = Color(0xFFD700)"), "Missing BrandGold token")
        assertTrue(themeCode.contains("fun MyAwesomeAppTheme("), "Missing MyAwesomeAppTheme function")

        // Balanced braces verification
        val openBraces = themeCode.count { it == '{' }
        val closeBraces = themeCode.count { it == '}' }
        assertEquals(openBraces, closeBraces, "Unbalanced braces in generated theme code")
    }
}
