# Falco — Results Page Value Enhancement
## Implementation Plan: End-to-End Pipeline Signal Propagation

> **Scope:** Enrich the results/Dossier screen by surfacing meaningful signals from all 5 pipeline stages. Each stage currently produces data that is either dropped or collapsed before it reaches the UI.
>
> **Approach:** No pipeline logic changes. Only data contracts, ViewModel state, and UI layers are touched. The orchestrator stays deterministic.

---

## 1. The Problem: What the Current Pipeline Throws Away

Each stage of `FalcoOrchestrator` produces richer intermediate data than what ultimately lands in the Dossier. Here is what is being discarded:

| Stage | Produces | Currently Surfaced | Dropped |
|---|---|---|---|
| Claim Classifier | Claim type, confidence, ambiguity flags, sub-claims | Claim type label (maybe) | Confidence, ambiguity, sub-claims |
| Query Expander | N expanded queries, reasoning per query | Nothing | All query reasoning |
| Multi-Source Retrieval | Paper count, source breakdown (OpenAlex vs. Semantic Scholar), fetch latency, failed sources | Paper count (maybe) | Source breakdown, latency, failures |
| Stance Actor | Per-paper stance + confidence + supporting excerpt | Aggregated stance | Individual stances, excerpts, per-paper confidence |
| Aggregator | Weighted confidence, conflict ratio, evidence quality tiers | Final verdict + score | Conflict analysis, quality tiers, weighting logic |

**The result:** the user sees a verdict but cannot understand *why* that verdict was reached, *how confident* the system is, or *where it struggled*.

---

## 2. Proposed Additions by Stage

### Stage 1 — Claim Classifier Enhancements

**What to add to the results page:**

**A. Claim Anatomy Card**
A visual breakdown of how the claim was parsed:
- Claim type badge (Causal / Comparative / Predictive / Descriptive / Normative)
- Confidence ring (0–100%)
- If confidence < 70%: amber warning — *"Claim is ambiguous — results may be less precise"*
- Sub-claim chips: if the classifier detects multiple nested assertions, list them as tappable chips that filter the evidence below

**B. Ambiguity Alert**
If the classifier flagged ambiguity, surface a collapsible banner:
> *"This claim contains [N] interpretations. Results are based on the primary interpretation: [restated claim]."*

**Data contract additions needed:**
```kotlin
data class ClaimClassification(
    val claimType: ClaimType,
    val confidence: Float,           // 0.0–1.0
    val isAmbiguous: Boolean,
    val ambiguityReason: String?,
    val subClaims: List<String>,     // NEW
    val restatedClaim: String        // Normalized form used downstream
)
```

---

### Stage 2 — Query Expander Enhancements

**What to add to the results page:**

**A. Search Strategy Section** (collapsible)
Show the N queries generated, grouped by intent:
- Broad query (anchors the search)
- Narrow queries (specificity variants)
- Contrastive query (opposite framing — used to find refuting evidence)

Each chip is tappable → filters the evidence list to papers found via that query.

**B. Query Coverage Indicator**
A horizontal bar showing how many of the N queries returned results vs. came up empty. A low coverage score (e.g., only 2 of 5 queries returned papers) signals a weak evidence base and should be reflected prominently.

**Data contract additions needed:**
```kotlin
data class ExpandedQuery(
    val text: String,
    val intent: QueryIntent,         // BROAD, NARROW, CONTRASTIVE
    val resultsFound: Int            // populated after Stage 3
)

enum class QueryIntent { BROAD, NARROW, CONTRASTIVE }
```

---

### Stage 3 — Multi-Source Retrieval Enhancements

**What to add to the results page:**

**A. Evidence Base Summary Bar**
A compact header above the paper list showing:
- Total papers retrieved: **N**
- Source split: OpenAlex **X** / Semantic Scholar **Y**
- Papers excluded (e.g., behind paywall / no abstract): **Z**
- Any failed source with reason (timeout, rate-limit, no results)

**B. Source Health Badges**
Small icons on each paper card indicating which database it came from + open access status.

**C. "Evidence Base Quality" Score**
Derived from: paper count, recency (avg. publish year), citation counts (if available), open-access ratio. Displayed as a letter grade (A–D) or star rating.

