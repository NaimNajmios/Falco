package com.najmi.falco.data.repository

import android.util.Log
import com.najmi.falco.data.remote.openapi.OpenAlexClient
import com.najmi.falco.data.remote.semanticscholar.RateLimitException
import com.najmi.falco.data.remote.semanticscholar.SemanticScholarClient
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.repository.IPaperRepository
import com.najmi.falco.domain.repository.PaperSearchResult
import com.najmi.falco.pipeline.PaperDeduplicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

    override suspend fun searchAll(queries: List<String>): PaperSearchResult = coroutineScope {
        val databasesQueried = mutableListOf<String>()

        val ssDeferred = async {
            val results = mutableListOf<Paper>()
            for (query in queries) {
                try {
                    semanticScholar.searchPapers(query, limit = DEFAULT_LIMIT_PER_QUERY).let { results.addAll(it) }
                } catch (e: RateLimitException) {
                    Log.w(TAG, "Semantic Scholar rate limited, backing off for query: $query")
                    delay(5000L)
                    try {
                        semanticScholar.searchPapers(query, limit = DEFAULT_LIMIT_PER_QUERY).let { results.addAll(it) }
                    } catch (e2: Exception) {
                        Log.w(TAG, "Semantic Scholar retry failed for '$query': ${e2.message}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Semantic Scholar failed for '$query': ${e.message}")
                }
                delay(1500L)
            }
            results
        }

        val oaDeferred = async {
            val results = mutableListOf<Paper>()
            for (query in queries) {
                try {
                    openAlex.searchPapers(query, limit = DEFAULT_LIMIT_PER_QUERY).let { results.addAll(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "OpenAlex failed for '$query': ${e.message}")
                }
                delay(500L)
            }
            results
        }

        val ssPapers = ssDeferred.await()
        val oaPapers = oaDeferred.await()

        if (ssPapers.isNotEmpty()) databasesQueried.add("Semantic Scholar")
        if (oaPapers.isNotEmpty()) databasesQueried.add("OpenAlex")

        val allPapers = ssPapers + oaPapers

        if (allPapers.isEmpty()) {
            Log.w(TAG, "No papers retrieved from any source for queries: $queries")
        }

        PaperSearchResult(
            papers = deduplicator.deduplicate(allPapers),
            databasesQueried = databasesQueried
        )
    }

    suspend fun getPapersWithMetadataRefresh(papers: List<Paper>): List<Paper> {
        if (papers.size <= 10) return papers
        return papers.take(10)
    }
}
