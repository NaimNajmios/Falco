![FALCO Banner](file:///C:/Users/NAIM/.gemini/antigravity/brain/cf64be94-ccc8-476f-b896-94f474b29c2a/falco_banner_1774661601895.png)

# FALCO

> **Production-Grade Self-Correcting Multi-Agent Verification System**

FALCO is a high-performance Android application designed to verify complex claims using a sophisticated, self-correcting multi-agent orchestration pipeline. By leveraging diverse academic databases and a fleet of advanced LLMs, FALCO bridges the gap between casual claims and peer-reviewed empirical evidence.

## Vision

In an era of information overload, FALCO provides a rigorous, automated framework for academic fact-checking. It doesn't just search; it **reasons**, **filters**, **challenges**, and **synthesizes** information from scientific literature to provide a definitive, evidence-backed verdict on any hypothesis.

## End-to-End Pipeline Signal Propagation

Unlike traditional "black box" AI systems, FALCO aggressively surfaces its intermediate reasoning to the user. The pipeline's decision-making process is fully propagated to the UI, providing unprecedented transparency:

-   **Claim Anatomy Card**: Visualizes how the pipeline classified the claim—showing the claim type, confidence rings, and ambiguity warnings.
-   **Search Strategy Expansion**: Detailed overview of the expanded queries (Broad, Narrow, Contrastive) used during retrieval and their respective result coverage.
-   **Evidence Base Summary**: A transparent breakdown of the source split, source health badges, and calculated evidence quality metrics.
-   **Stance Distribution**: Visual stacked bars and confidence histograms that clearly illustrate where the gathered evidence aligns (Supports/Neutral/Opposes) and flag scientific conflicts.
-   **Verdict Factors Breakdown**: A detailed analysis of the aggregator's weighting logic (Evidence Volume, Consensus Strength, etc.) combined with a plain-English verdict narrative.

## Consumer-Grade Configuration

-   **Dynamic Provider Ecosystem**: Integrated support for a diverse fleet of LLM providers including **Gemini**, **Groq**, **Cerebras**, **OpenRouter**, **Mistral**, **Cohere**, and **Routeway**.
-   **Real-Time Key Validation**: A sophisticated validation system that ensures API keys are functional with a live connectivity test before saving.
-   **User-Centric Preferences**:
    -   **Dark Mode**: A premium, eye-friendly interface for low-light research.
    -   **Debug Mode**: Toggleable diagnostic logging for full operational transparency.
    -   **Preferred Provider Selection**: High-level control over the system's primary agentic backbone.

## Production-Grade Infrastructure

-   **Background Orchestration**: The `VerificationForegroundService` ensures verifications are resilient to OS process management, continuing even when the app is minimized.
-   **Real-Time Progress Tracking**: Live updates on pipeline stages and processing counts via Android notifications.
-   **Operational Transparency**: A granular `DebugLogger` system tracking network latency, LLM token metrics, and pipeline stage benchmarks.

## Self-Correcting Orchestration Pipeline

FALCO's core is a dynamic, multi-stage pipeline managed by the `FalcoOrchestrator`:

1.  **Claim Classifier**: Analyzes technical nuances and claim types.
2.  **Query Expander**: Generates optimized academic search queries.
3.  **Multi-Source Retrieval**: Interfaces with **OpenAlex** and **Semantic Scholar**.
4.  **Paper Quality Gate**: Field-aware scoring (citations, open-access, depth).
5.  **Temporal Freshness Analysis**: Time-aware alerts for rapidly evolving fields.
6.  **Smart Stance Actor**: Incremental chunk analysis with **Early Stopping** for cost efficiency.
7.  **Cross-Reference Engine**: Identifies consensus and flags outlier evidence.
8.  **Stance Critic (Devil's Advocate)**: Challenges initial classifications to prevent bias.
9.  **Algorithmic Grounding**: Automated verification of reasoning against metadata.
10. **Adaptive Retrieval Loop**: Autonomous search-verify cycles if initial data is insufficient.

## Advanced Features

-   **Verdict Hero Section**: An animated and rich visual presentation of the final verdict, providing immediate and authoritative clarity.
-   **Advanced Dossier Management**: The **Dossier** screen provides a comprehensive, highly-organized view of past verifications:
    -   **Full-Text Search**: Instantly find past claims using keyword search.
    -   **Favorites & Filtering**: "Star" critical verifications and filter the history list to show only favorites.
    -   **Intelligent Sorting**: Organize history by Newest, Oldest, or Highest Confidence.
    -   **Stance-Lean Visualization**: Quick-glance indicators for SUPPORTS, OPPOSES, and NEUTRAL stances.
-   **Data Portability**: Users can export their entire verification history (claims, verdicts, S/O/N counts) as a structured **JSON file** via the Android Sharesheet.
-   **Intelligent Optimization**: Early-stopping stance analysis and incremental chunking for cost-effective verification.
-   **Security**: **AES256-GCM/SIV** encrypted storage for all sensitive API keys.
-   **Premium UI/UX**: Sleek, dark-themed interface built with Jetpack Compose.

## Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Architecture**: Clean Architecture + MVVM + Domain Model Propagation
-   **Security**: AndroidX Crypto (MasterKey & EncryptedSharedPreferences)
-   **Persistence**: Room (Dossier history, Quota tracking, and Claims)
-   **Remote**: Ktor (Multiplatform-ready HTTP client)
-   **Background Tasks**: Android WorkManager & Foreground Services
-   **Dependency Injection**: Hilt
-   **Asynchrony**: Kotlin Coroutines & Flow

## Getting Started

### Prerequisites

-   Android Studio Jellyfish or later
-   JDK 17

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/najminajmi/falco.git
    ```
2.  Open the project in Android Studio.
3.  **Configure API Keys**: In the app's **Settings**, provide your own keys for the providers you wish to use.
4.  Build and run on your device or emulator.
