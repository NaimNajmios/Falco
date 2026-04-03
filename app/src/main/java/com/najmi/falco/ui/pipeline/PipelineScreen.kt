package com.najmi.falco.ui.pipeline

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.falco.data.local.DebugLogBuffer
import com.najmi.falco.data.local.DebugLogEntry
import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.ui.hypothesis.HypothesisViewModel
import com.najmi.falco.ui.components.ConfidenceSegmentBar
import com.najmi.falco.ui.components.ConfidenceGauge
import com.najmi.falco.ui.components.ConsensusIndicator
import com.najmi.falco.ui.components.DebugLogExportButton
import com.najmi.falco.ui.components.EvidenceRow
import com.najmi.falco.ui.components.TokenUsageCard
import com.najmi.falco.ui.theme.LocalFalcoPalette
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.FalcoDimens
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun PipelineScreen(
    hypothesisViewModel: HypothesisViewModel,
    onNewClaim: () -> Unit,
    onCancel: () -> Unit
) {
    val state by hypothesisViewModel.verificationState.collectAsState()
    val claimText by hypothesisViewModel.claimText.collectAsState()
    val debugEntries by hypothesisViewModel.debugLogEntries.collectAsState()
    val stageTimings by hypothesisViewModel.stageTimings.collectAsState()
    val isDebugEnabled = DebugLogger.isEnabled()
    var stageStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currentTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(state) {
        if (state is VerificationState.InProgress) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                currentTick = System.currentTimeMillis()
            }
        }
    }
    
    LaunchedEffect(state) {
        if (state is VerificationState.InProgress) {
            stageStartTime = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalFalcoPalette.current.bg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    4.dp,
                    LocalFalcoPalette.current.divider,
                    FalcoZeroShape
                )
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp)
        ) {
            Column {
                Text(
                    claimText,
                    style = FalcoTypography.headlineLarge,
                    color = LocalFalcoPalette.current.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SOURCE_ID: ${hypothesisViewModel.currentClaimId}",
                        style = FalcoTypography.labelSmall,
                        color = LocalFalcoPalette.current.textGhost
                    )
                    if (isDebugEnabled) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "[DEBUG]",
                            style = FalcoTypography.labelSmall,
                            color = LocalFalcoPalette.current.textMuted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        when (state) {
            is VerificationState.Success -> {
                VerdictResult(state = state as VerificationState.Success, onNewClaim = onNewClaim)
            }
            is VerificationState.Error -> {
                ErrorResult(state = state as VerificationState.Error, onNewClaim = onNewClaim)
            }
            else -> {
                StageList(state = state, stageStartTime = stageStartTime, currentTick = currentTick)

                Spacer(Modifier.height(24.dp))

                RealTimeExtractionPanel(state = state, debugEntries = debugEntries)

                Spacer(Modifier.height(16.dp))

                LiveMonitor(debugEntries = debugEntries, stageTimings = stageTimings)

                Spacer(Modifier.height(32.dp))

                FalcoGhostButton("HALT_PROCESS", onClick = onCancel)

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun VerdictResult(
    state: VerificationState.Success,
    onNewClaim: () -> Unit
) {
    val verdict = state.verdict
    val stanceColor = when (verdict.lean) {
        Stance.SUPPORTS -> LocalFalcoPalette.current.stanceSupports
        Stance.NEUTRAL -> LocalFalcoPalette.current.stanceNeutral
        Stance.OPPOSES -> LocalFalcoPalette.current.stanceOpposes
        Stance.INSUFFICIENT_EVIDENCE -> LocalFalcoPalette.current.textMuted
    }
    
    var selectedFilter by remember { mutableStateOf<Stance?>(null) }
    var selectedSort by remember { mutableStateOf(EvidenceSort.CITATIONS) }
    
    val filteredStances = verdict.stances.filter { stance ->
        val finalStance = stance.finalStance ?: stance.actorStance
        selectedFilter == null || finalStance == selectedFilter
    }
    
    val sortedStances = when (selectedSort) {
        EvidenceSort.CITATIONS -> filteredStances.sortedByDescending { it.paper.citationCount }
        EvidenceSort.YEAR -> filteredStances.sortedByDescending { it.paper.year ?: 0 }
        EvidenceSort.GROUNDING -> filteredStances.sortedByDescending { it.groundingScore ?: it.confidence }
    }

    Text("VERIFICATION COMPLETE", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(16.dp))

    Text(verdict.lean.name, style = FalcoTypography.displayLarge, color = stanceColor)
    Spacer(Modifier.height(16.dp))

    ConfidenceGauge(
        confidence = verdict.confidence,
        certaintyLevel = verdict.certaintyLevel
    )

    Spacer(Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Column {
            Text("SUMMARY", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
            Spacer(Modifier.height(8.dp))
            Text(verdict.summary, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textBody)
            verdict.caveat?.let { caveat ->
                Spacer(Modifier.height(8.dp))
                Text(caveat, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textMuted)
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    ConsensusIndicator(consensusInfo = verdict.consensusInfo)

    verdict.temporalWarning?.let { warning ->
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalFalcoPalette.current.surface)
                .border(1.dp, LocalFalcoPalette.current.chip, FalcoZeroShape)
                .padding(12.dp)
        ) {
            Text(warning, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textMuted)
        }
    }

    Spacer(Modifier.height(24.dp))

    Text("EVIDENCE LIST [${sortedStances.size}]", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(12.dp))
    
    EvidenceFilterRow(
        selectedFilter = selectedFilter,
        onFilterChange = { selectedFilter = it },
        selectedSort = selectedSort,
        onSortChange = { selectedSort = it }
    )
    Spacer(Modifier.height(12.dp))

    if (sortedStances.isEmpty()) {
        EmptyEvidenceState()
    } else {
        sortedStances.forEach { stance ->
            EvidenceRow(paperStance = stance)
        }
    }

    Spacer(Modifier.height(24.dp))

    TokenUsageCard(metadata = verdict.analysisMetadata)

    Spacer(Modifier.height(32.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FalcoGhostButton("SHARE", onClick = { })
        FalcoGhostButton("NEW CLAIM", onClick = onNewClaim, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(80.dp))
}

@Composable
private fun ErrorResult(
    state: VerificationState.Error,
    onNewClaim: () -> Unit
) {
    Text("VERIFICATION FAILED", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(16.dp))

    Text("UNVERIFIABLE", style = FalcoTypography.displayLarge, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Text(state.message, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textMuted)
    }

    Spacer(Modifier.height(32.dp))

    FalcoGhostButton("TRY AGAIN", onClick = onNewClaim)
    Spacer(Modifier.height(80.dp))
}

@Composable
private fun StageList(state: VerificationState, stageStartTime: Long, currentTick: Long) {
    Text("VERIFICATION_STAGES", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
    Spacer(Modifier.height(16.dp))

    val allStages = VerificationStage.entries
    val currentStage = when (state) {
        is VerificationState.InProgress -> state.stage
        is VerificationState.Success -> allStages.last()
        is VerificationState.Error -> null
        is VerificationState.Idle -> null
    }
    
    val elapsedSeconds = if (state is VerificationState.InProgress) {
        ((currentTick - stageStartTime) / 1000).toInt()
    } else 0
    
    val elapsedText = if (elapsedSeconds > 0) {
        "${elapsedSeconds}s elapsed"
    } else null
    
    val progressInfo = if (state is VerificationState.InProgress && state.totalCount > 0) {
        "${state.progressText} papers"
    } else null

    allStages.forEachIndexed { index, stage ->
        val isComplete = currentStage != null && stage.ordinal < currentStage.ordinal
        val isActive = stage == currentStage
        val isPending = !isComplete && !isActive

        StageRow(
            label = stage.name.replace("_", " "),
            status = when {
                isComplete -> "COMPLETE"
                isActive && progressInfo != null -> progressInfo
                isActive && elapsedText != null -> elapsedText
                isActive -> "PROCESSING"
                else -> "PENDING"
            },
            isComplete = isComplete,
            isActive = isActive,
            isPending = isPending
        )
        if (index < allStages.size - 1) {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun StageRow(
    label: String,
    status: String,
    isComplete: Boolean,
    isActive: Boolean,
    isPending: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f, animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "blinkAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            val icon = when {
                isComplete -> "✓"
                isActive -> "■"
                else -> "—"
            }
            Text(
                text = icon,
                style = FalcoTypography.bodyMedium,
                color = when {
                    isComplete -> LocalFalcoPalette.current.textPrimary
                    isActive -> LocalFalcoPalette.current.textPrimary.copy(alpha = blinkAlpha)
                    else -> LocalFalcoPalette.current.textGhost
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = FalcoTypography.bodySmall.copy(letterSpacing = FalcoDimens.LetterSpacingWide),
            color = if (isPending) LocalFalcoPalette.current.textGhost else LocalFalcoPalette.current.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = status,
            style = FalcoTypography.labelSmall,
            color = when {
                isComplete -> LocalFalcoPalette.current.textMuted
                isActive -> LocalFalcoPalette.current.textPrimary
                else -> LocalFalcoPalette.current.textGhost
            }
        )
    }
}

@Composable
private fun RealTimeExtractionPanel(
    state: VerificationState,
    @Suppress("UNUSED_PARAMETER") debugEntries: List<DebugLogEntry>
) {
    val message = when (state) {
        is VerificationState.InProgress -> state.message
        is VerificationState.Error -> state.message
        is VerificationState.Success -> null
        is VerificationState.Idle -> "Awaiting input..."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
    ) {
        Column {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    message ?: "Awaiting input...",
                    style = FalcoTypography.bodySmall,
                    color = if (state is VerificationState.Error) LocalFalcoPalette.current.textMuted else LocalFalcoPalette.current.textBody
                )
            }
        }
    }
}

@Composable
private fun LiveMonitor(
    debugEntries: List<DebugLogEntry>,
    stageTimings: List<Pair<String, Long>>
) {
    var showDebug by remember { mutableStateOf(false) }
    val isDebugEnabled = DebugLogger.isEnabled()

    val infiniteTransition = rememberInfiniteTransition(label = "monitor")
    val bars = (0..11).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f + Random.nextFloat() * 0.6f,
            targetValue = 0.2f + Random.nextFloat() * 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(280, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "bar$index"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isDebugEnabled) Modifier.clickable { showDebug = !showDebug }
                        else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LIVE MONITOR", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
                Spacer(Modifier.weight(1f))
                if (isDebugEnabled) {
                    Text(
                        text = if (showDebug) "[- DEBUG]" else "[+ DEBUG]",
                        style = FalcoTypography.labelSmall,
                        color = LocalFalcoPalette.current.textMuted
                    )
                }
                Text("■", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textPrimary)
            }
            Spacer(Modifier.height(12.dp))

            if (!isDebugEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    bars.forEachIndexed { index, animState ->
                        val height = (animState.value * 32).dp
                        val color = when {
                            index % 4 == 0 -> LocalFalcoPalette.current.barFilled
                            index % 2 == 0 -> LocalFalcoPalette.current.textMuted
                            else -> LocalFalcoPalette.current.barEmpty.copy(alpha = 0.5f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(height)
                                .background(color, FalcoZeroShape)
                        )
                    }
                }
            }

            if (showDebug && isDebugEnabled) {
                Spacer(Modifier.height(12.dp))
                
                if (stageTimings.isNotEmpty()) {
                    Text("STAGE_TIMINGS", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
                    Spacer(Modifier.height(8.dp))
                    val totalMs = stageTimings.sumOf { it.second }
                    stageTimings.takeLast(5).forEach { (name, ms) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = FalcoTypography.labelMedium, color = LocalFalcoPalette.current.textMuted)
                            Text("${ms}ms", style = FalcoTypography.labelMedium, color = LocalFalcoPalette.current.textBody)
                        }
                    }
                    if (totalMs > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL", style = FalcoTypography.labelSmall.copy(fontWeight = FontWeight.Bold), color = LocalFalcoPalette.current.textPrimary)
                            Text("${totalMs}ms", style = FalcoTypography.labelSmall.copy(fontWeight = FontWeight.Bold), color = LocalFalcoPalette.current.textPrimary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                
                Text("LOG", style = FalcoTypography.labelSmall, color = LocalFalcoPalette.current.textGhost)
                Spacer(Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(LocalFalcoPalette.current.bg)
                        .padding(8.dp)
                ) {
                    LazyColumn {
                        items(debugEntries.takeLast(20).reversed()) { entry ->
                            DebugLogEntryRow(entry = entry)
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                DebugLogExportButton()
            }
        }
    }
}

@Composable
private fun DebugLogEntryRow(entry: DebugLogEntry) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "[${entry.category}]",
            style = FalcoTypography.labelMedium,
            color = when (entry.category) {
                "STAGE" -> LocalFalcoPalette.current.stanceSupports
                "LLM" -> LocalFalcoPalette.current.textMuted
                "NET" -> LocalFalcoPalette.current.stanceNeutral
                else -> LocalFalcoPalette.current.textGhost
            }
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = when (entry) {
                is DebugLogEntry.Stage -> "${entry.stageName}: ${entry.durationMs}ms"
                is DebugLogEntry.Llm -> "${entry.provider}/${entry.model}: ${entry.tokens}t (${entry.latencyMs}ms)"
                is DebugLogEntry.Network -> "${entry.method} ${entry.url.take(30)} -> ${entry.status}"
                is DebugLogEntry.Message -> "${entry.level}: ${entry.message.take(40)}"
            },
            style = FalcoTypography.labelMedium,
            color = LocalFalcoPalette.current.textMuted,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun FalcoGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(Modifier.fillMaxWidth())
            .height(56.dp)
            .background(LocalFalcoPalette.current.bg)
            .border(1.dp, LocalFalcoPalette.current.textGhost, FalcoZeroShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = FalcoTypography.bodySmall, color = LocalFalcoPalette.current.textPrimary)
    }
}

@Composable
private fun EmptyEvidenceState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalFalcoPalette.current.surface)
            .border(1.dp, LocalFalcoPalette.current.divider, FalcoZeroShape)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "[ ]",
            style = FalcoTypography.displayLarge,
            color = LocalFalcoPalette.current.textGhost
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "NO EVIDENCE FOUND",
            style = FalcoTypography.labelSmall,
            color = LocalFalcoPalette.current.textGhost
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "No papers passed the quality gate for this claim",
            style = FalcoTypography.bodySmall,
            color = LocalFalcoPalette.current.textMuted
        )
    }
}

private enum class EvidenceSort {
    CITATIONS, YEAR, GROUNDING
}

@Composable
private fun EvidenceFilterRow(
    selectedFilter: Stance?,
    onFilterChange: (Stance?) -> Unit,
    selectedSort: EvidenceSort,
    onSortChange: (EvidenceSort) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            label = "ALL",
            selected = selectedFilter == null,
            onClick = { onFilterChange(null) }
        )
        FilterChip(
            label = "SUPPORT",
            selected = selectedFilter == Stance.SUPPORTS,
            onClick = { onFilterChange(if (selectedFilter == Stance.SUPPORTS) null else Stance.SUPPORTS) },
            color = LocalFalcoPalette.current.stanceSupports
        )
        FilterChip(
            label = "OPPOSE",
            selected = selectedFilter == Stance.OPPOSES,
            onClick = { onFilterChange(if (selectedFilter == Stance.OPPOSES) null else Stance.OPPOSES) },
            color = LocalFalcoPalette.current.stanceOpposes
        )
        
        Spacer(Modifier.weight(1f))
        
        SortDropdown(
            selectedSort = selectedSort,
            onSortChange = onSortChange
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = LocalFalcoPalette.current.textGhost
) {
    val palette = LocalFalcoPalette.current
    Box(
        modifier = Modifier
            .background(
                if (selected) palette.chip else palette.bg
            )
            .border(
                1.dp,
                if (selected) color else palette.divider,
                FalcoZeroShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = FalcoTypography.labelSmall,
            color = if (selected) color else palette.textGhost
        )
    }
}

@Composable
private fun SortDropdown(
    selectedSort: EvidenceSort,
    onSortChange: (EvidenceSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val palette = LocalFalcoPalette.current
    
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (selectedSort) {
                    EvidenceSort.CITATIONS -> "CITATIONS"
                    EvidenceSort.YEAR -> "YEAR"
                    EvidenceSort.GROUNDING -> "GROUNDING"
                },
                style = FalcoTypography.labelSmall,
                color = palette.textMuted
            )
            Spacer(Modifier.width(4.dp))
            Text("▼", style = FalcoTypography.labelSmall, color = palette.textGhost)
        }
        
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Citations") },
                onClick = { onSortChange(EvidenceSort.CITATIONS); expanded = false }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Year") },
                onClick = { onSortChange(EvidenceSort.YEAR); expanded = false }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Grounding") },
                onClick = { onSortChange(EvidenceSort.GROUNDING); expanded = false }
            )
        }
    }
}

