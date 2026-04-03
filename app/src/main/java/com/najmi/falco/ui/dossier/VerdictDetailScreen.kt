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
import androidx.compose.ui.unit.dp
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.ui.components.ConfidenceFactorsTooltips
import com.najmi.falco.ui.components.ConfidenceSegmentBar
import com.najmi.falco.ui.components.EvidenceRow
import com.najmi.falco.ui.components.FalcoMetaRow
import com.najmi.falco.ui.components.ProvenanceFooter
import com.najmi.falco.ui.components.ShareBottomSheet
import com.najmi.falco.ui.components.UncertaintySection
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape

@Composable
fun VerdictDetailScreen(
    verdict: Verdict,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onShare: () -> Unit = {},
    onSave: () -> Unit = {}
) {
    val palette = LocalFalcoPalette.current
    val stanceColor = when (verdict.lean) {
        Stance.SUPPORTS -> palette.stanceSupports
        Stance.NEUTRAL -> palette.stanceNeutral
        Stance.OPPOSES -> palette.stanceOpposes
        Stance.INSUFFICIENT_EVIDENCE -> palette.textMuted
    }
    
    var showShareSheet by androidx.compose.runtime.mutableStateOf(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "VERDICT",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(8.dp))

        Text(verdict.lean.name, style = FalcoTypography.displayLarge, color = stanceColor)

        Spacer(Modifier.height(16.dp))

        if (verdict.lean == Stance.INSUFFICIENT_EVIDENCE) {
            verdict.caveat?.let { caveat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .border(1.dp, palette.divider, FalcoZeroShape)
                        .padding(16.dp)
                ) {
                    Text(caveat, style = FalcoTypography.bodySmall, color = palette.textMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            ConfidenceSegmentBar(confidence = verdict.confidence)
            Spacer(Modifier.height(8.dp))
            Text(
                "${(verdict.confidence * 100).toInt()}% CONFIDENCE",
                style = FalcoTypography.labelSmall,
                color = palette.textMuted
            )
            Spacer(Modifier.height(8.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FalcoMetaRow("LATENCY", "${verdict.analysisMetadata.analysisDurationMs}ms")
            FalcoMetaRow("METADATA", "${verdict.totalPapersPassedGate} passed · ${verdict.totalPapersRetrieved} retrieved")
        }

        Spacer(Modifier.height(16.dp))

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

        Text("EVIDENCE LIST [N=${verdict.totalPapersPassedGate}]", style = FalcoTypography.labelSmall, color = palette.textGhost)
        Spacer(Modifier.height(16.dp))

        verdict.stances.sortedByDescending { it.paper.citationCount }.forEach { stance ->
            EvidenceRow(paperStance = stance)
        }

        val primaryProvider = verdict.stances.firstOrNull()?.providerUsed
        val primaryModel = verdict.stances.firstOrNull()?.modelUsed

        Spacer(Modifier.height(24.dp))

        ProvenanceFooter(
            metadata = verdict.analysisMetadata,
            providerUsed = primaryProvider ?: "unknown",
            modelUsed = primaryModel
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                label = "SHARE VERDICT",
                onClick = { showShareSheet = true },
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                label = "SAVE TO HISTORY",
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        ActionButton(
            label = "NEW CLAIM",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(80.dp))
    }
    
    if (showShareSheet) {
        ShareBottomSheet(
            verdict = verdict,
            onDismiss = { showShareSheet = false }
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    Box(
        modifier = modifier
            .height(48.dp)
            .background(palette.bg, FalcoZeroShape)
            .border(1.dp, palette.textGhost, FalcoZeroShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = FalcoTypography.labelSmall,
            color = palette.textPrimary
        )
    }
}


