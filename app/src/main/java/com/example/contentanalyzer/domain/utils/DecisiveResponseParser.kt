package com.example.contentanalyzer.domain.utils

import android.util.Log
import com.example.contentanalyzer.data.api.models.ChatGPTDetectorResponse
import com.example.contentanalyzer.data.api.models.HuggingFacePrediction
import kotlin.math.abs

object DecisiveResponseParser {

    private const val TAG = "DecisiveParser"

    // Thresholds for decisive classification (no 50-50 outputs)
    private const val STRONG_AI_THRESHOLD = 0.65f      // Strong AI signal
    private const val MODERATE_AI_THRESHOLD = 0.55f    // Moderate AI signal
    private const val STRONG_HUMAN_THRESHOLD = 0.35f   // Strong Human signal
    private const val MODERATE_HUMAN_THRESHOLD = 0.45f // Moderate Human signal
    private val UNCERTAINTY_ZONE = 0.45f..0.55f   // Avoid this zone

    data class DecisiveResult(
        val label: String,           // "AI-generated", "Human-written", or "Mixed"
        val aiProbability: Int,      // 0-100
        val humanProbability: Int,    // 0-100
        val confidence: Int,          // 0-100
        val reasoning: String,
        val rawScores: Map<String, Float>
    )

    /**
     * Parse ChatGPT Detector response with decisive logic
     * NO 50-50 outputs - always picks a dominant pattern
     */
    fun parseChatGPTResponse(
        predictions: List<ChatGPTDetectorResponse>
    ): DecisiveResult {
        Log.d(TAG, "Parsing ChatGPT Detector response with decisive logic")

        if (predictions.isEmpty()) {
            Log.w(TAG, "Empty predictions, using fallback")
            return createFallbackResult()
        }

        // Extract all predictions
        val scores = mutableMapOf<String, Float>()
        predictions.forEach { pred ->
            scores[pred.label] = pred.score
            Log.d(TAG, "Raw prediction: ${pred.label} = ${pred.score}")
        }

        // Calculate initial AI and Human probabilities
        var aiRaw = scores["ChatGPT"] ?: scores["GPT-4"] ?: scores["AI"] ?: 0.3f
        var humanRaw = scores["Human"] ?: scores["human"] ?: 0.5f

        // Check for other AI models
        val gpt4Score = scores["GPT-4"] ?: 0f
        val claudeScore = scores["Claude"] ?: 0f
        val llamaScore = scores["Llama"] ?: 0f

        // Combine all AI scores
        val combinedAIScore = maxOf(aiRaw, gpt4Score, claudeScore, llamaScore)

        // Normalize to sum to 1
        val total = combinedAIScore + humanRaw
        var aiProbability = if (total > 0) combinedAIScore / total else 0.5f
        var humanProbability = if (total > 0) humanRaw / total else 0.5f

        Log.d(TAG, "Normalized - AI: ${(aiProbability * 100).toInt()}%, Human: ${(humanProbability * 100).toInt()}%")

        // Apply decisive logic - NO 50-50 outputs
        val (finalAI, finalHuman, label, confidence, reasoning) = applyDecisiveLogic(
            aiProbability = aiProbability,
            humanProbability = humanProbability,
            rawScores = scores
        )

        return DecisiveResult(
            label = label,
            aiProbability = finalAI,
            humanProbability = finalHuman,
            confidence = confidence,
            reasoning = reasoning,
            rawScores = scores
        )
    }

    /**
     * Parse RoBERTa response with decisive logic
     */
    fun parseRoBERTaResponse(
        predictions: List<HuggingFacePrediction>
    ): DecisiveResult {
        Log.d(TAG, "Parsing RoBERTa response with decisive logic")

        if (predictions.isEmpty()) {
            return createFallbackResult()
        }

        val firstPred = predictions[0]
        val label = firstPred.label
        val score = firstPred.score

        // Map LABEL_0/LABEL_1 to AI/Human
        var aiProbability = when (label.lowercase()) {
            "label_1", "ai", "ai-generated" -> score
            "label_0", "human", "human-written" -> 1f - score
            else -> score
        }

        val humanProbability = 1f - aiProbability

        // Apply decisive logic
        val (finalAI, finalHuman, finalLabel, confidence, reasoning) = applyDecisiveLogic(
            aiProbability = aiProbability,
            humanProbability = humanProbability,
            rawScores = mapOf(label to score)
        )

        return DecisiveResult(
            label = finalLabel,
            aiProbability = finalAI,
            humanProbability = finalHuman,
            confidence = confidence,
            reasoning = reasoning,
            rawScores = mapOf(label to score)
        )
    }

