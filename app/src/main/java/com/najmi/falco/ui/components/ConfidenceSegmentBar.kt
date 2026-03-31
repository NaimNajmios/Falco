package com.najmi.falco.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette
import kotlin.math.roundToInt

@Composable
fun ConfidenceSegmentBar(
    confidence: Float,
    segments: Int = 13,
    modifier: Modifier = Modifier
) {
    val filled = (confidence * segments).roundToInt()
    val palette = LocalFalcoPalette.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(segments) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        if (i < filled) palette.barFilled else palette.barEmpty,
                        FalcoZeroShape
                    )
            )
        }
    }
}
