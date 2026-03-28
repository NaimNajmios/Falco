package com.najmi.falco.data.repository

import com.najmi.falco.data.remote.openapi.OpenAlexClient
import com.najmi.falco.data.remote.semanticscholar.SemanticScholarClient
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.repository.IPaperRepository
import com.najmi.falco.pipeline.PaperDeduplicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaperRepositoryImpl @Inject constructor(
    private val semanticScholar: SemanticScholarClient,
    private val openAlex: OpenAlexClient,
    private val deduplicator: PaperDeduplicator
) : IPaperRepository {

    override suspend fun search(query: String, limit: Int): List<Paper> {
        return semanticScholar.searchPapers(query, limit)
    }

    override suspend fun searchAll(queries: List<String>): List<Paper> = coroutineScope {
        val ssDeferred = async {
            queries.flatMap { query ->
                semanticScholar.searchPapers(query, limit = 5)
            }
        }
        
        val oaDeferred = async {
            queries.flatMap { query ->
                openAlex.searchPapers(query, limit = 5)
            }
        }
        
        val allPapers = ssDeferred.await() + oaDeferred.await()
        deduplicator.deduplicate(allPapers)
    }
}
