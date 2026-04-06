package com.najmi.falco.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.najmi.falco.domain.model.PaperSource
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette
import kotlin.math.roundToInt

@Composable
fun StanceDistributionBar(
    supportingCount: Int,
    opposingCount: Int,
    neutralCount: Int,
    activeFilter: Stance?,
    onFilterChange: (Stance?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val total = (supportingCount + opposingCount + neutralCount).coerceAtLeast(1)

    val supportWidth by animateFloatAsState(
        targetValue = supportingCount.toFloat() / total,
        label = "supportWidth"
    )
    val opposeWidth by animateFloatAsState(
        targetValue = opposingCount.toFloat() / total,
        label = "opposeWidth"
    )
    val neutralWidth by animateFloatAsState(
        targetValue = neutralCount.toFloat() / total,
        label = "neutralWidth"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "EVIDENCE DISTRIBUTION",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onFilterChange(null) }
        ) {
            if (supportWidth > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(supportWidth)
                        .height(32.dp)
                        .background(
                            if (activeFilter == Stance.SUPPORTS) palette.textPrimary else palette.textGhost.copy(alpha = 0.5f),
                            RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                        )
                        .clickable(onClick = { onFilterChange(Stance.SUPPORTS) })
                )
            }
            if (opposeWidth > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(opposeWidth)
                        .height(32.dp)
                        .background(
                            if (activeFilter == Stance.OPPOSES) palette.textPrimary else palette.textGhost.copy(alpha = 0.5f),
                            RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                        .clickable(onClick = { onFilterChange(Stance.OPPOSES) })
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StanceCountLabel(
                count = supportingCount,
                label = "Support",
                isActive = activeFilter == Stance.SUPPORTS,
                onClick = { onFilterChange(if (activeFilter == Stance.SUPPORTS) null else Stance.SUPPORTS) }
            )
            StanceCountLabel(
                count = neutralCount,
                label = "Neutral",
                isActive = activeFilter == Stance.NEUTRAL,
                onClick = { onFilterChange(if (activeFilter == Stance.NEUTRAL) null else Stance.NEUTRAL) }
            )
            StanceCountLabel(
                count = opposingCount,
                label = "Refute",
                isActive = activeFilter == Stance.OPPOSES,
                onClick = { onFilterChange(if (activeFilter == Stance.OPPOSES) null else Stance.OPPOSES) }
            )
        }
    }
}

@Composable
private fun StanceBarSegment(
    widthFraction: Float,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalFalcoPalette.current
    val backgroundColor = if (isActive) palette.textPrimary else palette.textGhost.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .height(32.dp)
            .background(backgroundColor, RoundedCornerShape(topStart = if (widthFraction == 1f) 4.dp else 0.dp, bottomStart = if (widthFraction == 1f) 4.dp else 0.dp, topEnd = if (widthFraction == 1f) 4.dp else 0.dp, bottomEnd = if (widthFraction == 1f) 4.dp else 0.dp))
            .clickable(onClick = onClick)
    )
}

@Composable
private fun StanceCountLabel(
    count: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalFalcoPalette.current

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    when (label) {
                        "Support" -> palette.stanceSupports
                        "Refute" -> palette.stanceOpposes
                        else -> palette.stanceNeutral
                    },
                    CircleShape
                )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "$count $label",
            style = FalcoTypography.labelSmall,
            color = if (isActive) palette.textPrimary else palette.textMuted
        )
    }
}

