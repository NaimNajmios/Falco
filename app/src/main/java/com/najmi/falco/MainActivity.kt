package com.najmi.falco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.ui.dossier.DossierScreen
import com.najmi.falco.ui.hypothesis.HypothesisScreen
import com.najmi.falco.ui.hypothesis.HypothesisViewModel
import com.najmi.falco.ui.navigation.FalcoBottomNav
import com.najmi.falco.ui.navigation.FalcoTab
import com.najmi.falco.ui.pipeline.PipelineScreen
import com.najmi.falco.ui.settings.SettingsScreen
import com.najmi.falco.ui.theme.FalcoBg
import com.najmi.falco.ui.theme.FalcoDivider
import com.najmi.falco.ui.theme.FalcoTextGhost
import com.najmi.falco.ui.theme.FalcoTextPrimary
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FalcoTheme {
                FalcoApp()
            }
        }
    }
}

@Composable
private fun FalcoApp() {
    val hypothesisViewModel: HypothesisViewModel = hiltViewModel()
    val verificationState by hypothesisViewModel.verificationState.collectAsState()

    var selectedTab by remember {
        mutableStateOf(FalcoTab.Hypothesis)
    }

    LaunchedEffect(verificationState) {
        when (verificationState) {
            is VerificationState.InProgress -> {
                if (selectedTab != FalcoTab.Settings) {
                    selectedTab = FalcoTab.Pipeline
                }
            }
            is VerificationState.Success -> {
                if (selectedTab == FalcoTab.Pipeline) {
                    selectedTab = FalcoTab.Dossier
                }
            }
            is VerificationState.Error -> {
                if (selectedTab == FalcoTab.Pipeline) {
                    selectedTab = FalcoTab.Dossier
                }
            }
            is VerificationState.Idle -> {
            }
        }
    }

    val enabledTabs = remember(verificationState) {
        when (verificationState) {
            is VerificationState.InProgress -> setOf(FalcoTab.Hypothesis, FalcoTab.Pipeline, FalcoTab.Settings)
            else -> setOf(FalcoTab.Hypothesis, FalcoTab.Dossier, FalcoTab.Settings)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(FalcoBg)) {
        FalcoHeader()
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (selectedTab) {
                FalcoTab.Hypothesis -> {
                    HypothesisScreen(
                        viewModel = hypothesisViewModel,
                        onNavigateToPipeline = { selectedTab = FalcoTab.Pipeline }
                    )
                }
                FalcoTab.Pipeline -> {
                    PipelineScreen()
                }
                FalcoTab.Dossier -> {
                    DossierScreen(
                        viewModel = hypothesisViewModel,
                        onNewClaim = {
                            hypothesisViewModel.reset()
                            selectedTab = FalcoTab.Hypothesis
                        }
                    )
                }
                FalcoTab.Settings -> {
                    SettingsScreen()
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FalcoDivider))
        FalcoBottomNav(
            selectedTab = selectedTab,
            onTabSelected = { tab -> selectedTab = tab },
            enabledTabs = enabledTabs
        )
    }
}

@Composable
private fun FalcoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "[□] FALCO",
                style = FalcoTypography.bodySmall.copy(
                    letterSpacing = androidx.compose.ui.unit.TextUnit(2.5f, androidx.compose.ui.unit.TextUnitType.Sp),
                    fontWeight = FontWeight.Medium
                ),
                color = FalcoTextPrimary
            )
            Text(
                "v1.0.0_STABLE",
                style = FalcoTypography.labelSmall,
                color = FalcoTextGhost
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FalcoDivider))
}
