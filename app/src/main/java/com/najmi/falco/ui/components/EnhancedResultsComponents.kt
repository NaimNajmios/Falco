package com.najmi.falco.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.falco.domain.model.AnalysisDepth
import com.najmi.falco.domain.model.AnalysisMetadata
import com.najmi.falco.domain.model.AnalyzedChunk
import com.najmi.falco.domain.model.CertaintyLevel
import com.najmi.falco.domain.model.ConfidenceFactor
import com.najmi.falco.domain.model.ConsensusInfo
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.UncertaintyInfo
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TokenUsageCard(
    metadata: AnalysisMetadata,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TOKEN USAGE",
                style = FalcoTypography.labelSmall,
                color = palette.textGhost
            )
            metadata.efficiencyComparison?.let {
                Text(
                    it,
                    style = FalcoTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.stanceSupports
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    "${numberFormat.format(metadata.totalTokensAnalyzed)}",
                    style = FalcoTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    "tokens analyzed",
                    style = FalcoTypography.labelMedium,
                    color = palette.textGhost
                )
            }

            metadata.estimatedFullTextTokens?.let { fullText ->
                Column {
                    Text(
                        "~${numberFormat.format(fullText)}",
                        style = FalcoTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.textMuted
                    )
                    Text(
                        "full paper est.",
                        style = FalcoTypography.labelMedium,
                        color = palette.textGhost
                    )
                }
            }
        }

        if (metadata.databasesQueried.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metadata.databasesQueried.forEach { db ->
                    ProviderBadge(text = db)
                }
            }
        }
    }
}

@Composable
fun ProviderBadge(text: String, modifier: Modifier = Modifier) {
    val palette = LocalFalcoPalette.current
    Box(
        modifier = modifier
            .background(palette.chip)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = FalcoTypography.labelSmall,
            color = palette.textMuted
        )
    }
}

@Composable
fun AnalysisDepthMeter(
    depth: AnalysisDepth,
    chunksAnalyzed: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ANALYSIS DEPTH",
                style = FalcoTypography.labelSmall,
                color = palette.textGhost
            )
            Text(
                depth.name.lowercase().replaceFirstChar { it.uppercase() },
                style = FalcoTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = when (depth) {
                    AnalysisDepth.DEEP -> palette.stanceSupports
                    AnalysisDepth.STANDARD -> palette.textMuted
                    AnalysisDepth.LIGHT -> palette.textGhost
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            val filledCount = when (depth) {
                AnalysisDepth.LIGHT -> 1
                AnalysisDepth.STANDARD -> 2
                AnalysisDepth.DEEP -> 3
            }
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index < filledCount) palette.barFilled else palette.barEmpty,
                            RoundedCornerShape(topStart = if (index == 0) 2.dp else 0.dp, bottomStart = if (index == 0) 2.dp else 0.dp, topEnd = if (index == 2) 2.dp else 0.dp, bottomEnd = if (index == 2) 2.dp else 0.dp)
                        )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "$chunksAnalyzed chunk${if (chunksAnalyzed != 1) "s" else ""} analyzed",
            style = FalcoTypography.labelMedium,
            color = palette.textGhost
        )
    }
}

@Composable
fun ConfidenceGauge(
    confidence: Float,
    certaintyLevel: CertaintyLevel,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val confidencePct = (confidence * 100).toInt()

    val gaugeColor by animateColorAsState(
        targetValue = when {
            confidence < 0.5f -> Color(0xFFEF4444)
            confidence < 0.8f -> Color(0xFFF59E0B)
            else -> Color(0xFF22C55E)
        },
        label = "gaugeColor"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CONFIDENCE",
                style = FalcoTypography.labelSmall,
                color = palette.textGhost
            )
            Text(
                when (certaintyLevel) {
                    CertaintyLevel.HIGH -> "High certainty"
                    CertaintyLevel.MODERATE -> "Moderate certainty"
                    CertaintyLevel.LOW -> "Low - more research recommended"
                },
                style = FalcoTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = gaugeColor
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(palette.barEmpty)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(confidence)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(gaugeColor)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "$confidencePct%",
            style = FalcoTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = palette.textPrimary
        )
    }
}

@Composable
fun ConsensusIndicator(
    consensusInfo: ConsensusInfo,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Column(modifier = modifier) {
        Text(
            "CONSENSUS",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(4.dp))

        Text(
            consensusInfo.getConsensusText(),
            style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = palette.textBody
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConsensusPill(count = consensusInfo.supportingCount, label = "Support", color = palette.stanceSupports)
            ConsensusPill(count = consensusInfo.opposingCount, label = "Oppose", color = palette.stanceOpposes)
            ConsensusPill(count = consensusInfo.neutralCount, label = "Neutral", color = palette.stanceNeutral)
        }
    }
}

