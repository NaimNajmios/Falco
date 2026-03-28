package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TemporalFreshnessAnalyzerTest {
    
    private lateinit var analyzer: TemporalFreshnessAnalyzer
    
    @Before
    fun setup() {
        analyzer = TemporalFreshnessAnalyzer()
    }
    
    @Test
    fun `analyze - returns same papers`() {
        val papers = listOf(createPaperQuality(year = 2026))
        
        val result = analyzer.analyze(papers, ClaimType.EMPIRICAL)
        
        assertEquals(papers, result)
    }
    
    @Test
    fun `generateTemporalWarning - majority stale returns warning`() {
        val papers = listOf(
            createPaperQuality(year = 2010, freshnessFlag = FreshnessFlag.STALE),
            createPaperQuality(year = 2011, freshnessFlag = FreshnessFlag.STALE),
            createPaperQuality(year = 2026, freshnessFlag = FreshnessFlag.FRESH)
        )
        
        val warning = analyzer.generateTemporalWarning(papers)
        
        assertNotNull(warning)
        assertTrue(warning!!.contains("predates"))
    }
    
    @Test
    fun `generateTemporalWarning - minority stale returns null`() {
        val papers = listOf(
            createPaperQuality(year = 2026, freshnessFlag = FreshnessFlag.FRESH),
            createPaperQuality(year = 2026, freshnessFlag = FreshnessFlag.FRESH),
            createPaperQuality(year = 2018, freshnessFlag = FreshnessFlag.STALE)
        )
        
        val warning = analyzer.generateTemporalWarning(papers)
        
        assertNull(warning)
    }
    
    @Test
    fun `generateTemporalWarning - empty list returns null`() {
        val warning = analyzer.generateTemporalWarning(emptyList())
        
        assertNull(warning)
    }
    
    @Test
    fun `computeFieldFreshness - returns most common field`() {
        val papers = listOf(
            createPaperQuality(fieldsOfStudy = listOf("AI", "ML")),
            createPaperQuality(fieldsOfStudy = listOf("AI", "Vision")),
            createPaperQuality(fieldsOfStudy = listOf("NLP"))
        )
        
        val field = analyzer.computeFieldFreshness(papers)
        
        assertEquals("AI", field)
    }
    
    @Test
    fun `computeFieldFreshness - empty list returns Unknown`() {
        val field = analyzer.computeFieldFreshness(emptyList())
        
        assertEquals("Unknown", field)
    }
    
    private fun createPaper(
        id: String = "1",
        title: String = "Test Paper",
        abstract: String = "Test abstract",
        authors: List<String> = listOf("Author 1"),
        year: Int? = 2026,
        citationCount: Int = 50,
        isOpenAccess: Boolean = true,
        doi: String? = "10.1234/test",
        url: String? = null,
        source: PaperSource = PaperSource.SEMANTIC_SCHOLAR,
        fieldsOfStudy: List<String> = listOf("Computer Science")
    ) = Paper(id, title, abstract, authors, year, citationCount, isOpenAccess, doi, url, source, fieldsOfStudy)
    
    private fun createPaperQuality(
        year: Int = 2026,
        freshnessFlag: FreshnessFlag = FreshnessFlag.FRESH,
        fieldsOfStudy: List<String> = listOf("Computer Science")
    ) = PaperQuality(
        paper = createPaper(year = year, fieldsOfStudy = fieldsOfStudy),
        qualityScore = 0.8f,
        citationTier = CitationTier.HIGH,
        freshnessFlag = freshnessFlag,
        passesGate = true
    )
}
