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
        private const val DEFAULT_LIMIT_PER_SOURCE = 5
        private const val DEFAULT_LIMIT_PER_QUERY = 3
    }

    override suspend fun search(query: String, limit: Int): List<Paper> {
        return searchWithFallbacks(query, limit)
    }

    private suspend fun searchWithFallbacks(query: String, limit: Int): List<Paper> {
        val results = mutableListOf<Paper>()
        val errors = mutableListOf<String>()

        try {
            semanticScholar.searchPapers(query, limit).also {
                if (it.isNotEmpty()) results.addAll(it)
            }
        } catch (e: RateLimitException) {
            Log.w(TAG, "Semantic Scholar rate limited for query: $query")
            errors.add("Semantic Scholar: rate limited")
        } catch (e: Exception) {
            Log.w(TAG, "Semantic Scholar failed: ${e.message}")
            errors.add("Semantic Scholar: ${e.message}")
        }

        if (results.isEmpty()) {
            try {
                openAlex.searchPapers(query, limit).also {
                    if (it.isNotEmpty()) results.addAll(it)
                }
            } catch (e: Exception) {
                Log.w(TAG, "OpenAlex fallback failed: ${e.message}")
                errors.add("OpenAlex: ${e.message}")
            }
        }

        return deduplicator.deduplicate(results).take(limit)
    }

    override suspend fun searchAll(queries: List<String>): List<Paper> = coroutineScope {
        val ssDeferred = async {
            try {
                queries.flatMap { query ->
                    try {
                        semanticScholar.searchPapers(query, limit = DEFAULT_LIMIT_PER_QUERY)
                    } catch (e: RateLimitException) {
                        Log.w(TAG, "Semantic Scholar rate limited for query: $query")
                        emptyList()
                    } catch (e: Exception) {
                        Log.w(TAG, "Semantic Scholar failed for query '$query': ${e.message}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Semantic Scholar batch failed: ${e.message}")
                emptyList()
            }
        }
        
        val oaDeferred = async {
            try {
                queries.flatMap { query ->
                    try {
                        openAlex.searchPapers(query, limit = DEFAULT_LIMIT_PER_QUERY)
                    } catch (e: Exception) {
                        Log.w(TAG, "OpenAlex failed for query '$query': ${e.message}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenAlex batch failed: ${e.message}")
                emptyList()
            }
        }
        
        val allPapers = ssDeferred.await() + oaDeferred.await()
        
        if (allPapers.isEmpty()) {
            Log.w(TAG, "No papers retrieved from any source for queries: $queries")
        }
        
        deduplicator.deduplicate(allPapers)
    }

    suspend fun getPapersWithMetadataRefresh(papers: List<Paper>): List<Paper> {
        if (papers.size <= 10) return papers
        return papers.take(10)
    }
}
