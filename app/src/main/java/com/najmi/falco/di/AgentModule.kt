package com.najmi.falco.di

import com.najmi.falco.agent.AggregatorAgent
import com.najmi.falco.agent.ClaimClassifierAgent
import com.najmi.falco.agent.CrossReferenceAgent
import com.najmi.falco.agent.QueryExpansionAgent
import com.najmi.falco.agent.SmartStanceActor
import com.najmi.falco.agent.StanceActorAgent
import com.najmi.falco.agent.StanceCriticAgent
import com.najmi.falco.chunking.BatchAssembler
import com.najmi.falco.chunking.ContentChunker
import com.najmi.falco.chunking.EarlyStopEvaluator
import com.najmi.falco.chunking.FreeTierQuotaManager
import com.najmi.falco.chunking.IncrementalChunkAnalyzer
import com.najmi.falco.chunking.TieredProviderRouter
import com.najmi.falco.data.repository.OutletCredibilityRepository
import com.najmi.falco.pipeline.AlgorithmicGrounding
import com.najmi.falco.pipeline.FalcoOrchestrator
import com.najmi.falco.pipeline.PaperDeduplicator
import com.najmi.falco.pipeline.PaperQualityGate
import com.najmi.falco.pipeline.RagVerifier
import com.najmi.falco.pipeline.TemporalFreshnessAnalyzer
import com.najmi.falco.pipeline.TemporalOverrideVerifier
import com.najmi.falco.provider.ActorCriticProviderSelector
import com.najmi.falco.provider.LlmProviderHealthTracker
import com.najmi.falco.provider.ProviderRouter
import com.najmi.falco.provider.TokenSteward
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideStanceCriticAgent(
        router: ProviderRouter,
        json: kotlinx.serialization.json.Json
    ): StanceCriticAgent = StanceCriticAgent(router, json)

    @Provides
    @Singleton
    fun providePaperQualityGate(): PaperQualityGate = PaperQualityGate()

    @Provides
    @Singleton
    fun provideTemporalFreshnessAnalyzer(): TemporalFreshnessAnalyzer = TemporalFreshnessAnalyzer()

    @Provides
    @Singleton
    fun provideAlgorithmicGrounding(): AlgorithmicGrounding = AlgorithmicGrounding()

    @Provides
    @Singleton
    fun providePaperDeduplicator(): PaperDeduplicator = PaperDeduplicator()

    @Provides
    @Singleton
    fun provideTokenSteward(
        quotaRepository: com.najmi.falco.domain.repository.IQuotaRepository
    ): TokenSteward = TokenSteward(quotaRepository)

    @Provides
    @Singleton
    fun provideRagVerifier(): RagVerifier = RagVerifier()

    @Provides
    @Singleton
    fun provideTemporalOverrideVerifier(): TemporalOverrideVerifier = TemporalOverrideVerifier()

    @Provides
    @Singleton
    fun provideActorCriticProviderSelector(
        healthTracker: LlmProviderHealthTracker
    ): ActorCriticProviderSelector = ActorCriticProviderSelector(healthTracker)

    @Provides
    @Singleton
    fun provideOutletCredibilityRepository(): OutletCredibilityRepository = OutletCredibilityRepository()

    @Provides
    @Singleton
    fun provideSmartStanceActor(
        chunker: ContentChunker,
        assembler: BatchAssembler,
        providerRouter: TieredProviderRouter,
        earlyStopEvaluator: EarlyStopEvaluator,
        quotaManager: FreeTierQuotaManager,
        incrementalAnalyzer: IncrementalChunkAnalyzer
    ): SmartStanceActor = SmartStanceActor(
        chunker = chunker,
        assembler = assembler,
        providerRouter = providerRouter,
        earlyStopEvaluator = earlyStopEvaluator,
        quotaManager = quotaManager,
        incrementalAnalyzer = incrementalAnalyzer
    )

    @Provides
    @Singleton
    fun provideCrossReferenceAgent(): CrossReferenceAgent = CrossReferenceAgent()
}
