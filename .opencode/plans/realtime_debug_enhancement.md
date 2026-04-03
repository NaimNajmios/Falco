# Real-Time Extraction & Debug Mode Enhancement Plan

**Generated:** Fri Apr 03 2026  
**Status:** Ready for Implementation

---

## Phase 1 — Quick Wins

### 1.1 Connect RealTimeExtractionPanel to DebugLogger
**File:** `app/src/main/java/com/najmi/falco/data/local/DebugLogBuffer.kt` (new)
**Files to modify:** `DebugLogger.kt`, `PipelineScreen.kt`, `HypothesisViewModel.kt`

**Changes:**
1. Create `DebugLogBuffer.kt` - singleton in-memory log buffer with max 100 entries
2. Modify `DebugLogger.kt` to also emit to DebugLogBuffer when enabled
3. Update `HypothesisViewModel.kt` to expose DebugLogBuffer Flow
4. Update `RealTimeExtractionPanel` to observe DebugLogBuffer instead of local state
5. Add category prefixes (STAGE/LLM/NET) to log entries

### 1.2 Add Persistent Debug Mode Indicator
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`

**Changes:**
1. Add "[DEBUG]" indicator in PipelineScreen header next to SOURCE_ID
2. Only visible when `DebugLogger.isEnabled()` returns true

---

## Phase 2 — Core Enhancements

### 2.1 Make LiveMonitor Debug Panel Actually Useful
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`

**Changes:**
1. Replace static debug panel with scrollable LazyColumn
2. Show real stage timing data from DebugLogBuffer
3. Show LLM calls with provider/model/token counts
4. Show network request status and latency
5. Remove or make the fake neural layer bars optional
6. Keep "[DEBUG]" badge visible when debug mode enabled

### 2.2 Enhance DebugLogger to Capture More Metadata
**File:** `app/src/main/java/com/najmi/falco/data/local/DebugLogger.kt`

**Changes:**
1. Add structured data classes for different log types (StageEntry, LlmEntry, NetworkEntry)
2. Add timestamps to all entries
3. Add source stage/action to entries

---

## Phase 3 — New Capabilities

### 3.1 Stage Timeline Visualization
**File:** `app/src/main/java/com/najmi/falco/ui/pipeline/PipelineScreen.kt`

**Changes:**
1. Create `StageTimeline` composable
2. Show horizontal timeline with completed stages as bars with durations
3. Highlight current stage with animation
4. Show pending stages as dimmed placeholders
5. Add total pipeline time at the end

### 3.2 Export Debug Log
**File:** `app/src/main/java/com/najmi/falco/ui/components/DebugLogExport.kt` (new)

**Changes:**
1. Add export button in debug panel
2. Generate text file with formatted debug session
3. Use Android share intent to share or save

---

## Implementation Order

1. **DebugLogBuffer** (new singleton buffer class)
2. **Update DebugLogger** (emit to buffer)
3. **Update HypothesisViewModel** (expose buffer flow)
4. **Update RealTimeExtractionPanel** (observe buffer)
5. **Add debug indicator** (PipelineScreen header)
6. **Enhance LiveMonitor debug panel** (show real data)
7. **Stage timeline visualization** (new composable)
8. **Export debug log** (share functionality)

---

## Files to Modify/Create

| File | Action | Changes |
|------|--------|---------|
| `data/local/DebugLogBuffer.kt` | Create | In-memory log buffer singleton |
| `data/local/DebugLogger.kt` | Modify | Emit to buffer + structured entries |
| `ui/hypothesis/HypothesisViewModel.kt` | Modify | Expose DebugLogBuffer Flow |
| `ui/pipeline/PipelineScreen.kt` | Modify | Connect to buffer, enhance debug panel |
| `ui/components/StageTimeline.kt` | Create | Timeline visualization |
| `ui/components/DebugLogExport.kt` | Create | Export/share functionality |

---

## Verification Checklist

- [ ] DebugLogger output appears in RealTimeExtractionPanel when debug enabled
- [ ] Log entries show category prefix (STAGE/LLM/NET)
- [ ] Log persists across configuration changes
- [ ] "[DEBUG]" indicator visible in PipelineScreen when debug enabled
- [ ] Debug panel shows real stage timing data
- [ ] Debug panel shows LLM calls with tokens/latency
- [ ] Debug panel shows network requests
- [ ] Stage timeline shows completed stages with durations
- [ ] Current stage animates/highlights
- [ ] Export generates shareable debug log file
