package com.najmi.falco.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FalcoColorScheme = darkColorScheme(
    primary = FalcoTextPrimary,
    onPrimary = FalcoBg,
    secondary = FalcoTextMuted,
    background = FalcoBg,
    surface = FalcoBg,
    onBackground = FalcoTextPrimary,
    onSurface = FalcoTextPrimary,
    surfaceVariant = FalcoSurface,
    onSurfaceVariant = FalcoTextBody
)

@Composable
fun FalcoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FalcoColorScheme,
        typography = FalcoTypography,
        shapes = FalcoShapes,
        content = content
    )
}
