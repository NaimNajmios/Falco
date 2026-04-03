package com.najmi.falco.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette

@Composable
fun StageTimeline(
    completedStages: List<Pair<String, Long>>,
    currentStage: String?,
    pendingStages: List<String>,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val totalMs = completedStages.sumOf { it.second }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "STAGE_TIMELINE",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            completedStages.forEach { (stage, duration) ->
                val widthFraction = if (totalMs > 0) duration.toFloat() / totalMs else 0f
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(widthFraction.coerceAtLeast(0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette.barFilled)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stage.take(8),
                        style = FalcoTypography.labelMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                        color = palette.textMuted
                    )
                    Text(
                        "${duration}ms",
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                }
            }
            
            if (currentStage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.2f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette.textPrimary.copy(alpha = pulseAlpha))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currentStage.take(8),
                        style = FalcoTypography.labelMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        "...",
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                }
            }
            
            pendingStages.take(3).forEach { stage ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.15f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette.barEmpty)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stage.take(6),
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                }
            }
        }
        
        if (totalMs > 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total time:",
                    style = FalcoTypography.labelSmall,
                    color = palette.textGhost
                )
                Text(
                    "${totalMs}ms",
                    style = FalcoTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
            }
        }
    }
}
