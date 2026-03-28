package com.najmi.falco.data.repository

import android.util.Log
import com.najmi.falco.data.remote.openapi.OpenAlexClient
import com.najmi.falco.data.remote.semanticscholar.RateLimitException
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

    companion object {
        private const val TAG = "PaperRepository"
    }

    override suspend fun search(query: String, limit: Int): List<Paper> {
        return try {
            semanticScholar.searchPapers(query, limit)
        } catch (e: RateLimitException) {
            Log.w(TAG, "Semantic Scholar rate limited, using OpenAlex fallback")
            openAlex.searchPapers(query, limit)
        }
    }

    override suspend fun searchAll(queries: List<String>): List<Paper> = coroutineScope {
        val ssDeferred = async {
            try {
                queries.flatMap { query ->
                    semanticScholar.searchPapers(query, limit = 5)
                }
            } catch (e: RateLimitException) {
                Log.w(TAG, "Semantic Scholar rate limited in batch search")
                emptyList()
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
