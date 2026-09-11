package dev.chandradsl.m3ecanvas.editor.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import m3ecanvas.shared.generated.resources.Res
import m3ecanvas.shared.generated.resources.inter_variable
import org.jetbrains.compose.resources.Font

@Composable
fun rememberInterFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.inter_variable, weight = FontWeight.Light),
        Font(Res.font.inter_variable, weight = FontWeight.Normal),
        Font(Res.font.inter_variable, weight = FontWeight.Medium),
        Font(Res.font.inter_variable, weight = FontWeight.SemiBold),
        Font(Res.font.inter_variable, weight = FontWeight.Bold),
    )
}

@Composable
fun getEditorTypography(): Typography {
    val inter = rememberInterFontFamily()
    val baseline = Typography()
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = inter),
        displayMedium = baseline.displayMedium.copy(fontFamily = inter),
        displaySmall = baseline.displaySmall.copy(fontFamily = inter),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = inter),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = inter),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = inter),
        titleLarge = baseline.titleLarge.copy(fontFamily = inter),
        titleMedium = baseline.titleMedium.copy(fontFamily = inter),
        titleSmall = baseline.titleSmall.copy(fontFamily = inter),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = inter),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = inter),
        bodySmall = baseline.bodySmall.copy(fontFamily = inter),
        labelLarge = baseline.labelLarge.copy(fontFamily = inter),
        labelMedium = baseline.labelMedium.copy(fontFamily = inter),
        labelSmall = baseline.labelSmall.copy(fontFamily = inter),
    )
}

@Composable
fun EditorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = getEditorTypography(),
        content = content
    )
}
