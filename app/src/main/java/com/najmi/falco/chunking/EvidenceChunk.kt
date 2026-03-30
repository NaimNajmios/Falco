package com.najmi.falco.chunking

enum class ChunkSource {
    ABSTRACT,
    CONCLUSION,
    INTRODUCTION,
    METHODS,
    RESULTS,
    DISCUSSION,
    BODY_PARAGRAPH
}

data class EvidenceChunk(
    val id: Int,
    val priority: Int,
    val label: String,
    val content: String,
    val estimatedTokens: Int,
    val sourceSection: ChunkSource,
    val sourceParagraph: Int? = null
) {
    companion object {
        const val MAX_CHUNKS_PER_PAPER = 3
        const val MAX_TOKENS_PER_CHUNK = 2000
        const val MAX_TOKENS_PER_PAPER = 3000
        const val ABSTRACT_MAX_TOKENS = 500
        const val CONCLUSION_MAX_TOKENS = 800
        const val BODY_PARAGRAPH_MAX_TOKENS = 600
    }
}

data class ChunkValidation(
    val totalChunks: Int,
    val totalTokens: Int,
    val isValid: Boolean,
    val errors: List<String>
) {
    companion object {
        fun validate(chunks: List<EvidenceChunk>): ChunkValidation {
            val errors = mutableListOf<String>()
            
            if (chunks.size > EvidenceChunk.MAX_CHUNKS_PER_PAPER) {
                errors.add("Too many chunks: ${chunks.size} > ${EvidenceChunk.MAX_CHUNKS_PER_PAPER}")
            }
            
            val totalTokens = chunks.sumOf { it.estimatedTokens }
            if (totalTokens > EvidenceChunk.MAX_TOKENS_PER_PAPER) {
                errors.add("Total tokens exceeded: $totalTokens > ${EvidenceChunk.MAX_TOKENS_PER_PAPER}")
            }
            
            chunks.forEach { chunk ->
                if (chunk.estimatedTokens > EvidenceChunk.MAX_TOKENS_PER_CHUNK) {
                    errors.add("Chunk ${chunk.id} exceeds max tokens: ${chunk.estimatedTokens} > ${EvidenceChunk.MAX_TOKENS_PER_CHUNK}")
                }
            }
            
            return ChunkValidation(
                totalChunks = chunks.size,
                totalTokens = totalTokens,
                isValid = errors.isEmpty(),
                errors = errors
            )
        }
    }
}
