package com.korczak.control.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val Ink = Color(0xFF0A0D12)
private val Surface = Color(0xFF10151D)
private val SurfaceRaised = Color(0xFF171D27)
private val SurfaceSubtle = Color(0xFF1D2430)
private val Accent = Color(0xFF7BA7FF)
private val AccentSoft = Color(0xFF243B63)
private val Text = Color(0xFFF2F5FA)
private val Muted = Color(0xFFAEB8C7)
private val Outline = Color(0xFF303A49)
private val Danger = Color(0xFFFFB4AB)

private val KorczakColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF07101F),
    primaryContainer = AccentSoft,
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFB8C8E6),
    onSecondary = Color(0xFF101826),
    secondaryContainer = SurfaceSubtle,
    onSecondaryContainer = Color(0xFFDDE6F5),
    tertiary = Color(0xFF9FD6C0),
    background = Ink,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    outline = Outline,
    outlineVariant = Color(0xFF252D39),
    error = Danger,
    onError = Color(0xFF690005)
)

private val KorczakTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun KorczakControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KorczakColors,
        typography = KorczakTypography,
        content = content
    )
}
