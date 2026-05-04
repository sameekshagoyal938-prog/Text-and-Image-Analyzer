package com.example.contentanalyzer.domain.utils

import android.util.Log

/**
 * AI Content Threshold Classifier
 * Converts raw model scores into meaningful categories with user-friendly messages
 */
class AIThresholdClassifier {

    companion object {
        private const val TAG = "AIThresholdClassifier"

        // Dynamic thresholds that adjust based on text characteristics
        private const val DEFAULT_AI_THRESHOLD = 0.75f
        private const val DEFAULT_HUMAN_THRESHOLD = 0.25f
        private const val UNCERTAINTY_ZONE_START = 0.40f
        private const val UNCERTAINTY_ZONE_END = 0.60f

        /**
         * Main classification function
         * Converts AI probability to meaningful category
         */
        fun classify(
            aiProbability: Float,
            text: String,
            confidenceScore: Float
        ): ClassificationResult {

            // Adjust thresholds based on text characteristics
            val adjustedThresholds = adjustThresholdsForText(text, confidenceScore)

            Log.d(TAG, """
                Classification Input:
                - AI Probability: ${(aiProbability * 100).toInt()}%
                - Confidence: ${(confidenceScore * 100).toInt()}%
                - Text Length: ${text.length} chars
                - Adjusted Thresholds: AI>${adjustedThresholds.aiThreshold}, Human<${adjustedThresholds.humanThreshold}
            """.trimIndent())

            // Determine category using when expression
            val category = when {
                // Highly Likely AI
                aiProbability >= adjustedThresholds.highlyLikelyAIThreshold -> {
                    AICategory.HIGHLY_LIKELY_AI
                }
                // Likely AI
                aiProbability >= adjustedThresholds.likelyAIThreshold -> {
                    AICategory.LIKELY_AI
                }
                // Possibly AI
                aiProbability >= adjustedThresholds.possiblyAIThreshold -> {
                    AICategory.POSSIBLY_AI
                }
                // Uncertain Zone
                aiProbability >= UNCERTAINTY_ZONE_START &&
                        aiProbability <= UNCERTAINTY_ZONE_END -> {
                    AICategory.UNCERTAIN
                }
                // Possibly Human
                aiProbability <= adjustedThresholds.possiblyHumanThreshold -> {
                    AICategory.POSSIBLY_HUMAN
                }
                // Likely Human
                aiProbability <= adjustedThresholds.likelyHumanThreshold -> {
                    AICategory.LIKELY_HUMAN
                }
                // Highly Likely Human
                aiProbability <= adjustedThresholds.highlyLikelyHumanThreshold -> {
                    AICategory.HIGHLY_LIKELY_HUMAN
                }
                // Fallback - should never reach here
                else -> {
                    Log.w(TAG, "Unclassified probability: $aiProbability")
                    AICategory.UNCERTAIN
                }
            }

            // Generate user-friendly message
            val message = generateMessage(category, aiProbability, text)
            val recommendation = generateRecommendation(category, text)
            val color = getCategoryColor(category)

            Log.d(TAG, "Classification Result: $category")

            return ClassificationResult(
                category = category,
                aiProbability = aiProbability,
                confidenceScore = confidenceScore,
                message = message,
                recommendation = recommendation,
                color = color
            )
        }

        /**
         * Dynamic threshold adjustment based on text characteristics
         */
        private fun adjustThresholdsForText(
            text: String,
            confidenceScore: Float
        ): ThresholdAdjustments {
            val wordCount = text.split(" ").filter { it.isNotBlank() }.size

            // Text length adjustment
            val lengthFactor = when {
                wordCount < 30 -> 0.15f  // Short text - make thresholds stricter (need higher score)
                wordCount < 100 -> 0.05f // Medium text - slight adjustment
                else -> 0.0f             // Long text - no adjustment
            }

            // Confidence score adjustment
            val confidenceFactor = when {
                confidenceScore < 0.4f -> 0.10f  // Low confidence - make thresholds stricter
                confidenceScore < 0.7f -> 0.05f  // Medium confidence - slight adjustment
                else -> 0.0f                      // High confidence - no adjustment
            }

            val totalAdjustment = lengthFactor + confidenceFactor

            return ThresholdAdjustments(
                highlyLikelyAIThreshold = 0.90f + totalAdjustment,
                likelyAIThreshold = 0.75f + totalAdjustment,
                possiblyAIThreshold = 0.60f + totalAdjustment,
                possiblyHumanThreshold = 0.40f - totalAdjustment,
                likelyHumanThreshold = 0.25f - totalAdjustment,
                highlyLikelyHumanThreshold = 0.10f - totalAdjustment,
                aiThreshold = DEFAULT_AI_THRESHOLD + totalAdjustment,
                humanThreshold = DEFAULT_HUMAN_THRESHOLD - totalAdjustment
            )
        }

        /**
         * Generate user-friendly message based on category
         */
        private fun generateMessage(
            category: AICategory,
            aiProbability: Float,
            text: String
        ): String {
            val percentage = (aiProbability * 100).toInt()
            val wordCount = text.split(" ").filter { it.isNotBlank() }.size

            return when (category) {
                AICategory.HIGHLY_LIKELY_AI ->
                    "🎯 Highly Likely AI-Generated ($percentage%)\n\n" +
                            "Strong AI patterns detected with high confidence. " +
                            "The text shows characteristic patterns of AI language models."

                AICategory.LIKELY_AI ->
                    "🤖 Likely AI-Generated ($percentage%)\n\n" +
                            "Clear AI indicators present. The text exhibits patterns " +
                            "commonly associated with AI writing."

                AICategory.POSSIBLY_AI ->
                    "⚡ Possibly AI-Generated ($percentage%)\n\n" +
                            "Some AI-like patterns detected, but not conclusive. " +
                            "Additional context could improve accuracy."

                AICategory.UNCERTAIN ->
                    "❓ Uncertain ($percentage%)\n\n" +
                            when {
                                wordCount < 50 -> "Text is too short for reliable analysis. " +
                                        "Please provide at least 100-200 words for better accuracy."
                                else -> "The model cannot confidently classify this text. " +
                                        "It may have mixed characteristics or be ambiguous."
                            }

                AICategory.POSSIBLY_HUMAN ->
                    "✍️ Possibly Human-Written ($percentage%)\n\n" +
                            "Shows some human-like patterns, but not definitive. " +
                            "Natural language variations suggest human authorship."

                AICategory.LIKELY_HUMAN ->
                    "👤 Likely Human-Written ($percentage%)\n\n" +
                            "Strong human indicators present. The text shows natural " +
                            "writing patterns and personal expression."

                AICategory.HIGHLY_LIKELY_HUMAN ->
                    "✅ Highly Likely Human-Written ($percentage%)\n\n" +
                            "Very strong human patterns detected with high confidence. " +
                            "Text exhibits natural variation and authentic voice."
            }
        }

        /**
         * Generate actionable recommendation
         */
        private fun generateRecommendation(
            category: AICategory,
            text: String
        ): String {
            val wordCount = text.split(" ").filter { it.isNotBlank() }.size

            return when (category) {
                AICategory.UNCERTAIN -> {
                    if (wordCount < 100) {
                        "💡 Add more text (aim for 200+ words) to improve classification accuracy."
                    } else {
                        "💡 The text may have mixed characteristics. Consider providing clearer context."
                    }
                }

                AICategory.POSSIBLY_AI -> {
                    "💡 Consider reviewing the content. " +
                            "If this is human-written, adding personal anecdotes may help."
                }

                AICategory.POSSIBLY_HUMAN -> {
                    "💡 Results show human-like patterns. For borderline cases, " +
                            "longer text typically yields clearer results."
                }

                AICategory.HIGHLY_LIKELY_AI -> {
                    "💡 If this is intended to be human-written, consider " +
                            "adding more personal voice, varied sentence structures, and specific examples."
                }

                else -> ""
            }
        }

        /**
         * Get color for UI display
         */
        fun getCategoryColor(category: AICategory): CategoryColor {
            return when (category) {
                AICategory.HIGHLY_LIKELY_AI -> CategoryColor(0xFFF44336, "Dark Red")
                AICategory.LIKELY_AI -> CategoryColor(0xFFFF5722, "Orange Red")
                AICategory.POSSIBLY_AI -> CategoryColor(0xFFFF9800, "Orange")
                AICategory.UNCERTAIN -> CategoryColor(0xFF9E9E9E, "Gray")
                AICategory.POSSIBLY_HUMAN -> CategoryColor(0xFF4CAF50, "Light Green")
                AICategory.LIKELY_HUMAN -> CategoryColor(0xFF8BC34A, "Green")
                AICategory.HIGHLY_LIKELY_HUMAN -> CategoryColor(0xFF00BCD4, "Teal")
            }
        }

        /**
         * Get detailed analysis for UI
         */
        fun getDetailedAnalysis(
            category: AICategory,
            aiProbability: Float,
            confidenceScore: Float,
            text: String
        ): DetailedAnalysis {
            val wordCount = text.split(" ").filter { it.isNotBlank() }.size
            val uniqueWords = text.split(" ").distinct().size
            val vocabularyRichness = if (wordCount > 0) uniqueWords.toFloat() / wordCount else 0f

            return DetailedAnalysis(
                category = category,
                aiPercentage = (aiProbability * 100).toInt(),
                confidencePercentage = (confidenceScore * 100).toInt(),
                textLength = wordCount,
                vocabularyRichness = vocabularyRichness,
                factors = listOf(
                    AnalysisFactor("Text Length", if (wordCount > 100) "Good" else "Too Short"),
                    AnalysisFactor("Vocabulary",
                        if (vocabularyRichness > 0.5) "Rich" else "Repetitive"),
                    AnalysisFactor("Confidence",
                        if (confidenceScore > 0.7) "High" else if (confidenceScore > 0.4) "Medium" else "Low")
                )
            )
        }
    }

