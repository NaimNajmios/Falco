package com.najmi.falco.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun FalcoTheme(
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    FalcoColorsProvider(isDarkMode = isDarkMode) {
        val p = LocalFalcoPalette.current
        MaterialTheme(
            colorScheme = if (isDarkMode) darkColorScheme(
                primary = p.textPrimary,
                onPrimary = p.bg,
                secondary = p.textMuted,
                background = p.bg,
                surface = p.bg,
                onBackground = p.textPrimary,
                onSurface = p.textPrimary,
                surfaceVariant = p.surface,
                onSurfaceVariant = p.textBody
            ) else lightColorScheme(
                primary = p.textPrimary,
                onPrimary = p.bg,
                secondary = p.textMuted,
                background = p.bg,
                surface = p.bg,
                onBackground = p.textPrimary,
                onSurface = p.textPrimary,
                surfaceVariant = p.surface,
                onSurfaceVariant = p.textBody
            ),
            typography = FalcoTypography,
            shapes = FalcoShapes,
            content = content
        )
    }
}
