package com.example.contentanalyzer.domain.analyzers

import android.util.Log
import com.example.contentanalyzer.domain.models.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * Text Analyzer - Detects AI-generated vs Human-written text
 */
class TextAnalyzer {

    companion object {
        private const val TAG = "TextAnalyzer"
        private const val AI_REPETITION_THRESHOLD = 0.35f
        private const val AI_PERPLEXITY_THRESHOLD = 0.45f
        private const val AI_STRUCTURE_THRESHOLD = 0.40f
    }

    data class TextFeatures(
        val repetitiveness: Float,
        val burstiness: Float,
        val vocabularyRichness: Float
    )

    data class TextMetrics(
        val wordCount: Int,
        val sentenceCount: Int,
        val avgWordLength: Float,
        val repetitionScore: Float,
        val perplexityScore: Float,
        val structureScore: Float,
        val uniqueWordRatio: Float
    )

    /**
     * Legacy analyze method for AnalyzerRepository
     */
    fun analyze(text: String): TextFeatures {
        val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return TextFeatures(0f, 0f, 1f)
        
        val uniqueWordsRatio = words.distinct().size.toFloat() / words.size
        val repetitiveness = 1f - uniqueWordsRatio
        
        val sentences = text.split(Regex("[.!?]")).filter { it.isNotBlank() }
        val sentenceLengths = sentences.map { it.split(Regex("\\s+")).filter { s -> s.isNotBlank() }.size }
        
        val burstiness = if (sentenceLengths.size > 1) {
            val mean = sentenceLengths.average()
            val stdDev = sqrt(sentenceLengths.map { (it - mean).pow(2.0) }.average())
            (stdDev / (mean + 1.0)).toFloat().coerceIn(0f, 1f)
        } else 0f

        return TextFeatures(repetitiveness, burstiness, uniqueWordsRatio)
    }

    suspend fun analyzeText(inputText: String): AnalysisResult {
        return withContext(Dispatchers.Default) {
            if (inputText.isBlank()) {
                return@withContext AnalysisResult(
                    type = AnalysisResult.AnalysisType.TEXT,
                    classification = "INCONCLUSIVE",
                    confidence = 0,
                    details = "No text provided"
                )
            }

            val metrics = extractTextMetrics(inputText)
            val aiProbability = calculateAIProbability(metrics)

            val (classification, confidence) = when {
                aiProbability >= 0.65 -> Pair("AI GENERATED", (65 + (aiProbability - 0.65) * 100).toInt().coerceIn(65, 95))
                aiProbability <= 0.35 -> Pair("HUMAN WRITTEN", (65 + (0.35 - aiProbability) * 100).toInt().coerceIn(65, 95))
                else -> Pair("INCONCLUSIVE", (50 + (aiProbability - 0.35) / 0.3 * 20).toInt().coerceIn(50, 70))
            }

            AnalysisResult(
                type = AnalysisResult.AnalysisType.TEXT,
                classification = classification,
                confidence = confidence,
                details = generateExplanation(metrics, aiProbability, classification),
                rawScore = aiProbability.toFloat()
            )
        }
    }

    private fun extractTextMetrics(text: String): TextMetrics {
        val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = text.split(Regex("[.!?]")).filter { it.isNotBlank() }
        
        val wordCount = words.size
        val sentenceCount = sentences.size
        val avgWordLength = if (wordCount > 0) words.map { it.length }.average().toFloat() else 0f

        val uniqueWordRatio = if (wordCount > 0) words.distinct().size.toFloat() / wordCount else 0f
        val repetitionScore = 1f - uniqueWordRatio

        val wordLengths = words.map { it.length }
        val wordLengthStd = if (wordLengths.isNotEmpty()) {
            val mean = wordLengths.average()
            sqrt(wordLengths.map { (it.toDouble() - mean).pow(2.0) }.average())
        } else 0.0
        val perplexityScore = (wordLengthStd / 5.0).toFloat().coerceIn(0f, 1f)

        val sentenceLengths = sentences.map { it.split(" ").size }
        val sentenceLengthStd = if (sentenceLengths.isNotEmpty()) {
            val mean = sentenceLengths.average()
            sqrt(sentenceLengths.map { (it.toDouble() - mean).pow(2.0) }.average())
        } else 0.0
        val structureScore = (sentenceLengthStd / 15.0).toFloat().coerceIn(0f, 1f)

        return TextMetrics(wordCount, sentenceCount, avgWordLength, repetitionScore, 1f - perplexityScore, structureScore, uniqueWordRatio)
    }

    private fun calculateAIProbability(metrics: TextMetrics): Double {
        var score = 0.5
        if (metrics.repetitionScore > AI_REPETITION_THRESHOLD) score += 0.15
        if (metrics.perplexityScore < 0.4) score += 0.2
        if (metrics.structureScore > AI_STRUCTURE_THRESHOLD) score += 0.1
        if (metrics.uniqueWordRatio > 0.6) score -= 0.15
        return score.coerceIn(0.0, 1.0)
    }

    private fun generateExplanation(metrics: TextMetrics, prob: Double, classification: String): String {
        return "Text analysis suggests $classification with ${(prob * 100).toInt()}% AI probability. " +
               "Metrics: Repetition=${(metrics.repetitionScore*100).toInt()}%, Richness=${(metrics.uniqueWordRatio*100).toInt()}%."
    }
}
