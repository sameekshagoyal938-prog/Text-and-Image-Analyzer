package com.example.contentanalyzer.domain.utils

import android.util.Log
import kotlin.math.abs

object LabelMapper {

    private const val TAG = "LabelMapper"

    data class MappingResult(
        val isAI: Boolean,
        val probability: Float,
        val rawLabel: String,
        val rawScore: Float,
        val confidence: Float,
        val mappingStrategy: String
    )

    fun mapPrediction(
        label: String,
        score: Float,
        context: String = ""
    ): MappingResult {
        val normalizedLabel = label.lowercase().trim()

        return when {
            // Explicit AI indicators
            normalizedLabel in listOf("ai", "ai-generated", "machine-generated", "chatgpt", "gpt") -> {
                MappingResult(true, score, label, score, 0.95f, "explicit_ai_label")
            }
            // Explicit Human indicators
            normalizedLabel in listOf("human", "human-written", "person", "real") -> {
                MappingResult(false, 1f - score, label, score, 0.95f, "explicit_human_label")
            }
            // LABEL pattern (common in binary classifiers)
            normalizedLabel.matches(Regex("label_\\d+")) -> {
                val isAI = normalizedLabel == "label_1"
                val probability = if (isAI) score else 1f - score
                MappingResult(isAI, probability, label, score, 0.85f, "label_pattern")
            }
            // Zero-shot classification patterns
            normalizedLabel.contains("ai") && !normalizedLabel.contains("human") -> {
                MappingResult(true, score, label, score, 0.8f, "keyword_ai")
            }
            normalizedLabel.contains("human") -> {
                MappingResult(false, 1f - score, label, score, 0.8f, "keyword_human")
            }
            // Fallback - use score threshold
            else -> {
                val isAI = score > 0.6
                val probability = if (isAI) score else 1f - score
                val confidence = abs(score - 0.5f) * 2f
                MappingResult(isAI, probability, label, score, confidence, "fallback_threshold")
            }
        }.also {
            Log.d(TAG, "Mapped '$label'($score) -> AI=${it.isAI}, prob=${it.probability}, strategy=${it.mappingStrategy}")
        }
    }
}
