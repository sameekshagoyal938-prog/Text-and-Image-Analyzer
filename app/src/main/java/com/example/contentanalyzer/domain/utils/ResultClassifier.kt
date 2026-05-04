package com.example.contentanalyzer.domain.utils

import kotlin.math.abs

/**
 * Fixed Result Classifier
 * Only makes definitive claims when confidence is sufficient
 */
class ResultClassifier {

    companion object {
        // Optimized thresholds based on real-world testing
        const val UNCERTAIN_THRESHOLD = 0.15f      // <15% margin → Uncertain
        const val AI_THRESHOLD = 0.20f             // AI > Human by 20% → AI
        const val HUMAN_THRESHOLD = 0.20f          // Human > AI by 20% → Human

        // Additional thresholds for nuanced classification
        const val STRONG_AI_THRESHOLD = 0.40f      // >40% margin → Strong AI
        const val STRONG_HUMAN_THRESHOLD = 0.40f   // >40% margin → Strong Human
    }

    sealed class ClassificationResult {
        data class Uncertain(
            val message: String,
            val recommendation: String,
            val margin: Float
        ) : ClassificationResult()

        data class AIGenerated(
            val strength: AIStrength,
            val confidence: Int,
            val message: String,
            val indicators: List<String>
        ) : ClassificationResult()

        data class HumanWritten(
            val strength: HumanStrength,
            val confidence: Int,
            val message: String,
            val indicators: List<String>
        ) : ClassificationResult()

        enum class AIStrength {
            WEAK,      // 20-40% margin
            MODERATE,  // 40-60% margin
            STRONG,    // 60-80% margin
            VERY_STRONG // >80% margin
        }

        enum class HumanStrength {
            WEAK,      // 20-40% margin
            MODERATE,  // 40-60% margin
            STRONG,    // 60-80% margin
            VERY_STRONG // >80% margin
        }
    }

    fun classify(
        aiScore: Float,
        humanScore: Float,
        confidenceResult: ConfidenceCalculator.ConfidenceResult
    ): ClassificationResult {
        val margin = confidenceResult.margin
        val difference = aiScore - humanScore

        // Case 1: Uncertain (scores too close)
        if (margin < UNCERTAIN_THRESHOLD) {
            return ClassificationResult.Uncertain(
                message = generateUncertainMessage(margin, aiScore, humanScore),
                recommendation = generateUncertainRecommendation(),
                margin = margin
            )
        }

        // Case 2: AI Generated
        if (difference > AI_THRESHOLD) {
            val strength = when {
                margin >= STRONG_AI_THRESHOLD -> ClassificationResult.AIStrength.VERY_STRONG
                margin >= 0.6f -> ClassificationResult.AIStrength.STRONG
                margin >= 0.4f -> ClassificationResult.AIStrength.MODERATE
                else -> ClassificationResult.AIStrength.WEAK
            }

            return ClassificationResult.AIGenerated(
                strength = strength,
                confidence = confidenceResult.confidencePercentage,
                message = generateAIMessage(strength, margin),
                indicators = generateAIIndicators(aiScore, humanScore, margin)
            )
        }

        // Case 3: Human Written
        if (-difference > HUMAN_THRESHOLD) {
            val strength = when {
                margin >= STRONG_HUMAN_THRESHOLD -> ClassificationResult.HumanStrength.VERY_STRONG
                margin >= 0.6f -> ClassificationResult.HumanStrength.STRONG
                margin >= 0.4f -> ClassificationResult.HumanStrength.MODERATE
                else -> ClassificationResult.HumanStrength.WEAK
            }

            return ClassificationResult.HumanWritten(
                strength = strength,
                confidence = confidenceResult.confidencePercentage,
                message = generateHumanMessage(strength, margin),
                indicators = generateHumanIndicators(aiScore, humanScore, margin)
            )
        }

        // Fallback - shouldn't reach here
        return ClassificationResult.Uncertain(
            message = "Results are inconclusive. Please provide more text.",
            recommendation = "Add more content for better analysis",
            margin = margin
        )
    }

