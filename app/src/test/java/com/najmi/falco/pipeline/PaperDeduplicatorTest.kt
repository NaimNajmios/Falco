package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PaperDeduplicatorTest {
    
    private lateinit var deduplicator: PaperDeduplicator
    
    @Before
    fun setup() {
        deduplicator = PaperDeduplicator()
    }
    
    private fun createPaper(
        id: String = "1",
        title: String = "Test Paper",
        abstract: String = "Test abstract",
        authors: List<String> = listOf("Author 1"),
        year: Int? = 2024,
        citationCount: Int = 50,
        isOpenAccess: Boolean = true,
        doi: String? = "10.1234/test",
        url: String? = null,
        source: PaperSource = PaperSource.SEMANTIC_SCHOLAR,
        fieldsOfStudy: List<String> = listOf("Computer Science")
    ) = Paper(id, title, abstract, authors, year, citationCount, isOpenAccess, doi, url, source, fieldsOfStudy)
    
    @Test
    fun `deduplicate - same DOI returns single paper`() {
        val papers = listOf(
            createPaper(doi = "10.1234/test"),
            createPaper(doi = "10.1234/test")
        )
        
        val result = deduplicator.deduplicate(papers)
        
        assertEquals(1, result.size)
    }
    
    @Test
    fun `deduplicate - different DOI returns both papers`() {
        val papers = listOf(
            createPaper(doi = "10.1234/test1"),
            createPaper(doi = "10.1234/test2")
        )
        
        val result = deduplicator.deduplicate(papers)
        
        assertEquals(2, result.size)
    }
    
    @Test
    fun `deduplicate - identical title returns single paper`() {
        val papers = listOf(
            createPaper(doi = null, title = "Deep Learning for Computer Vision"),
            createPaper(doi = null, title = "Deep Learning for Computer Vision")
        )
        
        val result = deduplicator.deduplicate(papers)
        
        assertEquals(1, result.size)
    }
    
    @Test
    fun `deduplicate - different titles returns both`() {
        val papers = listOf(
            createPaper(doi = null, title = "Deep Learning for Vision"),
            createPaper(doi = null, title = "Natural Language Processing Advances")
        )
        
        val result = deduplicator.deduplicate(papers)
        
        assertEquals(2, result.size)
    }
    
    @Test
    fun `deduplicate - empty list returns empty`() {
        val result = deduplicator.deduplicate(emptyList())
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `deduplicate - preserves first occurrence`() {
        val papers = listOf(
            createPaper(doi = "10.1234/test", title = "First"),
            createPaper(doi = "10.1234/test", title = "Second")
        )
        
        val result = deduplicator.deduplicate(papers)
        
        assertEquals(1, result.size)
        assertEquals("First", result.first().title)
    }
}
