package com.najmi.falco.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.najmi.falco.ui.theme.LocalFalcoPalette

@Composable
fun ConfidenceSegmentBar(
    confidence: Float,
    modifier: Modifier = Modifier,
    segments: Int = 13
) {
    val palette = LocalFalcoPalette.current
    val filledSegments = (confidence * segments).toInt()
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(
                        if (index < filledSegments) palette.barFilled else palette.barEmpty,
                        shape = RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}

@Composable
fun LiveBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    maxBars: Int = 12
) {
    val palette = LocalFalcoPalette.current
    val displayValues = if (values.size > maxBars) values.takeLast(maxBars) else values
    val maxValue = displayValues.maxOrNull() ?: 1f
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(maxBars) { index ->
            val value = displayValues.getOrNull(index) ?: 0f
            val height = if (maxValue > 0) (value / maxValue) else 0f
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .fillMaxHeight(height.coerceIn(0.05f, 1f))
                    .background(
                        palette.barFilled,
                        shape = RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}
