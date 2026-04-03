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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.RecentClaim
import com.najmi.falco.ui.components.ConfidenceSegmentBar
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoDimens
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DossierScreen(
    viewModel: DossierViewModel = hiltViewModel(),
    onVerdictSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun showUndoSnackbar() {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Claim deleted",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        }
    }

    fun exportHistory() {
        scope.launch {
            try {
                val verdicts: List<Verdict> = viewModel.exportVerdicts().first()
                val json = Json { prettyPrint = true }
                val exportData = verdicts.map { v: Verdict ->
                    mapOf(
                        "claim" to v.claim,
                        "verdict" to v.lean.name,
                        "confidence" to v.confidence,
                        "summary" to v.summary,
                        "supporting" to v.supportingCount,
                        "opposing" to v.opposingCount,
                        "neutral" to v.neutralCount,
                        "date" to SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date(v.completedAt))
                    )
                }
                val jsonString = json.encodeToString(exportData)

                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, jsonString)
                    type = "application/json"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, "Export History")
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Export failed: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalFalcoPalette.current.bg)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SEARCH HISTORY",
                style = FalcoTypography.headlineLarge,
                color = LocalFalcoPalette.current.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterButton(
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    onClick = { viewModel.setShowFavoritesOnly(!uiState.showFavoritesOnly) }
                )
                Spacer(Modifier.width(8.dp))
                ExportButton(onClick = { exportHistory() })
                Spacer(Modifier.width(8.dp))
                SortButton(
                    currentSort = uiState.sortOrder,
                    onSortChange = { viewModel.setSortOrder(it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) }
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LocalFalcoPalette.current.divider)
        )

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    SkeletonLoadingList()
                }
                uiState.filteredClaims.isEmpty() && uiState.recentClaims.isEmpty() -> {
                    EmptyState()
                }
                uiState.filteredClaims.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    NoResultsState(searchQuery = uiState.searchQuery)
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.filteredClaims,
                            key = { it.id }
                        ) { claim ->
                            SwipeableHistoryItem(
                                claim = claim,
                                onClick = { onVerdictSelected(claim.id) },
                                onDelete = {
                                    viewModel.deleteClaim(claim.id)
                                    showUndoSnackbar()
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(claim.id) }
                            )
                        }
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SwipeableHistoryItem(
    claim: RecentClaim,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = LocalFalcoPalette.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider, FalcoZeroShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (claim.isFavorite) {
                        Text(
                            "[*]",
                            style = FalcoTypography.labelSmall,
                            color = palette.chip,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        claim.text.take(40).let { if (claim.text.length > 40) "$it..." else it },
                        style = FalcoTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    claim.lean?.let { lean ->
                        val stanceColor = when (lean) {
                            "SUPPORTS" -> palette.stanceSupports
                            "OPPOSES" -> palette.stanceOpposes
                            "INSUFFICIENT_EVIDENCE" -> palette.textMuted
                            else -> palette.stanceNeutral
                        }
                        Text(
                            lean,
                            style = FalcoTypography.labelSmall.copy(letterSpacing = FalcoDimens.LetterSpacingWide),
                            color = stanceColor
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        formatTimestamp(claim.submittedAt),
                        style = FalcoTypography.labelSmall,
                        color = palette.textGhost
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(Modifier.width(8.dp))
                Text(
                    if (claim.isFavorite) "[-]" else "[*]",
                    style = FalcoTypography.labelSmall,
                    color = if (claim.isFavorite) palette.chip else palette.textGhost,
                    modifier = Modifier.clickable { onToggleFavorite() }
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "[X]",
                    style = FalcoTypography.labelSmall,
                    color = palette.textGhost,
                    modifier = Modifier.clickable { showDeleteConfirm = true }
                )
            }
        }

        if (showDeleteConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.bg.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Delete?",
                        style = FalcoTypography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        "YES",
                        style = FalcoTypography.labelSmall,
                        color = palette.stanceOpposes,
                        modifier = Modifier.clickable {
                            onDelete()
                            showDeleteConfirm = false
                        }
                    )
                    Text(
                        "NO",
                        style = FalcoTypography.labelSmall,
                        color = palette.textMuted,
                        modifier = Modifier.clickable { showDeleteConfirm = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val palette = LocalFalcoPalette.current
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, FalcoZeroShape)
            .border(1.dp, if (isFocused) palette.chip else palette.divider, FalcoZeroShape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (query.isEmpty()) "Search claims..." else query,
                style = FalcoTypography.bodyMedium,
                color = if (query.isEmpty()) palette.textGhost else palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                Text(
                    "X",
                    style = FalcoTypography.labelSmall,
                    color = palette.textGhost,
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .padding(4.dp)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SortButton(
    currentSort: SortOrder,
    onSortChange: (SortOrder) -> Unit
) {
    val palette = LocalFalcoPalette.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        Text(
            when (currentSort) {
                SortOrder.NEWEST_FIRST -> "NEWEST"
                SortOrder.OLDEST_FIRST -> "OLDEST"
                SortOrder.HIGHEST_CONFIDENCE -> "CONFIDENCE"
            },
            style = FalcoTypography.labelSmall,
            color = palette.textMuted,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(8.dp)
        )

        if (expanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(120.dp)
                    .background(palette.surface, FalcoZeroShape)
                    .border(1.dp, palette.divider, FalcoZeroShape)
            ) {
                Column {
                    SortOption(
                        label = "Newest",
                        selected = currentSort == SortOrder.NEWEST_FIRST,
                        onClick = {
                            onSortChange(SortOrder.NEWEST_FIRST)
                            expanded = false
                        }
                    )
                    SortOption(
                        label = "Oldest",
                        selected = currentSort == SortOrder.OLDEST_FIRST,
                        onClick = {
                            onSortChange(SortOrder.OLDEST_FIRST)
                            expanded = false
                        }
                    )
                    SortOption(
                        label = "Confidence",
                        selected = currentSort == SortOrder.HIGHEST_CONFIDENCE,
                        onClick = {
                            onSortChange(SortOrder.HIGHEST_CONFIDENCE)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalFalcoPalette.current
    Text(
        label,
        style = FalcoTypography.bodySmall,
        color = if (selected) palette.chip else palette.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    )
}

@Composable
private fun ExportButton(onClick: () -> Unit) {
    val palette = LocalFalcoPalette.current
    Text(
        "[EXP]",
        style = FalcoTypography.labelSmall,
        color = palette.textMuted,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    )
}

@Composable
private fun FilterButton(
    showFavoritesOnly: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalFalcoPalette.current
    Text(
        if (showFavoritesOnly) "[*]" else "[ ]",
        style = FalcoTypography.labelSmall,
        color = if (showFavoritesOnly) palette.chip else palette.textMuted,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    )
}

@Composable
private fun SkeletonLoadingList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(5) {
            SkeletonItem()
        }
    }
}

@Composable
private fun SkeletonItem() {
    val palette = LocalFalcoPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(palette.surface, FalcoZeroShape)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.divider)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.divider)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.divider)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val palette = LocalFalcoPalette.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "[ ]",
                style = FalcoTypography.displayLarge,
                color = palette.textGhost
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No previous searches",
                style = FalcoTypography.headlineMedium,
                color = palette.textGhost
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Completed verifications will appear here",
                style = FalcoTypography.bodyMedium,
                color = palette.textGhost
            )
        }
    }
}

@Composable
private fun NoResultsState(searchQuery: String) {
    val palette = LocalFalcoPalette.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No results",
                style = FalcoTypography.headlineMedium,
                color = palette.textGhost
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No matches for \"$searchQuery\"",
                style = FalcoTypography.bodyMedium,
                color = palette.textGhost
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}