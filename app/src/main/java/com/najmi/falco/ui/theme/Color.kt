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
}

private val DarkFalcoPalette = object : FalcoPalette {
    override val bg = Color(0xFF000000)
    override val surface = Color(0xFF080808)
    override val surfaceBorder = Color(0xFF141414)
    override val divider = Color(0xFF0F0F0F)
    override val chip = Color(0xFF1C1C1C)
    override val textPrimary = Color(0xFFFFFFFF)
    override val textBody = Color(0xFFBBBBBB)
    override val textMuted = Color(0xFF555555)
    override val textGhost = Color(0xFF3A3A3A)
    override val textInvisible = Color(0xFF1E1E1E)
    override val stanceSupports = Color(0xFFFFFFFF)
    override val stanceNeutral = Color(0xFF555555)
    override val stanceOpposes = Color(0xFF3A3A3A)
    override val barFilled = Color(0xFFFFFFFF)
    override val barEmpty = Color(0xFF1E1E1E)
}

private val LightFalcoPalette = object : FalcoPalette {
    override val bg = Color(0xFFFFFFFF)
    override val surface = Color(0xFFF5F5F5)
    override val surfaceBorder = Color(0xFFE0E0E0)
    override val divider = Color(0xFFE8E8E8)
    override val chip = Color(0xFFEEEEEE)
    override val textPrimary = Color(0xFF000000)
    override val textBody = Color(0xFF444444)
    override val textMuted = Color(0xFF888888)
    override val textGhost = Color(0xFFBBBBBB)
    override val textInvisible = Color(0xFFF0F0F0)
    override val stanceSupports = Color(0xFF000000)
    override val stanceNeutral = Color(0xFF888888)
    override val stanceOpposes = Color(0xFFBBBBBB)
    override val barFilled = Color(0xFF000000)
    override val barEmpty = Color(0xFFE8E8E8)
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