    // Data classes for results
    sealed class AICategory {
        object HIGHLY_LIKELY_AI : AICategory()
        object LIKELY_AI : AICategory()
        object POSSIBLY_AI : AICategory()
        object UNCERTAIN : AICategory()
        object POSSIBLY_HUMAN : AICategory()
        object LIKELY_HUMAN : AICategory()
        object HIGHLY_LIKELY_HUMAN : AICategory()

        override fun toString(): String {
            return when (this) {
                HIGHLY_LIKELY_AI -> "Highly Likely AI"
                LIKELY_AI -> "Likely AI"
                POSSIBLY_AI -> "Possibly AI"
                UNCERTAIN -> "Uncertain"
                POSSIBLY_HUMAN -> "Possibly Human"
                LIKELY_HUMAN -> "Likely Human"
                HIGHLY_LIKELY_HUMAN -> "Highly Likely Human"
            }
        }
    }

    data class ClassificationResult(
        val category: AICategory,
        val aiProbability: Float,
        val confidenceScore: Float,
        val message: String,
        val recommendation: String,
        val color: CategoryColor
    )

    data class CategoryColor(
        val value: Long,
        val name: String
    )

    data class ThresholdAdjustments(
        val highlyLikelyAIThreshold: Float,
        val likelyAIThreshold: Float,
        val possiblyAIThreshold: Float,
        val possiblyHumanThreshold: Float,
        val likelyHumanThreshold: Float,
        val highlyLikelyHumanThreshold: Float,
        val aiThreshold: Float,
        val humanThreshold: Float
    )

    data class DetailedAnalysis(
        val category: AICategory,
        val aiPercentage: Int,
        val confidencePercentage: Int,
        val textLength: Int,
        val vocabularyRichness: Float,
        val factors: List<AnalysisFactor>
    )

    data class AnalysisFactor(
        val name: String,
        val value: String
    )
}
