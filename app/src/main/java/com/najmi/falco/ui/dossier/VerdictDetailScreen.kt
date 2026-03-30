package com.najmi.falco.ui.dossier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.falco.domain.model.AnalysisDepth
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.ui.components.AnalysisDepthMeter
import com.najmi.falco.ui.components.ChunksExplorer
import com.najmi.falco.ui.components.ConfidenceFactorsTooltips
import com.najmi.falco.ui.components.ConfidenceGauge
import com.najmi.falco.ui.components.ConsensusIndicator
import com.najmi.falco.ui.components.ProvenanceFooter
import com.najmi.falco.ui.components.TokenUsageCard
import com.najmi.falco.ui.components.UncertaintySection
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import kotlin.math.roundToInt

@Composable
fun VerdictDetailScreen(
    verdict: Verdict,
    onBack: () -> Unit
) {
    val palette = LocalFalcoPalette.current
    val stanceColor = when (verdict.lean) {
        Stance.SUPPORTS -> palette.stanceSupports
        Stance.NEUTRAL -> palette.stanceNeutral
        Stance.OPPOSES -> palette.stanceOpposes
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "VERDICT",
                style = FalcoTypography.labelSmall,
                color = palette.textGhost
            )
            Text(
                "← BACK",
                style = FalcoTypography.labelSmall,
                color = palette.textMuted,
                modifier = Modifier.clickable { onBack() }
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(verdict.lean.name, style = FalcoTypography.displayLarge, color = stanceColor)

        Spacer(Modifier.height(16.dp))

        ConfidenceGauge(
            confidence = verdict.confidence,
            certaintyLevel = verdict.certaintyLevel
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Spacer(Modifier.height(24.dp))

        TokenUsageCard(metadata = verdict.analysisMetadata)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
        AnalysisDepthMeter(
            depth = verdict.stances.firstOrNull()?.analysisDepth
                ?: AnalysisDepth.STANDARD,
                    chunksAnalyzed = verdict.stances.sumOf { it.chunksAnalyzed.size }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ConsensusIndicator(consensusInfo = verdict.consensusInfo)
            }
        }

        verdict.temporalWarning?.let { warning ->
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.chip, FalcoZeroShape)
                    .padding(12.dp)
            ) {
                Text(warning, style = FalcoTypography.bodySmall, color = palette.textMuted)
            }
        }

        val allFactors = verdict.stances.flatMap { it.confidenceFactors }.distinctBy { it.type + it.value }
        if (allFactors.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            ConfidenceFactorsTooltips(factors = allFactors.take(4))
        }

        val totalUncertainty = verdict.uncertaintyInfo
        if (totalUncertainty.gaps.isNotEmpty() || totalUncertainty.qualityWarnings.isNotEmpty() ||
            totalUncertainty.recencyAlert != null || totalUncertainty.fundingDisclosure != null
        ) {
            Spacer(Modifier.height(16.dp))
            UncertaintySection(uncertaintyInfo = totalUncertainty)
        }

        Spacer(Modifier.height(24.dp))

        Text("Synthesis of grounding data.", style = FalcoTypography.headlineMedium, color = palette.textPrimary)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
                .background(palette.surface)
                .border(1.dp, palette.divider, FalcoZeroShape)
                .padding(16.dp)
        ) {
            Text(verdict.summary, style = FalcoTypography.bodyMedium, color = palette.textBody)
        }

        Spacer(Modifier.height(32.dp))

        Text("EVIDENCE LIST [N=${verdict.stances.size}]", style = FalcoTypography.labelSmall, color = palette.textGhost)
        Spacer(Modifier.height(16.dp))

        verdict.stances.sortedByDescending { it.paper.citationCount }.forEach { stance ->
            EvidenceRow(stance = stance)
            Spacer(Modifier.height(1.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
            Spacer(Modifier.height(1.dp))
        }

        val primaryProvider = verdict.stances.firstOrNull()?.providerUsed
        val primaryModel = verdict.stances.firstOrNull()?.modelUsed

        Spacer(Modifier.height(24.dp))

        ProvenanceFooter(
            metadata = verdict.analysisMetadata,
            providerUsed = primaryProvider ?: "unknown",
            modelUsed = primaryModel
        )

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun ConfidenceSegmentBar(confidence: Float, segments: Int = 13) {
    val filled = (confidence * segments).roundToInt()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        if (i < filled) LocalFalcoPalette.current.barFilled else LocalFalcoPalette.current.barEmpty,
                        FalcoZeroShape
                    )
            )
        }
    }
}

@Composable
private fun MetadataBlock(verdict: Verdict) {
    Column {
        MetaRow("PAPERS ANALYSED", "${verdict.stances.size} PASSED QUALITY GATE")
        Spacer(Modifier.height(12.dp))
        MetaRow("EVIDENCE BREAKDOWN", "${verdict.supportingCount} SUPPORT · ${verdict.opposingCount} OPPOSE · ${verdict.neutralCount} NEUTRAL")
        Spacer(Modifier.height(12.dp))
        MetaRow("DOMINANT FIELD", verdict.dominantField.uppercase())
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Column {
        Text(label, style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = FalcoTypography.bodySmall.copy(letterSpacing = FalcoDimens.LetterSpacingMeta),
            color = LocalFalcoPalette.current.textBody
        )
    }
}

@Composable
private fun EvidenceRow(stance: PaperStance) {
    var expanded by remember { mutableStateOf(false) }
    val finalStance = stance.finalStance ?: stance.actorStance
    val stanceTextColor = when (finalStance) {
        Stance.SUPPORTS -> LocalFalcoPalette.current.stanceSupports
        Stance.NEUTRAL -> LocalFalcoPalette.current.stanceNeutral
        Stance.OPPOSES -> LocalFalcoPalette.current.stanceOpposes
    }
    val groundingScore = stance.groundingScore ?: stance.confidence

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("[${finalStance.name}]", style = FalcoTypography.labelSmall, color = stanceTextColor)
            Spacer(Modifier.width(8.dp))
            Text(
                stance.paper.title.uppercase().take(50),
                style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = LocalFalcoPalette.current.textPrimary
            )
            Spacer(Modifier.weight(1f))
            Text("→", style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textGhost)
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Spacer(Modifier.width(24.dp))
            Text(
                "${stance.paper.authors.firstOrNull() ?: "Unknown"}, (${stance.paper.year ?: "N/A"}) · ${stance.paper.citationCount} citations",
                style = FalcoTypography.labelMedium,
                color = LocalFalcoPalette.current.textMuted
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(24.dp))
            Text("GROUNDING", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
            Spacer(Modifier.width(8.dp))
            Text(
                String.format("%.2f", groundingScore),
                style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = LocalFalcoPalette.current.textBody
            )
            stance.didStopEarly?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Early stop",
                    style = FalcoTypography.labelSmall,
                    color = LocalFalcoPalette.current.textGhost
                )
            }
        }

        if (stance.chunksAnalyzed.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            var chunksExpanded by remember { mutableStateOf(false) }
            ChunksExplorer(
                chunks = stance.chunksAnalyzed,
                expanded = chunksExpanded,
                onToggle = { chunksExpanded = !chunksExpanded },
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .background(LocalFalcoPalette.current.surface)
                    .padding(12.dp)
            ) {
                Column {
                    Text("ACTOR: ${stance.actorReasoning}", style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textMuted)
                    stance.criticChallenge?.let { challenge ->
                        Spacer(Modifier.height(8.dp))
                        Text("CRITIC: $challenge", style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textGhost)
                    }
                }
            }
        }
    }
}
