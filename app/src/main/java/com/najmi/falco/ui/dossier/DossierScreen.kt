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
                        onClick = { onVerdictSelected(claim.id) }
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
    onClick: () -> Unit
) {
    val stanceColor = claim.lean?.let { lean ->
        when (lean) {
            "SUPPORTS" -> LocalFalcoPalette.current.stanceSupports
            "OPPOSES" -> LocalFalcoPalette.current.stanceOpposes
            else -> LocalFalcoPalette.current.stanceNeutral
        }
    } ?: LocalFalcoPalette.current.textGhost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    claim.text.take(40).let { if (claim.text.length > 40) "$it..." else it },
                    style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = LocalFalcoPalette.current.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(12.dp))
                claim.lean?.let { lean ->
                    Text(
                        lean,
                        style = FalcoTypography.labelSmall.copy(letterSpacing = FalcoDimens.LetterSpacingWide),
                        color = stanceColor
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    claim.type,
                    style = FalcoTypography.labelSmall,
                    color = LocalFalcoPalette.current.textGhost
                )
                claim.confidence?.let { confidence ->
                    Text(
                        "${(confidence * 100).toInt()}% confidence",
                        style = FalcoTypography.labelSmall,
                        color = LocalFalcoPalette.current.textMuted
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatTimestamp(claim.submittedAt),
                style = FalcoTypography.labelSmall,
                color = LocalFalcoPalette.current.textGhost
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
