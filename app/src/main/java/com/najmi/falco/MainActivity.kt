package com.najmi.falco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.falco.ui.dossier.DossierScreen
import com.najmi.falco.ui.dossier.DossierViewModel
import com.najmi.falco.ui.dossier.VerdictDetailScreen
import com.najmi.falco.ui.hypothesis.HypothesisScreen
import com.najmi.falco.ui.hypothesis.HypothesisViewModel
import com.najmi.falco.ui.navigation.FalcoBottomNav
import com.najmi.falco.ui.navigation.FalcoTab
import com.najmi.falco.ui.pipeline.PipelineScreen
import com.najmi.falco.ui.settings.SettingsScreen
import com.najmi.falco.ui.settings.SettingsViewModel
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTheme
import com.najmi.falco.ui.theme.FalcoTypography
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            FalcoTheme(isDarkMode = settingsState.preferences.isDarkMode) {
                FalcoApp()
            }
        }
    }
}

@Composable
private fun FalcoApp() {
    val hypothesisViewModel: HypothesisViewModel = hiltViewModel()
    val dossierViewModel: DossierViewModel = hiltViewModel()
    val dossierState by dossierViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember {
        mutableStateOf(FalcoTab.Hypothesis)
    }

    var showSettings by remember { mutableStateOf(false) }

    val enabledTabs = setOf(FalcoTab.Hypothesis, FalcoTab.Pipeline, FalcoTab.Dossier, FalcoTab.Settings)

    Column(modifier = Modifier.fillMaxSize().background(LocalFalcoPalette.current.bg)) {
        FalcoHeader(modifier = Modifier.statusBarsPadding())
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            if (showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalFalcoPalette.current.bg)
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SETTINGS", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
                        Text("← BACK", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textMuted, modifier = Modifier.clickable { showSettings = false })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        com.najmi.falco.ui.settings.SettingsScreen()
                    }
                }
            } else {
                when (selectedTab) {
                    FalcoTab.Hypothesis -> {
                        HypothesisScreen(
                            viewModel = hypothesisViewModel,
                            onNavigateToPipeline = { selectedTab = FalcoTab.Pipeline },
                            onNavigateToSettings = { showSettings = true }
                        )
                    }
                    FalcoTab.Pipeline -> {
                        PipelineScreen(
                            hypothesisViewModel = hypothesisViewModel,
                            onNewClaim = {
                                hypothesisViewModel.reset()
                                selectedTab = FalcoTab.Hypothesis
                            },
                            onCancel = {
                                hypothesisViewModel.cancelVerification()
                                selectedTab = FalcoTab.Hypothesis
                            }
                        )
                    }
                    FalcoTab.Dossier -> {
                        if (dossierState.selectedVerdict != null) {
                            val verdict = dossierState.selectedVerdict!!
                            VerdictDetailScreen(
                                verdict = verdict,
                                onBack = { dossierViewModel.clearSelection() },
                                onReverify = { claimText ->
                                    hypothesisViewModel.setClaimText(claimText)
                                    dossierViewModel.clearSelection()
                                    selectedTab = FalcoTab.Hypothesis
                                },
                                onShare = {
                                    val shareText = buildString {
                                        appendLine("FALCO Verdict")
                                        appendLine("═".repeat(30))
                                        appendLine("Claim: ${verdict.claim}")
                                        appendLine()
                                        appendLine("VERDICT: ${verdict.lean.name}")
                                        appendLine("Confidence: ${(verdict.confidence * 100).toInt()}%")
                                        appendLine()
                                        appendLine("Summary: ${verdict.summary}")
                                        appendLine()
                                        appendLine("Evidence: ${verdict.stances.size} papers")
                                        appendLine("  - Supporting: ${verdict.supportingCount}")
                                        appendLine("  - Opposing: ${verdict.opposingCount}")
                                        appendLine("  - Neutral: ${verdict.neutralCount}")
                                        if (verdict.caveat != null) {
                                            appendLine()
                                            appendLine("Caveat: ${verdict.caveat}")
                                        }
                                        appendLine()
                                        appendLine("Source ID: ${verdict.claimId}")
                                    }
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Verdict")
                                    context.startActivity(shareIntent)
                                },
                                onSave = {
                                    dossierViewModel.clearSelection()
                                    selectedTab = FalcoTab.Dossier
                                }
                            )
                        } else {
                            DossierScreen(
                                viewModel = dossierViewModel,
                                onVerdictSelected = { claimId ->
                                    dossierViewModel.selectVerdict(claimId)
                                }
                            )
                        }
                    }
                    FalcoTab.Settings -> {
                        SettingsScreen()
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LocalFalcoPalette.current.divider))
        FalcoBottomNav(
            modifier = Modifier.navigationBarsPadding(),
            selectedTab = selectedTab,
            onTabSelected = { tab -> selectedTab = tab },
            enabledTabs = enabledTabs
        )
    }
}

@Composable
private fun FalcoHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "[□] FALCO",
                style = FalcoTypography.bodySmall.copy(
                    letterSpacing = FalcoDimens.LetterSpacingHeadline,
                    fontWeight = FontWeight.Medium
                ),
                color = LocalFalcoPalette.current.textPrimary
            )
            Text(
                "v1.0.0_STABLE",
                style = FalcoTypography.labelSmall,
                color = LocalFalcoPalette.current.textGhost
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LocalFalcoPalette.current.divider))
}
