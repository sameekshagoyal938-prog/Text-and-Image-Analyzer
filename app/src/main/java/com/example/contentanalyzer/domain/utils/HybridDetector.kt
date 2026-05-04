package com.example.contentanalyzer.domain.utils

import android.util.Log
import kotlin.math.*

/**
 * Hybrid Detection System combining multiple signals
 * - Model-based predictions
 * - Text pattern analysis
 * - Style and structure analysis
 */
class HybridDetector {

    companion object {
        private const val TAG = "HybridDetector"

        // Signal weights (can be tuned based on performance)
        private const val WEIGHT_MODEL = 0.55f      // Model predictions (primary)
        private const val WEIGHT_PATTERN = 0.25f    // Text patterns
        private const val WEIGHT_STYLE = 0.20f      // Style analysis

        // Confidence thresholds for signal weighting
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.5f
    }

    /**
     * Main hybrid detection method
     * Combines multiple signals for robust prediction
     */
    fun detectHybrid(
        modelProbability: Float,
        text: String,
        modelConfidence: Float
    ): HybridDetectionResult {

        Log.d(TAG, "Starting hybrid detection")

        // Signal 1: Model-based prediction
        val modelSignal = extractModelSignal(modelProbability, modelConfidence)

        // Signal 2: Text pattern analysis
        val patternSignal = extractPatternSignal(text)

        // Signal 3: Style and structure analysis
        val styleSignal = extractStyleSignal(text)

        Log.d(TAG, """
            Individual Signals:
            - Model Signal: ${modelSignal.aiProbability} (weight: ${modelSignal.weight})
            - Pattern Signal: ${patternSignal.aiProbability} (weight: ${patternSignal.weight})
            - Style Signal: ${styleSignal.aiProbability} (weight: ${styleSignal.weight})
        """.trimIndent())

        // Calculate dynamic weights based on signal quality
        val weights = calculateDynamicWeights(
            modelSignal, patternSignal, styleSignal, text
        )

        // Combine signals using weighted average
        val combinedAIProbability = (
                modelSignal.aiProbability * weights.model +
                        patternSignal.aiProbability * weights.pattern +
                        styleSignal.aiProbability * weights.style
                )

        // Calculate overall confidence
        val overallConfidence = calculateOverallConfidence(
            combinedAIProbability,
            modelSignal,
            patternSignal,
            styleSignal,
            weights
        )

        // Generate detailed explanation
        val explanation = generateHybridExplanation(
            combinedAIProbability, modelSignal, patternSignal, styleSignal, weights, text
        )

        Log.d(TAG, """
            Hybrid Result:
            - Combined AI Probability: ${(combinedAIProbability * 100).toInt()}%
            - Overall Confidence: ${(overallConfidence * 100).toInt()}%
        """.trimIndent())

        return HybridDetectionResult(
            aiProbability = combinedAIProbability,
            confidenceScore = overallConfidence,
            modelSignal = modelSignal,
            patternSignal = patternSignal,
            styleSignal = styleSignal,
            weightsUsed = weights,
            explanation = explanation
        )
    }

    /**
     * Signal 1: Extract from Model Prediction
     */
    private fun extractModelSignal(
        probability: Float,
        confidence: Float
    ): SignalResult {
        // Adjust model signal based on its confidence
        val adjustedProbability = when {
            confidence > HIGH_CONFIDENCE_THRESHOLD -> probability
            confidence > LOW_CONFIDENCE_THRESHOLD -> probability * 0.9f + 0.5f * 0.1f
            else -> probability * 0.7f + 0.5f * 0.3f  // Regress toward 0.5
        }

        val weight = confidence.coerceIn(0.3f, 0.9f)

        return SignalResult(
            aiProbability = adjustedProbability.coerceIn(0f, 1f),
            confidence = confidence,
            weight = weight,
            details = mapOf(
                "raw_probability" to probability,
                "adjusted_probability" to adjustedProbability,
                "model_confidence" to confidence
            )
        )
    }

