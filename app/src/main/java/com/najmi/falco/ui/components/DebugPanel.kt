package com.najmi.falco.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.najmi.falco.domain.model.AnalysisMetadata
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette

@Composable
fun DebugPanel(
    metadata: AnalysisMetadata,
    providerUsed: String,
    modelUsed: String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Text(
            "DEBUG INFO",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DebugItem(label = "Provider", value = providerUsed)
            DebugItem(label = "Model", value = modelUsed ?: "unknown")
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DebugItem(label = "Tokens", value = "${metadata.totalTokensAnalyzed}")
            DebugItem(label = "Latency", value = "${metadata.analysisDurationMs}ms")
            DebugItem(label = "Databases", value = metadata.databasesQueried.joinToString(", ").ifEmpty { "N/A" })
        }

        Spacer(Modifier.height(8.dp))

        metadata.algorithmVersion.let { version ->
            DebugItem(label = "Algorithm", value = version)
        }
    }
}

@Composable
private fun DebugItem(
    label: String,
    value: String
) {
    val palette = LocalFalcoPalette.current

    Column {
        Text(
            label,
            style = FalcoTypography.labelSmall,
            color = palette.textMuted
        )
        Text(
            value,
            style = FalcoTypography.bodySmall,
            color = palette.textBody
        )
    }
}

@Composable
fun StagingInfoSection(
    stages: List<StageDebugInfo>,
    modifier: Modifier = Modifier
) {
    val palette = LocalFalcoPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Text(
            "PIPELINE STAGES",
            style = FalcoTypography.labelSmall,
            color = palette.textGhost
        )

        Spacer(Modifier.height(12.dp))

        stages.forEach { stage ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stage.name,
                    style = FalcoTypography.bodySmall,
                    color = palette.textBody
                )
                Text(
                    "${stage.latencyMs}ms • ${stage.tokens} tokens",
                    style = FalcoTypography.bodySmall,
                    color = palette.textMuted
                )
            }
        }
    }
}

data class StageDebugInfo(
    val name: String,
    val latencyMs: Long,
    val tokens: Int,
    val provider: String
)