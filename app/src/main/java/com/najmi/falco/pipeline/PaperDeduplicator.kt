package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.Paper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaperDeduplicator @Inject constructor() {

    fun deduplicate(papers: List<Paper>): List<Paper> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Paper>()
        
        for (paper in papers) {
            val key = paper.doi ?: normalizeTitle(paper.title)
            if (seen.add(key.lowercase())) {
                result.add(paper)
            }
        }
        
        return result
    }

    private fun normalizeTitle(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .split(" ")
            .filter { it.length > 3 }
            .take(6)
            .joinToString(" ")
    }
}
