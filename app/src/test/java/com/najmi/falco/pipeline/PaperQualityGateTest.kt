package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PaperQualityGateTest {
    
    private lateinit var gate: PaperQualityGate
    
    @Before
    fun setup() {
        gate = PaperQualityGate()
    }
    
    private fun createPaper(
        id: String = "1",
        title: String = "Test Paper",
        abstract: String = "This is a test abstract with sufficient length to pass the quality gate requirements for testing purposes",
        authors: List<String> = listOf("Author 1"),
        year: Int? = 2024,
        citationCount: Int = 50,
        isOpenAccess: Boolean = true,
        doi: String? = null,
        url: String? = null,
        source: PaperSource = PaperSource.SEMANTIC_SCHOLAR,
        fieldsOfStudy: List<String> = listOf("Computer Science")
    ) = Paper(id, title, abstract, authors, year, citationCount, isOpenAccess, doi, url, source, fieldsOfStudy)
    
    @Test
    fun `filter - high citation paper passes gate`() {
        val paper = createPaper(citationCount = 100, year = 2024)
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isNotEmpty())
        assertTrue(result.first().passesGate)
    }
    
    @Test
    fun `filter - low citation paper fails gate`() {
        val paper = createPaper(citationCount = 1, year = 2020)
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `filter - short abstract fails gate`() {
        val paper = createPaper(abstract = "Short")
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `filter - stale paper fails gate when low citation`() {
        val paper = createPaper(citationCount = 5, year = 2015)
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `citation tier - high citations returns HIGH`() {
        val paper = createPaper(citationCount = 100)
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isNotEmpty())
        assertEquals(CitationTier.HIGH, result.first().citationTier)
    }
    
    @Test
    fun `citation tier - medium citations returns MEDIUM`() {
        val paper = createPaper(citationCount = 15)
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isNotEmpty())
        assertEquals(CitationTier.MEDIUM, result.first().citationTier)
    }
    
    @Test
    fun `freshness - recent paper returns FRESH`() {
        val paper = createPaper(year = 2026)
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isNotEmpty())
        assertEquals(FreshnessFlag.FRESH, result.first().freshnessFlag)
    }
    
    @Test
    fun `freshness - old CS paper returns STALE`() {
        val paper = createPaper(year = 2025, fieldsOfStudy = listOf("Computer Science"))
        val result = gate.filter(listOf(paper), ClaimType.EMPIRICAL)
        
        assertTrue(result.isNotEmpty())
        assertEquals(FreshnessFlag.STALE, result.first().freshnessFlag)
    }
}
