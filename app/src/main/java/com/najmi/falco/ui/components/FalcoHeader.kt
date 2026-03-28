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
fun FalcoHeader(
    modifier: Modifier = Modifier,
    showVersion: Boolean = true
) {
    val palette = LocalFalcoPalette.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "F",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = palette.textPrimary
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "FALCO",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 4.sp,
            color = palette.textMuted
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (showVersion) {
            Text(
                text = "v1.0",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = palette.textGhost
            )
        }
    }
}