**Data contract additions needed:**
```kotlin
data class RetrievalSummary(
    val totalFetched: Int,
    val openAlexCount: Int,
    val semanticScholarCount: Int,
    val excludedCount: Int,
    val failedSources: List<FailedSource>,
    val evidenceQualityScore: Float   // 0.0–1.0, computed by retrieval agent
)

data class FailedSource(
    val name: String,
    val reason: FailureReason         // TIMEOUT, RATE_LIMITED, NO_RESULTS
)

// On each paper:
data class RetrievedPaper(
    val title: String,
    val authors: List<String>,
    val year: Int,
    val abstract: String,
    val source: DataSource,           // OPEN_ALEX, SEMANTIC_SCHOLAR
    val isOpenAccess: Boolean,
    val citationCount: Int?,
    val doi: String?,
    val queryId: String               // links back to ExpandedQuery
)
```

---

### Stage 4 — Stance Actor Enhancements

**What to add to the results page:**

**A. Evidence List (the core of the results page)**
Each paper rendered as an expandable card:

```
┌─────────────────────────────────────────────┐
│ [SUPPORTS]  87%  ●●●●○                      │
│ "Dietary interventions and cognitive..."    │
│ Smith et al. (2023) · Semantic Scholar · OA │
│ ▼ Expand                                    │
├─────────────────────────────────────────────┤
│ Supporting excerpt:                         │
│ "...subjects showed 34% improvement in..."  │
│ [View paper ↗]                              │
└─────────────────────────────────────────────┘
```

**B. Stance Distribution Chart**
A horizontal stacked bar at the top of the evidence list:
```
[■■■■■■■□□□□□□□□] 6 Support / 3 Neutral / 2 Refute
```
Color coded: green / grey / red. Tapping a segment filters the list.

**C. Confidence Histogram**
A small bar chart showing the distribution of per-paper confidence scores. A bimodal distribution (many very high AND very low) signals a contested claim — worth flagging explicitly.

**D. Conflict Detection Banner**
If Supporting and Refuting papers both exceed a threshold (e.g., ≥ 2 papers on each side), surface:
> *"⚡ Scientific Conflict Detected — Evidence is divided on this claim."*

**Data contract additions needed:**
```kotlin
data class PaperStance(
    val paper: RetrievedPaper,
    val stance: Stance,               // SUPPORT, REFUTE, NEUTRAL
    val confidence: Float,            // 0.0–1.0
    val supportingExcerpt: String?,   // key sentence from abstract
    val reasoning: String             // LLM's chain of thought (debug only)
)

enum class Stance { SUPPORT, REFUTE, NEUTRAL }
```

---

### Stage 5 — Aggregator Enhancements

**What to add to the results page:**

**A. Verdict Header (already exists — enhance it)**
Current: probably just a label + score.
Enhanced:
- Large verdict label: SUPPORTED / CONTESTED / REFUTED / INSUFFICIENT EVIDENCE
- Confidence arc (animated from 0 to final value on first render)
- Confidence label: "High Confidence" / "Moderate" / "Low — treat with caution"
- Evidence count: "Based on 11 peer-reviewed papers"

**B. Weighting Transparency Panel** (collapsible, power-user feature)
Show how the aggregator weighted evidence:
- Citation-weighted score vs. unweighted score
- Recency-weighted score vs. unweighted score
- If scores diverge significantly: "Recent research trends toward [X], but historical literature favors [Y]"

**C. Verdict Confidence Breakdown**
A small table:

| Factor | Score |
|---|---|
| Evidence volume | 8 / 10 |
| Source diversity | 6 / 10 |
| Consensus strength | 4 / 10 |
| Evidence recency | 7 / 10 |
| **Overall** | **6.3 / 10** |

**D. Verdict Narrative** (the highest-value addition)
A 2–3 sentence LLM-generated plain English summary:
> *"The majority of evidence (6 of 9 papers) supports this claim, particularly from studies conducted after 2018. However, two high-citation papers raise methodological concerns about the effect size. The claim is likely true but overstated in its current form."*

This is already in the aggregator prompt — just needs to be extracted as a dedicated field instead of buried in a raw response string.

