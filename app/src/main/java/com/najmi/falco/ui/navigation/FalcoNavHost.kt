package com.najmi.falco.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.najmi.falco.ui.dossier.DossierScreen
import com.najmi.falco.ui.hypothesis.HypothesisScreen
import com.najmi.falco.ui.pipeline.PipelineScreen

@Composable
fun FalcoNavHost(
    navController: NavHostController = rememberNavController(),
    selectedTab: FalcoTab,
    onTabSelected: (FalcoTab) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = selectedTab.route
    ) {
        composable(FalcoTab.Hypothesis.route) {
            HypothesisScreen(
                onNavigateToPipeline = { onTabSelected(FalcoTab.Pipeline) }
            )
        }
        composable(FalcoTab.Pipeline.route) {
            PipelineScreen()
        }
        composable(FalcoTab.Dossier.route) {
            DossierScreen(
                onNewClaim = { onTabSelected(FalcoTab.Hypothesis) }
            )
        }
    }
}
