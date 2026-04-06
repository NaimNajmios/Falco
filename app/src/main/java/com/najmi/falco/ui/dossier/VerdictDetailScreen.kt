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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.activity.compose.BackHandler
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.ui.components.ConfidenceFactorsTooltips
import com.najmi.falco.ui.components.DebugPanel
import com.najmi.falco.ui.components.EvidenceBaseSummaryBar
import com.najmi.falco.ui.components.PaperStanceCard
import com.najmi.falco.ui.components.SearchStrategySection
import com.najmi.falco.ui.components.StanceDistributionBar
import com.najmi.falco.ui.components.VerdictFactorsPanel
import com.najmi.falco.ui.components.VerdictHeroSection
import com.najmi.falco.ui.components.ClaimAnatomyCard
import com.najmi.falco.ui.components.FalcoMetaRow
import com.najmi.falco.ui.components.ProvenanceFooter
import com.najmi.falco.ui.components.ShareBottomSheet
import com.najmi.falco.ui.components.UncertaintySection
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape

@Composable
fun VerdictDetailScreen(
    verdict: Verdict,
    onBack: () -> Unit,
    onReverify: (String) -> Unit = {},
    onShare: () -> Unit = {},
    onSave: () -> Unit = {}
) {
    val palette = LocalFalcoPalette.current
    
    var showShareSheet by remember { mutableStateOf(false) }
    var isVerdictExpanded by remember { mutableStateOf(false) }
    var isSearchStrategyExpanded by remember { mutableStateOf(false) }
    var isFactorsExpanded by remember { mutableStateOf(false) }
    var activeStanceFilter by remember { mutableStateOf<Stance?>(null) }
    var activeQueryFilter by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "← BACK",
                style = FalcoTypography.labelSmall.copy(letterSpacing = FalcoDimens.LetterSpacingWide),
                color = palette.textMuted,
                modifier = Modifier.clickable { onBack() }
            )
        }

        Spacer(Modifier.height(24.dp))

        VerdictHeroSection(
            verdict = verdict,
            isExpanded = isVerdictExpanded,
            onToggleExpand = { isVerdictExpanded = !isVerdictExpanded }
        )

        Spacer(Modifier.height(24.dp))

        if (verdict.claimAnalysis != null) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.divider, FalcoZeroShape)
                    .padding(16.dp)
            ) {
                ClaimAnatomyCard(claimAnalysis = verdict.claimAnalysis)
            }
        }

        if (verdict.factorScores.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFactorsExpanded = !isFactorsExpanded }
                    .background(palette.surface)
                    .border(1.dp, palette.divider, FalcoZeroShape)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "VERDICT FACTORS",
                            style = FalcoTypography.labelSmall,
                            color = palette.textGhost
                        )
                        Text(
                            if (isFactorsExpanded) "▼" else "▶",
                            style = FalcoTypography.labelSmall,
                            color = palette.textMuted
                        )
                    }
                    if (isFactorsExpanded) {
                        Spacer(Modifier.height(12.dp))
                        VerdictFactorsPanel(factorScores = verdict.factorScores)
                    }
                }
            }
        }

        if (verdict.retrievalSummary.totalFetched > 0 || verdict.expandedQueries.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.divider, FalcoZeroShape)
                    .padding(16.dp)
            ) {
                EvidenceBaseSummaryBar(retrievalSummary = verdict.retrievalSummary)
            }

            if (verdict.expandedQueries.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .border(1.dp, palette.divider, FalcoZeroShape)
                        .padding(16.dp)
                ) {
                    SearchStrategySection(
                        expandedQueries = verdict.expandedQueries,
                        isExpanded = isSearchStrategyExpanded,
                        onToggleExpand = { isSearchStrategyExpanded = !isSearchStrategyExpanded },
                        onQueryFilter = { activeQueryFilter = it },
                        activeQueryFilter = activeQueryFilter
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FalcoMetaRow("LATENCY", "${verdict.analysisMetadata.analysisDurationMs}ms")
            FalcoMetaRow("METADATA", "${verdict.totalPapersPassedGate} passed · ${verdict.totalPapersRetrieved} retrieved")
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

        Spacer(Modifier.height(32.dp))

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

        StanceDistributionBar(
            supportingCount = verdict.supportingCount,
            opposingCount = verdict.opposingCount,
            neutralCount = verdict.neutralCount,
            activeFilter = activeStanceFilter,
            onFilterChange = { activeStanceFilter = it }
        )

        Spacer(Modifier.height(16.dp))

        val filteredStances = remember(verdict.stances, activeStanceFilter, activeQueryFilter) {
            var filtered = verdict.stances

            if (activeStanceFilter != null) {
                filtered = filtered.filter {
                    (it.finalStance ?: it.actorStance) == activeStanceFilter
                }
            }

            if (activeQueryFilter != null) {
                filtered = filtered.filter { true }
            }

            filtered.sortedByDescending { it.confidence }
        }

        if (filteredStances.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.chip, FalcoZeroShape)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No papers match this filter",
                    style = FalcoTypography.bodyMedium,
                    color = palette.textMuted
                )
            }
        } else {
            filteredStances.forEach { stance ->
                var isExpanded by remember { mutableStateOf(false) }
                PaperStanceCard(
                    paperStance = stance,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded }
                )
                Spacer(Modifier.height(12.dp))
            }
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

        DebugPanel(
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
                label = "RE-VERIFY",
                onClick = { onReverify(verdict.claim) },
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