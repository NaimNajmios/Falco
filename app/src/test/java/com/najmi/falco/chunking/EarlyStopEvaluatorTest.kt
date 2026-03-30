package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Stance
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EarlyStopEvaluatorTest {

    private lateinit var evaluator: EarlyStopEvaluator

    @Before
    fun setup() {
        evaluator = EarlyStopEvaluator()
    }

    private fun createState(
        analyzedCount: Int,
        stances: List<Stance>,
        confidences: List<Float>
    ): StanceAnalysisState {
        val results = stances.mapIndexed { idx, stance ->
            SmartStanceResult(
                overallStance = stance,
                overallConfidence = confidences.getOrElse(idx) { 0.5f },
                excerptAnalyses = emptyList(),
                reasoning = "Test reasoning $idx",
                chunksUsed = 1
            )
        }
        return StanceAnalysisState(
            analyzedCount = analyzedCount,
            stances = stances,
            confidences = confidences
        )
    }

    @Test
    fun `shouldContinue - needs minimum papers`() {
        val state = createState(2, listOf(Stance.SUPPORTS, Stance.SUPPORTS), listOf(0.9f, 0.9f))
        
        val result = evaluator.shouldContinue(state)
        
        assertTrue(result)
    }

    @Test
    fun `shouldContinue - consensus reached with high confidence stops`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.NEUTRAL, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.92f, 0.7f, 0.85f)
        )
        
        val result = evaluator.shouldContinue(state)
        
        assertFalse(result)
    }

    @Test
    fun `shouldContinue - max papers reached stops`() {
        val state = createState(
            10,
            listOf(
                Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS,
                Stance.SUPPORTS, Stance.NEUTRAL, Stance.SUPPORTS,
                Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.SUPPORTS
            ),
            listOf(0.8f, 0.7f, 0.9f, 0.85f, 0.6f, 0.88f, 0.92f, 0.75f, 0.87f, 0.83f)
        )
        
        val result = evaluator.shouldContinue(state)
        
        assertFalse(result)
    }

    @Test
    fun `shouldContinue - no consensus continues`() {
        val state = createState(
            4,
            listOf(Stance.SUPPORTS, Stance.OPPOSES, Stance.NEUTRAL, Stance.SUPPORTS),
            listOf(0.8f, 0.75f, 0.6f, 0.82f)
        )
        
        val result = evaluator.shouldContinue(state)
        
        assertTrue(result)
    }

    @Test
    fun `shouldContinue - low confidence continues`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.5f, 0.55f, 0.48f, 0.52f, 0.53f)
        )
        
        val result = evaluator.shouldContinue(state)
        
        assertTrue(result)
    }

    @Test
    fun `evaluate - returns correct decision reason`() {
        val state = createState(10, listOf(Stance.SUPPORTS), listOf(0.9f))
        
        val decision = evaluator.evaluate(state)
        
        assertEquals("Max papers reached (10)", decision.reason)
    }

    @Test
    fun `evaluate - reports dominant stance`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.85f, 0.7f, 0.88f, 0.92f)
        )
        
        val decision = evaluator.evaluate(state)
        
        assertEquals(Stance.SUPPORTS, decision.dominantStance)
    }

    @Test
    fun `evaluate - custom criteria respected`() {
        val state = createState(
            4,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.92f, 0.87f)
        )
        
        val criteria = EarlyStopEvaluator.StopCriteria(minPapersAnalyzed = 4)
        val result = evaluator.shouldContinue(state, criteria)
        
        assertFalse(result)
    }

    @Test
    fun `consensusReached - 70 percent threshold`() {
        val state = createState(
            4,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.OPPOSES),
            listOf(0.9f, 0.85f, 0.88f, 0.7f)
        )
        
        assertTrue(state.consensusReached())
    }

    @Test
    fun `consensusReached - below threshold`() {
        val state = createState(
            4,
            listOf(Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.OPPOSES),
            listOf(0.9f, 0.85f, 0.88f, 0.87f)
        )
        
        assertFalse(state.consensusReached())
    }

    @Test
    fun `consensusReached - exactly 70 percent`() {
        val state = createState(
            10,
            listOf(
                Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS,
                Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS,
                Stance.OPPOSES, Stance.OPPOSES, Stance.OPPOSES
            ),
            listOf(0.9f, 0.85f, 0.88f, 0.87f, 0.92f, 0.86f, 0.91f, 0.7f, 0.75f, 0.73f)
        )
        
        assertTrue(state.consensusReached())
    }

    @Test
    fun `averageConfidence - correct calculation`() {
        val state = createState(
            3,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.8f, 0.9f, 1.0f)
        )
        
        assertEquals(0.9f, state.averageConfidence(), 0.01f)
    }

    @Test
    fun `averageConfidence - empty returns zero`() {
        val state = StanceAnalysisState()
        
        assertEquals(0f, state.averageConfidence())
    }

    @Test
    fun `dominantStance - returns most common`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.85f, 0.88f, 0.87f, 0.92f)
        )
        
        assertEquals(Stance.SUPPORTS, state.dominantStance())
    }

    @Test
    fun `dominantStance - empty returns null`() {
        val state = StanceAnalysisState()
        
        assertNull(state.dominantStance())
    }

    @Test
    fun `stanceDistribution - correct counts`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.NEUTRAL, Stance.SUPPORTS),
            listOf(0.9f, 0.85f, 0.88f, 0.6f, 0.87f)
        )
        
        val dist = state.stanceDistribution()
        
        assertEquals(3, dist[Stance.SUPPORTS])
        assertEquals(1, dist[Stance.OPPOSES])
        assertEquals(1, dist[Stance.NEUTRAL])
    }

    @Test
    fun `canStopEarly - true when consensus and high confidence`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.92f, 0.87f, 0.91f)
        )
        
        assertTrue(state.canStopEarly())
    }

    @Test
    fun `canStopEarly - false when no consensus`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.85f, 0.87f, 0.91f)
        )
        
        assertFalse(state.canStopEarly())
    }

    @Test
    fun `progress - correct percentage`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.92f, 0.87f, 0.91f)
        )
        
        assertEquals(0.5f, state.progress(), 0.01f)
    }

    @Test
    fun `progress - caps at 1`() {
        val state = createState(
            15,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.88f)
        )
        
        assertEquals(1f, state.progress(), 0.01f)
    }

    @Test
    fun `summary - provides full state overview`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.92f, 0.7f, 0.91f)
        )
        
        val summary = state.summary()
        
        assertEquals(5, summary.analyzedCount)
        assertEquals(10, summary.maxPapers)
        assertEquals(0.5f, summary.progress, 0.01f)
        assertEquals(Stance.SUPPORTS, summary.dominantStance)
    }

    @Test
    fun `estimateStoppingPoint - returns null when no consensus and max papers exceeded`() {
        val state = createState(
            10,
            listOf(Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS,
                   Stance.OPPOSES, Stance.SUPPORTS, Stance.OPPOSES, Stance.SUPPORTS, Stance.OPPOSES),
            listOf(0.9f, 0.85f, 0.88f, 0.82f, 0.87f, 0.84f, 0.86f, 0.83f, 0.89f, 0.81f)
        )
        
        val point = evaluator.estimateStoppingPoint(state)
        
        assertNull(point)
    }

    @Test
    fun `isClearCutCase - true for unanimous results`() {
        val state = createState(
            3,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.92f)
        )
        
        assertTrue(evaluator.isClearCutCase(state))
    }

    @Test
    fun `isClearCutCase - false for insufficient papers`() {
        val state = createState(
            1,
            listOf(Stance.SUPPORTS),
            listOf(0.9f)
        )
        
        assertFalse(evaluator.isClearCutCase(state))
    }

    @Test
    fun `confidenceThreshold - returns average when all same stances`() {
        val state = createState(
            5,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f)
        )
        
        val threshold = evaluator.confidenceThreshold(state, 0.6f)
        
        assertNotNull(threshold)
        if (threshold != null) {
            assertEquals(0.7f, threshold, 0.01f)
        }
    }

    @Test
    fun `shouldAnalyzeMore - returns false at max`() {
        val state = createState(
            10,
            listOf(Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS,
                   Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS, Stance.SUPPORTS),
            listOf(0.9f, 0.88f, 0.92f, 0.87f, 0.91f, 0.85f, 0.89f, 0.93f, 0.86f, 0.9f)
        )
        
        assertFalse(state.shouldAnalyzeMore())
    }

    @Test
    fun `withResult - adds result correctly`() {
        val state = StanceAnalysisState()
        val newResult = SmartStanceResult(
            overallStance = Stance.SUPPORTS,
            overallConfidence = 0.85f,
            excerptAnalyses = emptyList(),
            reasoning = "Test",
            chunksUsed = 1
        )
        
        val updated = state.withResult(newResult)
        
        assertEquals(1, updated.analyzedCount)
        assertEquals(1, updated.stances.size)
        assertEquals(Stance.SUPPORTS, updated.stances.first())
        assertEquals(1, updated.confidences.size)
        assertEquals(0.85f, updated.confidences.first())
    }
}
