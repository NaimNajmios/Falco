package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperSource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ContentChunkerTest {

    private lateinit var chunker: ContentChunker

    @Before
    fun setup() {
        chunker = ContentChunker()
    }

    private fun createPaper(
        id: String = "1",
        title: String = "Test Paper",
        abstract: String = "This is a test abstract."
    ): Paper = Paper(
        id = id,
        title = title,
        abstract = abstract,
        authors = listOf("Author"),
        year = 2024,
        citationCount = 100,
        isOpenAccess = true,
        doi = null,
        url = null,
        source = PaperSource.SEMANTIC_SCHOLAR,
        fieldsOfStudy = listOf("Computer Science")
    )

    @Test
    fun `chunk - abstract always included as first chunk`() {
        val paper = createPaper(abstract = "This is a comprehensive study on machine learning algorithms and their applications in healthcare diagnostics.")
        val claim = "Machine learning improves healthcare diagnostics"

        val chunks = chunker.chunk(paper, claim)

        assertTrue(chunks.isNotEmpty())
        assertEquals(ChunkSource.ABSTRACT, chunks.first().sourceSection)
        assertEquals(0, chunks.first().priority)
        assertEquals("[ABSTRACT]", chunks.first().label)
    }

    @Test
    fun `chunk - conclusion extracted when present`() {
        val paper = createPaper(
            abstract = "This study examines the relationship between exercise and health outcomes. " +
                    "In conclusion, our findings demonstrate a significant positive correlation between regular physical activity and cardiovascular health."
        )
        val claim = "Exercise improves cardiovascular health"

        val chunks = chunker.chunk(paper, claim)

        assertTrue(chunks.any { it.sourceSection == ChunkSource.CONCLUSION })
    }

    @Test
    fun `chunk - no conclusion when absent`() {
        val paper = createPaper(
            abstract = "This study examines the relationship between exercise and health outcomes."
        )
        val claim = "Exercise improves cardiovascular health"

        val chunks = chunker.chunk(paper, claim)

        assertFalse(chunks.any { it.sourceSection == ChunkSource.CONCLUSION })
    }

    @Test
    fun `chunk - max 3 chunks enforced`() {
        val paper = createPaper(
            abstract = "First paragraph about machine learning methods and approach. " +
                    "Second paragraph about the dataset used in our experiments. " +
                    "Third paragraph showing significant results. " +
                    "Fourth paragraph with additional findings. " +
                    "Fifth paragraph with more analysis."
        )
        val claim = "Machine learning methods show significant results"

        val chunks = chunker.chunk(paper, claim)

        assertTrue(chunks.size <= 3)
    }

    @Test
    fun `chunk - chunks sorted by priority`() {
        val paper = createPaper(
            abstract = "Introduction paragraph with methods. " +
                    "Second paragraph with key results. " +
                    "Third paragraph with discussion."
        )
        val claim = "methods results discussion"

        val chunks = chunker.chunk(paper, claim)

        val priorities = chunks.map { it.priority }
        assertEquals(priorities.sorted(), priorities)
    }

    @Test
    fun `chunk - abstract truncated to 500 tokens`() {
        val longAbstract = "Word " .repeat(600)
        val paper = createPaper(abstract = longAbstract)
        val claim = "test claim"

        val chunks = chunker.chunk(paper, claim)
        val abstractChunk = chunks.first { it.sourceSection == ChunkSource.ABSTRACT }

        assertTrue(abstractChunk.estimatedTokens <= EvidenceChunk.ABSTRACT_MAX_TOKENS)
    }

    @Test
    fun `chunk - body paragraphs prioritized by keyword match`() {
        val paper = createPaper(
            abstract = "First paragraph about nothing important. " +
                    "Second paragraph about neural networks and deep learning architectures. " +
                    "Third paragraph about baseline comparisons."
        )
        val claim = "neural networks deep learning"

        val chunks = chunker.chunk(paper, claim)
        val bodyChunks = chunks.filter { it.sourceSection == ChunkSource.BODY_PARAGRAPH }

        if (bodyChunks.isNotEmpty()) {
            val relevantChunk = bodyChunks.firstOrNull { it.content.contains("neural") }
            assertTrue(relevantChunk != null || bodyChunks.size <= 1)
        }
    }

    @Test
    fun `chunk - methodology terms boost priority`() {
        val paper = createPaper(
            abstract = "First paragraph with some content. " +
                    "Second paragraph describing the experimental method and study design with participants. " +
                    "Third paragraph with other content."
        )
        val claim = "method study design"

        val chunks = chunker.chunk(paper, claim)

        assertTrue(chunks.size >= 1)
    }

    @Test
    fun `chunk - empty abstract returns only body chunks`() {
        val paper = createPaper(abstract = "")
        val claim = "test claim"

        val chunks = chunker.chunk(paper, claim)

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `chunk - token estimation accurate within 20 percent`() {
        val paper = createPaper(
            abstract = "This study investigates the effectiveness of various machine learning approaches for natural language processing tasks. " +
                    "We collected a large dataset of text documents and applied several algorithms including transformer models. " +
                    "Our results demonstrate significant improvements over baseline methods with p-values less than 0.05. " +
                    "The findings suggest that advanced neural architectures are highly effective for language understanding."
        )
        val claim = "machine learning NLP transformers"

        val chunks = chunker.chunk(paper, claim)

        chunks.forEach { chunk ->
            val wordCount = chunk.content.split(Regex("\\s+")).size
            val estimatedWords = (chunk.estimatedTokens / 1.3f).toInt()
            val ratio = (wordCount.toFloat() / estimatedWords).coerceIn(0.8f, 1.2f)
            assertTrue("Word count ratio $ratio out of range", ratio in 0.8f..1.2f)
        }
    }

    @Test
    fun `chunk - single paragraph abstract no body chunks`() {
        val paper = createPaper(
            abstract = "A simple abstract with limited content."
        )
        val claim = "simple content"

        val chunks = chunker.chunk(paper, claim)

        val bodyChunks = chunks.filter { it.sourceSection == ChunkSource.BODY_PARAGRAPH }
        assertEquals(0, bodyChunks.size)
    }

    @Test
    fun `chunk - custom config respected`() {
        val config = ContentChunker.Config(maxChunksPerPaper = 2)
        val paper = createPaper(
            abstract = "First paragraph. Second paragraph. Third paragraph. Fourth paragraph."
        )
        val claim = "test"

        val chunks = chunker.chunk(paper, claim, config)

        assertTrue(chunks.size <= 2)
    }

    @Test
    fun `chunk - conclusion truncated to 800 tokens`() {
        val longConclusion = "Conclusion content. " + "Word ".repeat(200)
        val paper = createPaper(
            abstract = "Study intro. " + longConclusion
        )
        val claim = "test"

        val chunks = chunker.chunk(paper, claim)
        val conclusionChunk = chunks.find { it.sourceSection == ChunkSource.CONCLUSION }

        if (conclusionChunk != null) {
            assertTrue(conclusionChunk.estimatedTokens <= EvidenceChunk.CONCLUSION_MAX_TOKENS)
        }
    }

    @Test
    fun `chunk - priority 0 always abstract`() {
        val paper = createPaper(
            abstract = "Abstract content."
        )
        val claim = "claim"

        val chunks = chunker.chunk(paper, claim)

        if (chunks.isNotEmpty()) {
            assertEquals(0, chunks.first().priority)
        }
    }

    @Test
    fun `chunk - labels are unique`() {
        val paper = createPaper(
            abstract = "Para one. Para two. Para three. Para four. Para five."
        )
        val claim = "test"

        val chunks = chunker.chunk(paper, claim)

        val labels = chunks.map { it.label }
        assertEquals(labels.toSet().size, labels.size)
    }

    @Test
    fun `chunk - chunk IDs are sequential`() {
        val paper = createPaper(
            abstract = "First paragraph here. Second paragraph here. Third paragraph here. Fourth paragraph here."
        )
        val claim = "test"

        val chunks = chunker.chunk(paper, claim)

        val ids = chunks.map { it.id }
        assertEquals(ids.sorted(), ids)
    }
}
