package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AlgorithmicGroundingTest {
    
    private lateinit var grounding: AlgorithmicGrounding
    
    @Before
    fun setup() {
        grounding = AlgorithmicGrounding()
    }
    
    @Test
    fun `verify - reasoning matches abstract returns high score`() {
        val stance = createPaperStance(
            reasoning = "The paper demonstrates that neural networks can achieve better accuracy",
            abstract = "This paper demonstrates that neural networks can achieve better accuracy than traditional methods"
        )
        
        val result = grounding.verify(listOf(stance))
        
        assertTrue(result.first().groundingScore!! > 0.5f)
    }
    
    @Test
    fun `verify - reasoning doesn't match abstract returns low score`() {
        val stance = createPaperStance(
            reasoning = "The paper proves that AI is harmful to society",
            abstract = "This paper demonstrates the benefits of artificial intelligence"
        )
        
        val result = grounding.verify(listOf(stance))
        
        assertTrue(result.first().groundingScore!! < 0.5f)
    }
    
    @Test
    fun `verify - empty reasoning returns zero`() {
        val stance = createPaperStance(
            reasoning = "",
            abstract = "Some abstract content"
        )
        
        val result = grounding.verify(listOf(stance))
        
        assertEquals(0f, result.first().groundingScore)
    }
    
    @Test
    fun `verify - empty abstract returns zero`() {
        val stance = createPaperStance(
            reasoning = "Some reasoning about the paper",
            abstract = ""
        )
        
        val result = grounding.verify(listOf(stance))
        
        assertEquals(0f, result.first().groundingScore)
    }
    
    @Test
    fun `verify - multiple stances returns scored list`() {
        val stances = listOf(
            createPaperStance(
                reasoning = "Shows improved performance",
                abstract = "The method shows improved performance"
            ),
            createPaperStance(
                reasoning = "Completely unrelated claim",
                abstract = "This paper studies plant biology"
            )
        )
        
        val result = grounding.verify(stances)
        
        assertEquals(2, result.size)
        assertTrue(result[0].groundingScore!! > result[1].groundingScore!!)
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
    
    private fun createPaperStance(
        reasoning: String,
        abstract: String
    ) = PaperStance(
        paper = createPaper(abstract = abstract),
        actorStance = Stance.SUPPORTS,
        actorReasoning = reasoning,
        confidence = 0.8f,
        keyEvidence = "",
        relevanceScore = 0.8f
    )
}