    /**
     * Signal 2: Text Pattern Analysis
     * Detects AI patterns like repetition, uniformity, etc.
     */
    private fun extractPatternSignal(text: String): SignalResult {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = text.split(Regex("[.!?]")).filter { it.isNotBlank() }

        if (words.isEmpty() || sentences.isEmpty()) {
            return SignalResult(
                aiProbability = 0.5f,
                confidence = 0.3f,
                weight = 0.2f,
                details = mapOf("error" to "Insufficient text")
            )
        }

        // Feature 1: Repetitiveness (AI often uses repetitive patterns)
        val uniqueWords = words.distinct()
        val repetitiveness = 1f - (uniqueWords.size.toFloat() / words.size.toFloat())

        // Feature 2: Sentence length variation (AI has less variation)
        val sentenceLengths = sentences.map { it.split(" ").size }
        val meanLength = sentenceLengths.average().toFloat()
        val variance = if (sentenceLengths.size > 1) {
            sentenceLengths.map { (it - meanLength).pow(2) }.average().toFloat()
        } else 0f
        val burstiness = (sqrt(variance) / (meanLength + 1)).coerceIn(0f, 1f)

        // Feature 3: Vocabulary richness (AI has lower richness)
        val vocabularyRichness = uniqueWords.size.toFloat() / words.size.toFloat()

        // Feature 4: Average word length (AI often uses longer, formal words)
        val avgWordLength = words.map { it.length }.average().toFloat()
        val wordLengthScore = ((avgWordLength - 3) / 5).coerceIn(0f, 1f)

        // Combine features into AI probability
        var aiProbability = (
                repetitiveness * 0.35f +           // High repetition = AI
                        (1f - burstiness) * 0.25f +        // Low variation = AI
                        (1f - vocabularyRichness) * 0.25f + // Low richness = AI
                        wordLengthScore * 0.15f             // Long words = AI
                ).coerceIn(0f, 1f)

        // Confidence based on text length and feature consistency
        val confidence = when {
            words.size < 50 -> 0.4f  // Short text = low confidence
            words.size < 100 -> 0.6f
            else -> 0.8f
        }

        // Adjust weight based on text length
        val weight = when {
            words.size < 50 -> 0.15f
            words.size < 100 -> 0.25f
            else -> 0.35f
        }

        return SignalResult(
            aiProbability = aiProbability,
            confidence = confidence,
            weight = weight,
            details = mapOf(
                "repetitiveness" to repetitiveness,
                "burstiness" to burstiness,
                "vocabulary_richness" to vocabularyRichness,
                "avg_word_length" to avgWordLength,
                "word_count" to words.size
            )
        )
    }

    /**
     * Signal 3: Style and Structure Analysis
     * Detects AI patterns in writing style
     */
    private fun extractStyleSignal(text: String): SignalResult {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = text.split(Regex("[.!?]")).filter { it.isNotBlank() }

        if (words.isEmpty() || sentences.isEmpty()) {
            return SignalResult(
                aiProbability = 0.5f,
                confidence = 0.3f,
                weight = 0.15f,
                details = mapOf("error" to "Insufficient text")
            )
        }

        // Feature 1: Personal pronoun usage (human indicator)
        val personalPronouns = listOf("i", "me", "my", "mine", "we", "us", "our")
        val pronounCount = words.count { it.lowercase() in personalPronouns }
        val pronounRatio = (pronounCount.toFloat() / words.size).coerceIn(0f, 0.2f)
        val humanPronounScore = pronounRatio * 5f  // Scale to 0-1

        // Feature 2: Question/Exclamation usage (human indicator)
        val questionCount = text.count { it == '?' }
        val exclamationCount = text.count { it == '!' }
        val punctuationScore = ((questionCount + exclamationCount) / 10f).coerceIn(0f, 1f)

        // Feature 3: Transition words (AI uses more transitions)
        val transitionWords = listOf(
            "however", "therefore", "furthermore", "consequently",
            "additionally", "nevertheless", "nonetheless"
        )
        val transitionCount = words.count { it.lowercase() in transitionWords }
        val transitionScore = (transitionCount.toFloat() / words.size * 20f).coerceIn(0f, 1f)

        // Feature 4: Sentence start variety (AI has less variety)
        val sentenceStarts = sentences.mapNotNull { it.trim().firstOrNull()?.toString() }
        val uniqueStarts = sentenceStarts.distinct()
        val startVariety = uniqueStarts.size.toFloat() / (sentenceStarts.size + 1)
        val varietyScore = (1f - startVariety).coerceIn(0f, 1f)

        // Combine into AI probability
        var aiProbability = (
                (1f - humanPronounScore) * 0.35f +    // Low pronouns = AI
                        (1f - punctuationScore) * 0.15f +     // Low punctuation = AI
                        transitionScore * 0.25f +              // High transitions = AI
                        varietyScore * 0.25f                   // Low variety = AI
                ).coerceIn(0f, 1f)

        // Confidence based on consistency
        val confidence = when {
            words.size < 50 -> 0.35f
            words.size < 100 -> 0.55f
            else -> 0.75f
        }

        val weight = when {
            words.size < 50 -> 0.12f
            words.size < 100 -> 0.20f
            else -> 0.30f
        }

        return SignalResult(
            aiProbability = aiProbability,
            confidence = confidence,
            weight = weight,
            details = mapOf(
                "pronoun_ratio" to pronounRatio,
                "punctuation_score" to punctuationScore,
                "transition_score" to transitionScore,
                "start_variety" to startVariety,
                "sentence_count" to sentences.size
            )
        )
    }

    /**
     * Calculate dynamic weights based on signal quality
     */
    private fun calculateDynamicWeights(
        modelSignal: SignalResult,
        patternSignal: SignalResult,
        styleSignal: SignalResult,
        text: String
    ): HybridWeights {
        val wordCount = text.split(" ").filter { it.isNotBlank() }.size

        // Base weights
        var modelWeight = WEIGHT_MODEL
        var patternWeight = WEIGHT_PATTERN
        var styleWeight = WEIGHT_STYLE

        // Adjust based on text length
        if (wordCount < 50) {
            // Short text: rely less on patterns, more on model
            modelWeight += 0.15f
            patternWeight -= 0.10f
            styleWeight -= 0.05f
        } else if (wordCount > 300) {
            // Long text: patterns become more reliable
            modelWeight -= 0.05f
            patternWeight += 0.05f
        }

        // Adjust based on signal confidence
        modelWeight *= modelSignal.confidence
        patternWeight *= patternSignal.confidence
        styleWeight *= styleSignal.confidence

        // Normalize to sum to 1
        val total = modelWeight + patternWeight + styleWeight
        return HybridWeights(
            model = (modelWeight / total).coerceIn(0.3f, 0.8f),
            pattern = (patternWeight / total).coerceIn(0.1f, 0.4f),
            style = (styleWeight / total).coerceIn(0.1f, 0.3f)
        )
    }

