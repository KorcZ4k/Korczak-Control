package com.korczak.control.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KorczakDark = darkColorScheme(
    primary = Color(0xFFFFFFFF), background = Color(0xFF090909), surface = Color(0xFF111111), surfaceVariant = Color(0xFF181818), onPrimary = Color(0xFF090909), onBackground = Color(0xFFF5F5F5), onSurface = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFFA3A3A3)
)
@Composable fun KorczakControlTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = KorczakDark, content = content) }
