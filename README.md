![FALCO Banner](file:///C:/Users/NAIM/.gemini/antigravity/brain/cf64be94-ccc8-476f-b896-94f474b29c2a/falco_banner_1774661601895.png)

# FALCO

> **Production-Grade Self-Correcting Multi-Agent Verification System**

FALCO is a high-performance Android application designed to verify complex claims using a sophisticated, self-correcting multi-agent orchestration pipeline. By leveraging diverse academic databases and a fleet of advanced LLMs, FALCO bridges the gap between casual claims and peer-reviewed empirical evidence.

## 🚀 Vision

In an era of information overload, FALCO provides a rigorous, automated framework for academic fact-checking. It doesn't just search; it **reasons**, **filters**, **challenges**, and **synthesizes** information from scientific literature to provide a definitive, evidence-backed verdict on any hypothesis.

## 🧠 Self-Correcting Orchestration Pipeline

FALCO's core is a dynamic, multi-stage pipeline managed by the `FalcoOrchestrator`. It employs a series of specialized agents and diagnostic gates to ensure maximum reliability:

1.  **Claim Classifier**: Analyzes the input to determine claim type and underlying technical nuances.
2.  **Query Expander**: Transforms claims into optimized academic search queries.
3.  **Multi-Source Retrieval**: Interfaces with **OpenAlex** and **Semantic Scholar** for comprehensive evidence gathering.
4.  **Paper Quality Gate**: Field-aware scoring (citations, open-access, abstract depth) to filter for high-impact evidence.
5.  **Temporal Freshness Analysis**: Time-aware alerts for rapidly evolving fields (e.g., AI/ML, Medicine).
6.  **Smart Stance Actor**: Incremental chunk analysis with **Early Stopping** to minimize token usage and latency.
7.  **Cross-Reference Engine**: Identifies consensus and flags outliers across all gathered stances to ensure balanced reporting.
8.  **Stance Critic (Devil's Advocate)**: A dedicated agent that challenges initial interpretations to prevent over-inference or model bias.
9.  **Algorithmic Grounding**: Automated verification of agent reasoning against original metadata.
10. **Adaptive Retrieval Loop**: If the aggregator detects insufficient evidence, the system autonomously triggers a new search-verify cycle with refined queries.

## 🛡 Security & Resilience

FALCO is built for production-grade reliability and data privacy:

-   **Industry-Standard Encryption**: All user API keys are secured using **AES256-GCM/SIV** via `EncryptedSharedPreferences` and Android's `MasterKey` system.
-   **Resilient Tiered Routing**: The `ProviderRouter` implements intelligent fallback logic (**Groq** → **Gemini** → **Cerebras/Mistral/Cohere**), ensuring high availability even during provider outages.
-   **Live Health Monitoring**: Continuous status tracking to optimize for the fastest and most reliable model at any given moment.

## ✨ Advanced Features

-   **Deep Evidence Analytics**: Transparency on tokens analyzed, analysis duration, and efficiency gains vs. traditional full-text processing.
-   **Field-Aware Verification**: Specialized thresholds for different academic domains (AI, Medicine, Social Sciences, etc.).
-   **Persistent Dossier History**: Full CRUD support for past claims, allowing users to browse, re-examine, or delete detailed verification records.
-   **Real-time Diagnostic Logging**: Integrated `DebugLogger` for full transparency into the orchestration process.
-   **Premium UI/UX**: Sleek, dark-themed interface built with Jetpack Compose, featuring real-time stage tracking in the `PipelineScreen`.

## 🛠 Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Architecture**: Clean Architecture + MVVM + Agent-Oriented Design
-   **Security**: AndroidX Crypto (MasterKey & EncryptedSharedPreferences)
-   **Persistence**: Room (Dossier history, Quota tracking, and Claims)
-   **Remote**: Ktor (Multiplatform-ready HTTP client)
-   **Background Tasks**: Android WorkManager
-   **Dependency Injection**: Hilt
-   **Asynchrony**: Kotlin Coroutines & Flow

## 📦 Getting Started

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
