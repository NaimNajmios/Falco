package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Stance
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BatchAssemblerTest {

    private lateinit var assembler: BatchAssembler

    @Before
    fun setup() {
        assembler = BatchAssembler()
    }

    private fun createChunk(
        id: Int = 0,
        priority: Int = 0,
        label: String = "[ABSTRACT]",
        content: String = "Test content",
        sourceSection: ChunkSource = ChunkSource.ABSTRACT
    ) = EvidenceChunk(
        id = id,
        priority = priority,
        label = label,
        content = content,
        estimatedTokens = (content.split(" ").size * 1.3).toInt(),
        sourceSection = sourceSection
    )

    @Test
    fun `assemble - creates valid prompt with all sections`() {
        val chunks = listOf(
            createChunk(id = 0, label = "[ABSTRACT]", content = "This study shows positive effects."),
            createChunk(id = 1, label = "[CONCLUSION]", content = "We conclude the treatment works.")
        )

        val result = assembler.assemble(
            claim = "Treatment is effective",
            paperTitle = "Test Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertTrue(result.prompt.contains("[ABSTRACT]"))
        assertTrue(result.prompt.contains("[CONCLUSION]"))
        assertTrue(result.prompt.contains("Treatment is effective"))
        assertTrue(result.prompt.contains("Test Paper"))
        assertTrue(result.prompt.contains("2024"))
    }

    @Test
    fun `assemble - throws on empty chunks`() {
        assertThrows(IllegalArgumentException::class.java) {
            assembler.assemble(
                claim = "Test claim",
                paperTitle = "Test Paper",
                paperYear = 2024,
                chunks = emptyList()
            )
        }
    }

    @Test
    fun `assemble - includes JSON output schema in prompt`() {
        val chunks = listOf(createChunk())

        val result = assembler.assemble(
            claim = "Test claim",
            paperTitle = "Test Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertTrue(result.prompt.contains("overall_stance"))
        assertTrue(result.prompt.contains("overall_confidence"))
        assertTrue(result.prompt.contains("excerpt_analyses"))
        assertTrue(result.prompt.contains("reasoning"))
    }

    @Test
    fun `assemble - correct chunk count in result`() {
        val chunks = listOf(
            createChunk(id = 0),
            createChunk(id = 1),
            createChunk(id = 2)
        )

        val result = assembler.assemble(
            claim = "Test",
            paperTitle = "Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertEquals(3, result.chunkCount)
    }

    @Test
    fun `assemble - sections match input chunks`() {
        val chunks = listOf(
            createChunk(id = 0, label = "[ABSTRACT]", content = "Abstract content"),
            createChunk(id = 1, label = "[EXCERPT 1]", content = "Excerpt content")
        )

        val result = assembler.assemble(
            claim = "Test",
            paperTitle = "Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertEquals(2, result.sections.size)
        assertEquals("[ABSTRACT]", result.sections[0].label)
        assertEquals("Abstract content", result.sections[0].content)
        assertEquals("[EXCERPT 1]", result.sections[1].label)
        assertEquals("Excerpt content", result.sections[1].content)
    }

    @Test
    fun `assemble - estimated tokens calculated`() {
        val chunks = listOf(createChunk(content = "Word " + "test ".repeat(100)))

        val result = assembler.assemble(
            claim = "Test",
            paperTitle = "Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertTrue(result.estimatedInputTokens > 0)
    }

    @Test
    fun `assemble - handles null year`() {
        val chunks = listOf(createChunk())

        val result = assembler.assemble(
            claim = "Test",
            paperTitle = "Paper",
            paperYear = null,
            chunks = chunks
        )

        assertTrue(result.prompt.contains("Unknown"))
    }

    @Test
    fun `validate - valid chunks pass`() {
        val chunks = listOf(
            createChunk(id = 0),
            createChunk(id = 1),
            createChunk(id = 2)
        )

        val result = assembler.validate(chunks, 2000)

        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate - too many chunks fails`() {
        val chunks = (0..5).map { createChunk(id = it) }

        val result = assembler.validate(chunks, 2000)

        assertFalse(result.isValid)
        assertTrue(result.warnings.any { it.contains("max chunks") })
    }

    @Test
    fun `validate - too many tokens fails`() {
        val chunks = listOf(createChunk())

        val result = assembler.validate(chunks, 4000)

        assertFalse(result.isValid)
        assertTrue(result.warnings.any { it.contains("max tokens") })
    }

    @Test
    fun `validate - returns warning count`() {
        val chunks = (0..5).map { createChunk(id = it) }

        val result = assembler.validate(chunks, 4000)

        assertTrue(result.warnings.size >= 2)
    }

    @Test
    fun `parseExpectedStance - extracts SUPPORTS`() {
        val json = """{"overall_stance": "SUPPORTS", "overall_confidence": 0.8}"""
        val result = BatchAssembler.parseExpectedStance(json)
        assertEquals(Stance.SUPPORTS, result)
    }

    @Test
    fun `parseExpectedStance - extracts OPPOSES`() {
        val json = """{"overall_stance": "OPPOSES"}"""
        val result = BatchAssembler.parseExpectedStance(json)
        assertEquals(Stance.OPPOSES, result)
    }

    @Test
    fun `parseExpectedStance - extracts NEUTRAL`() {
        val json = """{"overall_stance": "NEUTRAL"}"""
        val result = BatchAssembler.parseExpectedStance(json)
        assertEquals(Stance.NEUTRAL, result)
    }

    @Test
    fun `parseExpectedStance - handles lowercase`() {
        val json = """{"overall_stance": "supports"}"""
        val result = BatchAssembler.parseExpectedStance(json)
        assertEquals(Stance.SUPPORTS, result)
    }

    @Test
    fun `parseExpectedStance - handles markdown code blocks`() {
        val json = """```json
{"overall_stance": "SUPPORTS"}
```"""
        val result = BatchAssembler.parseExpectedStance(json)
        assertEquals(Stance.SUPPORTS, result)
    }

    @Test
    fun `parseExpectedStance - returns null for invalid`() {
        val json = """{"overall_stance": "INVALID"}"""
        val result = BatchAssembler.parseExpectedStance(json)
        assertNull(result)
    }

    @Test
    fun `parseExpectedStance - returns null for missing field`() {
        val json = """{"other_field": "value"}"""
        val result = BatchAssembler.parseExpectedStance(json)
        assertNull(result)
    }

    @Test
    fun `parseExpectedConfidence - extracts value`() {
        val json = """{"overall_stance": "SUPPORTS", "overall_confidence": 0.85}"""
        val result = BatchAssembler.parseExpectedConfidence(json)
        assertEquals(0.85f, result)
    }

    @Test
    fun `parseExpectedConfidence - coerces high values`() {
        val json = """{"overall_confidence": 1.5}"""
        val result = BatchAssembler.parseExpectedConfidence(json)
        assertEquals(1.0f, result)
    }

    @Test
    fun `parseExpectedConfidence - coerces low values`() {
        val json = """{"overall_confidence": -0.5}"""
        val result = BatchAssembler.parseExpectedConfidence(json)
        assertEquals(0.0f, result)
    }

    @Test
    fun `parseExpectedConfidence - returns null for missing`() {
        val json = """{"other": "value"}"""
        val result = BatchAssembler.parseExpectedConfidence(json)
        assertNull(result)
    }

    @Test
    fun `parseExpectedConfidence - handles markdown blocks`() {
        val json = """```
{"overall_confidence": 0.7}
```"""
        val result = BatchAssembler.parseExpectedConfidence(json)
        assertEquals(0.7f, result)
    }

    @Test
    fun `assemble - prompt includes analyze instructions`() {
        val chunks = listOf(createChunk())

        val result = assembler.assemble(
            claim = "Test",
            paperTitle = "Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertTrue(result.prompt.contains("Analyze each excerpt INDEPENDENTLY"))
        assertTrue(result.prompt.contains("Evaluate the overall stance"))
    }

    @Test
    fun `assemble - prompt includes confidence scale info`() {
        val chunks = listOf(createChunk())

        val result = assembler.assemble(
            claim = "Test",
            paperTitle = "Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertTrue(result.prompt.contains("Confidence 0.0"))
        assertTrue(result.prompt.contains("1.0"))
    }

    @Test
    fun `assemble - single chunk works`() {
        val chunks = listOf(createChunk(id = 0, label = "[ABSTRACT]"))

        val result = assembler.assemble(
            claim = "Test claim",
            paperTitle = "Paper",
            paperYear = 2024,
            chunks = chunks
        )

        assertEquals(1, result.chunkCount)
        assertEquals(1, result.sections.size)
    }

    @Test
    fun `validate - chunk token sum checked`() {
        val chunks = listOf(
            EvidenceChunk(0, 0, "[A]", "x", 2500, ChunkSource.ABSTRACT),
            EvidenceChunk(1, 1, "[B]", "y", 1000, ChunkSource.BODY_PARAGRAPH)
        )

        val result = assembler.validate(chunks, 3000)

        assertFalse(result.isValid)
        assertTrue(result.warnings.any { it.contains("Chunk tokens") })
    }
}
