package com.najmi.falco.provider

import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.ClaimType
import javax.inject.Inject
import javax.inject.Singleton

data class ProviderAssignment(
    val actor: LlmProvider,
    val critic: LlmProvider,
    val rationale: String
)

@Singleton
class ActorCriticProviderSelector @Inject constructor(
    private val healthTracker: LlmProviderHealthTracker
) {

    companion object {
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
        
        val rationale = buildRationale(claimType, language, isHighHarm, actor, critic)
        
        return ProviderAssignment(actor, critic, rationale)
    }

    private fun selectActor(claimType: ClaimType, language: String, isHighHarm: Boolean): LlmProvider {
        val preferences = when {
            isHighHarm -> listOf(LlmProvider.GEMINI, LlmProvider.CEREBRAS, LlmProvider.GROQ)
            language == "BM" || language == "MIXED" -> listOf(LlmProvider.MISTRAL, LlmProvider.GEMINI, LlmProvider.GROQ)
            claimType == ClaimType.SCIENTIFIC || claimType == ClaimType.STATISTICAL -> 
                listOf(LlmProvider.GROQ, LlmProvider.CEREBRAS)
            else -> ACTOR_PREFERENCE
        }

        return preferences.firstOrNull { healthTracker.isAvailable(it) }
            ?: LlmProvider.GEMINI
    }

    private fun selectCritic(claimType: ClaimType, language: String, isHighHarm: Boolean, excludeActor: LlmProvider): LlmProvider {
        val preferences = when {
            isHighHarm -> listOf(LlmProvider.GEMINI, LlmProvider.CEREBRAS)
            language == "BM" || language == "MIXED" -> listOf(LlmProvider.GEMINI, LlmProvider.MISTRAL)
            claimType == ClaimType.SCIENTIFIC || claimType == ClaimType.STATISTICAL -> 
                listOf(LlmProvider.GEMINI, LlmProvider.GROQ, LlmProvider.COHERE)
            else -> CRITIC_PREFERENCE
        }

        return preferences
            .filter { it != excludeActor }
            .firstOrNull { healthTracker.isAvailable(it) }
            ?: LlmProvider.GEMINI
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
        val malayRatio = malayCount.toFloat() / wordList.size
        
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
            "climate change", "global warming", "environment"
        )
        
        val lowerText = text.lowercase()
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
        
        parts.add("Claim type: ${claimType.name}")
        parts.add("Language: $language")
        
        if (isHighHarm) {
            parts.add("High-harm claim - using rigorous providers")
        }
        
        parts.add("Actor: ${actor.name} (drafting)")
        parts.add("Critic: ${critic.name} (verification)")
        
        return parts.joinToString(" | ")
    }
}
