package com.najmi.falco.agent

import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import javax.inject.Inject
import javax.inject.Singleton

data class CrossReferenceInput(
    val claimText: String,
    val stances: List<PaperStance>
)

data class CrossReferenceOutput(
    val enrichedStances: List<PaperStance>,
    val consensusClusters: List<ConsensusCluster>,
    val outliers: List<PaperStance>
)

data class ConsensusCluster(
    val stance: Stance,
    val papers: List<PaperStance>,
    val isMajority: Boolean
)

@Singleton
class CrossReferenceAgent @Inject constructor() : IFalcoAgent<CrossReferenceInput, CrossReferenceOutput> {

    companion object {
        private const val MIN_CLUSTER_SIZE = 2
        private const val MAJORITY_THRESHOLD = 0.25f
    }

    override val agentName = "CrossReferenceAgent"
    override val defaultProvider = com.najmi.falco.data.remote.LlmProvider.GEMINI

    override suspend fun execute(
        input: CrossReferenceInput,
        preferredProvider: com.najmi.falco.data.remote.LlmProvider?
    ): Result<CrossReferenceOutput> {
        return try {
            val clusters = identifyClusters(input.stances)
            val outliers = identifyOutliers(input.stances, clusters)
            
            val enrichedStances = input.stances.map { stance ->
                val cluster = clusters.find { it.papers.contains(stance) }
                stance.copy(
                    isConsensus = cluster != null && cluster.papers.size >= MIN_CLUSTER_SIZE,
                    isOutlier = outliers.contains(stance)
                )
            }

            DebugLogger.d("[CROSS-REFERENCE] Found ${clusters.size} clusters, ${outliers.size} outliers")
            
            Result.success(CrossReferenceOutput(enrichedStances, clusters, outliers))
        } catch (e: Exception) {
            DebugLogger.e("[CROSS-REFERENCE] Failed: ${e.message}")
            Result.success(CrossReferenceOutput(input.stances, emptyList(), emptyList()))
        }
    }

    private fun identifyClusters(stances: List<PaperStance>): List<ConsensusCluster> {
        val stanceGroups = stances.groupBy { it.finalStance ?: it.actorStance }
        val totalStances = stances.size
        
        return stanceGroups.map { (stance, papers) ->
            ConsensusCluster(
                stance = stance,
                papers = papers,
                isMajority = papers.size.toFloat() / totalStances >= MAJORITY_THRESHOLD
            )
        }.filter { it.papers.size >= MIN_CLUSTER_SIZE }
            .sortedByDescending { it.papers.size }
    }

    private fun identifyOutliers(
        stances: List<PaperStance>,
        clusters: List<ConsensusCluster>
    ): List<PaperStance> {
        val totalStances = stances.size
        if (totalStances == 0) return emptyList()

        return stances.filter { stance ->
            val cluster = clusters.find { it.papers.contains(stance) }
            cluster == null || cluster.papers.size < MIN_CLUSTER_SIZE
        }
    }
}