**Data contract additions needed:**
```kotlin
data class Dossier(
    val claimId: String,
    val originalClaim: String,
    val classification: ClaimClassification,    // Stage 1
    val expandedQueries: List<ExpandedQuery>,   // Stage 2
    val retrievalSummary: RetrievalSummary,     // Stage 3
    val paperStances: List<PaperStance>,        // Stage 4
    val verdict: Verdict,                       // Stage 5
    val createdAt: Long,
    val durationMs: Long
)

data class Verdict(
    val label: VerdictLabel,
    val overallConfidence: Float,
    val verdictNarrative: String,              // NEW — plain English summary
    val weightedScore: Float,
    val unweightedScore: Float,
    val factorScores: Map<VerdictFactor, Float>, // NEW — breakdown
    val conflictDetected: Boolean
)

enum class VerdictLabel {
    SUPPORTED,
    LIKELY_SUPPORTED,
    CONTESTED,
    LIKELY_REFUTED,
    REFUTED,
    INSUFFICIENT_EVIDENCE
}

enum class VerdictFactor {
    EVIDENCE_VOLUME, SOURCE_DIVERSITY,
    CONSENSUS_STRENGTH, EVIDENCE_RECENCY
}
```

---

## 3. Results Page UI Structure (Compose Screen Layout)

```
ResultsScreen
│
├── VerdictHeroSection          ← animated confidence arc, verdict label, narrative
│   └── VerdictFactorsPanel     ← collapsible score breakdown table
│
├── ClaimAnatomyCard            ← claim type, confidence, ambiguity alert
│   └── SubClaimChips           ← tappable, filter evidence list
│
├── EvidenceBaseSummaryBar      ← total papers, source split, quality score
│   └── FailedSourcesRow        ← inline warning if any source failed
│
├── StanceDistributionBar       ← tappable stacked bar (Support/Neutral/Refute)
│
├── EvidenceList                ← filtered by SubClaimChip or StanceBar tap
│   └── PaperStanceCard[]       ← expandable, excerpt, source badge, link
│
├── SearchStrategySection       ← collapsible, shows expanded queries + coverage
│
└── DebugPanel                  ← dev only, gated behind BuildConfig.DEBUG
    ├── TokensUsed per stage
    ├── Latency per stage
    └── Provider used per stage
```

---

## 4. ViewModel State Design

```kotlin
// ResultsViewModel.kt
data class ResultsUiState(
    val dossier: Dossier?,
    val activeStanceFilter: Stance?,       // null = show all
    val activeQueryFilter: String?,        // null = show all papers
    val isVerdictExpanded: Boolean,
    val isSearchStrategyExpanded: Boolean,
    val isFactorsExpanded: Boolean,
    val isLoading: Boolean,
    val error: String?
)

sealed class ResultsUiEvent {
    data class FilterByStance(val stance: Stance?) : ResultsUiEvent()
    data class FilterByQuery(val queryId: String?) : ResultsUiEvent()
    object ToggleVerdictExpanded : ResultsUiEvent()
    object ToggleSearchStrategy : ResultsUiEvent()
    object ShareDossier : ResultsUiEvent()
    object SaveDossier : ResultsUiEvent()
}
```

---

## 5. Implementation Phases

### Phase 1 — Data Contract Hardening (No UI changes)
*Estimated effort: 2–3 days*

**Goal:** Ensure every pipeline stage persists its intermediate output into the Dossier without dropping it.

Tasks:
1. Expand `ClaimClassification` to include `subClaims`, `restatedClaim`, `ambiguityReason`
2. Add `ExpandedQuery` model with `intent` and `resultsFound` (back-filled by Stage 3)
3. Add `RetrievalSummary` model; populate it inside the retrieval agent
4. Add `supportingExcerpt` and `confidence` to `PaperStance` — verify the Stance Actor prompt actually extracts these (update prompt if not)
5. Expand `Verdict` with `verdictNarrative`, `factorScores`, `conflictDetected`
6. Update Room schema / migrations for expanded Dossier
7. Verify `FalcoOrchestrator` passes intermediate outputs forward through the chain

**Deliverable:** All data exists in the Dossier entity. UI still unchanged.

---

### Phase 2 — Verdict Hero & Claim Anatomy (High-impact, low-risk)
*Estimated effort: 2 days*

**Goal:** Replace the current static verdict display with the animated hero + claim anatomy.

Tasks:
1. `VerdictHeroSection` composable — animated confidence arc using `Canvas` + `animateFloatAsState`
2. `VerdictLabel` mapping → color + icon + copy
3. `VerdictNarrative` text block with serif-ish styling to distinguish from UI chrome
4. `ClaimAnatomyCard` composable — claim type badge, confidence bar, ambiguity banner
5. `SubClaimChips` row — with `FilterChip` composables, wired to `ResultsViewModel`
6. Collapsible `VerdictFactorsPanel` — simple `AnimatedVisibility` wrapping a factor table

**Deliverable:** Results page hero is meaningfully richer. Sharing this screen is now "impressive."

---

### Phase 3 — Evidence List Upgrade (Core value)
*Estimated effort: 3 days*

