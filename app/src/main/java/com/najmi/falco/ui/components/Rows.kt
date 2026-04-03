package com.najmi.falco.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.ui.theme.LocalFalcoPalette

private fun stanceColorFor(stance: Stance, palette: com.najmi.falco.ui.theme.FalcoPalette): androidx.compose.ui.graphics.Color {
    return when (stance) {
        Stance.SUPPORTS -> palette.stanceSupports
        Stance.OPPOSES -> palette.stanceOpposes
        Stance.NEUTRAL -> palette.stanceNeutral
        Stance.INSUFFICIENT_EVIDENCE -> palette.textMuted
    }
}

@Composable
fun EvidenceRow(
    paperStance: PaperStance,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onToggle: () -> Unit = {}
) {
    val palette = LocalFalcoPalette.current
    var isExpanded by remember { mutableStateOf(expanded) }
    val finalStance = paperStance.finalStance ?: paperStance.actorStance
    val stanceClr = stanceColorFor(finalStance, palette)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(stanceClr, shape = RoundedCornerShape(0.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paperStance.paper.title,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    paperStance.paper.year?.let { year ->
                        Text(
                            text = "$year",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = palette.textMuted
                        )
                        Text(
                            text = " · ",
                            fontSize = 10.sp,
                            color = palette.textGhost
                        )
                    }
                    Text(
                        text = "${paperStance.paper.citationCount} citations",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textMuted
                    )
                }
            }

            Text(
                text = finalStance.name.take(4),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = stanceClr
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 12.dp)
            ) {
                FalcoLabel(text = "Actor reasoning")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = paperStance.actorReasoning,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textBody
                )
                paperStance.criticChallenge?.let { challenge ->
                    Spacer(modifier = Modifier.height(12.dp))
                    FalcoLabel(text = "Critic challenge")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = challenge,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textBody
                    )
                }
                paperStance.groundingScore?.let { score ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FalcoLabel(text = "Grounding")
                        Spacer(modifier = Modifier.width(8.dp))
                        ConfidenceSegmentBar(
                            confidence = score,
                            modifier = Modifier.weight(1f),
                            segments = 5
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(score * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = palette.textMuted
                        )
                    }
                }

                if (paperStance.chunksAnalyzed.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    var chunksExpanded by remember { mutableStateOf(false) }
                    ChunksExplorer(
                        chunks = paperStance.chunksAnalyzed,
                        expanded = chunksExpanded,
                        onToggle = { chunksExpanded = !chunksExpanded }
                    )
                }
            }
        }

        FalcoHairlineDivider()
    }
}

@Composable
fun StageRow(
    stage: VerificationStage,
    isActive: Boolean,
    isCompleted: Boolean,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when {
                        isCompleted -> palette.textPrimary
                        isActive -> palette.textGhost
                        else -> palette.textInvisible
                    },
                    shape = RoundedCornerShape(0.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Text(
                    text = "✓",
                    fontSize = 12.sp,
                    color = palette.bg,
                    fontWeight = FontWeight.Bold
                )
            } else if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(palette.textPrimary, shape = RoundedCornerShape(0.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stage.label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isActive || isCompleted) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive || isCompleted) palette.textPrimary else palette.textGhost
            )
            if (isActive && message != null) {
                Text(
                    text = message,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textMuted
                )
            }
        }
    }
}
