package com.najmi.falco.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
fun FalcoLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
        color = palette.textGhost,
        modifier = modifier
    )
}

@Composable
fun FalcoMetaRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FalcoLabel(text = label)
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = palette.textBody
        )
    }
}

@Composable
fun FalcoHairlineDivider(
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.divider)
    )
}
