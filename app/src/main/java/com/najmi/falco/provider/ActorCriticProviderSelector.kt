package com.najmi.falco.provider

import android.util.Log
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.ClaimType
import javax.inject.Inject
import javax.inject.Singleton

data class ProviderAssignment(
    val actor: LlmProvider,
    val critic: LlmProvider,
    val rationale: String,
    val actorFallbacks: List<LlmProvider> = emptyList(),
    val criticFallbacks: List<LlmProvider> = emptyList()
)

@Singleton
class ActorCriticProviderSelector @Inject constructor(
    private val healthTracker: LlmProviderHealthTracker
) {
    companion object {
        private const val TAG = "ActorCriticSelector"
        
        private val ACTOR_PREFERENCE = listOf(
            LlmProvider.GROQ,
            LlmProvider.CEREBRAS,
            LlmProvider.MISTRAL,
            LlmProvider.GEMINI,
            LlmProvider.COHERE,
            LlmProvider.OPENROUTER
        )

        private val CRITIC_PREFERENCE = listOf(
            LlmProvider.GEMINI,
            LlmProvider.GROQ,
            LlmProvider.CEREBRAS,
            LlmProvider.MISTRAL,
            LlmProvider.COHERE,
            LlmProvider.OPENROUTER
        )
    }

    fun selectProviders(claimType: ClaimType, claimText: String): ProviderAssignment {
        val language = detectLanguage(claimText)
        val isHighHarm = isHighHarmClaim(claimText)
        
        val actor = selectActor(claimType, language, isHighHarm)
        val critic = selectCritic(claimType, language, isHighHarm, actor)
        
        val actorFallbacks = getFallbacks(actor, CRITIC_PREFERENCE)
        val criticFallbacks = getFallbacks(critic, ACTOR_PREFERENCE)
        
        Log.d(TAG, "Provider selection - Claim: ${claimText.take(50)}...")
        Log.d(TAG, "  Claim type: ${claimType.name}, Language: $language, HighHarm: $isHighHarm")
        Log.d(TAG, "  Selected Actor: ${actor.name} with fallbacks: ${actorFallbacks.map { it.name }}")
        Log.d(TAG, "  Selected Critic: ${critic.name} with fallbacks: ${criticFallbacks.map { it.name }}")
        
        val rationale = buildRationale(claimType, language, isHighHarm, actor, critic)
        
        return ProviderAssignment(actor, critic, rationale, actorFallbacks, criticFallbacks)
    }

    private fun selectActor(claimType: ClaimType, language: String, isHighHarm: Boolean): LlmProvider {
        val preferences = when {
            isHighHarm -> listOf(LlmProvider.GEMINI, LlmProvider.CEREBRAS, LlmProvider.GROQ)
            language == "BM" || language == "MIXED" -> listOf(LlmProvider.MISTRAL, LlmProvider.GEMINI, LlmProvider.GROQ)
            claimType == ClaimType.SCIENTIFIC || claimType == ClaimType.STATISTICAL -> 
                listOf(LlmProvider.GROQ, LlmProvider.CEREBRAS)
            else -> ACTOR_PREFERENCE
        }

        Log.d(TAG, "Actor preferences order: ${preferences.map { "${it}(${if (healthTracker.isAvailable(it)) "✓" else "✗"})" }}")
        
        return preferences.firstOrNull { healthTracker.isAvailable(it) }
            ?: run {
                Log.w(TAG, "No healthy actor provider found, trying all providers")
                LlmProvider.entries.firstOrNull { healthTracker.isAvailable(it) }
                    ?: LlmProvider.GEMINI
            }
    }

    private fun selectCritic(claimType: ClaimType, language: String, isHighHarm: Boolean, excludeActor: LlmProvider): LlmProvider {
        val preferences = when {
            isHighHarm -> listOf(LlmProvider.GEMINI, LlmProvider.CEREBRAS)
            language == "BM" || language == "MIXED" -> listOf(LlmProvider.GEMINI, LlmProvider.MISTRAL)
            claimType == ClaimType.SCIENTIFIC || claimType == ClaimType.STATISTICAL -> 
                listOf(LlmProvider.GEMINI, LlmProvider.GROQ, LlmProvider.COHERE)
            else -> CRITIC_PREFERENCE
        }

        Log.d(TAG, "Critic preferences order: ${preferences.map { "${it}(${if (healthTracker.isAvailable(it)) "✓" else "✗"})" }}")
        
        val selectedCritic = preferences
            .filter { it != excludeActor }
            .firstOrNull { healthTracker.isAvailable(it) }
            ?: run {
                Log.w(TAG, "No healthy critic provider found (excluding $excludeActor), trying all providers")
                LlmProvider.entries.firstOrNull { healthTracker.isAvailable(it) && it != excludeActor }
                    ?: LlmProvider.GEMINI
            }
        
        Log.d(TAG, "Selected critic: ${selectedCritic.name} (excluded actor: ${excludeActor.name})")
        return selectedCritic
    }

    private fun getFallbacks(primary: LlmProvider, preferenceList: List<LlmProvider>): List<LlmProvider> {
        val allProviders = preferenceList + LlmProvider.entries.filter { it !in preferenceList }
        return allProviders
            .filter { it != primary && healthTracker.isAvailable(it) }
            .take(3)
    }

    private fun detectLanguage(text: String): String {
        val malayIndicators = listOf(
            "yang", "dan", "atau", "ini", "itu", "saya", "anda", "kami", "mereka",
            "adalah", "oleh", "dengan", "untuk", "dari", "pada", "dalam", "ke",
            "tidak", "bukan", "akan", "sudah", "ada", "tidak", "mana", "apa",
            "siapa", "bilang", "sini", "situ", "sana"
        )
        
        val wordList = text.lowercase().split(Regex("\\s+"))
        val malayCount = wordList.count { it in malayIndicators }
        val malayRatio = malayCount.toFloat() / wordList.size.coerceAtLeast(1)
        
        return when {
            malayRatio > 0.15 -> "BM"
            malayRatio > 0.05 -> "MIXED"
            else -> "EN"
        }
    }

    private fun isHighHarmClaim(text: String): Boolean {
        val highHarmKeywords = listOf(
            "vaccine", "vaccination", "pandemic", "epidemic", "outbreak",
            "death", "die", "kill", "harm", "dangerous", "unsafe",
            "terrorism", "terrorist", "attack", "weapon", "nuclear",
            "abuse", "assault", "violence", "murder", "suicide",
            "cancer", "cure", "treatment", "medical", "health risk",
            "climate change", "global warming", "environment",
            "fraud", "scandal", "hoax", "conspiracy"
        )
        
        val lowerText = text.lowercase()
        val matches = highHarmKeywords.filter { it in lowerText }
        if (matches.isNotEmpty()) {
            Log.d(TAG, "High harm keywords detected: $matches")
        }
        return highHarmKeywords.any { it in lowerText }
    }

    private fun buildRationale(
        claimType: ClaimType,
        language: String,
        isHighHarm: Boolean,
        actor: LlmProvider,
        critic: LlmProvider
    ): String {
        val parts = mutableListOf<String>()
        
        parts.add("type:${claimType.name}")
        parts.add("lang:$language")
        
        if (isHighHarm) {
            parts.add("HIGH_HARM")
        }
        
        parts.add("actor:${actor.name}")
        parts.add("critic:${critic.name}")
        
        return parts.joinToString("|")
    }

    fun getAvailableProviders(): Map<LlmProvider, Boolean> {
        return LlmProvider.entries.associateWith { healthTracker.isAvailable(it) }
    }

    fun logHealthStatus() {
        Log.d(TAG, "=== Provider Health Status ===")
        LlmProvider.entries.forEach { provider ->
            val status = if (healthTracker.isAvailable(provider)) "✓ HEALTHY" else "✗ UNHEALTHY"
            Log.d(TAG, "  ${provider.name}: $status")
        }
        Log.d(TAG, "============================")
    }
}
