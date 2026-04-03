package com.najmi.falco.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Palettes ─────────────────────────────────────────────────────────────

interface FalcoPalette {
    val bg: Color
    val surface: Color
    val surfaceBorder: Color
    val divider: Color
    val chip: Color
    val textPrimary: Color
    val textBody: Color
    val textMuted: Color
    val textGhost: Color
    val textInvisible: Color
    val stanceSupports: Color
    val stanceNeutral: Color
    val stanceOpposes: Color
    val barFilled: Color
    val barEmpty: Color
    val errorIndicator: Color
}

private val DarkFalcoPalette = object : FalcoPalette {
    override val bg = Color(0xFF000000)
    override val surface = Color(0xFF121212)
    override val surfaceBorder = Color(0xFF2A2A2A)
    override val divider = Color(0xFF1E1E1E)
    override val chip = Color(0xFF1A1A1A)
    override val textPrimary = Color(0xFFFFFFFF)
    override val textBody = Color(0xFFD0D0D0)
    override val textMuted = Color(0xFF808080)
    override val textGhost = Color(0xFF505050)
    override val textInvisible = Color(0xFF2E2E2E)
    override val stanceSupports = Color(0xFFFFFFFF)
    override val stanceNeutral = Color(0xFF808080)
    override val stanceOpposes = Color(0xFF505050)
    override val barFilled = Color(0xFFFFFFFF)
    override val barEmpty = Color(0xFF2E2E2E)
    override val errorIndicator = Color(0xFFFF4444)
}

private val LightFalcoPalette = object : FalcoPalette {
    override val bg = Color(0xFFFFFFFF)
    override val surface = Color(0xFFFAFAFA)
    override val surfaceBorder = Color(0xFFD0D0D0)
    override val divider = Color(0xFFE0E0E0)
    override val chip = Color(0xFFF0F0F0)
    override val textPrimary = Color(0xFF000000)
    override val textBody = Color(0xFF333333)
    override val textMuted = Color(0xFF666666)
    override val textGhost = Color(0xFFAAAAAA)
    override val textInvisible = Color(0xFFE5E5E5)
    override val stanceSupports = Color(0xFF000000)
    override val stanceNeutral = Color(0xFF666666)
    override val stanceOpposes = Color(0xFFAAAAAA)
    override val barFilled = Color(0xFF000000)
    override val barEmpty = Color(0xFFE5E5E5)
    override val errorIndicator = Color(0xFFCC0000)
}

// ─── Composition Local ─────────────────────────────────────────────────────

val LocalFalcoPalette: ProvidableCompositionLocal<FalcoPalette> = compositionLocalOf { DarkFalcoPalette }

@Composable
fun FalcoColorsProvider(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalFalcoPalette provides if (isDarkMode) DarkFalcoPalette else LightFalcoPalette) {
        content()
    }
}
