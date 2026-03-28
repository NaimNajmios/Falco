package com.najmi.falco.pipeline

import com.najmi.falco.agent.AggregatorAgent
import com.najmi.falco.agent.AggregatorInput
import com.najmi.falco.agent.ClaimClassifierAgent
import com.najmi.falco.agent.QueryExpansionAgent
import com.najmi.falco.agent.StanceActorAgent
import com.najmi.falco.agent.StanceActorInput
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.repository.IPaperRepository
import com.najmi.falco.domain.repository.IVerdictRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FalcoOrchestrator @Inject constructor(
    private val claimClassifier: ClaimClassifierAgent,
    private val queryExpander: QueryExpansionAgent,
    private val paperRepo: IPaperRepository,
    private val stanceActor: StanceActorAgent,
    private val aggregator: AggregatorAgent,
    private val verdictRepo: IVerdictRepository
) {
    fun verify(claimText: String): Flow<VerificationState> = channelFlow {
        try {
            send(VerificationState.InProgress(VerificationStage.CLASSIFYING, "Identifying claim type..."))
            val claim = claimClassifier.execute(claimText)

            send(VerificationState.InProgress(VerificationStage.EXPANDING_QUERIES, "Generating academic search queries..."))
            val queries = queryExpander.execute(claim)

            send(VerificationState.InProgress(VerificationStage.RETRIEVING_PAPERS, "Searching academic databases..."))
            val papers = paperRepo.searchAll(queries)

            send(VerificationState.InProgress(VerificationStage.ACTOR_CLASSIFICATION, "Classifying stances across ${papers.size} papers..."))
            val stances: List<PaperStance> = papers.map { paper ->
                async { stanceActor.execute(StanceActorInput(claim.text, paper)) }
            }.awaitAll().filter { it.confidence > 0.3f }

            send(VerificationState.InProgress(VerificationStage.AGGREGATING, "Building verdict..."))
            val verdict = aggregator.execute(
                AggregatorInput(
                    claimId = claim.id,
                    claimText = claim.text,
                    claimType = claim.type,
                    stances = stances,
                    totalRetrieved = papers.size
                )
            )

            verdictRepo.save(verdict)
            send(VerificationState.Success(verdict))

        } catch (e: Exception) {
            send(VerificationState.Error(e.message ?: "Verification failed"))
        }
    }
}
