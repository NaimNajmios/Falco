package com.najmi.falco.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.najmi.falco.domain.model.ExpandedQuery
import com.najmi.falco.domain.model.FailedSource
import com.najmi.falco.domain.model.FailureReason
import com.najmi.falco.domain.model.PaperSource
import com.najmi.falco.domain.model.QueryIntent
import com.najmi.falco.domain.model.RetrievalSummary
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette

@Composable
fun EvidenceBaseSummaryBar(
    retrievalSummary: RetrievalSummary,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "EVIDENCE BASE",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = "Total",
                value = "${retrievalSummary.totalFetched}",
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "OpenAlex",
                value = "${retrievalSummary.openAlexCount}",
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Semantic",
                value = "${retrievalSummary.semanticScholarCount}",
                modifier = Modifier.weight(1f)
            )

            if (retrievalSummary.excludedCount > 0) {
                StatItem(
                    label = "Excluded",
                    value = "${retrievalSummary.excludedCount}",
                    modifier = Modifier.weight(1f),
                    valueColor = palette.errorIndicator
                )
            }
        }

        if (retrievalSummary.evidenceQualityScore > 0) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Quality:",
                    style = FalcoTypography.labelSmall,
                    color = palette.textMuted
                )
                Spacer(Modifier.width(8.dp))
                EvidenceQualityBadge(score = retrievalSummary.evidenceQualityScore)
            }
        }

        if (retrievalSummary.failedSources.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FailedSourcesRow(failedSources = retrievalSummary.failedSources)
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color? = null
) {
    val palette = LocalFalcoPalette.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = FalcoTypography.headlineMedium,
            color = valueColor ?: palette.textPrimary
        )
        Text(
            label,
            style = FalcoTypography.labelSmall,
            color = palette.textMuted
        )
    }
}

@Composable
fun EvidenceQualityBadge(score: Float) {
    val palette = LocalFalcoPalette.current

    val grade = when {
        score >= 0.8f -> "A"
        score >= 0.6f -> "B"
        score >= 0.4f -> "C"
        score >= 0.2f -> "D"
        else -> "F"
    }

    val color = when (grade) {
        "A" -> palette.stanceSupports
        "B" -> palette.stanceSupports.copy(alpha = 0.7f)
        "C" -> palette.textMuted
        "D", "F" -> palette.errorIndicator
        else -> palette.textMuted
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), FalcoZeroShape)
            .border(1.dp, color, FalcoZeroShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "Grade: $grade",
            style = FalcoTypography.labelSmall,
            color = color
        )
    }
}

@Composable
fun FailedSourcesRow(
    failedSources: List<FailedSource>,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Column(modifier = modifier.fillMaxWidth()) {
        failedSources.forEach { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.errorIndicator.copy(alpha = 0.1f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    modifier = Modifier.size(16.dp),
                    tint = palette.errorIndicator
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${source.name} failed: ${source.reason.displayName()}",
                    style = FalcoTypography.bodySmall,
                    color = palette.errorIndicator
                )
            }
        }
    }
}

@Composable
fun FailureReason.displayName(): String = when (this) {
    FailureReason.TIMEOUT -> "Timeout"
    FailureReason.RATE_LIMITED -> "Rate limited"
    FailureReason.NO_RESULTS -> "No results"
    FailureReason.UNKNOWN_ERROR -> "Unknown error"
}

@Composable
fun SearchStrategySection(
    expandedQueries: List<ExpandedQuery>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onQueryFilter: (String?) -> Unit,
    activeQueryFilter: String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val totalQueries = expandedQueries.size
    val queriesWithResults = expandedQueries.count { it.resultsFound > 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SEARCH STRATEGY",
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$queriesWithResults of $totalQueries queries returned results",
                        style = FalcoTypography.bodySmall,
                        color = palette.textMuted
                    )
                }

                Text(
                    if (isExpanded) "▼" else "▶",
                    style = FalcoTypography.labelSmall,
                    color = palette.textMuted
                )
            }
        }

        if (isExpanded && expandedQueries.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))

            QueryCoverageIndicator(
                total = totalQueries,
                withResults = queriesWithResults
            )

            Spacer(Modifier.height(16.dp))

            expandedQueries.forEach { query ->
                QueryChip(
                    expandedQuery = query,
                    isSelected = activeQueryFilter == query.text,
                    onClick = { onQueryFilter(if (activeQueryFilter == query.text) null else query.text) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun QueryCoverageIndicator(
    total: Int,
    withResults: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current
    val coverage = if (total > 0) withResults.toFloat() / total else 0f

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(
                        if (index < withResults) palette.stanceSupports else palette.barEmpty,
                        FalcoZeroShape
                    )
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Coverage: ${(coverage * 100).toInt()}%",
            style = FalcoTypography.labelSmall,
            color = palette.textMuted
        )
    }
}

@Composable
fun QueryChip(
    expandedQuery: ExpandedQuery,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    val intentLabel = when (expandedQuery.intent) {
        QueryIntent.BROAD -> "BROAD"
        QueryIntent.NARROW -> "NARROW"
        QueryIntent.CONTRASTIVE -> "CONTRAST"
    }

    val intentColor = when (expandedQuery.intent) {
        QueryIntent.BROAD -> palette.textMuted
        QueryIntent.NARROW -> palette.stanceSupports
        QueryIntent.CONTRASTIVE -> palette.stanceOpposes
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(
                1.dp,
                if (isSelected) palette.textPrimary else palette.chip,
                FalcoZeroShape
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(intentColor.copy(alpha = 0.2f), FalcoZeroShape)
                .border(1.dp, intentColor, FalcoZeroShape)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                intentLabel,
                style = FalcoTypography.labelSmall,
                color = intentColor
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                expandedQuery.text,
                style = FalcoTypography.bodySmall,
                color = palette.textBody,
                maxLines = 2
            )
        }

        Spacer(Modifier.width(8.dp))

        if (expandedQuery.resultsFound > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Has results",
                    modifier = Modifier.size(14.dp),
                    tint = palette.stanceSupports
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${expandedQuery.resultsFound}",
                    style = FalcoTypography.labelSmall,
                    color = palette.stanceSupports
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "No results",
                modifier = Modifier.size(14.dp),
                tint = palette.textMuted
            )
        }
    }
}