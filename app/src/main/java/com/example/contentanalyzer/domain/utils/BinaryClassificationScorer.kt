package com.example.contentanalyzer.domain.utils

import android.util.Log

class BinaryClassificationScorer {

    companion object {
        private const val TAG = "BinaryScorer"

        fun calculateBinaryScores(
            aiProbability: Float,
            text: String
        ): BinaryAnalysisResult {

            val humanProbability = 1f - aiProbability

            // Calculate calibrated confidence
            val probabilities = mapOf(
                "AI" to aiProbability,
                "Human" to humanProbability
            )

            val confidenceResult = ConfidenceCalculator.calculateConfidence(
                probabilities = probabilities,
                text = text
            )

            // Apply threshold classification
            val classification = AIThresholdClassifier.classify(
                aiProbability = aiProbability,
                text = text,
                confidenceScore = confidenceResult.confidence
            )

            // Get detailed analysis
            val detailedAnalysis = AIThresholdClassifier.getDetailedAnalysis(
                category = classification.category,
                aiProbability = aiProbability,
                confidenceScore = confidenceResult.confidence,
                text = text
            )

            Log.d(TAG, """
                Threshold Classification:
                - Category: ${classification.category}
                - Message: ${classification.message}
                - Recommendation: ${classification.recommendation}
                - Text Length: ${detailedAnalysis.textLength} words
                - Vocab Richness: ${(detailedAnalysis.vocabularyRichness * 100).toInt()}%
            """.trimIndent())

            // Generate final explanation
            val explanation = buildString {
                appendLine(classification.message)
                appendLine()
                appendLine("📊 Detailed Analysis:")
                appendLine("• AI Probability: ${(aiProbability * 100).toInt()}%")
                appendLine("• Model Confidence: ${(confidenceResult.confidence * 100).toInt()}%")
                appendLine("• Text Length: ${detailedAnalysis.textLength} words")
                appendLine("• Vocabulary Diversity: ${(detailedAnalysis.vocabularyRichness * 100).toInt()}%")
                appendLine()
                detailedAnalysis.factors.forEach { factor ->
                    appendLine("• ${factor.name}: ${factor.value}")
                }
                if (classification.recommendation.isNotEmpty()) {
                    appendLine()
                    appendLine(classification.recommendation)
                }
            }

            return BinaryAnalysisResult(
                aiPercentage = (aiProbability * 100).toInt(),
                humanPercentage = (humanProbability * 100).toInt(),
                confidenceScore = confidenceResult.confidence,
                explanation = explanation,
                modelCertainty = confidenceResult.rawProbability,
                confidenceLevel = ConfidenceCalculator.getConfidenceLevel(confidenceResult.confidence),
                category = classification.category,
                categoryMessage = classification.message,
                recommendation = classification.recommendation
            )
        }
    }
}

// Update BinaryAnalysisResult
data class BinaryAnalysisResult(
    val aiPercentage: Int,
    val humanPercentage: Int,
    val confidenceScore: Float,
    val explanation: String,
    val modelCertainty: Float,
    val confidenceLevel: ConfidenceCalculator.ConfidenceLevel,
    val category: AIThresholdClassifier.AICategory,
    val categoryMessage: String,
    val recommendation: String
)
