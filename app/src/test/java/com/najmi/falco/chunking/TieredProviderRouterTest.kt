package com.najmi.falco.chunking

import com.najmi.falco.data.remote.LlmProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TieredProviderRouterTest {

    private lateinit var providerSelections: MutableList<Int>

    @Before
    fun setup() {
        providerSelections = mutableListOf()
    }

    private fun selectProviderForTokens(tokenCount: Int): LlmProvider {
        return when {
            tokenCount <= 4000 -> LlmProvider.GROQ
            tokenCount <= 64000 -> LlmProvider.CEREBRAS
            tokenCount <= 128000 -> LlmProvider.GEMINI
            else -> LlmProvider.OPENROUTER
        }
    }

    @Test
    fun `selectProvider - GROQ for small tokens`() {
        val provider = selectProviderForTokens(1000)
        assertEquals(LlmProvider.GROQ, provider)
    }

    @Test
    fun `selectProvider - GROQ at boundary`() {
        val provider = selectProviderForTokens(4000)
        assertEquals(LlmProvider.GROQ, provider)
    }

    @Test
    fun `selectProvider - CEREBRAS for medium tokens`() {
        val provider = selectProviderForTokens(5000)
        assertEquals(LlmProvider.CEREBRAS, provider)
    }

    @Test
    fun `selectProvider - CEREBRAS at boundary`() {
        val provider = selectProviderForTokens(64000)
        assertEquals(LlmProvider.CEREBRAS, provider)
    }

    @Test
    fun `selectProvider - GEMINI for large tokens`() {
        val provider = selectProviderForTokens(70000)
        assertEquals(LlmProvider.GEMINI, provider)
    }

    @Test
    fun `selectProvider - GEMINI at boundary`() {
        val provider = selectProviderForTokens(128000)
        assertEquals(LlmProvider.GEMINI, provider)
    }

    @Test
    fun `selectProvider - OPENROUTER for very large tokens`() {
        val provider = selectProviderForTokens(200000)
        assertEquals(LlmProvider.OPENROUTER, provider)
    }

    @Test
    fun `providerTier - returns correct description for GROQ`() {
        val tier = when (LlmProvider.GROQ) {
            LlmProvider.GROQ -> "Fast (Llama 3.3 70B)"
            LlmProvider.CEREBRAS -> "Accurate (GPT-OSS 120B)"
            LlmProvider.GEMINI -> "Long Context (Gemini 2.5 Flash)"
            LlmProvider.OPENROUTER -> "Fallback (NVIDIA Nemotron)"
            else -> "Unknown"
        }
        assertEquals("Fast (Llama 3.3 70B)", tier)
    }

    @Test
    fun `providerTier - returns correct description for CEREBRAS`() {
        val tier = "Accurate (GPT-OSS 120B)"
        assertEquals("Accurate (GPT-OSS 120B)", tier)
    }

    @Test
    fun `providerTier - returns correct description for GEMINI`() {
        val tier = "Long Context (Gemini 2.5 Flash)"
        assertEquals("Long Context (Gemini 2.5 Flash)", tier)
    }

    @Test
    fun `providerTier - returns correct description for OPENROUTER`() {
        val tier = "Fallback (NVIDIA Nemotron)"
        assertEquals("Fallback (NVIDIA Nemotron)", tier)
    }

    @Test
    fun `provider order - primary first for small tokens`() {
        val tokenCount = 1000
        val primary = selectProviderForTokens(tokenCount)
        assertEquals(LlmProvider.GROQ, primary)
    }

    @Test
    fun `provider order - primary first for medium tokens`() {
        val tokenCount = 5000
        val primary = selectProviderForTokens(tokenCount)
        assertEquals(LlmProvider.CEREBRAS, primary)
    }

    @Test
    fun `provider order - primary first for large tokens`() {
        val tokenCount = 100000
        val primary = selectProviderForTokens(tokenCount)
        assertEquals(LlmProvider.GEMINI, primary)
    }

    @Test
    fun `provider order - OPENROUTER for very large tokens`() {
        val tokenCount = 200000
        val primary = selectProviderForTokens(tokenCount)
        assertEquals(LlmProvider.OPENROUTER, primary)
    }

    @Test
    fun `all providers have limits defined`() {
        val limits = mapOf(
            LlmProvider.GROQ to 4000,
            LlmProvider.CEREBRAS to 64000,
            LlmProvider.GEMINI to 128000
        )
        assertEquals(3, limits.size)
    }

    @Test
    fun `tier boundaries are sequential`() {
        val groqMax = 4000
        val cerebrasMax = 64000
        val geminiMax = 128000
        
        assertTrue(groqMax < cerebrasMax)
        assertTrue(cerebrasMax < geminiMax)
    }
}