**Goal:** Make individual papers visible, filterable, and inspectable.

Tasks:
1. `StanceDistributionBar` composable — stacked `LinearProgressIndicator`-style bar, tappable segments
2. `PaperStanceCard` composable:
   - Stance chip (SUPPORTS / REFUTES / NEUTRAL) with color coding
   - Confidence dots or mini bar
   - Paper title, authors, year, source badge
   - `AnimatedVisibility` expand/collapse for excerpt + "View paper" deep link
3. Lazy column with filter logic driven by ViewModel state
4. Empty state handling ("No papers match this filter")
5. Sort control: by confidence (default) | by recency | by citation count

**Deliverable:** The evidence list becomes the primary interactive surface of the app.

---

### Phase 4 — Retrieval Summary & Search Strategy (Transparency layer)
*Estimated effort: 1–2 days*

**Goal:** Surface the "how did we search" information for power users.

Tasks:
1. `EvidenceBaseSummaryBar` composable — compact stats row
2. `FailedSourcesRow` — only renders if `retrievalSummary.failedSources` is non-empty
3. `SearchStrategySection` — collapsible, shows `ExpandedQuery` list with intent labels and per-query paper count
4. Query coverage indicator — small dot grid (filled = returned results, empty = no results)

**Deliverable:** Users who get weak results understand *why* — reduces perceived app failure.

---

### Phase 5 — Debug Panel & Polish (Quality of life)
*Estimated effort: 1 day*

Tasks:
1. `DebugPanel` composable, gated behind `BuildConfig.DEBUG`
   - Per-stage token usage from `TokenSteward`
   - Per-stage latency in ms
   - Provider used per stage (e.g., "Stage 1: Groq llama-3.3-70b")
2. Share Dossier — export as formatted plain text or PDF
3. Save Dossier — persist to Room if not already done
4. Confidence arc entrance animation (delay after screen enter transition)
5. Skeleton loading states for each section while pipeline is running

**Deliverable:** Production-polish + developer observability.

---

## 6. Prompt Engineering Changes Required

Two existing LLM prompts need updates to support the new data fields:

### Stance Actor Prompt Addition
Add to the output schema:
```
"supporting_excerpt": "<most relevant sentence from the abstract, verbatim, max 60 words>",
"confidence_reasoning": "<one sentence explaining why this confidence level was assigned>"
```

### Aggregator Prompt Addition
Add to the output schema:
```
"verdict_narrative": "<2-3 sentence plain English summary of the evidence landscape>",
"conflict_detected": true/false,
"factor_scores": {
  "evidence_volume": 0.0-1.0,
  "source_diversity": 0.0-1.0,
  "consensus_strength": 0.0-1.0,
  "evidence_recency": 0.0-1.0
}
```

These additions are purely additive — no existing output fields change, so prompt updates are backwards-safe.

---

## 7. Room Migration Strategy

The expanded Dossier schema requires a migration. Suggested approach:

- Bump database version by 1
- Add a `MIGRATION_X_Y` that ALTERs existing tables to add nullable columns (new fields default to null for old records)
- For complex new nested objects (`RetrievalSummary`, `factorScores`), use `@TypeConverter` with JSON serialization (Kotlin Serialization) — already available in the stack

No destructive migration needed.

---

## 8. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Stance Actor doesn't reliably extract excerpts | Medium | Medium | Add excerpt extraction as a separate pass if inline fails; fall back to first 60 words of abstract |
| Aggregator narrative adds significant token cost | Low | Low | Narrative is ~100 tokens; acceptable given it's the final stage |
| Room schema migration breaks existing Dossiers | Low | High | Test migration with `MigrationTestHelper` before release; keep fallback to re-run pipeline |
| Sub-claim filtering creates confusing UX | Medium | Low | Default filter to "All"; make chips optional per user preference |
| Per-paper confidence is inconsistently formatted by LLM | High | Medium | Parse defensively; clamp to 0.0–1.0; default to 0.5 if unparseable |

---

## 9. Summary — Value Delivered Per Phase

| Phase | User-Visible Value | Dev Value |
|---|---|---|
| 1 | None (groundwork) | Richer data model, no dropped signals |
| 2 | Dramatic verdict screen upgrade | Claim parsing is transparent |
| 3 | Inspectable evidence — *the core loop* | Filter state in ViewModel |
| 4 | Explains weak results; builds trust | Source health observable |
| 5 | Polish + shareability | Stage-level diagnostics |
