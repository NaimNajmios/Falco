![FALCO Banner](file:///C:/Users/NAIM/.gemini/antigravity/brain/cf64be94-ccc8-476f-b896-94f474b29c2a/falco_banner_1774661601895.png)

# FALCO

> **Production-Grade Self-Correcting Multi-Agent Verification System**

FALCO is a high-performance Android application designed to verify complex claims using a sophisticated, self-correcting multi-agent orchestration pipeline. By leveraging diverse academic databases and a fleet of advanced LLMs, FALCO bridges the gap between casual claims and peer-reviewed empirical evidence.

## 🚀 Vision

In an era of information overload, FALCO provides a rigorous, automated framework for academic fact-checking. It doesn't just search; it **reasons**, **filters**, **challenges**, and **synthesizes** information from scientific literature to provide a definitive, evidence-backed verdict on any hypothesis.

## ⚙️ Consumer-Grade Configuration

-   **Dynamic Provider Ecosystem**: Integrated support for a diverse fleet of LLM providers including **Gemini**, **Groq**, **Cerebras**, **OpenRouter**, **Mistral**, **Cohere**, and **Routeway**.
-   **Real-Time Key Validation**: A sophisticated validation system that ensures API keys are functional with a live connectivity test before saving.
-   **User-Centric Preferences**:
    -   **Dark Mode**: A premium, eye-friendly interface for low-light research.
    -   **Debug Mode**: Toggleable diagnostic logging for full operational transparency.
    -   **Preferred Provider Selection**: High-level control over the system's primary agentic backbone.

## 🛡 Production-Grade Infrastructure

-   **Background Orchestration**: The `VerificationForegroundService` ensures verifications are resilient to OS process management, continuing even when the app is minimized.
-   **Real-Time Progress Tracking**: Live updates on pipeline stages and processing counts via Android notifications.
-   **Operational Transparency**: A granular `DebugLogger` system tracking network latency, LLM token metrics, and pipeline stage benchmarks.

## 🧠 Self-Correcting Orchestration Pipeline

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
10. **Adaptive Retrieval Loop**: Autonomous search-verify cycles if the initial data is insufficient.

## ✨ Advanced Features

-   **Deep Evidence Synthesis**: The **Verdict Detail** screen provides a deep-dive into synthesis quality, evidence tiers, and provider provenance.
-   **Evidence-Backed History**: Persistent **Dossier** history with stance-lean visualizations and confidence segment bars.
-   **Security**: **AES256-GCM/SIV** encrypted storage for all sensitive API keys.
-   **Premium UI/UX**: Sleek, dark-themed interface built with Jetpack Compose.

## 🛠 Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Architecture**: Clean Architecture + MVVM + Agent-Oriented Design
-   **Security**: AndroidX Crypto (MasterKey & EncryptedSharedPreferences)
-   **Persistence**: Room (Dossier history, Quota tracking, and Claims)
-   **Remote**: Ktor (Multiplatform-ready HTTP client)
-   **Background Tasks**: Android WorkManager & Foreground Services
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
