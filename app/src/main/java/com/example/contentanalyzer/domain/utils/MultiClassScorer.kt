package com.example.contentanalyzer.domain.utils

import android.util.Log
import com.example.contentanalyzer.domain.models.MultiClassAnalysisResult

object MultiClassScorer {
    private const val TAG = "MultiClassScorer"

    /**
     * Calculate scores for multi-class model output.
     */
    fun calculateMultiClassScores(
        aiProbability: Float,
        humanProbability: Float,
        fakeProbability: Float,
        text: String
    ): MultiClassAnalysisResult {

        // Validate that we have real data
        val total = aiProbability + humanProbability + fakeProbability

        if (total == 0f) {
            Log.e(TAG, "Invalid: All probabilities are zero")
            throw IllegalArgumentException("Model must provide valid probabilities for all classes")
        }

        // Normalize (ensure they sum to 1.0)
        val normalizedAI = aiProbability / total
        val normalizedHuman = humanProbability / total
        val normalizedFake = fakeProbability / total

        Log.d(TAG, """
            Multi-Class Results (from model):
            - AI: ${(normalizedAI * 100).toInt()}%
            - Human: ${(normalizedHuman * 100).toInt()}%
            - Fake/Manipulated: ${(normalizedFake * 100).toInt()}%
        """.trimIndent())

        // Confidence is the highest probability
        val confidenceScore = maxOf(normalizedAI, normalizedHuman, normalizedFake)

        val explanation = generateMultiClassExplanation(
            normalizedAI, normalizedHuman, normalizedFake, confidenceScore, text
        )

        return MultiClassAnalysisResult(
            aiPercentage = (normalizedAI * 100).toInt(),
            humanPercentage = (normalizedHuman * 100).toInt(),
            fakePercentage = (normalizedFake * 100).toInt(),
            confidenceScore = confidenceScore,
            explanation = explanation,
            dominantClass = getDominantClass(normalizedAI, normalizedHuman, normalizedFake)
        )
    }

    private fun generateMultiClassExplanation(
        ai: Float, human: Float, fake: Float, confidence: Float, text: String
    ): String {
        val classes = listOf(
            "AI-generated" to ai,
            "Human-written" to human,
            "Fake/Manipulated" to fake
        )
        val dominant = classes.maxByOrNull { it.second }!!

        return """
            Multi-Class Analysis Results:
            • Primary Classification: ${dominant.first} (${(dominant.second * 100).toInt()}%)
            • Confidence: ${(confidence * 100).toInt()}%
            
            Detailed Breakdown:
            ${classes.joinToString("\n") { "  - ${it.first}: ${(it.second * 100).toInt()}%" }}
            
            ${getMultiClassInsight(ai, human, fake, text)}
        """.trimIndent()
    }

    private fun getDominantClass(ai: Float, human: Float, fake: Float): String {
        return when (maxOf(ai, human, fake)) {
            ai -> "AI-generated"
            human -> "Human-written"
            else -> "Fake/Manipulated"
        }
    }

    private fun getMultiClassInsight(ai: Float, human: Float, fake: Float, text: String): String {
        return when {
            fake > 0.5 -> "High probability of manipulated or fake content detected."
            ai > 0.6 -> "Strong AI-generated patterns detected."
            human > 0.6 -> "Text shows natural human writing patterns."
            else -> "Mixed characteristics - text may require human review."
        }
    }
}