@Composable
fun PaperStanceCard(
    paperStance: PaperStance,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val stanceColor = when (paperStance.finalStance ?: paperStance.actorStance) {
        Stance.SUPPORTS -> palette.stanceSupports
        Stance.NEUTRAL -> palette.stanceNeutral
        Stance.OPPOSES -> palette.stanceOpposes
        Stance.INSUFFICIENT_EVIDENCE -> palette.textMuted
    }

    val stanceLabel = when (paperStance.finalStance ?: paperStance.actorStance) {
        Stance.SUPPORTS -> "SUPPORTS"
        Stance.NEUTRAL -> "NEUTRAL"
        Stance.OPPOSES -> "REFUTES"
        Stance.INSUFFICIENT_EVIDENCE -> "INSUFFICIENT"
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                StanceChip(label = stanceLabel, color = stanceColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${(paperStance.confidence * 100).roundToInt()}%",
                    style = FalcoTypography.labelSmall,
                    color = palette.textMuted
                )
                Spacer(Modifier.width(4.dp))
                ConfidenceDots(confidence = paperStance.confidence)
            }

            Text(
                if (expanded) "▼" else "▶",
                style = FalcoTypography.labelSmall,
                color = palette.textMuted
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            paperStance.paper.title,
            style = FalcoTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        val authors = paperStance.paper.authors.take(2).joinToString(", ")
        val year = paperStance.paper.year?.toString() ?: "Unknown"
        val source = when (paperStance.paper.source) {
            PaperSource.SEMANTIC_SCHOLAR -> "Semantic Scholar"
            PaperSource.OPEN_ALEX -> "OpenAlex"
        }
        val openAccess = if (paperStance.paper.isOpenAccess) "· OA" else ""

        Text(
            "$authors ($year) · $source$openAccess",
            style = FalcoTypography.bodySmall,
            color = palette.textMuted
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))

                if (paperStance.supportingExcerpt != null) {
                    Text(
                        "Supporting excerpt:",
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.bg)
                            .padding(12.dp)
                    ) {
                        Text(
                            paperStance.supportingExcerpt,
                            style = FalcoTypography.bodySmall,
                            color = palette.textBody
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (paperStance.keyEvidence.isNotBlank()) {
                    Text(
                        "Key evidence:",
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        paperStance.keyEvidence,
                        style = FalcoTypography.bodySmall,
                        color = palette.textBody,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                }

                paperStance.paper.url?.let { url ->
                    Row(
                        modifier = Modifier
                            .clickable { /* Open URL - to be implemented */ }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "View paper →",
                            style = FalcoTypography.labelSmall,
                            color = palette.textMuted
                        )
                    }
                }

                if (paperStance.criticChallenge != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.errorIndicator.copy(alpha = 0.1f))
                            .border(1.dp, palette.errorIndicator.copy(alpha = 0.3f), FalcoZeroShape)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Critic note:",
                                style = FalcoTypography.labelSmall,
                                color = palette.errorIndicator
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                paperStance.criticChallenge,
                                style = FalcoTypography.bodySmall,
                                color = palette.textBody
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StanceChip(label: String, color: Color) {
    val palette = LocalFalcoPalette.current

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), FalcoZeroShape)
            .border(1.dp, color, FalcoZeroShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = FalcoTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun ConfidenceDots(confidence: Float) {
    val palette = LocalFalcoPalette.current
    val filled = ((confidence * 5).roundToInt()).coerceIn(0, 5)

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (i < filled) palette.barFilled else palette.barEmpty,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun ConfidenceHistogram(
    confidenceScores: List<Float>,
    modifier: Modifier = Modifier
) {
    if (confidenceScores.isEmpty()) return

    val palette = LocalFalcoPalette.current

    val buckets = MutableList(5) { 0 }
    confidenceScores.forEach { score ->
        val bucket = ((score * 5).toInt()).coerceIn(0, 4)
        buckets[bucket] = buckets[bucket] + 1
    }

    val maxCount = buckets.maxOrNull() ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "CONFIDENCE DISTRIBUTION",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            buckets.forEachIndexed { index, count ->
                val heightFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((40 * heightFraction).dp.coerceAtLeast(4.dp))
                            .background(palette.barFilled, FalcoZeroShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0%", style = FalcoTypography.labelSmall, color = palette.textMuted)
            Text("100%", style = FalcoTypography.labelSmall, color = palette.textMuted)
        }
    }
}