@Composable
private fun ConsensusPill(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, FalcoZeroShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "$count $label",
            style = FalcoTypography.labelMedium,
            color = color
        )
    }
}

@Composable
fun ConfidenceFactorsTooltips(
    factors: List<ConfidenceFactor>,
    modifier: Modifier = Modifier
) {
    if (factors.isEmpty()) return

    val palette = LocalFalcoPalette.current

    Column(modifier = modifier) {
        Text(
            "CONFIDENCE FACTORS",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(8.dp))

        factors.forEach { factor ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    factor.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = FalcoTypography.labelMedium,
                    color = palette.textMuted
                )
                Text(
                    factor.value,
                    style = FalcoTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = when (factor.impact) {
                        "positive" -> palette.stanceSupports
                        "negative" -> palette.stanceOpposes
                        else -> palette.textBody
                    }
                )
            }
        }
    }
}

@Composable
fun ChunksExplorer(
    chunks: List<AnalyzedChunk>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CHUNKS ANALYZED [${chunks.size}]",
                style = FalcoTypography.labelSmall,
                color = palette.textGhost
            )
            Text(
                if (expanded) "−" else "+",
                style = FalcoTypography.headlineSmall,
                color = palette.textMuted
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            chunks.forEach { chunk ->
                ChunkItem(chunk = chunk)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ChunkItem(chunk: AnalyzedChunk) {
    val palette = LocalFalcoPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(palette.barFilled, FalcoZeroShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    chunk.sourceSection,
                    style = FalcoTypography.labelSmall,
                    color = palette.textMuted
                )
            }
            Text(
                "${chunk.estimatedTokens} tokens",
                style = FalcoTypography.labelSmall,
                color = palette.textGhost
            )
        }

        chunk.keyEvidence?.let { evidence ->
            Spacer(Modifier.height(4.dp))
            Text(
                evidence.take(150) + if (evidence.length > 150) "..." else "",
                style = FalcoTypography.bodySmall,
                color = palette.textBody
            )
        }
    }
}

@Composable
fun UncertaintySection(
    uncertaintyInfo: UncertaintyInfo,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val hasContent = uncertaintyInfo.gaps.isNotEmpty() ||
            uncertaintyInfo.qualityWarnings.isNotEmpty() ||
            uncertaintyInfo.recencyAlert != null ||
            uncertaintyInfo.fundingDisclosure != null

    if (!hasContent) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "LIMITATIONS & UNCERTAINTY",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(8.dp))

        uncertaintyInfo.recencyAlert?.let { alert ->
            UncertaintyItem(
                icon = "⏱",
                text = alert,
                color = Color(0xFFF59E0B)
            )
            Spacer(Modifier.height(4.dp))
        }

        uncertaintyInfo.qualityWarnings.take(2).forEach { warning ->
            UncertaintyItem(
                icon = "⚠",
                text = warning,
                color = Color(0xFFF59E0B)
            )
            Spacer(Modifier.height(4.dp))
        }

        uncertaintyInfo.gaps.forEach { gap ->
            UncertaintyItem(
                icon = "−",
                text = "Limited evidence on $gap",
                color = palette.textMuted
            )
            Spacer(Modifier.height(4.dp))
        }

        uncertaintyInfo.fundingDisclosure?.let { disclosure ->
            UncertaintyItem(
                icon = "§",
                text = disclosure,
                color = palette.textGhost
            )
        }
    }
}

@Composable
private fun UncertaintyItem(
    icon: String,
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    val palette = LocalFalcoPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, color.copy(alpha = 0.3f), FalcoZeroShape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = FalcoTypography.bodySmall, color = color)
        Spacer(Modifier.width(8.dp))
        Text(text, style = FalcoTypography.bodySmall, color = palette.textMuted)
    }
}

@Composable
fun ProvenanceFooter(
    metadata: AnalysisMetadata,
    providerUsed: String,
    modelUsed: String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.divider, FalcoZeroShape)
            .padding(12.dp)
    ) {
        Text(
            "VERIFICATION PROVENANCE",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Timestamp", style = FalcoTypography.labelMedium, color = palette.textGhost)
                Text(
                    dateFormat.format(Date(metadata.completedAt)),
                    style = FalcoTypography.bodySmall,
                    color = palette.textMuted
                )
            }

            Column {
                Text("Algorithm", style = FalcoTypography.labelMedium, color = palette.textGhost)
                Text(
                    metadata.algorithmVersion,
                    style = FalcoTypography.bodySmall,
                    color = palette.textMuted
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Model", style = FalcoTypography.labelMedium, color = palette.textGhost)
                Text(
                    modelUsed ?: "Unknown",
                    style = FalcoTypography.bodySmall,
                    color = palette.textMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Provider", style = FalcoTypography.labelMedium, color = palette.textGhost)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        providerUsed,
                        style = FalcoTypography.bodySmall,
                        color = palette.textMuted
                    )
                }
            }
        }
    }
}
