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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.model.VerificationState.Error
import com.najmi.falco.domain.model.VerificationState.InProgress
import com.najmi.falco.domain.model.VerificationState.Success
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.ui.hypothesis.HypothesisViewModel
import com.najmi.falco.ui.theme.FalcoBg
import com.najmi.falco.ui.theme.FalcoBarEmpty
import com.najmi.falco.ui.theme.FalcoBarFilled
import com.najmi.falco.ui.theme.FalcoDivider
import com.najmi.falco.ui.theme.FalcoStanceNeutral
import com.najmi.falco.ui.theme.FalcoStanceOpposes
import com.najmi.falco.ui.theme.FalcoStanceSupports
import com.najmi.falco.ui.theme.FalcoSurface
import com.najmi.falco.ui.theme.FalcoTextBody
import com.najmi.falco.ui.theme.FalcoTextGhost
import com.najmi.falco.ui.theme.FalcoTextMuted
import com.najmi.falco.ui.theme.FalcoTextPrimary
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import kotlin.math.roundToInt

@Composable
fun DossierScreen(
    viewModel: HypothesisViewModel = hiltViewModel(),
    onNewClaim: () -> Unit
) {
    val state by viewModel.verificationState.collectAsState()

    when (val s = state) {
        is VerificationState.Idle -> IdleState()
        is InProgress -> InProgressState(s.message)
        is Success -> VerdictState(verdict = s.verdict, onNewClaim = onNewClaim)
        is Error -> ErrorState(message = s.message, onRetry = onNewClaim)
    }
}

@Composable
private fun IdleState() {
    Column(
        modifier = Modifier.fillMaxSize().background(FalcoBg).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No verdict yet.", style = FalcoTypography.bodyMedium, color = FalcoTextGhost)
    }
}

@Composable
private fun InProgressState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(FalcoBg).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = FalcoTypography.bodyMedium, color = FalcoTextMuted)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(FalcoBg).padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))
        Text("VERDICT STATUS: ERROR", style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        Spacer(Modifier.height(16.dp))
        Text("UNVERIFIABLE", style = FalcoTypography.displayLarge, color = FalcoTextGhost)
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth().background(FalcoSurface)
                .border(1.dp, FalcoDivider, FalcoZeroShape)
                .padding(16.dp)
        ) {
            Text(message, style = FalcoTypography.bodyMedium, color = FalcoTextBody)
        }
        Spacer(Modifier.height(32.dp))
        FalcoGhostButton("RETRY", onClick = onRetry)
    }
}

@Composable
private fun VerdictState(verdict: Verdict, onNewClaim: () -> Unit) {
    val stanceColor = when (verdict.lean) {
        Stance.SUPPORTS -> FalcoStanceSupports
        Stance.NEUTRAL -> FalcoStanceNeutral
        Stance.OPPOSES -> FalcoStanceOpposes
    }
    val confidencePct = (verdict.confidence * 100).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FalcoBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Text("VERDICT STATUS: FINAL", style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        Spacer(Modifier.height(8.dp))

        Text(verdict.lean.name, style = FalcoTypography.displayLarge, color = stanceColor)

        Spacer(Modifier.height(16.dp))

        Text("$confidencePct%  CONFIDENCE", style = FalcoTypography.headlineSmall, color = FalcoTextBody)
        Spacer(Modifier.height(8.dp))

        ConfidenceSegmentBar(confidence = verdict.confidence)

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FalcoDivider))

        Spacer(Modifier.height(24.dp))

        MetadataBlock(verdict = verdict)

        Spacer(Modifier.height(24.dp))

        Text("Synthesis of grounding data.", style = FalcoTypography.headlineMedium, color = FalcoTextPrimary)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
                .background(FalcoSurface)
                .border(1.dp, FalcoDivider, FalcoZeroShape)
                .padding(16.dp)
        ) {
            Text(verdict.summary, style = FalcoTypography.bodyMedium, color = FalcoTextBody)
        }

        Spacer(Modifier.height(32.dp))

        Text("EVIDENCE LIST [N=${verdict.stances.size}]", style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        Spacer(Modifier.height(16.dp))

        verdict.stances.sortedByDescending { it.paper.citationCount }.forEach { stance ->
            EvidenceRow(stance = stance)
            Spacer(Modifier.height(1.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FalcoDivider))
            Spacer(Modifier.height(1.dp))
        }

        Spacer(Modifier.height(32.dp))

        FalcoGhostButton("NEW CLAIM", onClick = onNewClaim)
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ConfidenceSegmentBar(confidence: Float, segments: Int = 13) {
    val filled = (confidence * segments).roundToInt()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        if (i < filled) FalcoBarFilled else FalcoBarEmpty,
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
        Text(label, style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = FalcoTypography.bodySmall.copy(
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            color = FalcoTextBody
        )
    }
}

@Composable
private fun EvidenceRow(stance: PaperStance) {
    var expanded by remember { mutableStateOf(false) }
    val stanceTextColor = when (stance.actorStance) {
        Stance.SUPPORTS -> FalcoStanceSupports
        Stance.NEUTRAL -> FalcoStanceNeutral
        Stance.OPPOSES -> FalcoStanceOpposes
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("[${stance.actorStance.name}]", style = FalcoTypography.labelSmall, color = stanceTextColor)
            Spacer(Modifier.width(8.dp))
            Text(
                stance.paper.title.uppercase().take(50),
                style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = FalcoTextPrimary
            )
            Spacer(Modifier.weight(1f))
            Text("→", style = FalcoTypography.bodySmall, color = FalcoTextGhost)
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Spacer(Modifier.width(24.dp))
            Text(
                "${stance.paper.authors.firstOrNull() ?: "Unknown"}, (${stance.paper.year ?: "N/A"})",
                style = FalcoTypography.labelMedium,
                color = FalcoTextMuted
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(24.dp))
            Text("GROUNDING", style = FalcoTypography.labelSmall, color = FalcoTextGhost)
            Spacer(Modifier.width(8.dp))
            Text(
                String.format("%.2f", stance.confidence),
                style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = FalcoTextBody
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .background(FalcoSurface)
                    .padding(12.dp)
            ) {
                Text(stance.actorReasoning, style = FalcoTypography.bodySmall, color = FalcoTextMuted)
            }
        }
    }
}

@Composable
private fun FalcoGhostButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FalcoBg, contentColor = FalcoTextPrimary),
        shape = FalcoZeroShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FalcoTextGhost, FalcoZeroShape)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = FalcoTypography.bodySmall)
        }
    }
}
