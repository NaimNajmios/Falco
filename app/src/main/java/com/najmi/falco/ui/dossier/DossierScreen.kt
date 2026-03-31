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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.repository.RecentClaim
import com.najmi.falco.ui.components.ConfidenceSegmentBar
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DossierScreen(
    viewModel: DossierViewModel = hiltViewModel(),
    onVerdictSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalFalcoPalette.current.bg)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "SEARCH HISTORY",
            style = FalcoTypography.headlineLarge,
            color = LocalFalcoPalette.current.textPrimary
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LocalFalcoPalette.current.divider)
        )

        Spacer(Modifier.height(32.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Loading...",
                    style = FalcoTypography.bodyMedium,
                    color = LocalFalcoPalette.current.textGhost
                )
            }
        } else if (uiState.recentClaims.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No previous searches",
                        style = FalcoTypography.headlineMedium,
                        color = LocalFalcoPalette.current.textGhost
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Completed verifications will appear here",
                        style = FalcoTypography.bodyMedium,
                        color = LocalFalcoPalette.current.textGhost
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.recentClaims) { claim ->
                    HistoryItem(
                        claim = claim,
                        onClick = { onVerdictSelected(claim.id) },
                        onDelete = { viewModel.deleteClaim(claim.id) }
                    )
                }
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    claim: RecentClaim,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalFalcoPalette.current

    val stanceColor = claim.lean?.let { lean ->
        when (lean) {
            "SUPPORTS" -> palette.stanceSupports
            "OPPOSES" -> palette.stanceOpposes
            "INSUFFICIENT_EVIDENCE" -> palette.textMuted
            else -> palette.stanceNeutral
        }
    } ?: palette.textGhost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    claim.text.take(40).let { if (claim.text.length > 40) "$it..." else it },
                    style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    claim.lean?.let { lean ->
                        Text(
                            lean,
                            style = FalcoTypography.labelSmall.copy(letterSpacing = FalcoDimens.LetterSpacingWide),
                            color = stanceColor
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        "[X]",
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost,
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    claim.type,
                    style = FalcoTypography.labelSmall,
                    color = palette.textGhost
                )
                claim.confidence?.let { confidence ->
                    ConfidenceSegmentBar(
                        confidence = confidence,
                        segments = 5,
                        modifier = Modifier.width(60.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(confidence * 100).toInt()}%",
                        style = FalcoTypography.labelSmall,
                        color = palette.textMuted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val total = claim.supportingCount + claim.opposingCount + claim.neutralCount
                if (total > 0) {
                    EvidenceCountChip("S", claim.supportingCount, palette.stanceSupports)
                    EvidenceCountChip("O", claim.opposingCount, palette.stanceOpposes)
                    EvidenceCountChip("N", claim.neutralCount, palette.stanceNeutral)
                    Spacer(Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Text(
                    formatTimestamp(claim.submittedAt),
                    style = FalcoTypography.labelSmall,
                    color = palette.textGhost
                )
            }
        }
    }
}

@Composable
private fun EvidenceCountChip(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    val palette = LocalFalcoPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "[$label]",
            style = FalcoTypography.labelSmall.copy(letterSpacing = FalcoDimens.LetterSpacingWide),
            color = color
        )
        Spacer(Modifier.width(2.dp))
        Text(
            "$count",
            style = FalcoTypography.labelSmall,
            color = palette.textBody
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
