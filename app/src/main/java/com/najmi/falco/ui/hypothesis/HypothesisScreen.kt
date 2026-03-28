package com.najmi.falco.ui.hypothesis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.falco.ui.theme.FalcoBg
import com.najmi.falco.ui.theme.FalcoDivider
import com.najmi.falco.ui.theme.FalcoTextBody
import com.najmi.falco.ui.theme.FalcoTextGhost
import com.najmi.falco.ui.theme.FalcoTextMuted
import com.najmi.falco.ui.theme.FalcoTextPrimary
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape

@Composable
fun HypothesisScreen(
    viewModel: HypothesisViewModel = hiltViewModel(),
    onNavigateToPipeline: () -> Unit
) {
    val state by viewModel.verificationState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FalcoBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "Formalize your\nInquiry",
            style = FalcoTypography.headlineLarge,
            color = FalcoTextPrimary
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, FalcoTextMuted, FalcoZeroShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "DETECTED TYPE: EMPIRICAL",
                    style = FalcoTypography.labelSmall,
                    color = FalcoTextMuted
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .padding(start = 12.dp)
                    .background(FalcoDivider)
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            placeholder = {
                Text(
                    "Enter the parameters of your inquiry here...",
                    color = FalcoTextGhost,
                    style = FalcoTypography.bodyMedium
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = FalcoTextBody,
                unfocusedTextColor = FalcoTextBody,
                cursorColor = FalcoTextPrimary,
                focusedBorderColor = FalcoTextMuted,
                unfocusedBorderColor = FalcoDivider
            ),
            textStyle = FalcoTypography.bodyMedium,
            enabled = state !is com.najmi.falco.domain.model.VerificationState.InProgress
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "INPUT IS BEING CROSS-REFERENCED AGAINST GLOBAL\n" +
            "DOSSIERS IN REAL-TIME. ENSURE TECHNICAL\n" +
            "NOMENCLATURE IS PRECISE FOR OPTIMAL VERIFICATION.",
            style = FalcoTypography.labelSmall,
            color = FalcoTextGhost,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.onTextChanged(inputText)
                viewModel.verify(inputText)
                onNavigateToPipeline()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = inputText.isNotBlank() && state !is com.najmi.falco.domain.model.VerificationState.InProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = FalcoTextPrimary,
                contentColor = FalcoBg,
                disabledContainerColor = FalcoTextGhost,
                disabledContentColor = FalcoBg
            ),
            shape = FalcoZeroShape
        ) {
            Text("VERIFY WITH EVIDENCE", style = FalcoTypography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))

        Text("RECENT HYPOTHESES", style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        Spacer(Modifier.height(12.dp))

        listOf(
            "QUANTUM_ENTANGLEMENT_DISCREPANCY_V4" to "VERIFIED",
            "NEURAL_SYNAPSE_MAPPING_DEVIATION" to "PENDING",
            "ATMOSPHERIC_CARBON_VALENCE_SHIFT" to "NEW"
        ).forEach { (name, status) ->
            RecentHypothesisRow(name = name, status = status)
            Spacer(Modifier.height(8.dp))
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
                    "VERIFIED" -> FalcoTextPrimary
                    "PENDING" -> FalcoTextMuted
                    else -> FalcoTextGhost
                },
                FalcoZeroShape
            )
            .padding(14.dp, 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(name, style = FalcoTypography.bodySmall, color = FalcoTextBody)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.border(1.dp, FalcoTextGhost, FalcoZeroShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(status, style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        }
    }
}