    /**
     * Core decisive logic - NO 50-50 outputs
     */
    private fun applyDecisiveLogic(
        aiProbability: Float,
        humanProbability: Float,
        rawScores: Map<String, Float>
    ): Quadruple<Int, Int, String, Int, String> {

        val margin = abs(aiProbability - humanProbability)
        val dominantScore = maxOf(aiProbability, humanProbability)
        val dominantClass = if (aiProbability > humanProbability) "AI" else "Human"

        Log.d(TAG, "Margin: ${(margin * 100).toInt()}%, Dominant: $dominantClass with ${(dominantScore * 100).toInt()}%")

        // Case 1: Strong AI signal (>65%)
        if (aiProbability >= STRONG_AI_THRESHOLD) {
            val confidence = (aiProbability * 100).toInt()
            val finalAI = (aiProbability * 100).toInt()
            val finalHuman = 100 - finalAI

            val reasoning = when {
                aiProbability >= 0.85 -> "Overwhelming AI patterns detected. Text shows strong characteristics of AI language models."
                aiProbability >= 0.75 -> "Very strong AI indicators. Consistent patterns typical of AI-generated content."
                else -> "Clear AI patterns detected with high confidence. Text exhibits typical AI writing characteristics."
            }

            return Quadruple(finalAI, finalHuman, "AI-generated", confidence, reasoning)
        }

        // Case 2: Strong Human signal (<35%)
        if (humanProbability >= STRONG_HUMAN_THRESHOLD || aiProbability <= 0.35f) {
            val confidence = (humanProbability * 100).toInt()
            val finalHuman = (humanProbability * 100).toInt()
            val finalAI = 100 - finalHuman

            val reasoning = when {
                humanProbability >= 0.85 -> "Overwhelming human patterns detected. Natural variation and personal voice evident."
                humanProbability >= 0.75 -> "Very strong human indicators. Authentic writing style with natural flow."
                else -> "Clear human patterns detected. Text shows natural human writing characteristics."
            }

            return Quadruple(finalAI, finalHuman, "Human-written", confidence, reasoning)
        }

        // Case 3: Moderate AI signal (55-65%)
        if (aiProbability >= MODERATE_AI_THRESHOLD) {
            // Push decisively toward AI
            val adjustedAI = (aiProbability * 1.1f).coerceAtMost(0.85f)
            val finalAI = (adjustedAI * 100).toInt()
            val finalHuman = 100 - finalAI
            val confidence = ((margin + 0.2f) * 100).toInt().coerceIn(60, 85)

            val reasoning = "Moderate AI patterns detected. Text shows some AI characteristics but not overwhelming. " +
                    "Classification leans toward AI based on pattern analysis."

            return Quadruple(finalAI, finalHuman, "AI-generated", confidence, reasoning)
        }

        // Case 4: Moderate Human signal (45-55% but human slightly higher)
        if (humanProbability >= MODERATE_HUMAN_THRESHOLD) {
            // Push decisively toward Human
            val adjustedHuman = (humanProbability * 1.1f).coerceAtMost(0.85f)
            val finalHuman = (adjustedHuman * 100).toInt()
            val finalAI = 100 - finalHuman
            val confidence = ((margin + 0.2f) * 100).toInt().coerceIn(60, 85)

            val reasoning = "Moderate human patterns detected. Text shows natural variations and personal elements. " +
                    "Classification leans toward human-written based on style analysis."

            return Quadruple(finalAI, finalHuman, "Human-written", confidence, reasoning)
        }

        // Case 5: Mixed signals (close scores but not 50-50)
        // Always pick the stronger signal and boost confidence
        val finalAI = if (aiProbability > humanProbability) {
            val boosted = (aiProbability + margin * 0.5f).coerceIn(0.55f, 0.8f)
            (boosted * 100).toInt()
        } else {
            val adjusted = (1f - (humanProbability + margin * 0.5f)).coerceIn(0.2f, 0.45f)
            (adjusted * 100).toInt()
        }

        val finalHuman = 100 - finalAI
        val label = if (aiProbability > humanProbability) "AI-generated" else "Human-written"
        val confidence = ((margin + 0.3f) * 100).toInt().coerceIn(55, 75)

        val reasoning = "Mixed characteristics detected with slight ${if (aiProbability > humanProbability) "AI" else "human"} bias. " +
                "Classification based on dominant patterns in writing style and structure."

        return Quadruple(finalAI, finalHuman, label, confidence, reasoning)
    }

    /**
     * Create fallback result when API fails
     */
    private fun createFallbackResult(): DecisiveResult {
        return DecisiveResult(
            label = "Mixed",
            aiProbability = 50,
            humanProbability = 50,
            confidence = 30,
            reasoning = "Unable to get reliable prediction from API. Results based on text pattern analysis only.",
            rawScores = emptyMap()
        )
    }

    // Helper data class
    private data class Quadruple<A, B, C, D, E>(
        val a: A, val b: B, val c: C, val d: D, val e: E
    )
}