    private fun generateUncertainMessage(margin: Float, ai: Float, human: Float): String {
        val aiPercent = (ai * 100).toInt()
        val humanPercent = (human * 100).toInt()
        val marginPercent = (margin * 100).toInt()

        return """
            ⚠️ UNCERTAIN RESULT
            
            AI: $aiPercent% | Human: $humanPercent%
            Difference: $marginPercent%
            
            The model cannot confidently determine if this text is AI-generated or human-written.
            The scores are too close (${marginPercent}% margin), indicating ambiguous content.
            
            This often happens with:
            • Very short text
            • Mixed AI and human writing
            • Highly creative or unusual content
        """.trimIndent()
    }

    private fun generateUncertainRecommendation(): String {
        return "For more accurate results, try:\n" +
                "• Adding more text (200+ words recommended)\n" +
                "• Providing clearer, more distinctive content\n" +
                "• Including personal anecdotes if human-written\n" +
                "• Reviewing the text for mixed patterns"
    }

    private fun generateAIMessage(strength: ClassificationResult.AIStrength, margin: Float): String {
        val marginPercent = (margin * 100).toInt()

        return when (strength) {
            ClassificationResult.AIStrength.WEAK ->
                "🤖 AI-Generated (Weak)\n\n" +
                        "AI probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Shows some AI patterns but not strongly conclusive."

            ClassificationResult.AIStrength.MODERATE ->
                "🤖 AI-Generated (Moderate)\n\n" +
                        "AI probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Clear AI patterns detected with moderate confidence."

            ClassificationResult.AIStrength.STRONG ->
                "🤖 AI-Generated (Strong)\n\n" +
                        "AI probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Strong AI patterns detected with high confidence."

            ClassificationResult.AIStrength.VERY_STRONG ->
                "🤖 AI-Generated (Very Strong)\n\n" +
                        "AI probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Overwhelming evidence of AI authorship."
        }
    }

    private fun generateHumanMessage(strength: ClassificationResult.HumanStrength, margin: Float): String {
        val marginPercent = (margin * 100).toInt()

        return when (strength) {
            ClassificationResult.HumanStrength.WEAK ->
                "👤 Human-Written (Weak)\n\n" +
                        "Human probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Shows some human patterns but not strongly conclusive."

            ClassificationResult.HumanStrength.MODERATE ->
                "👤 Human-Written (Moderate)\n\n" +
                        "Human probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Clear human patterns detected with moderate confidence."

            ClassificationResult.HumanStrength.STRONG ->
                "👤 Human-Written (Strong)\n\n" +
                        "Human probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Strong human patterns detected with high confidence."

            ClassificationResult.HumanStrength.VERY_STRONG ->
                "👤 Human-Written (Very Strong)\n\n" +
                        "Human probability: ${(marginPercent + 50).toInt()}%\n" +
                        "Overwhelming evidence of human authorship."
        }
    }

    private fun generateAIIndicators(ai: Float, human: Float, margin: Float): List<String> {
        val indicators = mutableListOf<String>()

        indicators.add("• AI probability exceeds human by ${(margin * 100).toInt()}%")

        if (ai > 0.7) {
            indicators.add("• Very high AI probability (${(ai * 100).toInt()}%)")
        }

        if (margin > 0.5) {
            indicators.add("• Strong, consistent AI patterns throughout")
        }

        return indicators
    }

    private fun generateHumanIndicators(ai: Float, human: Float, margin: Float): List<String> {
        val indicators = mutableListOf<String>()

        indicators.add("• Human probability exceeds AI by ${(margin * 100).toInt()}%")

        if (human > 0.7) {
            indicators.add("• Very high human probability (${(human * 100).toInt()}%)")
        }

        if (margin > 0.5) {
            indicators.add("• Strong, consistent human patterns throughout")
        }

        return indicators
    }
}