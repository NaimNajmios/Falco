package com.najmi.falco.ui.pipeline

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
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
import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.ui.hypothesis.HypothesisViewModel
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun PipelineScreen(
    hypothesisViewModel: HypothesisViewModel,
    onNewClaim: () -> Unit
) {
    val state by hypothesisViewModel.verificationState.collectAsState()
    val claimText by hypothesisViewModel.claimText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalFalcoPalette.current.bg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            claimText.take(30).let { if (claimText.length > 30) "$it..." else it },
            style = FalcoTypography.headlineLarge,
            color = LocalFalcoPalette.current.textPrimary
        )

        Spacer(Modifier.height(24.dp))

        when (state) {
            is VerificationState.Success -> {
                VerdictResult(state = state as VerificationState.Success, onNewClaim = onNewClaim)
            }
            is VerificationState.Error -> {
                ErrorResult(state = state as VerificationState.Error, onNewClaim = onNewClaim)
            }
            else -> {
                StageList(state = state)

                Spacer(Modifier.height(24.dp))

                RealTimeExtractionPanel(state = state)

                Spacer(Modifier.height(16.dp))

                LiveMonitor()

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun VerdictResult(
    state: VerificationState.Success,
    onNewClaim: () -> Unit
) {
    val verdict = state.verdict
    val stanceColor = when (verdict.lean) {
        Stance.SUPPORTS -> LocalFalcoPalette.current.stanceSupports
        Stance.NEUTRAL -> LocalFalcoPalette.current.stanceNeutral
        Stance.OPPOSES -> LocalFalcoPalette.current.stanceOpposes
    }
    val confidencePct = (verdict.confidence * 100).roundToInt()

    Text("VERIFICATION COMPLETE", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(16.dp))

    Text(verdict.lean.name, style = FalcoTypography.displayLarge, color = stanceColor)
    Spacer(Modifier.height(8.dp))

    Text("$confidencePct%  CONFIDENCE", style = FalcoTypography.headlineSmall, color = LocalFalcoPalette.current.textBody)
    Spacer(Modifier.height(8.dp))

    ConfidenceSegmentBar(confidence = verdict.confidence)

    Spacer(Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Column {
            Text("SUMMARY", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
            Spacer(Modifier.height(8.dp))
            Text(verdict.summary, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textBody)
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MetaChip("EVIDENCE", "${verdict.stances.size} PAPERS")
        MetaChip("SUPPORT", "${verdict.supportingCount}")
        MetaChip("OPPOSE", "${verdict.opposingCount}")
    }

    verdict.temporalWarning?.let { warning ->
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalFalcoPalette.current.surface)
                .border(1.dp, LocalFalcoPalette.current.chip, FalcoZeroShape)
                .padding(12.dp)
        ) {
            Text(warning, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textMuted)
        }
    }

    Spacer(Modifier.height(32.dp))

    FalcoGhostButton("NEW CLAIM", onClick = onNewClaim)
    Spacer(Modifier.height(80.dp))
}

@Composable
private fun ErrorResult(
    state: VerificationState.Error,
    onNewClaim: () -> Unit
) {
    Text("VERIFICATION FAILED", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(16.dp))

    Text("UNVERIFIABLE", style = FalcoTypography.displayLarge, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Text(state.message, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textMuted)
    }

    Spacer(Modifier.height(32.dp))

    FalcoGhostButton("TRY AGAIN", onClick = onNewClaim)
    Spacer(Modifier.height(80.dp))
}

@Composable
private fun MetaChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
        Spacer(Modifier.height(4.dp))
        Text(value, style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium), color = LocalFalcoPalette.current.textBody)
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
                        if (i < filled) LocalFalcoPalette.current.barFilled else LocalFalcoPalette.current.barEmpty,
                        FalcoZeroShape
                    )
            )
        }
    }
}

