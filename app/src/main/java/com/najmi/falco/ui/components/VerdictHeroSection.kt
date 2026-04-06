package com.najmi.falco.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.falco.domain.model.CertaintyLevel
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.VerdictLabel
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette
import kotlin.math.roundToInt

@Composable
fun VerdictHeroSection(
    verdict: Verdict,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val stanceColor = when (verdict.lean) {
        Stance.SUPPORTS -> palette.stanceSupports
        Stance.NEUTRAL -> palette.stanceNeutral
        Stance.OPPOSES -> palette.stanceOpposes
        Stance.INSUFFICIENT_EVIDENCE -> palette.textMuted
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConfidenceArc(
            confidence = verdict.confidence,
            stance = verdict.lean,
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = verdict.lean.name,
            style = FalcoTypography.displayLarge,
            color = stanceColor
        )

        Spacer(Modifier.height(8.dp))

        val certaintyLabel = when (verdict.certaintyLevel) {
            CertaintyLevel.HIGH -> "HIGH CONFIDENCE"
            CertaintyLevel.MODERATE -> "MODERATE CONFIDENCE"
            CertaintyLevel.LOW -> "LOW CONFIDENCE — Treat with caution"
        }
        Text(
            text = certaintyLabel,
            style = FalcoTypography.labelSmall,
            color = palette.textMuted
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Based on ${verdict.totalPapersPassedGate} peer-reviewed papers",
            style = FalcoTypography.labelSmall.copy(letterSpacing = FalcoDimens.LetterSpacingMeta),
            color = palette.textGhost
        )

        if (verdict.verdictNarrative.isNotBlank()) {
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.divider, FalcoZeroShape)
                    .clickable { onToggleExpand() }
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SUMMARY",
                            style = FalcoTypography.labelSmall,
                            color = palette.textGhost
                        )
                        Text(
                            if (isExpanded) "▼" else "▶",
                            style = FalcoTypography.labelSmall,
                            color = palette.textMuted
                        )
                    }

                    if (isExpanded) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            verdict.verdictNarrative,
                            style = FalcoTypography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Normal
                            ),
                            color = palette.textBody
                        )
                    }
                }
            }
        }

        if (verdict.conflictDetected) {
            Spacer(Modifier.height(16.dp))
            ConflictBanner()
        }
    }
}

@Composable
fun ConfidenceArc(
    confidence: Float,
    stance: Stance,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(confidence) {
        animatedProgress.animateTo(
            targetValue = confidence,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val arcColor = when (stance) {
        Stance.SUPPORTS -> palette.stanceSupports
        Stance.NEUTRAL -> palette.stanceNeutral
        Stance.OPPOSES -> palette.stanceOpposes
        Stance.INSUFFICIENT_EVIDENCE -> palette.textMuted
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            drawArc(
                color = palette.barEmpty,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress.value,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(confidence * 100).roundToInt()}%",
                style = FalcoTypography.headlineMedium,
                color = palette.textPrimary
            )
        }
    }
}

@Composable
fun ConflictBanner(modifier: Modifier = Modifier) {
    val palette = LocalFalcoPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surfaceBorder.copy(alpha = 0.3f))
            .border(1.dp, palette.errorIndicator, FalcoZeroShape)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                "⚡",
                style = FalcoTypography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Scientific Conflict Detected — Evidence is divided on this claim.",
                style = FalcoTypography.bodySmall,
                color = palette.textPrimary
            )
        }
    }
}

@Composable
fun ClaimAnatomyCard(
    claimAnalysis: com.najmi.falco.domain.model.ClaimAnalysis?,
    modifier: Modifier = Modifier
) {
    if (claimAnalysis == null) return

    val palette = LocalFalcoPalette.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            "CLAIM ANATOMY",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClaimTypeBadge(claimType = claimAnalysis.claimType)

            if (claimAnalysis.confidence < 0.7f) {
                AmbiguityIndicator(confidence = claimAnalysis.confidence)
            }
        }

        Spacer(Modifier.height(12.dp))

        ConfidenceSegmentBar(confidence = claimAnalysis.confidence)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Confidence: ${(claimAnalysis.confidence * 100).roundToInt()}%",
                style = FalcoTypography.labelSmall,
                color = palette.textMuted
            )

            if (claimAnalysis.isAmbiguous) {
                Text(
                    "Ambiguous claim",
                    style = FalcoTypography.labelSmall,
                    color = palette.errorIndicator
                )
            }
        }

        if (claimAnalysis.isAmbiguous && claimAnalysis.ambiguityReason != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                claimAnalysis.ambiguityReason,
                style = FalcoTypography.bodySmall,
                color = palette.textMuted
            )
        }

        if (claimAnalysis.subClaims.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Sub-claims detected:",
                style = FalcoTypography.labelSmall,
                color = palette.textGhost
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                claimAnalysis.subClaims.take(3).forEach { subClaim ->
                    SubClaimChip(text = subClaim)
                }
            }

            if (claimAnalysis.subClaims.size > 3) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "+${claimAnalysis.subClaims.size - 3} more",
                    style = FalcoTypography.labelSmall,
                    color = palette.textMuted
                )
            }
        }

        if (claimAnalysis.restatedClaim.isNotBlank() && claimAnalysis.restatedClaim != claimAnalysis.restatedClaim) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.divider, FalcoZeroShape)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        "Normalized claim:",
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        claimAnalysis.restatedClaim,
                        style = FalcoTypography.bodySmall,
                        color = palette.textBody
                    )
                }
            }
        }
    }
}