    /**
     * Calculate overall confidence from all signals
     */
    private fun calculateOverallConfidence(
        combinedProbability: Float,
        modelSignal: SignalResult,
        patternSignal: SignalResult,
        styleSignal: SignalResult,
        weights: HybridWeights
    ): Float {
        // Agreement between signals
        val signals = listOf(
            modelSignal.aiProbability,
            patternSignal.aiProbability,
            styleSignal.aiProbability
        )
        val mean = signals.average().toFloat()
        val variance = signals.map { (it - mean).pow(2) }.average().toFloat()
        val agreement = (1f - variance).coerceIn(0f, 1f)

        // Distance from 0.5 (certainty)
        val certainty = abs(combinedProbability - 0.5f) * 2f

        // Weighted signal confidences
        val weightedConfidence = (
                modelSignal.confidence * weights.model +
                        patternSignal.confidence * weights.pattern +
                        styleSignal.confidence * weights.style
                )

        // Combine factors
        return (agreement * 0.4f + certainty * 0.3f + weightedConfidence * 0.3f)
            .coerceIn(0.3f, 0.95f)
    }

    /**
     * Generate detailed explanation of hybrid detection
     */
    private fun generateHybridExplanation(
        combinedAIProbability: Float,
        modelSignal: SignalResult,
        patternSignal: SignalResult,
        styleSignal: SignalResult,
        weights: HybridWeights,
        text: String
    ): String {
        val wordCount = text.split(" ").filter { it.isNotBlank() }.size

        return buildString {
            appendLine("📊 Hybrid Analysis Results")
            appendLine()
            appendLine("Multi-Signal Detection:")
            appendLine()
            appendLine("1. 🤖 Model-Based Analysis (${(weights.model * 100).toInt()}% weight)")
            appendLine("   • AI Probability: ${(modelSignal.aiProbability * 100).toInt()}%")
            appendLine("   • Model Confidence: ${(modelSignal.confidence * 100).toInt()}%")
            appendLine()
            appendLine("2. 📝 Pattern Analysis (${(weights.pattern * 100).toInt()}% weight)")
            appendLine("   • AI Probability: ${(patternSignal.aiProbability * 100).toInt()}%")
            appendLine("   • Repetitiveness: ${((patternSignal.details["repetitiveness"] as? Float ?: 0f) * 100).toInt()}%")
            appendLine("   • Vocabulary Richness: ${((patternSignal.details["vocabulary_richness"] as? Float ?: 0f) * 100).toInt()}%")
            appendLine()
            appendLine("3. 🎨 Style Analysis (${(weights.style * 100).toInt()}% weight)")
            appendLine("   • AI Probability: ${(styleSignal.aiProbability * 100).toInt()}%")
            appendLine("   • Personal Pronouns: ${((styleSignal.details["pronoun_ratio"] as? Float ?: 0f) * 100).toInt()}%")
            appendLine()
            appendLine("🔍 Combined Result:")
            appendLine("• Final AI Probability: ${(combinedAIProbability * 100).toInt()}%")
            appendLine()
            appendLine(getSignalAgreementAnalysis(modelSignal, patternSignal, styleSignal))

            if (wordCount < 100) {
                appendLine()
                appendLine("⚠️ Note: Text length (${wordCount} words) affects signal reliability.")
                appendLine("Longer text improves pattern and style analysis.")
            }
        }
    }

    private fun getSignalAgreementAnalysis(
        model: SignalResult,
        pattern: SignalResult,
        style: SignalResult
    ): String {
        val signals = listOf(model.aiProbability, pattern.aiProbability, style.aiProbability)
        val maxDiff = signals.maxOrNull()!! - signals.minOrNull()!!

        return when {
            maxDiff < 0.15 -> "✓ All signals strongly agree on classification"
            maxDiff < 0.30 -> "⚠️ Signals show moderate agreement"
            else -> "⚠️ Signals disagree - results based on weighted consensus"
        }
    }

    data class SignalResult(
        val aiProbability: Float,
        val confidence: Float,
        val weight: Float,
        val details: Map<String, Any>
    )

    data class HybridWeights(
        val model: Float,
        val pattern: Float,
        val style: Float
    )

    data class HybridDetectionResult(
        val aiProbability: Float,
        val confidenceScore: Float,
        val modelSignal: SignalResult,
        val patternSignal: SignalResult,
        val styleSignal: SignalResult,
        val weightsUsed: HybridWeights,
        val explanation: String
    )
}
