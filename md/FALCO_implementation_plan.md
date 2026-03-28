# 🦅 FALCO — Implementation Plan
### *Back Every Claim with Evidence*

> **Reference baseline:** Corvus (`NaimNajmios/Corvus`) — a high-fidelity fact-checking Android
> app using an 11-stage verification pipeline, Actor-Critic LLM architecture, provider-agnostic
> routing, and deterministic grounding. FALCO inherits its engineering DNA and adapts it
> specifically for academic paper–backed claim verification.

---

## Table of Contents

1. [Corvus Reference Analysis](#1-corvus-reference-analysis)
2. [FALCO Core Concept & Differentiator](#2-falco-core-concept--differentiator)
3. [Full Architecture Diagram](#3-full-architecture-diagram)
4. [Project Module & Package Structure](#4-project-module--package-structure)
5. [Data Models](#5-data-models)
6. [Pipeline: 8-Stage Verification Flow](#6-pipeline-8-stage-verification-flow)
7. [Agent System (Actor-Critic Adaptation)](#7-agent-system-actor-critic-adaptation)
8. [Paper Retrieval Layer](#8-paper-retrieval-layer)
9. [Source Quality Gating](#9-source-quality-gating)
10. [Temporal Freshness Analysis](#10-temporal-freshness-analysis)
11. [Provider Router & Token Stewardship](#11-provider-router--token-stewardship)
12. [Room Persistence Layer](#12-room-persistence-layer)
13. [WorkManager Integration](#13-workmanager-integration)
14. [Dependency Injection — Hilt Modules](#14-dependency-injection--hilt-modules)
15. [UI Design System (Corvus-Inspired)](#15-ui-design-system-corvus-inspired)
16. [Screens & Navigation](#16-screens--navigation)
17. [Verdict Card Specification](#17-verdict-card-specification)
18. [Prompt Templates (Full)](#18-prompt-templates-full)
19. [API Reference: Semantic Scholar & OpenAlex](#19-api-reference-semantic-scholar--openalex)
20. [Free Tier Quota Summary](#20-free-tier-quota-summary)
21. [Phased Delivery Roadmap](#21-phased-delivery-roadmap)
22. [Open Questions & Future Extensions](#22-open-questions--future-extensions)

---

## 1. Corvus Reference Analysis

### What Corvus Is

Corvus is a **fact-checking** app using 11 stages: Claim Classification → Query Rewriting →
Multi-Query Retrieval → Knowledge Base Lookup → Source Quality Gating → Temporal Analysis →
Actor-Critic Synthesis → Temporal Override → Algorithmic Grounding → RAG Verification →
Token Stewardship.

### Elements Directly Reused in FALCO

| Corvus Element | Reuse in FALCO | Adaptation Notes |
|---|---|---|
| **Actor-Critic LLM Architecture** | ✅ Full reuse | Actor = Stance Classifier; Critic = Stance Challenger |
| **Provider-Agnostic Router** | ✅ Full reuse | Same routing logic, adapted model assignments |
| **Token Stewardship** | ✅ Full reuse | Per-provider quota tracking, same data structure |
| **Claim Classification** | ✅ Adapted | Classify as Empirical / Comparative / Causal / Definitional |
| **Intelligent Query Rewriting** | ✅ Full reuse | Expands hypothesis into academic search queries |
| **Source Quality Gating** | ✅ Adapted | Filter by citation count, year, open-access instead of outlet bias |
| **Temporal Analysis** | ✅ Adapted | Flag papers >5 years old for rapidly evolving fields |
| **Algorithmic Grounding** | ✅ Adapted | Verify AI-stated stances match abstract content deterministically |
| **Room Database** | ✅ Full reuse | Persist claim history, verdict audit log |
| **WorkManager** | ✅ Full reuse | Background paper retrieval + classification |
| **DM Serif Display + IBM Plex Mono** | ✅ Full reuse | Same typography identity |
| **Dynamic Color Palettes** | ❌ Not used | FALCO uses pure black monochrome only — no accent color, no dynamic themes |
| **ShareBottomSheet** | ✅ Adapted | Share verdict card as image/text |
| **Hilt DI** | ✅ Full reuse | Same module structure |
| **Ktor Networking** | ✅ Full reuse | Add Semantic Scholar + OpenAlex clients |
| **MVVM + UseCase** | ✅ Full reuse | Same architectural pattern |

### Elements NOT Used in FALCO (Corvus-Only)

| Element | Reason Excluded |
|---|---|
| Tavily / Google Fact Check API | Replaced by Semantic Scholar + OpenAlex |
| Wikidata SPARQL / Hansard | Not relevant for academic claims |
| Vision Extraction / PaliGemma | Out of scope for FALCO v1 |
| Zombie-hoax temporal override | Not applicable for academic claims |
| RAG Verification pass | Expensive; paper abstracts are short enough for direct analysis |
| LiteRT on-device ML | Out of scope for FALCO v1 |

---

## 2. FALCO Core Concept & Differentiator

**FALCO** (pronounced like the bird of prey — fast, precise) backs any hypothesis or claim
with real academic literature, then reasons whether each paper supports or opposes that claim.

### Core Differentiator vs. Corvus

| Dimension | Corvus | FALCO |
|---|---|---|
| Input | Tweet / news claim / statement | Scientific hypothesis / research gap |
| Evidence Source | Web + fact-check databases | Academic papers + theses |
| Output | Fact-check verdict (TRUE/FALSE) | Academic stance verdict (SUPPORTED/OPPOSED) |
| Audience | General public | Researchers, students, developers |
| Trust Mechanism | Source credibility ratings | Paper citation count + peer-review status |

### FALCO's Value Proposition

> "Write a hypothesis. FALCO finds the papers. You focus on the argument."

Primary use case: **Students and developers who need to justify claims in technical writing,
research proposals, or product pitches** — without spending hours on Google Scholar.

---

## 3. Full Architecture Diagram

```
                         ┌─────────────────────────────────────────────┐
                         │              FALCO Android App               │
                         │                                             │
                         │  ┌────────┐   ┌──────────┐  ┌──────────┐  │
                         │  │  Input │   │  History │  │ Settings │  │
                         │  │ Screen │   │  Screen  │  │  Screen  │  │
                         │  └───┬────┘   └──────────┘  └──────────┘  │
                         │      │                                       │
                         │  ┌───▼──────────────────────────────────┐  │
                         │  │           VerifyClaimUseCase          │  │
                         │  └───┬──────────────────────────────────┘  │
                         └──────┼──────────────────────────────────────┘
                                │
              ┌─────────────────▼──────────────────────┐
              │           FalcoOrchestrator             │
              │       (8-Stage Pipeline Manager)        │
              └──┬──────┬──────┬──────┬──────┬─────────┘
                 │      │      │      │      │
        ┌────────▼──┐ ┌─▼────┐ ┌─▼──┐ ┌─▼───▼────────┐
        │  Claim    │ │Query │ │ QA │ │  Actor-Critic │
        │Classifier │ │Expand│ │Gate│ │Stance Engine  │
        │  Agent    │ │Agent │ │    │ │               │
        └───────────┘ └──┬───┘ └────┘ └──────┬────────┘
                         │                    │
              ┌──────────▼──────┐   ┌─────────▼─────────┐
              │  Paper Retrieval │   │  Aggregator Agent  │
              │     Layer        │   │  (Final Verdict)   │
              │                  │   └───────────────────┘
              │ ┌──────────────┐ │
              │ │SemanticScholar│ │
              │ │   Client     │ │
              │ └──────────────┘ │
              │ ┌──────────────┐ │
              │ │  OpenAlex    │ │
              │ │   Client     │ │
              │ └──────────────┘ │
              └─────────────────┘
                         │
              ┌──────────▼──────────────────────────────┐
              │        Provider Router                   │
              │  Groq | Gemini | OpenRouter | Cerebras  │
              │        + Token Stewardship               │
              └──────────────────────────────────────────┘
                         │
              ┌──────────▼──────────────────────────────┐
              │         Room Database                    │
              │   ClaimEntity | VerdictEntity |          │
              │   PaperStanceEntity | QuotaEntity        │
              └──────────────────────────────────────────┘
```

---

## 4. Project Module & Package Structure

```
falco/
├── app/
│   └── src/main/java/com/najmi/falco/
│
│   ├── data/
│   │   ├── remote/
│   │   │   ├── semanticscholar/
│   │   │   │   ├── SemanticScholarClient.kt       ← Ktor HTTP client
│   │   │   │   ├── SemanticScholarApiService.kt   ← Interface with endpoint defs
│   │   │   │   └── dto/
│   │   │   │       ├── PaperSearchResponseDto.kt
│   │   │   │       └── PaperDetailDto.kt
│   │   │   ├── openalex/
│   │   │   │   ├── OpenAlexClient.kt
│   │   │   │   ├── OpenAlexApiService.kt
│   │   │   │   └── dto/
│   │   │   │       └── WorksResponseDto.kt
│   │   │   └── llm/
│   │   │       ├── GeminiClient.kt                ← Borrowed from Corvus
│   │   │       ├── GroqClient.kt                  ← Borrowed from Corvus
│   │   │       ├── OpenRouterClient.kt            ← Borrowed from Corvus
│   │   │       └── CerebrasClient.kt              ← Borrowed from Corvus
│   │   │
│   │   ├── local/
│   │   │   ├── FalcoDatabase.kt
│   │   │   ├── dao/
│   │   │   │   ├── ClaimDao.kt
│   │   │   │   ├── VerdictDao.kt
│   │   │   │   ├── PaperStanceDao.kt
│   │   │   │   └── QuotaDao.kt
│   │   │   └── entity/
│   │   │       ├── ClaimEntity.kt
│   │   │       ├── VerdictEntity.kt
│   │   │       ├── PaperStanceEntity.kt
│   │   │       └── QuotaEntity.kt
│   │   │
│   │   └── repository/
│   │       ├── PaperRepositoryImpl.kt
│   │       ├── VerdictRepositoryImpl.kt
│   │       └── QuotaRepositoryImpl.kt
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Claim.kt
│   │   │   ├── ClaimType.kt
│   │   │   ├── Paper.kt
│   │   │   ├── PaperQuality.kt
│   │   │   ├── PaperStance.kt
│   │   │   ├── Stance.kt
│   │   │   ├── Verdict.kt
│   │   │   ├── VerificationStage.kt              ← Pipeline progress tracking
│   │   │   └── ProviderQuota.kt
│   │   │
│   │   ├── repository/
│   │   │   ├── IPaperRepository.kt
│   │   │   ├── IVerdictRepository.kt
│   │   │   └── IQuotaRepository.kt
│   │   │
│   │   └── usecase/
│   │       ├── VerifyClaimUseCase.kt             ← Main orchestration entry point
│   │       ├── GetVerificationHistoryUseCase.kt
│   │       └── GetProviderQuotaUseCase.kt
│   │
│   ├── agent/
│   │   ├── IFalcoAgent.kt                        ← Base agent contract
│   │   ├── ClaimClassifierAgent.kt               ← Stage 1: Classify claim type
│   │   ├── QueryExpansionAgent.kt                ← Stage 2: Expand to search queries
│   │   ├── StanceActorAgent.kt                   ← Stage 6a: Actor — draft stance
│   │   ├── StanceCriticAgent.kt                  ← Stage 6b: Critic — challenge stance
│   │   └── AggregatorAgent.kt                    ← Stage 7: Synthesize final verdict
│   │
│   ├── pipeline/
│   │   ├── FalcoOrchestrator.kt                  ← Coordinates all 8 stages
│   │   ├── PaperQualityGate.kt                   ← Stage 4: Filter low-quality papers
│   │   ├── TemporalFreshnessAnalyzer.kt          ← Stage 5: Flag stale evidence
│   │   ├── AlgorithmicGrounding.kt               ← Stage 8: Verify stance vs abstract
│   │   └── PaperDeduplicator.kt                  ← Dedupe by DOI / title similarity
│   │
│   ├── provider/
│   │   ├── ProviderRouter.kt                     ← Borrowed from Corvus, adapted
│   │   ├── LlmProvider.kt                        ← Enum: GEMINI, GROQ, OPENROUTER, CEREBRAS
│   │   ├── TokenSteward.kt                       ← Quota tracking per provider
│   │   └── AgentModelConfig.kt                   ← Which agent uses which model
│   │
│   ├── work/
│   │   ├── VerificationWorker.kt                 ← WorkManager worker
│   │   └── WorkerFactory.kt                      ← Hilt-assisted worker factory
│   │
│   ├── di/
│   │   ├── NetworkModule.kt                      ← Ktor client bindings
│   │   ├── DatabaseModule.kt                     ← Room + DAO bindings
│   │   ├── RepositoryModule.kt                   ← Repo interface → impl bindings
│   │   ├── AgentModule.kt                        ← Agent bindings
│   │   └── WorkerModule.kt                       ← WorkManager bindings
│   │
│   └── ui/
│       ├── theme/
│       │   ├── FalcoTheme.kt
│       │   ├── Color.kt                          ← Pure black monochrome (no accent color)
│       │   ├── Type.kt                           ← DM Serif Display + IBM Plex Mono
│       │   └── Shape.kt                          ← Zero rounding everywhere
│       ├── components/
│       │   ├── FalcoHeader.kt                    ← Shared top bar (icon + wordmark + version)
│       │   ├── FalcoLabel.kt                     ← 10sp uppercase ghost mono label
│       │   ├── FalcoMetaRow.kt                   ← Label + value metadata block
│       │   ├── FalcoHairlineDivider.kt           ← 1dp #141414 divider
│       │   ├── FalcoChip.kt                      ← Zero-radius bordered tag
│       │   ├── FalcoGhostButton.kt               ← Full-width transparent bordered button
│       │   ├── FalcoSolidButton.kt               ← Full-width white fill button
│       │   ├── ConfidenceSegmentBar.kt           ← 13-segment white/dark bar
│       │   ├── EvidenceRow.kt                    ← Expandable stance row
│       │   ├── StageRow.kt                       ← Pipeline stage: icon + label + status
│       │   ├── LiveBarChart.kt                   ← 12-bar animated monitor
│       │   ├── RealTimeExtractionPanel.kt        ← Dark panel with fade-in ref cards
│       │   ├── NeuralNetworkBackground.kt        ← SVG node-graph overlay
│       │   └── ShareBottomSheet.kt               ← Borrowed from Corvus
│       ├── hypothesis/
│       │   ├── HypothesisScreen.kt               ← Input + recent list + neural SVG
│       │   └── HypothesisViewModel.kt
│       ├── pipeline/
│       │   ├── PipelineScreen.kt                 ← Stage list + extraction panel + monitor
│       │   └── PipelineViewModel.kt
│       ├── dossier/
│       │   ├── DossierScreen.kt                  ← Full verdict output (formerly VerdictScreen)
│       │   └── DossierViewModel.kt
│       └── navigation/
│           ├── FalcoNavHost.kt                   ← Bottom tab host (3 tabs)
│           └── FalcoTab.kt                       ← Hypothesis | Pipeline | Dossier
│
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties.example
└── md/
    ├── falco-brand-brief.md
    └── falco-prompt-library.md
```

---

## 5. Data Models

### 5.1 Core Domain Models

```kotlin
// ─── Claim ────────────────────────────────────────────────────────────────
data class Claim(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: ClaimType,
    val submittedAt: Long = System.currentTimeMillis()
)

enum class ClaimType {
    EMPIRICAL,      // "X causes Y"
    COMPARATIVE,    // "X performs better than Y by Z%"
    CAUSAL,         // "X leads to Y under Z conditions"
    DEFINITIONAL,   // "X is defined as Y"
    STATISTICAL     // "X% of Y do Z"
}

// ─── Paper ────────────────────────────────────────────────────────────────
data class Paper(
    val id: String,                     // Semantic Scholar paperId or OpenAlex id
    val title: String,
    val abstract: String,
    val authors: List<String>,
    val year: Int?,
    val citationCount: Int,
    val isOpenAccess: Boolean,
    val doi: String?,
    val url: String?,
    val source: PaperSource,
    val fieldsOfStudy: List<String>
)

enum class PaperSource { SEMANTIC_SCHOLAR, OPEN_ALEX }

// ─── Quality ──────────────────────────────────────────────────────────────
data class PaperQuality(
    val paper: Paper,
    val qualityScore: Float,            // 0.0–1.0 composite
    val citationTier: CitationTier,
    val freshnessFlag: FreshnessFlag,
    val passesGate: Boolean
)

enum class CitationTier { HIGH, MEDIUM, LOW, UNKNOWN }
enum class FreshnessFlag { CURRENT, AGING, STALE, UNKNOWN }

// ─── Stance ───────────────────────────────────────────────────────────────
enum class Stance { SUPPORTS, OPPOSES, NEUTRAL }

data class PaperStance(
    val paper: Paper,
    val quality: PaperQuality,
    val actorStance: Stance,
    val criticStance: Stance,           // Critic may revise
    val finalStance: Stance,            // Resolved after grounding
    val actorReasoning: String,
    val criticChallenge: String,
    val groundingScore: Float,          // 0.0–1.0; penalised if LLM hallucinates
    val confidence: Float               // Per-paper confidence
)

// ─── Verdict ──────────────────────────────────────────────────────────────
data class Verdict(
    val claimId: String,
    val lean: Stance,
    val confidence: Float,              // 0.0–1.0 overall
    val summary: String,
    val stances: List<PaperStance>,
    val totalPapersRetrieved: Int,
    val totalPapersPassedGate: Int,
    val temporalWarning: String?,       // Set if evidence is stale
    val tokenUsage: Map<LlmProvider, Int>,
    val completedAt: Long = System.currentTimeMillis()
)

// ─── Pipeline State ───────────────────────────────────────────────────────
sealed class VerificationState {
    object Idle : VerificationState()
    data class InProgress(val stage: VerificationStage, val message: String) : VerificationState()
    data class Success(val verdict: Verdict) : VerificationState()
    data class Error(val stage: VerificationStage?, val message: String) : VerificationState()
}

enum class VerificationStage(val label: String) {
    CLASSIFYING("Classifying claim"),
    EXPANDING_QUERIES("Expanding queries"),
    RETRIEVING_PAPERS("Retrieving papers"),
    QUALITY_GATING("Quality gating"),
    TEMPORAL_CHECK("Temporal analysis"),
    ACTOR_CLASSIFICATION("Actor classifying"),
    CRITIC_REVIEW("Critic reviewing"),
    AGGREGATING("Building verdict")
}
```

### 5.2 Room Entities

```kotlin
@Entity(tableName = "claims")
data class ClaimEntity(
    @PrimaryKey val id: String,
    val text: String,
    val type: String,
    val submittedAt: Long
)

@Entity(tableName = "verdicts", foreignKeys = [ForeignKey(
    entity = ClaimEntity::class,
    parentColumns = ["id"],
    childColumns = ["claimId"],
    onDelete = CASCADE
)])
data class VerdictEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val claimId: String,
    val lean: String,
    val confidence: Float,
    val summary: String,
    val totalPapersRetrieved: Int,
    val totalPapersPassedGate: Int,
    val temporalWarning: String?,
    val completedAt: Long
)

@Entity(tableName = "paper_stances")
data class PaperStanceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val verdictId: String,
    val paperTitle: String,
    val paperAbstract: String,
    val paperYear: Int?,
    val paperCitationCount: Int,
    val paperUrl: String?,
    val finalStance: String,
    val actorReasoning: String,
    val criticChallenge: String,
    val groundingScore: Float,
    val confidence: Float
)

@Entity(tableName = "provider_quota")
data class QuotaEntity(
    @PrimaryKey val provider: String,
    val tokensUsedToday: Int,
    val requestsUsedToday: Int,
    val lastResetDate: String          // "2026-03-27"
)
```

---

## 6. Pipeline: 8-Stage Verification Flow

FALCO's pipeline is adapted from Corvus's 11-stage flow, trimmed to fit the academic
verification context. Each stage emits a `VerificationStage` update to the UI.

```
Stage 1: CLAIM CLASSIFICATION
  └─ ClaimClassifierAgent
  └─ Input:  raw claim string
  └─ Output: ClaimType enum + cleaned claim text

Stage 2: QUERY EXPANSION
  └─ QueryExpansionAgent
  └─ Input:  Claim + ClaimType
  └─ Output: List<String> (3 targeted academic search queries)

Stage 3: PAPER RETRIEVAL
  └─ PaperRepositoryImpl (SemanticScholar + OpenAlex in parallel)
  └─ Input:  List<String> queries
  └─ Output: List<Paper> (up to 5 per query, 15 total before dedup)
  └─ Dedup:  PaperDeduplicator by DOI first, then title cosine similarity

Stage 4: QUALITY GATING
  └─ PaperQualityGate (algorithmic, no LLM)
  └─ Input:  List<Paper>
  └─ Output: List<PaperQuality> — only papers passing the gate proceed
  └─ Rules:  citationCount ≥ threshold by field, year filter, abstract length ≥ 100 chars

Stage 5: TEMPORAL FRESHNESS
  └─ TemporalFreshnessAnalyzer (algorithmic)
  └─ Input:  List<PaperQuality>, ClaimType
  └─ Output: FreshnessFlag per paper + optional temporalWarning string
  └─ Rules:  CS/AI fields: papers >3y flagged AGING, >6y flagged STALE
             Medical:     papers >2y flagged AGING, >5y flagged STALE
             Historical:  no staleness flag

Stage 6: ACTOR-CRITIC CLASSIFICATION
  └─ StanceActorAgent + StanceCriticAgent (parallel per paper, then sequential)
  └─ For each paper:
       a) StanceActorAgent → { stance, reasoning }
       b) StanceCriticAgent receives Actor output → may revise stance
  └─ Output: List<PaperStance> (with both Actor and Critic reasoning)

Stage 7: ALGORITHMIC GROUNDING
  └─ AlgorithmicGrounding (deterministic, no LLM)
  └─ Input:  List<PaperStance>
  └─ Check:  Does actor reasoning reference terms actually in the abstract?
  └─ Output: groundingScore per paper (penalty applied if reasoning hallucinates)

Stage 8: AGGREGATION
  └─ AggregatorAgent
  └─ Input:  Claim, List<PaperStance with grounding scores>
  └─ Output: Verdict { lean, confidence, summary, temporalWarning }
```

### Orchestrator Pseudocode

```kotlin
class FalcoOrchestrator @Inject constructor(
    private val claimClassifier: ClaimClassifierAgent,
    private val queryExpander: QueryExpansionAgent,
    private val paperRepo: IPaperRepository,
    private val qualityGate: PaperQualityGate,
    private val temporalAnalyzer: TemporalFreshnessAnalyzer,
    private val stanceActor: StanceActorAgent,
    private val stanceCritic: StanceCriticAgent,
    private val grounding: AlgorithmicGrounding,
    private val aggregator: AggregatorAgent,
    private val verdictRepo: IVerdictRepository
) {
    fun verify(claim: String): Flow<VerificationState> = flow {

        // Stage 1
        emit(InProgress(CLASSIFYING, "Identifying claim type..."))
        val classifiedClaim = claimClassifier.classify(claim)

        // Stage 2
        emit(InProgress(EXPANDING_QUERIES, "Generating academic search queries..."))
        val queries = queryExpander.expand(classifiedClaim)

        // Stage 3
        emit(InProgress(RETRIEVING_PAPERS, "Searching Semantic Scholar + OpenAlex..."))
        val papers = paperRepo.searchAll(queries)

        // Stage 4
        emit(InProgress(QUALITY_GATING, "Filtering ${papers.size} papers by quality..."))
        val qualityPapers = qualityGate.filter(papers, classifiedClaim.type)

        // Stage 5
        emit(InProgress(TEMPORAL_CHECK, "Checking evidence freshness..."))
        val analyzedPapers = temporalAnalyzer.analyze(qualityPapers, classifiedClaim.type)

        // Stage 6 — parallel Actor, then sequential Critic
        emit(InProgress(ACTOR_CLASSIFICATION, "Classifying stances across ${analyzedPapers.size} papers..."))
        val actorResults = analyzedPapers.map { paper ->
            async { stanceActor.classify(classifiedClaim.text, paper) }
        }.awaitAll()

        emit(InProgress(CRITIC_REVIEW, "Critic reviewing stances..."))
        val criticResults = actorResults.map { actorStance ->
            stanceCritic.challenge(classifiedClaim.text, actorStance)
        }

        // Stage 7
        val groundedStances = grounding.verify(criticResults)

        // Stage 8
        emit(InProgress(AGGREGATING, "Building verdict..."))
        val verdict = aggregator.aggregate(classifiedClaim.text, groundedStances)

        // Persist
        verdictRepo.save(classifiedClaim, verdict)

        emit(Success(verdict))
    }.catch { e ->
        emit(Error(null, e.message ?: "Unknown error"))
    }
}
```

---

## 7. Agent System (Actor-Critic Adaptation)

### 7.1 Base Agent Contract

```kotlin
interface IFalcoAgent<I, O> {
    val agentName: String
    val preferredProvider: LlmProvider
    val preferredModel: String
    suspend fun execute(input: I): O
}
```

### 7.2 ClaimClassifierAgent

**Role:** Identify the semantic type of the hypothesis so downstream agents can tailor
their reasoning. Borrowed directly from Corvus's Stage 1 Claim Classification.

**Provider:** Groq `llama-3.1-8b-instant` — fast, trivial task.

**System Prompt:**
```
You are an academic claim classifier. 
Analyze the claim and return ONLY a JSON object with no preamble or markdown.
```

**User Prompt:**
```
Claim: "{claim}"

Classify this claim and return:
{
  "type": "EMPIRICAL" | "COMPARATIVE" | "CAUSAL" | "DEFINITIONAL" | "STATISTICAL",
  "cleanedClaim": "<cleaned, grammatically precise version of the claim>",
  "keyTerms": ["<term1>", "<term2>", "<term3>"]
}
```

### 7.3 QueryExpansionAgent

**Role:** Transform the claim into 3 distinct academic search queries. Each query should
approach the topic from a different angle — confirming the claim, refuting it, or finding
related methodology papers.

**Provider:** Groq `llama-3.1-8b-instant`.

**System Prompt:**
```
You are an academic search query engineer.
Generate search queries that would be entered into Google Scholar or Semantic Scholar.
Return ONLY a JSON array of 3 strings. No numbering, no markdown.
```

**User Prompt:**
```
Claim type: {claimType}
Claim: "{claim}"
Key terms: {keyTerms}

Generate 3 academic search queries:
- Query 1: Should retrieve papers that could CONFIRM the claim
- Query 2: Should retrieve papers that could CHALLENGE or REFUTE the claim  
- Query 3: Should retrieve foundational/methodology papers about the topic

Return: ["query1", "query2", "query3"]
```

### 7.4 StanceActorAgent (Actor in Actor-Critic)

**Role:** Given a claim and a paper's abstract, draft an initial stance with reasoning.
This is the high-throughput pass — runs in parallel across all papers.

**Provider:** Groq `llama-3.3-70b-versatile` for quality reasoning at speed.

**System Prompt:**
```
You are a rigorous academic stance classifier.
You read paper abstracts and determine whether they support, oppose, or are neutral 
toward a given research claim.
Return ONLY a JSON object. No preamble, no markdown.
```

**User Prompt:**
```
CLAIM: "{claim}"

PAPER:
Title: "{title}"
Year: {year}
Citation Count: {citationCount}
Abstract: "{abstract}"

Analyze whether this paper's abstract SUPPORTS, OPPOSES, or is NEUTRAL toward the claim.

Return:
{
  "stance": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
  "reasoning": "<one to two sentences citing specific content from the abstract>",
  "relevanceScore": 0.0-1.0,
  "keyEvidence": "<the specific phrase or finding in the abstract that drives the stance>"
}
```

### 7.5 StanceCriticAgent (Critic in Actor-Critic)

**Role:** Receive the Actor's output and challenge it. The Critic may agree, partially
revise, or fully overturn the Actor's stance. This is the quality assurance pass.

Borrowed from Corvus's Actor-Critic architecture where the Critic audits the Actor's draft.

**Provider:** Gemini `gemini-2.0-flash` — better nuanced reasoning for critique.

**System Prompt:**
```
You are an academic peer-reviewer acting as devil's advocate.
You receive a stance classification for a research paper and must critically evaluate it.
Your job is to challenge overconfident classifications and correct misreadings of abstracts.
Return ONLY a JSON object.
```

**User Prompt:**
```
CLAIM: "{claim}"

PAPER ABSTRACT: "{abstract}"

ACTOR CLASSIFICATION:
- Stance: {actorStance}
- Reasoning: "{actorReasoning}"
- Key Evidence: "{keyEvidence}"

Critically evaluate this classification:
1. Is the actor's reasoning actually grounded in what the abstract says?
2. Does the abstract address the claim directly, or is the actor over-inferring?
3. Are there nuances the actor missed?

Return:
{
  "agreedWithActor": true | false,
  "revisedStance": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
  "challenge": "<one sentence: your critique of the actor or confirmation of it>",
  "finalReasoning": "<the definitive one-sentence reasoning for the revised stance>"
}
```

### 7.6 AggregatorAgent

**Role:** Consume all grounded stances and produce the final verdict card content.

**Provider:** Gemini `gemini-2.0-flash`.

**System Prompt:**
```
You are a research synthesis AI. You receive a list of academic paper stance evaluations
for a hypothesis. Produce a calibrated verdict with a clear confidence level.
Be conservative: if evidence is mixed, reflect that in the confidence score.
Return ONLY a JSON object.
```

**User Prompt:**
```
CLAIM: "{claim}"
CLAIM TYPE: {claimType}

PAPER STANCES (grounded):
{papersJson}

Each paper has: title, year, citationCount, finalStance, finalReasoning, groundingScore

Produce a synthesis:
{
  "lean": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
  "confidence": 0.0-1.0,
  "summary": "<2-3 sentences synthesizing the evidence landscape>",
  "supportingCount": <integer>,
  "opposingCount": <integer>,
  "neutralCount": <integer>,
  "dominantField": "<primary field of study from the papers>",
  "caveat": "<optional: one sentence about limitations of this evidence set, or null>"
}
```

---

## 8. Paper Retrieval Layer

### 8.1 IPaperRepository

```kotlin
interface IPaperRepository {
    suspend fun search(query: String, limit: Int = 5): List<Paper>
    suspend fun searchAll(queries: List<String>): List<Paper>
    suspend fun getPaperById(id: String, source: PaperSource): Paper?
}
```

### 8.2 SemanticScholarClient

**Base URL:** `https://api.semanticscholar.org/graph/v1`

**No API key required for basic use. Recommended fields:**

```
fields=paperId,title,abstract,authors,year,citationCount,
       isOpenAccess,openAccessPdf,externalIds,fieldsOfStudy,publicationTypes
```

**Key Endpoints:**

```
GET /paper/search?query={query}&limit={limit}&fields={fields}
GET /paper/{paperId}?fields={fields}
```

**Ktor Client Setup:**

```kotlin
class SemanticScholarClient @Inject constructor(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://api.semanticscholar.org/graph/v1"
    private val fields = listOf(
        "paperId", "title", "abstract", "authors", "year",
        "citationCount", "isOpenAccess", "externalIds", "fieldsOfStudy"
    ).joinToString(",")

    suspend fun searchPapers(query: String, limit: Int = 5): List<Paper> {
        val response: PaperSearchResponseDto = httpClient.get("$baseUrl/paper/search") {
            parameter("query", query)
            parameter("limit", limit)
            parameter("fields", fields)
        }.body()
        return response.data.map { it.toDomain() }
    }
}
```

**Rate Limit:** 100 requests/5 min unauthenticated; 1 request/second recommended.
Use exponential back-off on 429 responses.

### 8.3 OpenAlexClient

**Base URL:** `https://api.openalex.org`

**Recommended to include polite pool email:**
`https://api.openalex.org/works?mailto=youremail@example.com`

**Key Endpoint:**

```
GET /works?search={query}&filter=has_abstract:true&per_page={limit}&select={fields}
```

**Fields:**
```
id,doi,title,abstract_inverted_index,authorships,publication_year,
cited_by_count,open_access,primary_location,concepts
```

**Note:** OpenAlex stores abstracts as inverted index. FALCO must reconstruct the abstract:

```kotlin
fun reconstructAbstract(invertedIndex: Map<String, List<Int>>): String {
    if (invertedIndex.isEmpty()) return ""
    val maxPos = invertedIndex.values.flatten().max()
    val words = Array(maxPos + 1) { "" }
    invertedIndex.forEach { (word, positions) ->
        positions.forEach { pos -> words[pos] = word }
    }
    return words.joinToString(" ")
}
```

### 8.4 PaperDeduplicator

```kotlin
class PaperDeduplicator {
    fun deduplicate(papers: List<Paper>): List<Paper> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Paper>()
        for (paper in papers) {
            // Primary dedup: DOI
            val key = paper.doi ?: normalizeTitle(paper.title)
            if (seen.add(key)) result.add(paper)
        }
        return result
    }

    private fun normalizeTitle(title: String): String =
        title.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .split(" ")
            .filter { it.length > 3 }
            .take(6)
            .joinToString(" ")
}
```

---

## 9. Source Quality Gating

No LLM calls. Purely algorithmic. Borrowed from Corvus's Source Quality Gating stage
(adapted from outlet credibility to academic paper quality).

### 9.1 Quality Score Formula

```
qualityScore = (citationWeight * 0.4) + (freshnessWeight * 0.3) + (openAccessWeight * 0.15) + (abstractLengthWeight * 0.15)
```

### 9.2 Citation Tier Thresholds

| Field | HIGH (≥) | MEDIUM (≥) | LOW |
|---|---|---|---|
| Computer Science / AI | 50 | 10 | <10 |
| Medicine / Biology | 100 | 20 | <20 |
| Social Sciences | 20 | 5 | <5 |
| Engineering | 30 | 5 | <5 |
| Unknown / Other | 10 | 3 | <3 |

### 9.3 Gate Pass Rules

A paper passes the quality gate if ALL of the following are true:
- `abstract.length >= 80` (enough text for stance analysis)
- `abstract != null && abstract != ""`
- `qualityScore >= 0.25` (minimum composite threshold)
- NOT (`citationTier == LOW && freshnessFlag == STALE`) — low-quality AND stale is filtered

### 9.4 PaperQualityGate Implementation

```kotlin
class PaperQualityGate @Inject constructor() {
    
    fun filter(papers: List<Paper>, claimType: ClaimType): List<PaperQuality> {
        return papers.map { paper ->
            val citationTier = citationTier(paper)
            val freshnessFlag = freshnessFlag(paper, claimType)
            val score = compositeScore(paper, citationTier, freshnessFlag)
            val passes = passesGate(paper, score, citationTier, freshnessFlag)
            PaperQuality(paper, score, citationTier, freshnessFlag, passes)
        }.filter { it.passesGate }
    }

    private fun citationTier(paper: Paper): CitationTier {
        val field = paper.fieldsOfStudy.firstOrNull() ?: "Unknown"
        val thresholds = CITATION_THRESHOLDS[field] ?: CITATION_THRESHOLDS["Unknown"]!!
        return when {
            paper.citationCount >= thresholds.high -> HIGH
            paper.citationCount >= thresholds.medium -> MEDIUM
            else -> LOW
        }
    }
}
```

---

## 10. Temporal Freshness Analysis

Directly adapted from Corvus's Temporal Analysis stage. Instead of detecting "zombie hoaxes"
in news, FALCO detects stale academic evidence that may have been superseded.

### 10.1 Staleness Thresholds by Field

| Field Category | AGING (years) | STALE (years) |
|---|---|---|
| Computer Science, AI, ML | > 3 | > 6 |
| Medicine, Biology, Pharma | > 2 | > 5 |
| Physics, Chemistry | > 4 | > 8 |
| Engineering, Electronics | > 4 | > 8 |
| Social Sciences, Psychology | > 5 | > 10 |
| History, Philosophy | No flag | No flag |
| Unknown | > 5 | > 10 |

### 10.2 Temporal Warning Generation

If >50% of papers passed the gate are flagged STALE, a `temporalWarning` is appended to
the final Verdict:

```
"⚠ Temporal Note: Most supporting evidence predates [year]. This field evolves rapidly.
   Findings may have been superseded by more recent work."
```

---

## 11. Provider Router & Token Stewardship

Directly ported from Corvus's intelligent provider routing and token stewardship.

### 11.1 Agent → Model Assignment

| Agent | Primary Provider | Primary Model | Fallback |
|---|---|---|---|
| ClaimClassifier | Groq | `llama-3.1-8b-instant` | Cerebras `llama3.1-8b` |
| QueryExpansion | Groq | `llama-3.1-8b-instant` | Cerebras `llama3.1-8b` |
| StanceActor | Groq | `llama-3.3-70b-versatile` | OpenRouter `meta-llama/llama-3.3-70b` |
| StanceCritic | Gemini | `gemini-2.0-flash` | Groq `llama-3.3-70b-versatile` |
| Aggregator | Gemini | `gemini-2.0-flash-lite` | Groq `llama-3.3-70b-versatile` |

**Rationale:**
- StanceActor runs N times in parallel → Groq for speed and free token volume.
- StanceCritic runs sequentially → Gemini for nuanced critique quality.
- ClaimClassifier + QueryExpansion are simple → 8B model is sufficient.

### 11.2 ProviderRouter

```kotlin
class ProviderRouter @Inject constructor(
    private val tokenSteward: TokenSteward,
    private val agentModelConfig: AgentModelConfig
) {
    suspend fun routeFor(agentName: String): Pair<LlmProvider, String> {
        val primary = agentModelConfig.primaryFor(agentName)
        return if (tokenSteward.hasQuota(primary.first)) {
            primary
        } else {
            agentModelConfig.fallbackFor(agentName)
        }
    }
}
```

### 11.3 TokenSteward

```kotlin
class TokenSteward @Inject constructor(
    private val quotaRepo: IQuotaRepository
) {
    // Daily limits (conservative, below free tier maximums)
    private val dailyLimits = mapOf(
        LlmProvider.GROQ to 400_000,        // ~80% of 500k/day
        LlmProvider.GEMINI to 1_200,        // ~80% of 1500 req/day
        LlmProvider.CEREBRAS to 800_000,    // ~80% of 1M/day
        LlmProvider.OPENROUTER to 50         // Conservative for free tier
    )

    suspend fun recordUsage(provider: LlmProvider, tokensUsed: Int) {
        quotaRepo.incrementTokens(provider, tokensUsed)
    }

    suspend fun hasQuota(provider: LlmProvider): Boolean {
        val used = quotaRepo.getTokensUsedToday(provider)
        return used < (dailyLimits[provider] ?: 0)
    }

    suspend fun getQuotaSummary(): Map<LlmProvider, Float> {
        return LlmProvider.values().associateWith { provider ->
            val used = quotaRepo.getTokensUsedToday(provider).toFloat()
            val limit = dailyLimits[provider]?.toFloat() ?: 1f
            used / limit
        }
    }
}
```

---

## 12. Room Persistence Layer

### 12.1 FalcoDatabase

```kotlin
@Database(
    entities = [ClaimEntity::class, VerdictEntity::class,
                PaperStanceEntity::class, QuotaEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(FalcoTypeConverters::class)
abstract class FalcoDatabase : RoomDatabase() {
    abstract fun claimDao(): ClaimDao
    abstract fun verdictDao(): VerdictDao
    abstract fun paperStanceDao(): PaperStanceDao
    abstract fun quotaDao(): QuotaDao
}
```

### 12.2 VerdictDao

```kotlin
@Dao
interface VerdictDao {
    @Transaction
    @Query("SELECT * FROM verdicts ORDER BY completedAt DESC")
    fun getAllVerdicts(): Flow<List<VerdictWithStances>>

    @Query("SELECT * FROM verdicts WHERE claimId = :claimId LIMIT 1")
    suspend fun getVerdictForClaim(claimId: String): VerdictEntity?

    @Insert(onConflict = REPLACE)
    suspend fun insert(verdict: VerdictEntity)

    @Query("DELETE FROM verdicts WHERE completedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

data class VerdictWithStances(
    @Embedded val verdict: VerdictEntity,
    @Relation(parentColumn = "id", entityColumn = "verdictId")
    val stances: List<PaperStanceEntity>
)
```

### 12.3 QuotaDao

```kotlin
@Dao
interface QuotaDao {
    @Query("SELECT tokensUsedToday FROM provider_quota WHERE provider = :provider AND lastResetDate = :today")
    suspend fun getTokensUsedToday(provider: String, today: String): Int?

    @Insert(onConflict = REPLACE)
    suspend fun upsertQuota(quota: QuotaEntity)

    @Query("UPDATE provider_quota SET tokensUsedToday = tokensUsedToday + :tokens WHERE provider = :provider")
    suspend fun incrementTokens(provider: String, tokens: Int)
}
```

---

## 13. WorkManager Integration

For longer verifications (>10 papers), FALCO offloads the pipeline to WorkManager,
allowing the user to navigate away and receive a notification when the verdict is ready.
Borrowed pattern from Corvus's "Resilient Backgrounding" feature.

```kotlin
class VerificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val orchestrator: FalcoOrchestrator,
    private val notificationManager: FalcoNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val claimText = inputData.getString(KEY_CLAIM) ?: return Result.failure()
        val claimId = inputData.getString(KEY_CLAIM_ID) ?: return Result.failure()

        return try {
            orchestrator.verify(claimText).collect { state ->
                when (state) {
                    is VerificationState.InProgress ->
                        notificationManager.updateProgress(claimId, state.message)
                    is VerificationState.Success ->
                        notificationManager.showVerdictReady(claimId, state.verdict)
                    is VerificationState.Error ->
                        notificationManager.showError(claimId, state.message)
                    else -> {}
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_CLAIM = "claim"
        const val KEY_CLAIM_ID = "claimId"
        const val WORK_TAG = "falco_verification"
    }
}
```

**Enqueue pattern in VerifyClaimUseCase:**

```kotlin
val workRequest = OneTimeWorkRequestBuilder<VerificationWorker>()
    .setInputData(workDataOf(KEY_CLAIM to claim, KEY_CLAIM_ID to claimId))
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    .addTag(WORK_TAG)
    .build()

WorkManager.getInstance(context).enqueueUniqueWork(
    claimId,
    ExistingWorkPolicy.REPLACE,
    workRequest
)
```

---

## 14. Dependency Injection — Hilt Modules

### NetworkModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    }

    @Provides @Singleton
    fun provideSemanticScholarClient(client: HttpClient) =
        SemanticScholarClient(client)

    @Provides @Singleton
    fun provideOpenAlexClient(client: HttpClient) =
        OpenAlexClient(client)
}
```

### DatabaseModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FalcoDatabase =
        Room.databaseBuilder(context, FalcoDatabase::class.java, "falco.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideClaimDao(db: FalcoDatabase) = db.claimDao()
    @Provides fun provideVerdictDao(db: FalcoDatabase) = db.verdictDao()
    @Provides fun providePaperStanceDao(db: FalcoDatabase) = db.paperStanceDao()
    @Provides fun provideQuotaDao(db: FalcoDatabase) = db.quotaDao()
}
```

---

## 15. UI Design System (Mockup-Refined)

FALCO's visual identity is a strict **Monochromatic Precise** system — pure black canvas,
white typography, no accent color whatsoever. Stances are conveyed by text brightness alone,
not by color. The design communicates authority through scarcity: every element must justify
its presence.

### 15.1 Visual Identity Principles

| Principle | Application |
|---|---|
| **Pure black canvas** | `#000000` background throughout — no grey surfaces |
| **No accent color** | Amber dropped. Stance contrast via white/grey/dark-grey text only |
| **Strict typography hierarchy** | 82sp verdict label → 42sp serif headings → 10-13sp mono UI |
| **Zero rounding** | No `BorderRadius` anywhere, including type-detection chips |
| **Hairline separators** | `#141414` / `#0F0F0F` — nearly invisible, structural only |
| **Uppercase mono for metadata** | All labels, tags, stage names: uppercase + letter-spacing 1.5–2.5 |
| **Left-border accent** | Only decorative element: 2px `#1A1A1A`–`#2A2A2A` left border on blockquotes and list items |
| **Neural SVG background** | Dashed-line node graph at 25% opacity, masked by top/bottom gradient, on Hypothesis screen only |

### 15.2 Color Palette

```kotlin
// falco/ui/theme/Color.kt

// Canvas
val FalcoBg          = Color(0xFF000000)   // True black — all screens
val FalcoSurface     = Color(0xFF080808)   // Inset panels (extraction box, monitor)
val FalcoSurfaceBorder = Color(0xFF141414) // Panel border
val FalcoDivider     = Color(0xFF0F0F0F)   // Row separators (nearly invisible)
val FalcoChip        = Color(0xFF1C1C1C)   // Chip / tag border

// Text hierarchy
val FalcoTextPrimary = Color(0xFFFFFFFF)   // Headlines, active stages
val FalcoTextBody    = Color(0xFFBBBBBB)   // Body paragraph text
val FalcoTextMuted   = Color(0xFF555555)   // Secondary metadata
val FalcoTextGhost   = Color(0xFF3A3A3A)   // Labels, inactive stages, placeholder
val FalcoTextInvisible = Color(0xFF1E1E1E) // Pending stage text

// Stance — brightness only, no hue
val FalcoStanceSupports = Color(0xFFFFFFFF) // White
val FalcoStanceNeutral  = Color(0xFF555555) // Mid-grey
val FalcoStanceOpposes  = Color(0xFF3A3A3A) // Dark-grey (barely visible — low weight)

// Confidence bar segments
val FalcoBarFilled  = Color(0xFFFFFFFF)
val FalcoBarEmpty   = Color(0xFF1E1E1E)
```

> **No amber, no green, no red.** Stance weight is encoded purely in text brightness
> and the numeric grounding score. This is intentional — FALCO presents evidence, not verdicts.

### 15.3 Typography

```kotlin
// falco/ui/theme/Type.kt

val FalcoTypography = Typography(
    // Used for: "SUPPORTED" verdict label on Dossier screen
    displayLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.dm_serif_display)),
        fontSize = 82.sp,
        lineHeight = 72.sp,      // Tight — allows controlled overflow at screen edge
        letterSpacing = (-1).sp
    ),
    // Used for: Screen titles ("Formalize your Inquiry", "Verifying claim...")
    headlineLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.dm_serif_display)),
        fontSize = 42.sp,
        lineHeight = 44.sp
    ),
    // Used for: Synthesis heading on Dossier ("Synthesis of grounding data...")
    headlineMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.dm_serif_display)),
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    // Used for: Confidence percentage "72%"
    headlineSmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.dm_serif_display)),
        fontSize = 36.sp
    ),
    // Used for: Body text in summary box, blockquotes
    bodyMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.ibm_plex_mono)),
        fontSize = 13.sp,
        lineHeight = 22.sp
    ),
    // Used for: Evidence row titles, stage names
    bodySmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.ibm_plex_mono)),
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
    ),
    // Used for: All uppercase labels (METADATA, GROUNDING, EVIDENCE LIST, etc.)
    labelSmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.ibm_plex_mono)),
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Normal
    ),
    // Used for: Source IDs, auth hash, stat values in monitor
    labelMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.ibm_plex_mono)),
        fontSize = 9.sp,
        letterSpacing = 0.5.sp,
        color = FalcoTextGhost
    )
)
```

### 15.4 Shape

Zero rounding everywhere. No exceptions — not even chips.

```kotlin
val FalcoShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(0.dp),
    medium     = RoundedCornerShape(0.dp),
    large      = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)
```

### 15.5 Reusable Compose Components

| Component | Description |
|---|---|
| `FalcoHeader` | Top bar: book icon + "FALCO" wordmark left; version/ID string right |
| `FalcoLabel` | Uppercase 10sp mono label with letterSpacing 2sp and `FalcoTextGhost` color |
| `FalcoMetaRow` | Two-line block: `FalcoLabel` above, 11sp mono value below |
| `FalcoHairlineDivider` | 1dp `#141414` horizontal rule |
| `FalcoChip` | Text inside `Border(1dp, #2A2A2A)`, 0dp rounding, 5dp×12dp padding |
| `FalcoGhostButton` | Full-width, `Border(1dp, #1C1C1C)`, transparent bg, 20dp vertical padding, 3sp letter-spacing |
| `FalcoSolidButton` | Full-width, white bg, black text, 22dp vertical padding |
| `ConfidenceSegmentBar` | Row of 13 equally-spaced 3dp-tall rectangles: filled=`#FFF`, empty=`#1E1E1E` |
| `NeuralNetworkBackground` | SVG overlay: dashed edges + dot nodes at 25% opacity, masked by gradient |
| `LiveBarChart` | 12 animated bars, 3 tones (white/mid/dark), 280ms transition interval |
| `SourceIdTag` | 10sp `FalcoTextGhost` mono below blockquote: `SOURCE_ID: ALFA_992_BETA` |

---

## 16. Screens & Navigation

### Navigation Model

FALCO uses a **persistent bottom navigation bar** with three tabs instead of a deep navigation
graph. The pipeline and dossier are not separate destinations pushed onto a stack — they are
the same bottom-nav tabs that activate as verification progresses.

```kotlin
sealed class FalcoTab(val route: String, val label: String) {
    object Hypothesis : FalcoTab("hypothesis", "HYPOTHESIS")
    object Pipeline   : FalcoTab("pipeline",   "PIPELINE")
    object Dossier    : FalcoTab("dossier",    "DOSSIER")
}
```

**Bottom nav icons** (custom SVG, no Material icons):
- `Hypothesis` — circle with filled inner dot (inquiry symbol)
- `Pipeline` — 2×2 grid of squares (process symbol)
- `Dossier` — rectangle with three horizontal lines (document symbol)

Active tab shows `borderTop: 1dp white` on the tab button itself. Inactive tabs use
`#3A3A3A` for both icon stroke and label text.

### Header Component

Every screen shares the same `FalcoHeader` composable:

```
┌────────────────────────────────────────────────────┐
│  [□] FALCO                       v4.0.2_STABLE     │
│      ──────────────────────────────────────────────│
```

- Left: book/document icon (custom SVG, 18×18) + "FALCO" in 13sp mono, letterSpacing 2.5sp
- Right: version string / session ID in 10sp `FalcoTextGhost` mono
- Bottom border: 1dp `#141414` hairline

### 16.1 HypothesisScreen

```
┌────────────────────────────────────────────────────┐
│  [□] FALCO                          ID: 882-0X     │
│  ─────────────────────────────────────────────────│
│                                                    │
│  Formalize your                                    │  ← 42sp DM Serif Display
│  Inquiry                                           │
│                                                    │
│  [DETECTED TYPE: COMPARATIVE] ─────────────────── │  ← Zero-radius chip + hairline
│                                                    │
│                                                    │
│  Enter the parameters of your inquiry here...     │  ← Ghost textarea, no border
│                                                    │
│                                                    │
│                                                    │
│  INPUT IS BEING CROSS-REFERENCED AGAINST GLOBAL   │  ← 10sp uppercase ghost text
│  DOSSIERS IN REAL-TIME. ENSURE TECHNICAL          │
│  NOMENCLATURE IS PRECISE FOR OPTIMAL VERIFICATION.│
│                                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │           VERIFY WITH EVIDENCE               │  │  ← Solid white button
│  └──────────────────────────────────────────────┘  │
│                                                    │
│                                                    │
│  RECENT HYPOTHESES                                │  ← 10sp ghost label
│  │ QUANTUM_ENTANGLEMENT_DISCREPANCY_V4 [VERIFIED]│  ← Left-border row + status tag
│  │ NEURAL_SYNAPSE_MAPPING_DEVIATION    [PENDING] │
│  │ ATMOSPHERIC_CARBON_VALENCE_SHIFT    [NEW]     │
│                                                    │
│  ≋ ≋ ≋  SVG neural network @ 25% opacity  ≋ ≋ ≋  │  ← Fades into black
└────────────────────────────────────────────────────┘
```

**Claim type chip** auto-updates as the user types (debounced 600ms). The chip and its
trailing hairline are on the same row using `Row { Chip(); Divider(modifier = weight(1f)) }`.

**Recent hypotheses** rows: left-bordered `2dp #1A1A1A`, left-padded 14dp. Status tag is a
zero-radius `Border(1dp, #1C1C1C)` box. Names are displayed in ALL_CAPS with underscores
to reinforce the academic identifier aesthetic.

**Neural network SVG background** renders at the bottom of the scrollable content, clipped
by a `Box` with a `Brush.verticalGradient` overlay from transparent at center to black at
top and bottom. The SVG draws ~20 nodes (filled circles, 2.5dp radius) connected by dashed
edges (2dp dash, 4dp gap), all in white at 25% alpha.

### 16.2 PipelineScreen

```
┌────────────────────────────────────────────────────┐
│  [□] FALCO                          v4.0.2_STABLE  │
│  ─────────────────────────────────────────────────│
│                                                    │
│  Verifying                                        │  ← 42sp DM Serif
│  claim...                                          │
│                                                    │
│  │ "The metabolic half-life of synthetic           │  ← Left-border blockquote
│  │  glucocorticoids in canine models is            │    14sp body, #ccc
│  │  inversely proportional to receptor             │
│  │  affinity indices."                             │
│  │ SOURCE_ID: ALFA_992_BETA                        │    10sp ghost mono below
│                                                    │
│  VERIFICATION_STAGES                              │  ← Ghost label
│                                                    │
│  ✓  CLASSIFYING         COMPLETE                  │  ← Done: white tick
│  ✓  EXPANDING           COMPLETE                  │
│  ✓  RETRIEVING          COMPLETE                  │
│  ■  GATING              PROCESSING_88%            │  ← Active: blinking square
│  —  SYNTHESIZING        PENDING                   │  ← Pending: em-dash, ghost color
│  —  VALIDATING          PENDING                   │
│  —  CORROBORATING       PENDING                   │
│  —  FINALIZING          PENDING                   │
│                                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │  REAL-TIME_EXTRACTION                        │  │  ← Dark panel #080808
│  │                                              │  │
│  │  REF_001: J. Endocrine Res. (2022)           │  │  ← Fade-in as retrieved
│  │  "Kinetic modeling suggests higher..."       │  │
│  │                                              │  │
│  │  REF_002: Synthetic Steroid Dynamics         │  │
│  │  "Non-linear metabolic decay observed..."   │  │
│  └──────────────────────────────────────────────┘  │
│                                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │  LIVE MONITOR: NEURAL_LAYER_08          [■]  │  │  ← Dark panel
│  │                                              │  │
│  │  ▁▃▂▅▄▇▅▃▆▇▄▆  (animated bar chart)        │  │  ← 12 bars, 3 tones
│  │                                              │  │
│  │  SIG.STRENGTH:0.992  LATENCY:0.04MS  14.2GB │  │  ← 9sp ghost stats
│  └──────────────────────────────────────────────┘  │
│                                                    │
│  [           HALT_PROCESS              ]           │  ← Ghost button
└────────────────────────────────────────────────────┘
```

**Stage row anatomy:**

```kotlin
// Each row: icon (14px, fixed width) + stage name (12sp mono, letterSpacing 1.8sp) + status (10sp, right-aligned)
Row(verticalAlignment = CenterVertically) {
    Text(icon, modifier = Modifier.width(24.dp), ...)  // ✓ | ■ | —
    Text(stage.key, modifier = Modifier.weight(1f), ...)
    Text(statusLabel, ...)
}
```

Active stage icon (`■`) uses a `blink` animation (alpha 1.0 → 0.3 → 1.0, 1s loop).
`PROCESSING_XX%` increments numerically as the stage progresses.

**REAL-TIME_EXTRACTION panel** — each `REF_00X` card fades in with a `fadeInUp` animation
(translateY 6dp → 0, opacity 0 → 1, 400ms ease) as papers are retrieved. Ref ID is muted
grey, title is brighter grey, excerpt is ghost-colored quoted text.

**LIVE MONITOR bar chart** — 12 bars updated every 280ms with ±6% random walk. Bar colors:
`#FFFFFF` for every 4th bar, `#555555` for alternating, `#2A2A2A` for rest. Height transition
is `animateFloatAsState` with 280ms tween.

**HALT_PROCESS** is a `FalcoGhostButton` that cancels the `VerificationWorker` via
`WorkManager.getInstance(context).cancelUniqueWork(claimId)`.

### 16.3 DossierScreen

The final verdict output. This is the "Dossier" tab — previously called VerdictScreen.
See Section 17 for the full component breakdown.

---

## 17. Dossier Screen (Verdict Output) Specification

The Dossier is the full verdict output. It replaces the previous "VerdictCard" concept —
the entire screen IS the dossier. No floating card; the verdict fills the scroll canvas.

### 17.1 Full Screen Layout

```
┌────────────────────────────────────────────────────┐
│  [□] FALCO                                         │
│  ─────────────────────────────────────────────────│
│                                                    │
│  VERDICT STATUS: FINAL                            │  ← 10sp ghost label
│                                                    │
│  SUPPORTED                                        │  ← 82sp DM Serif, clips at edge
│                                                    │
│  72%  CONFIDENCE                                  │  ← 36sp serif + 11sp ghost mono
│  █ █ █ █ █ █ █ █ █ ░ ░ ░ ░                       │  ← 13-segment bar
│                                                    │
│  ─────────────────────────────────────────────── │  ← #141414 hairline
│                                                    │
│  METADATA                                         │  ← FalcoLabel
│  7 PAPERS ANALYSED · 5 PASSED QUALITY GATE       │  ← 11sp mono #ccc
│                                                    │
│  LATENCY                                          │
│  842MS PROCESSING TIME                            │
│                                                    │
│  AUTH                                             │
│  SHA-256: 8F2A...C91E                             │  ← Truncated hash
│                                                    │
│  ─────────────────────────────────────────────── │
│                                                    │
│                                                    │
│  Synthesis of grounding                           │  ← 28sp DM Serif
│  data and cross-                                   │
│  referenced claims.                               │
│                                                    │
│  ┌──────────────────────────────────────────────┐ │
│  │ The central hypothesis regarding neural      │ │  ← 13sp mono #bbb, 1dp #141414 border
│  │ plasticity in high-stress environments is   │ │    Two paragraphs, gap between
│  │ validated by a majority of the analyzed...  │ │
│  │                                              │ │
│  │ Operational constraints were observed in    │ │
│  │ small-sample clinical trials, which were... │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  EVIDENCE LIST [N=7]                              │  ← Ghost label with count
│                                                    │
│  [SUPPORTS] NEUROPLASTICITY AND CORTISOL...      │  ← Evidence row (see 17.2)
│  GROUNDING                                        │
│  0.94  →                                          │
│                                                    │
│  [SUPPORTS] SYNAPTIC DENSITY IN CHRONICALLY...   │
│  GROUNDING                                        │
│  0.82  →                                          │
│                                                    │
│  [NEUTRAL]  METHODOLOGICAL VARIATIONS IN...      │
│  GROUNDING                                        │
│  0.51  →                                          │
│                                                    │
│  [SUPPOSES] THE MYTH OF PERMANENT SYNAPTIC...    │
│  GROUNDING                                        │
│  0.14  →                                          │
│                                                    │
│  ─────────────────────────────────────────────── │
│                                                    │
│  [           SHARE VERDICT              ]          │  ← Ghost buttons, stacked
│  [           SAVE TO HISTORY            ]
│  [           NEW CLAIM                  ]
│                                                    │
└────────────────────────────────────────────────────┘
```

### 17.2 Evidence Row Anatomy

Each evidence row has no card surface — it's a flat section separated only by `#0F0F0F`
top-border hairlines. Tap anywhere on the row to expand/collapse.

```
[SUPPORTS]                         ← 10sp uppercase, stance brightness (white/mid-grey/dark-grey)
PAPER TITLE IN ALL CAPS            ← 12sp mono, 500 weight, #fff
DR. ELENA VOLKOV, ET AL. (2022)   ← 10sp mono, #555

GROUNDING                          ← 9sp ghost label
0.94                               ← 16sp light weight number
                                →  ← Arrow rotates 45° when expanded

── (tap to expand) ────────────────────────────────

[expanded state]
─────────────────────────────────────────────────
Grounding score 0.94 — actor reasoning verified    ← 11sp mono, #555
against abstract content. Critic confirmed         ← Fade in on expand
classification.
```

**Stance text brightness mapping:**

| Stance | Text color | Meaning |
|---|---|---|
| `[SUPPORTS]` | `#FFFFFF` | Full brightness — strong signal |
| `[NEUTRAL]` | `#555555` | Mid-grey — weak/equivocal signal |
| `[OPPOSES]` | `#3A3A3A` | Near-invisible — counterevidence, low visual weight |

This deliberate de-emphasis of opposing evidence reflects the Algorithmic Grounding score:
a heavily penalised opposing paper (grounding 0.14) should carry little visual weight.

**Evidence row sort order:** citation tier descending, then year descending (same as before),
but SUPPORTS rows always rendered before NEUTRAL before OPPOSES within the same tier.

### 17.3 Metadata Block

Three `FalcoMetaRow` components stacked between hairlines:

```kotlin
@Composable
fun FalcoMetaRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, style = FalcoTypography.labelSmall, color = FalcoTextGhost)
        Spacer(Modifier.height(5.dp))
        Text(value, style = FalcoTypography.bodySmall.copy(fontSize = 11.sp,
             letterSpacing = 1.2.sp), color = Color(0xFFCCCCCC))
    }
}
```

**AUTH hash** is the SHA-256 of the concatenated paper DOIs + verdict lean + timestamp,
truncated to first 4 and last 4 hex chars: `8F2A...C91E`. Gives the verdict a cryptographic
"ledger entry" feel consistent with FALCO's authority identity.

### 17.4 Confidence Segment Bar

```kotlin
@Composable
fun ConfidenceSegmentBar(confidence: Float, segments: Int = 13) {
    val filled = (confidence * segments).roundToInt()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            Box(modifier = Modifier
                .weight(1f).height(3.dp)
                .background(if (i < filled) FalcoBarFilled else FalcoBarEmpty)
            )
        }
    }
}
```

### 17.5 Action Buttons

Three `FalcoGhostButton` stacked with 8dp gap:

```kotlin
FalcoGhostButton("SHARE VERDICT",  onClick = onShare)
FalcoGhostButton("SAVE TO HISTORY", onClick = onSave)
FalcoGhostButton("NEW CLAIM",      onClick = onNewClaim)  // Navigates back to Hypothesis tab
```

`NEW CLAIM` sets `selectedTab = FalcoTab.Hypothesis` and clears the current claim state.

---

## 18. Prompt Templates (Full)

Stored in `md/falco-prompt-library.md` for use with AI coding assistants.

### Template: New Agent

```markdown
## Task: Implement FALCO Agent — {AgentName}

### Context
- Project: FALCO (academic claim verification Android app)
- Architecture: 8-stage pipeline. Agents live in `agent/` package.
- Agent base: `IFalcoAgent<I, O>` interface
- Provider routing: via `ProviderRouter.routeFor(agentName)`
- Response format: JSON parsed via `kotlinx.serialization`

### Agent Spec
- Name: {AgentName}
- Stage: Stage {N} in the pipeline
- Input type: {InputType}
- Output type: {OutputType}
- Primary model: {model} via {provider}
- System prompt: {systemPrompt}
- User prompt template: {userPromptTemplate}

### Requirements
1. Implement `IFalcoAgent<{InputType}, {OutputType}>`
2. Use `ProviderRouter` to get provider + model
3. Construct the LLM request via the appropriate client
4. Parse JSON response with `Json.decodeFromString`
5. Handle parsing errors with a sensible fallback
6. Record token usage via `TokenSteward`
7. No business logic — agents are pure I/O transformers

### Output
Implementation plan only (no code), with:
- Class signature
- Constructor dependencies
- execute() method steps
- Error handling strategy
- Fallback behavior if LLM returns malformed JSON
```

### Template: New Paper Repository

```markdown
## Task: Implement IPaperRepository for {SourceName}

### Context
- Project: FALCO
- Source: {SourceName} (e.g. Semantic Scholar, OpenAlex)
- Interface: `IPaperRepository`
- HTTP client: Ktor `HttpClient` injected via Hilt

### API Spec
- Base URL: {baseUrl}
- Auth: {authMethod}
- Search endpoint: {endpoint}
- Response DTO: {dtoClass}
- Rate limit: {rateLimit}

### Requirements
1. Implement `search(query: String, limit: Int): List<Paper>`
2. Map DTO to domain `Paper` model (mapper function in companion object)
3. Handle 429 rate limit: exponential back-off, max 3 retries
4. Handle empty abstract: return empty list (gate will filter anyway)
5. Log each request via `TokenSteward` (count as request, not tokens)

### Output
Implementation plan only.
```

---

## 19. API Reference: Semantic Scholar & OpenAlex

### Semantic Scholar

| Detail | Value |
|---|---|
| Base URL | `https://api.semanticscholar.org/graph/v1` |
| Auth | None (for basic use); API key header for higher limits |
| Rate limit | 100 req / 5 min unauthenticated |
| Search endpoint | `GET /paper/search` |
| Params | `query`, `limit`, `offset`, `fields` |
| Paper fields | `paperId,title,abstract,authors,year,citationCount,isOpenAccess,externalIds,fieldsOfStudy` |
| Docs | `https://api.semanticscholar.org/api-docs/graph` |

**Example request:**
```
GET https://api.semanticscholar.org/graph/v1/paper/search
    ?query=multi-agent+architecture+mobile+battery
    &limit=5
    &fields=paperId,title,abstract,authors,year,citationCount,isOpenAccess
```

### OpenAlex

| Detail | Value |
|---|---|
| Base URL | `https://api.openalex.org` |
| Auth | None; `mailto=email` param for polite pool |
| Rate limit | 10 req/sec (polite pool); 100k req/day |
| Search endpoint | `GET /works` |
| Params | `search`, `filter`, `per_page`, `select`, `sort` |
| Key filter | `has_abstract:true` |
| Sort | `cited_by_count:desc` |
| Docs | `https://docs.openalex.org` |

**Example request:**
```
GET https://api.openalex.org/works
    ?search=multi-agent+architecture+battery+consumption
    &filter=has_abstract:true
    &per_page=5
    &select=id,doi,title,abstract_inverted_index,authorships,publication_year,cited_by_count,open_access
    &sort=cited_by_count:desc
    &mailto=your@email.com
```

---

## 20. Free Tier Quota Summary

| Service | Free Tier | Daily Limit | Cost at Limit |
|---|---|---|---|
| Semantic Scholar | Unlimited (unauth) | ~100 req/5min | Free |
| OpenAlex | Unlimited | 100k req/day | Free |
| Groq | ~500k tokens/day | ~500k tokens | Free |
| Gemini (AI Studio) | 1,500 req/day | 1,500 req | Free |
| Cerebras | ~1M tokens/day | ~1M tokens | Free |
| OpenRouter (free models) | Varies | Varies | Free |

**Per-verification estimate:**
- QueryExpansion: ~300 tokens (Groq)
- StanceActor × 7 papers: ~350 tokens each → ~2,450 tokens (Groq)
- StanceCritic × 7 papers: ~400 tokens each → ~2,800 tokens (Gemini, counts as requests)
- Aggregator: ~600 tokens (Gemini)
- **Total per verification: ~6,150 tokens + ~8 Gemini requests**

At Groq's 500k/day limit: **~80 verifications/day free**
At Gemini's 1,500 req/day limit: **~187 verifications/day free**
**Effective ceiling: ~80 verifications/day with default config**

---

## 21. Phased Delivery Roadmap

### Phase 1 — Core Pipeline (2 weeks)
- [ ] Project setup: Hilt, Ktor, Room, WorkManager
- [ ] Data models: Claim, Paper, PaperStance, Verdict
- [ ] SemanticScholarClient + search integration
- [ ] QueryExpansionAgent (Groq)
- [ ] StanceActorAgent (Groq) — single-pass, no Critic yet
- [ ] AggregatorAgent (Gemini)
- [ ] FalcoOrchestrator: 5-stage pipeline (no gating, no temporal)
- [ ] Basic VerdictCard UI (zones 1 + 2)
- [ ] InputScreen + VerdictScreen

### Phase 2 — Quality & Accuracy (1 week)
- [ ] OpenAlexClient + deduplication
- [ ] PaperQualityGate
- [ ] TemporalFreshnessAnalyzer
- [ ] StanceCriticAgent — full Actor-Critic loop
- [ ] AlgorithmicGrounding
- [ ] Full 8-stage pipeline

### Phase 3 — Polish & Production (1 week)
- [ ] TokenSteward + ProviderRouter with fallbacks
- [ ] Room persistence — history + quota tracking
- [ ] WorkManager background verification + HALT_PROCESS cancellation
- [ ] `NeuralNetworkBackground` SVG composable on HypothesisScreen
- [ ] `LiveBarChart` animated monitor on PipelineScreen
- [ ] `RealTimeExtractionPanel` with fade-in ref cards
- [ ] `EvidenceRow` expandable rows with grounding score + critic text
- [ ] `ConfidenceSegmentBar` (13-segment)
- [ ] `FalcoMetaRow` metadata blocks (LATENCY, AUTH hash)
- [ ] AUTH SHA-256 hash generation from verdict data
- [ ] ShareBottomSheet (borrowed from Corvus)
- [ ] Phase 3 roadmap items previously under HistoryScreen → merged into Dossier save flow

### Phase 4 — Extensions (future)
- [ ] Semantic Scholar API key for higher rate limits
- [ ] Export verdict as PDF
- [ ] Batch claim verification
- [ ] Vision input: photograph a hypothesis from a paper
- [ ] BM (Bahasa Malaysia) hypothesis support via Mistral-Saba routing

---

## 22. Open Questions & Future Extensions

### Open Questions

1. **Abstract length floor** — 80 chars is conservative. Should it be higher (150+)?
   Short abstracts may be conference papers with limited evidence.

2. **Actor-Critic cost** — Running Critic on all papers doubles LLM calls.
   Consider running Critic only on papers where Actor confidence < 0.7.

3. **NEUTRAL handling** — Should NEUTRAL papers be shown in the verdict card?
   They add noise but provide context. Make it a user toggle.

4. **Claim decomposition** — Complex claims ("X reduces Y by Z% AND improves W")
   should be decomposed into atomic claims. Phase 4 extension.

5. **OpenAlex abstract reconstruction** — Inverted index reconstruction is lossy for
   some edge cases. Fall back to Semantic Scholar for the same paper if abstract
   quality is poor.

### Future Extensions

- **Citation graph traversal** — Use Semantic Scholar's citation graph to find
  papers that cite the found papers, expanding the evidence network.

- **Research gap detection** — Instead of just verifying claims, detect when a claim
  represents a genuine gap (few or no papers exist on the topic).

- **Collaborative verification** — Allow users to share and compare verdicts on the
  same claim. Shared Room sync via Firebase.

- **Custom knowledge base** — Allow users to upload their own PDFs (papers, theses)
  as an additional retrieval source, using on-device embedding search.

- **Hypothesis generator** — Reverse mode: give FALCO a topic, it suggests verifiable
  hypotheses based on what the literature currently debates.

---

*FALCO implementation plan — v1.0*
*Based on Corvus (NaimNajmios/Corvus) architecture analysis*
*Last updated: March 2026*