@Composable
fun ClaimTypeBadge(claimType: com.najmi.falco.domain.model.ClaimType) {
    val palette = LocalFalcoPalette.current

    val label = when (claimType) {
        com.najmi.falco.domain.model.ClaimType.EMPIRICAL -> "EMPIRICAL"
        com.najmi.falco.domain.model.ClaimType.COMPARATIVE -> "COMPARATIVE"
        com.najmi.falco.domain.model.ClaimType.CAUSAL -> "CAUSAL"
        com.najmi.falco.domain.model.ClaimType.DEFINITIONAL -> "DEFINITIONAL"
        com.najmi.falco.domain.model.ClaimType.STATISTICAL -> "STATISTICAL"
        com.najmi.falco.domain.model.ClaimType.SCIENTIFIC -> "SCIENTIFIC"
        com.najmi.falco.domain.model.ClaimType.CURRENT_EVENT -> "CURRENT EVENT"
        com.najmi.falco.domain.model.ClaimType.PERSON_FACT -> "PERSON FACT"
        com.najmi.falco.domain.model.ClaimType.QUOTE -> "QUOTE"
        com.najmi.falco.domain.model.ClaimType.GENERAL -> "GENERAL"
    }

    Box(
        modifier = Modifier
            .background(palette.surface, FalcoZeroShape)
            .border(1.dp, palette.textGhost, FalcoZeroShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = FalcoTypography.labelSmall,
            color = palette.textPrimary
        )
    }
}

@Composable
fun AmbiguityIndicator(confidence: Float) {
    val palette = LocalFalcoPalette.current

    Box(
        modifier = Modifier
            .background(palette.errorIndicator.copy(alpha = 0.15f), FalcoZeroShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "⚠ Low confidence",
            style = FalcoTypography.labelSmall,
            color = palette.errorIndicator
        )
    }
}

@Composable
fun SubClaimChip(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val palette = LocalFalcoPalette.current

    Box(
        modifier = Modifier
            .background(
                if (isSelected) palette.surface else palette.surface,
                FalcoZeroShape
            )
            .border(
                1.dp,
                if (isSelected) palette.textPrimary else palette.chip,
                FalcoZeroShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text.take(30) + if (text.length > 30) "..." else "",
            style = FalcoTypography.labelSmall,
            color = palette.textMuted
        )
    }
}

@Composable
fun VerdictFactorsPanel(
    factorScores: Map<com.najmi.falco.domain.model.VerdictFactor, Float>,
    modifier: Modifier = Modifier
) {
    if (factorScores.isEmpty()) return

    val palette = LocalFalcoPalette.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            "VERDICT BREAKDOWN",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(12.dp))

        factorScores.forEach { (factor, score) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (factor) {
                        com.najmi.falco.domain.model.VerdictFactor.EVIDENCE_VOLUME -> "Evidence Volume"
                        com.najmi.falco.domain.model.VerdictFactor.SOURCE_DIVERSITY -> "Source Diversity"
                        com.najmi.falco.domain.model.VerdictFactor.CONSENSUS_STRENGTH -> "Consensus Strength"
                        com.najmi.falco.domain.model.VerdictFactor.EVIDENCE_RECENCY -> "Evidence Recency"
                    },
                    style = FalcoTypography.bodySmall,
                    color = palette.textBody
                )

                Text(
                    "${(score * 10).roundToInt()} / 10",
                    style = FalcoTypography.bodySmall,
                    color = palette.textMuted
                )
            }

            Spacer(Modifier.height(4.dp))

            LinearConfidenceBar(score = score)
        }

        val overallScore = if (factorScores.isNotEmpty()) {
            factorScores.values.average().toFloat()
        } else 0f

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Overall",
                style = FalcoTypography.labelSmall,
                color = palette.textPrimary
            )

            Text(
                "${(overallScore * 10).roundToInt()} / 10",
                style = FalcoTypography.labelSmall,
                color = palette.textPrimary
            )
        }

        Spacer(Modifier.height(4.dp))

        LinearConfidenceBar(score = overallScore, filledColor = palette.textPrimary)
    }
}

@Composable
fun LinearConfidenceBar(
    score: Float,
    modifier: Modifier = Modifier,
    filledColor: androidx.compose.ui.graphics.Color? = null
) {
    val palette = LocalFalcoPalette.current
    val color = filledColor ?: palette.barFilled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(palette.barEmpty, FalcoZeroShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(score.coerceIn(0f, 1f))
                .height(4.dp)
                .background(color, FalcoZeroShape)
        )
    }
}