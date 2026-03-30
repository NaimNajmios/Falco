package com.najmi.falco.di

import android.content.Context
import com.najmi.falco.chunking.BatchAssembler
import com.najmi.falco.chunking.ContentChunker
import com.najmi.falco.chunking.EarlyStopEvaluator
import com.najmi.falco.chunking.FreeTierQuotaManager
import com.najmi.falco.chunking.TieredProviderRouter
import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChunkingModule {

    @Provides
    @Singleton
    fun provideContentChunker(): ContentChunker {
        return ContentChunker()
    }

    @Provides
    @Singleton
    fun provideBatchAssembler(): BatchAssembler {
        return BatchAssembler()
    }

    @Provides
    @Singleton
    fun provideEarlyStopEvaluator(): EarlyStopEvaluator {
        return EarlyStopEvaluator()
    }

    @Provides
    @Singleton
    fun provideFreeTierQuotaManager(
        @ApplicationContext context: Context
    ): FreeTierQuotaManager {
        return FreeTierQuotaManager(context)
    }

    @Provides
    @Singleton
    fun provideTieredProviderRouter(
        clients: Map<LlmProvider, @JvmSuppressWildcards LlmClient>,
        quotaManager: FreeTierQuotaManager,
        apiKeyProvider: ApiKeyProvider
    ): TieredProviderRouter {
        return TieredProviderRouter(
            clients = clients,
            quotaManager = quotaManager,
            apiKeyProvider = apiKeyProvider
        )
    }
}
