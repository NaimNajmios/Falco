package com.najmi.falco.ui.hypothesis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape

@Composable
fun HypothesisScreen(
    viewModel: HypothesisViewModel,
    onNavigateToPipeline: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val state by viewModel.verificationState.collectAsState()
    val inputText by viewModel.claimText.collectAsState()
    val isInProgress = state is com.najmi.falco.domain.model.VerificationState.InProgress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalFalcoPalette.current.bg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Formalize your\nInquiry",
                style = FalcoTypography.headlineLarge,
                color = LocalFalcoPalette.current.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onNavigateToSettings)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚙",
                    style = FalcoTypography.headlineSmall,
                    color = LocalFalcoPalette.current.textGhost
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, LocalFalcoPalette.current.textMuted, FalcoZeroShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    if (inputText.isNotBlank()) "TYPE: CLASSIFYING..." else "TYPE: AWAITING INPUT",
                    style = FalcoTypography.labelSmall,
                    color = LocalFalcoPalette.current.textMuted
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .padding(start = 12.dp)
                    .background(LocalFalcoPalette.current.divider)
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { viewModel.onTextChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            placeholder = {
                Text(
                    "Enter the parameters of your inquiry here...",
                    color = LocalFalcoPalette.current.textGhost,
                    style = FalcoTypography.bodyMedium
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = LocalFalcoPalette.current.textBody,
                unfocusedTextColor = LocalFalcoPalette.current.textBody,
                cursorColor = LocalFalcoPalette.current.textPrimary,
                focusedBorderColor = LocalFalcoPalette.current.textMuted,
                unfocusedBorderColor = LocalFalcoPalette.current.divider
            ),
            textStyle = FalcoTypography.bodyMedium,
            enabled = !isInProgress
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "INPUT IS BEING CROSS-REFERENCED AGAINST GLOBAL\n" +
            "DOSSIERS IN REAL-TIME. ENSURE TECHNICAL\n" +
            "NOMENCLATURE IS PRECISE FOR OPTIMAL VERIFICATION.",
            style = FalcoTypography.labelSmall,
            color = LocalFalcoPalette.current.textGhost,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.verify(inputText)
                onNavigateToPipeline()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = inputText.isNotBlank() && !isInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalFalcoPalette.current.textPrimary,
                contentColor = LocalFalcoPalette.current.bg,
                disabledContainerColor = LocalFalcoPalette.current.textGhost,
                disabledContentColor = LocalFalcoPalette.current.bg
            ),
            shape = FalcoZeroShape
        ) {
            Text("VERIFY WITH EVIDENCE", style = FalcoTypography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))

        val recentClaimsList by viewModel.recentClaims.collectAsState()

        if (recentClaimsList.isNotEmpty()) {
            Text("RECENT HYPOTHESES", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
            Spacer(Modifier.height(12.dp))

            recentClaimsList.take(5).forEach { claim ->
                val status = when {
                    claim.confidence == null -> "PENDING"
                    claim.confidence >= 0.7f -> "VERIFIED"
                    else -> "NEW"
                }
                RecentHypothesisRow(name = claim.text.take(40).replace(" ", "_"), status = status)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun RecentHypothesisRow(name: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                2.dp,
                when (status) {
                    "VERIFIED" -> LocalFalcoPalette.current.textPrimary
                    "PENDING" -> LocalFalcoPalette.current.textMuted
                    else -> LocalFalcoPalette.current.textGhost
                },
                FalcoZeroShape
            )
            .padding(14.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textBody)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.border(1.dp, LocalFalcoPalette.current.textGhost, FalcoZeroShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(status, style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
        }
    }
}