@Composable
private fun StageList(state: VerificationState) {
    Text("VERIFICATION_STAGES", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(16.dp))

    val allStages = VerificationStage.entries
    val currentStage = when (state) {
        is VerificationState.InProgress -> state.stage
        is VerificationState.Success -> allStages.last()
        is VerificationState.Error -> null
        is VerificationState.Idle -> null
    }

    allStages.forEachIndexed { index, stage ->
        val isComplete = currentStage != null && stage.ordinal < currentStage.ordinal
        val isActive = stage == currentStage
        val isPending = !isComplete && !isActive

        StageRow(
            label = stage.name.replace("_", " "),
            status = when {
                isComplete -> "COMPLETE"
                isActive -> "PROCESSING"
                else -> "PENDING"
            },
            isComplete = isComplete,
            isActive = isActive,
            isPending = isPending
        )
        if (index < allStages.size - 1) {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun StageRow(
    label: String,
    status: String,
    isComplete: Boolean,
    isActive: Boolean,
    isPending: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f, animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "blinkAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            val icon = when {
                isComplete -> "✓"
                isActive -> "■"
                else -> "—"
            }
            Text(
                text = icon,
                style = FalcoTypography.bodyMedium,
                color = when {
                    isComplete -> LocalFalcoPalette.current.textPrimary
                    isActive -> LocalFalcoPalette.current.textPrimary.copy(alpha = blinkAlpha)
                    else -> LocalFalcoPalette.current.textGhost
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = FalcoTypography.bodySmall.copy(letterSpacing = FalcoDimens.LetterSpacingWide),
            color = if (isPending) LocalFalcoPalette.current.textGhost else LocalFalcoPalette.current.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = status,
            style = FalcoTypography.labelSmall,
            color = when {
                isComplete -> LocalFalcoPalette.current.textMuted
                isActive -> LocalFalcoPalette.current.textPrimary
                else -> LocalFalcoPalette.current.textGhost
            }
        )
    }
}

@Composable
private fun RealTimeExtractionPanel(state: VerificationState) {
    val message = when (state) {
        is VerificationState.InProgress -> state.message
        is VerificationState.Success -> null
        is VerificationState.Error -> state.message
        is VerificationState.Idle -> "Awaiting input..."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Column {
            Text("REAL-TIME_EXTRACTION", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
            Spacer(Modifier.height(12.dp))

            if (state is VerificationState.Success) {
                state.verdict.stances.take(3).forEach { stance ->
                    Text(
                        "REF: ${stance.paper.title.take(50)}",
                        style = FalcoTypography.bodySmall,
                        color = LocalFalcoPalette.current.textMuted
                    )
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                Text(
                    message ?: "Awaiting input...",
                    style = FalcoTypography.bodySmall,
                    color = if (state is VerificationState.Error) LocalFalcoPalette.current.textMuted else LocalFalcoPalette.current.textBody
                )
            }
        }
    }
}

@Composable
private fun LiveMonitor() {
    var showDebug by remember { mutableStateOf(false) }
    val isDebugEnabled = DebugLogger.isEnabled()

    val infiniteTransition = rememberInfiniteTransition(label = "monitor")
    val bars = (0..11).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f + Random.nextFloat() * 0.6f,
            targetValue = 0.2f + Random.nextFloat() * 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(280, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "bar$index"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isDebugEnabled) Modifier.clickable { showDebug = !showDebug }
                        else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LIVE MONITOR: NEURAL_LAYER_08", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
                Spacer(Modifier.weight(1f))
                if (isDebugEnabled) {
                    Text("[DEBUG]", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textMuted)
                    Spacer(Modifier.width(8.dp))
                }
                Text("■", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textPrimary)
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                bars.forEachIndexed { index, animState ->
                    val height = (animState.value * 32).dp
                    val color = when {
                        index % 4 == 0 -> LocalFalcoPalette.current.barFilled
                        index % 2 == 0 -> LocalFalcoPalette.current.textMuted
                        else -> LocalFalcoPalette.current.barEmpty.copy(alpha = 0.5f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(height)
                            .background(color, FalcoZeroShape)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row {
                Text("SIG.STRENGTH: 0.992  ", style = FalcoTypography.labelMedium, color = LocalFalcoPalette.current.textGhost)
                Text("LATENCY: 0.04MS  ", style = FalcoTypography.labelMedium, color = LocalFalcoPalette.current.textGhost)
                Text("14.2GB", style = FalcoTypography.labelMedium, color = LocalFalcoPalette.current.textGhost)
            }

            if (showDebug && isDebugEnabled) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LocalFalcoPalette.current.bg)
                        .padding(8.dp)
                ) {
                    Column {
                        Text("DEBUG PANEL", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Toggle debug mode in Settings to enable logging", style = FalcoTypography.labelMedium, color = LocalFalcoPalette.current.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text("Pipeline stages logged to logcat (tag: FALCO)", style = FalcoTypography.labelMedium, color = LocalFalcoPalette.current.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun FalcoGhostButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LocalFalcoPalette.current.bg, contentColor = LocalFalcoPalette.current.textPrimary),
        shape = FalcoZeroShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LocalFalcoPalette.current.textGhost, FalcoZeroShape)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = FalcoTypography.bodySmall)
        }
    }
}
