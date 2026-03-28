![FALCO Banner](file:///C:/Users/NAIM/.gemini/antigravity/brain/cf64be94-ccc8-476f-b896-94f474b29c2a/falco_banner_1774661601895.png)

# FALCO

> **Multi-Agent Academic Verification System**

FALCO is a high-performance Android application designed to verify claims using a sophisticated multi-agent orchestration pipeline. By leveraging diverse academic databases and advanced LLM-driven agents, FALCO bridges the gap between casual claims and empirical evidence.

## 🚀 Vision

In an era of information overload, FALCO provides a rigorous, automated framework for academic fact-checking. It doesn't just search; it **reasons**, **extracts**, and **synthesizes** information from peer-reviewed literature to provide a definitive verdict on any given hypothesis.

## 🧠 The Orchestration Pipeline

FALCO's core is the `FalcoOrchestrator`, which manages a sequence of specialized AI agents:

1.  **Claim Classifier**: Analyzes the input text to determine the type of claim and its underlying nuances.
2.  **Query Expander**: Transforms the claim into complex academic search queries optimized for scientific databases.
3.  **Multi-Source Retrieval**: Interfaces with academic repositories like **OpenAlex** and **Semantic Scholar** to fetch relevant peer-reviewed papers.
4.  **Stance Actor**: Independently analyzes each paper to determine its stance (Support, Refute, or Neutral) relative to the claim.
5.  **Aggregator**: Synthesizes all gathered evidence, weighting confidence levels and source quality to produce a final Dossier.

## 🛡 Security & Resilience

FALCO is built for production-grade reliability and data privacy:

-   **Industry-Standard Encryption**: All user API keys are stored using **AES256-GCM/SIV** via `EncryptedSharedPreferences` and Android's `MasterKey` system.
-   **Resilient LLM Routing**: The `ProviderRouter` implements intelligent fallback logic. If a primary provider (e.g., **Groq**) is unavailable or rate-limited, FALCO automatically routes requests to **Gemini**, then to other configured providers.
-   **Health Tracking**: Continuous monitoring of provider availability ensures minimal latency and maximum uptime.

## ✨ Features

-   **Intelligent Hypothesis Analysis**: Depth-first classification of claim types.
-   **Academic Rigor**: Direct integration with **OpenAlex** and **Semantic Scholar**.
-   **Dynamic Quota Management**: Real-time token and request tracking via `TokenSteward` across multiple providers (**Gemini**, **Groq**, **Cerebras**, and **OpenRouter**).
-   **Background Verification**: Support for high-latency tasks using **WorkManager**, ensuring verifications continue even if the app is closed.
-   **Diagnostic Logging**: Integrated `DebugLogger` for real-time orchestration transparency and performance monitoring.
-   **Premium UI/UX**: A sleek, dark-themed interface built with Jetpack Compose, featuring smooth transitions and edge-to-edge design.

## 🛠 Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Architecture**: Clean Architecture + MVVM + Agent-Oriented Design
-   **Security**: AndroidX Crypto (EncryptedSharedPreferences)
-   **Local Storage**: Room (Persistent storage for Dossiers and Quotas)
-   **Networking**: Ktor (Multiplatform-ready HTTP client)
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
3.  **Configure API Keys**: In the app's **Settings**, provide your own keys for the providers you wish to use. These keys are stored securely on your device.
4.  Build and run on your device or emulator.
