# Pipeline & Result Display Enhancement Plan

**Generated:** Fri Apr 03 2026  
**Status:** Ready for Implementation

---

## Phase 1 — Quick Wins

### 1.1 Extract Shared EvidenceRow Component
**File:** `app/src/main/java/com/najmi/falco/ui/components/Rows.kt`
**Action:** Enhance existing EvidenceRow to include ChunksExplorer support, then update both PipelineScreen and VerdictDetailScreen to import and use the shared component.

**Changes:**
1. Add `ChunksExplorer` import and integration to EvidenceRow in Rows.kt
2. Remove inline `EvidenceRow` from PipelineScreen.kt (lines 492-559)
3. Update PipelineScreen.kt to import `EvidenceRow` from components
4. Remove inline `EvidenceRow` from VerdictDetailScreen.kt (lines 232-319)
5. Update VerdictDetailScreen.kt to import `EvidenceRow` from components

### 1.2 Fix/Remove Fake LiveMonitor Data
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`
**Action:** Replace hardcoded fake metrics with actual orchestrator data or remove misleading section.

**Changes:**
1. Remove lines 449-453 with fake data (SIG.STRENGTH: 0.992, LATENCY: 0.04MS, 14.2GB)
2. OR add real metrics state to ViewModel and connect LiveMonitor to actual data

### 1.3 Add Empty State for Evidence List
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`
**Action:** Add visual empty state when no evidence papers are available.

**Changes:**
1. In `VerdictResult`, check if `verdict.stances.isEmpty()`
2. Show empty state composable with icon and message when no evidence

---

## Phase 2 — Core Enhancements

### 2.1 Integrate Enhanced Result Components
**Files:** 
- `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`
- `app/src/main/java/com/najmi/falco/ui/components/EnhancedResultsComponents.kt`

**Action:** Replace basic confidence text in VerdictResult with rich components.

**Changes:**
1. Import `ConfidenceGauge`, `ConsensusIndicator`, `TokenUsageCard` from EnhancedResultsComponents
2. Replace basic confidence text (lines 144-147) with `ConfidenceGauge`
3. Add `ConsensusIndicator` below confidence bar
4. Add `TokenUsageCard` at the bottom of VerdictResult
5. Update imports in PipelineScreen.kt

### 2.2 Add Progress Percentage/ETA During Verification
**Files:**
- `app/src/main/java/com/najmi/falco/domain/model/VerificationState.kt`
- `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`
- `app/src/main/java/com/najmi/falco/pipeline/FalcoOrchestrator.kt`

**Action:** Add progress tracking to VerificationState.

**Changes:**
1. Add `progress: Float` and `processedCount: Int` and `totalCount: Int` to `VerificationState.InProgress`
2. Update FalcoOrchestrator to emit progress updates (e.g., "3 of 12 papers processed")
3. Update StageRow to display progress percentage
4. Add progress bar to StageList section

### 2.3 Add Stage Details with Elapsed Time
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`

**Action:** Add expandable stage details showing elapsed time and sub-steps.

**Changes:**
1. Add `stageStartTime` state to PipelineScreen
2. Modify StageRow to show elapsed time when active
3. Make StageRow expandable to show stage-specific details
4. Add stage completion animation

---

## Phase 3 — New Capabilities

### 3.1 Evidence Filter/Sort Functionality
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`

**Action:** Add filter chips and sort options for evidence list.

**Changes:**
1. Add filter row with chips: All, Supports, Opposes, Neutral
2. Add sort dropdown: Citations (default), Year, Grounding Score
3. Implement filtered/sorted list display
4. Persist filter preference

### 3.2 Share Improvements with Native Share Sheet
**Files:**
- `app/src/main/java/com/najmi/falco/ui/dossier/VerdictDetailScreen.kt`
- `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`
- `app/src/main/java/com/najmi/falco/ui/components/ShareBottomSheet.kt`

**Action:** Integrate existing ShareBottomSheet in both screens.

**Changes:**
1. Add `showShareSheet` state to VerdictDetailScreen
2. Trigger ShareBottomSheet when "SHARE VERDICT" is clicked
3. Add "SHARE" button to PipelineScreen's VerdictResult
4. Enhance share text format with more context

### 3.3 Real-time Evidence Preview During Processing
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`

**Action:** Show evidence items as they are processed during ACTOR_CLASSIFICATION stage.

**Changes:**
1. Add `processedStances: List<PaperStance>` state
2. Update FalcoOrchestrator to emit partial stance results
3. Add "Evidence Preview" section that shows processed stances
4. Use shared EvidenceRow component for preview items

### 3.4 Detailed Stage Log (Expandable)
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`

**Action:** Add expandable debug log showing detailed stage outputs.

**Changes:**
1. Add `showDebugLog` state
2. Create DebugLogPanel composable
3. Add toggle in LiveMonitor or separate section
4. Display stage messages with timestamps
5. Use LazyColumn for scrollable log

---

## Implementation Order

1. **EvidenceRow extraction** (shared component, immediate value)
2. **Share improvements** (uses existing ShareBottomSheet)
3. **Enhanced components integration** (visual polish)
4. **Empty states** (quick win)
5. **Fix fake data** (quick win)
6. **Progress tracking** (backend + UI)
7. **Stage details** (UI enhancement)
8. **Evidence filter/sort** (feature enhancement)
9. **Real-time preview** (advanced feature)
10. **Debug log** (developer feature)

---

## Files to Modify

| File | Changes |
|------|---------|
| `ui/components/Rows.kt` | Add ChunksExplorer to EvidenceRow |
| `ui/pipeline/PipelineScreen.kt` | Multiple: use shared EvidenceRow, add enhanced components, add filters, fix fake data |
| `ui/dossier/VerdictDetailScreen.kt` | Use shared EvidenceRow, add share integration |
| `domain/model/VerificationState.kt` | Add progress fields |
| `pipeline/FalcoOrchestrator.kt` | Emit progress updates |
| `ui/components/ShareBottomSheet.kt` | Enhance share text format |

---

## Verification Checklist

- [ ] EvidenceRow works in both PipelineScreen and VerdictDetailScreen
- [ ] No duplicate EvidenceRow code
- [ ] ConfidenceGauge displays correctly
- [ ] ConsensusIndicator shows supporting/opposing counts
- [ ] TokenUsageCard displays token analysis info
- [ ] Empty state shows when no evidence
- [ ] LiveMonitor shows real or no data (no fake data)
- [ ] Filter chips filter evidence list
- [ ] Sort options reorder evidence
- [ ] Share opens native share sheet
- [ ] Progress shows during verification
- [ ] Stage details show elapsed time
