package com.korczak.control.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KorczakDark = darkColorScheme(
    primary = Color(0xFFD8B4FE),
    onPrimary = Color(0xFF24112F),
    primaryContainer = Color(0xFF3B1D50),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFF08080B),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF101015),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF1A1A22),
    onSurfaceVariant = Color(0xFFB9B5C3),
    outline = Color(0xFF35333D),
    error = Color(0xFFFF8A80)
)

@Composable
fun KorczakControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KorczakDark,
        typography = Typography(),
        content = content
    )
}
