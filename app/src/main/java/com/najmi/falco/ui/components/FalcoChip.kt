package com.najmi.falco.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.najmi.falco.ui.theme.LocalFalcoPalette

@Composable
fun FalcoChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val palette = LocalFalcoPalette.current
    
    Box(
        modifier = modifier
            .background(
                if (selected) palette.chip else palette.bg,
                shape = RoundedCornerShape(0.dp)
            )
            .border(
                BorderStroke(1.dp, palette.chip),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = if (selected) palette.textPrimary else palette.textMuted
        )
    }
}

@Composable
fun FalcoGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalFalcoPalette.current
    
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, if (enabled) palette.surfaceBorder else palette.textInvisible),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = palette.bg,
            contentColor = palette.textPrimary,
            disabledContainerColor = palette.bg,
            disabledContentColor = palette.textGhost
        ),
        modifier = modifier.height(52.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun FalcoSolidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalFalcoPalette.current
    
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = palette.textPrimary,
            contentColor = palette.bg,
            disabledContainerColor = palette.textGhost,
            disabledContentColor = palette.bg
        ),
        modifier = modifier.height(52.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}
