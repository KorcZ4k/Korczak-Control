package com.korczak.control.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MoonBlack = Color(0xFF07070A)
private val MoonSurface = Color(0xFF0D0D12)
private val MoonSurfaceElevated = Color(0xFF15151D)
private val MoonPurple = Color(0xFF9B6BFF)
private val MoonViolet = Color(0xFF7047D7)
private val MoonText = Color(0xFFF7F5FF)
private val MoonMuted = Color(0xFFAAA7B3)

private val KorczakDark = darkColorScheme(
    primary = MoonPurple,
    onPrimary = Color(0xFF120B20),
    primaryContainer = Color(0xFF24183D),
    onPrimaryContainer = Color(0xFFEDE6FF),
    secondary = Color(0xFFC5B2FF),
    onSecondary = Color(0xFF1D1430),
    secondaryContainer = Color(0xFF1B1824),
    background = MoonBlack,
    onBackground = MoonText,
    surface = MoonSurface,
    onSurface = MoonText,
    surfaceVariant = MoonSurfaceElevated,
    onSurfaceVariant = MoonMuted,
    outline = Color(0xFF2A2933),
    outlineVariant = Color(0xFF1D1D25),
    error = Color(0xFFFFB4AB)
)

@Composable
fun KorczakControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KorczakDark,
        typography = Typography(),
        content = content
    )
}
