package com.najmi.falco.di

import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.data.remote.llm.CerebrasClient
import com.najmi.falco.data.remote.llm.GeminiClient
import com.najmi.falco.data.remote.llm.GroqClient
import com.najmi.falco.data.remote.llm.OpenRouterClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @IntoMap
    @LlmProviderKey(LlmProvider.GEMINI)
    abstract fun bindGeminiClient(client: GeminiClient): LlmClient

    @Binds
    @IntoMap
    @LlmProviderKey(LlmProvider.GROQ)
    abstract fun bindGroqClient(client: GroqClient): LlmClient

    @Binds
    @IntoMap
    @LlmProviderKey(LlmProvider.CEREBRAS)
    abstract fun bindCerebrasClient(client: CerebrasClient): LlmClient

    @Binds
    @IntoMap
    @LlmProviderKey(LlmProvider.OPENROUTER)
    abstract fun bindOpenRouterClient(client: OpenRouterClient): LlmClient
}
