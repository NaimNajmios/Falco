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

## ✨ Features

-   **Intelligent Hypothesis Analysis**: Depth-first classification of claim types.
-   **Academic Rigor**: Direct integration with **OpenAlex** and **Semantic Scholar**.
-   **Dynamic Quota Management**: Real-time token and request tracking via `TokenSteward` across multiple providers (**Gemini**, **Groq**, **Cerebras**, and **OpenRouter**).
-   **Background Verification**: Support for high-latency tasks using **WorkManager**, ensuring verifications continue even if the app is closed.
-   **Evidence Dossier**: Comprehensive reports containing stances, confidence scores, and source citations.
-   **Premium UI/UX**: A sleek, dark-themed interface built with Jetpack Compose, featuring smooth transitions and edge-to-edge design.

## 🛠 Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Architecture**: Clean Architecture + MVVM + Agent-Oriented Design
-   **Local Storage**: Room (Persistent storage for Dossiers and Quotas)
-   **Networking**: Ktor (Multiplatform-ready HTTP client)
-   **Background Tasks**: Android WorkManager
-   **Dependency Injection**: Hilt
-   **Asynchrony**: Kotlin Coroutines & Flow

## 📦 Getting Started

### Prerequisites

-   Android Studio Jellyfish or later
-   JDK 17
-   An API Key for the underlying LLM provider (configured in `local.properties`)

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/najminajmi/falco.git
    ```
2.  Open the project in Android Studio.
3.  Add your credentials to `local.properties`:
    ```properties
    GEMINI_API_KEY=your_api_key_here
    # Optional: ADD GROQ_API_KEY, CEREBRAS_API_KEY, etc.
    ```
4.  Build and run on your device or emulator.
