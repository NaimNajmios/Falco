package com.najmi.falco.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape

@Composable
fun FalcoBottomNav(
    selectedTab: FalcoTab,
    onTabSelected: (FalcoTab) -> Unit,
    enabledTabs: Set<FalcoTab> = FalcoTab.entries.toSet(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.bg)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FalcoTab.entries.forEach { tab ->
            val isEnabled = tab in enabledTabs
            val isSelected = tab == selectedTab
            FalcoNavItem(
                tab = tab,
                isSelected = isSelected,
                isEnabled = isEnabled,
                onClick = { if (isEnabled) onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun FalcoNavItem(
    tab: FalcoTab,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val activeColor = LocalFalcoPalette.current.textPrimary
    val inactiveColor = LocalFalcoPalette.current.textMuted
    val textColor = if (!isEnabled) LocalFalcoPalette.current.textGhost else if (isSelected) activeColor else inactiveColor
    val indicatorColor = if (isSelected) LocalFalcoPalette.current.textPrimary else Color.Transparent
    val iconColor = if (!isEnabled) LocalFalcoPalette.current.textGhost else if (isSelected) LocalFalcoPalette.current.textPrimary else LocalFalcoPalette.current.textMuted

    Column(
        modifier = Modifier
            .alpha(if (isEnabled) 1f else 0.4f)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .background(indicatorColor)
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(24.dp)
                .drawBehind {
                    drawIcon(tab, iconColor)
                }
        )
        Box(
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = tab.label,
                style = FalcoTypography.labelSmall,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIcon(
    tab: FalcoTab,
    color: Color
) {
    val fillColor = color

    when (tab) {
        FalcoTab.Hypothesis -> {
            drawCircle(color = color, style = Stroke(width = 1.5f), radius = 8f)
            drawCircle(color = fillColor, radius = 3f)
        }
        FalcoTab.Pipeline -> {
            val s = 4f
            val offset = 4f
            listOf(
                Offset(-offset, -offset), Offset(offset, -offset),
                Offset(-offset, offset), Offset(offset, offset)
            ).forEach {
                drawRect(color = color, topLeft = it - Offset(s, s), size = androidx.compose.ui.geometry.Size(s * 2, s * 2))
            }
        }
        FalcoTab.Dossier -> {
            drawRect(color = color, topLeft = Offset(-6f, -8f), size = androidx.compose.ui.geometry.Size(12f, 16f))
            listOf(-4f, 0f, 4f).forEach { y ->
                drawLine(color, Offset(-4f, y), Offset(4f, y), strokeWidth = 1f)
            }
        }
        FalcoTab.Settings -> {
            drawCircle(color = color, style = Stroke(width = 1.5f), radius = 6f)
            drawLine(color, Offset(-6f, 0f), Offset(-8f, 0f), strokeWidth = 2f)
            drawLine(color, Offset(6f, 0f), Offset(8f, 0f), strokeWidth = 2f)
            drawLine(color, Offset(0f, -6f), Offset(0f, -8f), strokeWidth = 2f)
            drawLine(color, Offset(0f, 6f), Offset(0f, 8f), strokeWidth = 2f)
        }
    }
}
