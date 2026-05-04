package com.example.contentanalyzer.data.repository

import android.util.Log
import com.example.contentanalyzer.BuildConfig
import com.example.contentanalyzer.data.api.RetrofitInstance
import com.example.contentanalyzer.data.api.models.AIRequest
import com.example.contentanalyzer.data.api.models.ChatGPTDetectorRequest
import com.example.contentanalyzer.data.api.models.HuggingFacePrediction
import com.example.contentanalyzer.domain.utils.DecisiveResponseParser
import com.example.contentanalyzer.domain.utils.TextAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs

class AnalyzerRepository @Inject constructor() {

    companion object {
        private const val TAG = "AnalyzerRepository"
        private const val API_KEY = BuildConfig.HUGGING_FACE_API_KEY
        private const val MAX_RETRIES = 2
    }

    private val textAnalyzer = TextAnalyzer()

    /**
     * Main analysis method - returns JSON-compatible result
     */
    suspend fun analyzeText(text: String): Result<Map<String, Any>> {
        Log.d(TAG, "========== STARTING ANALYSIS ==========")
        Log.d(TAG, "Text length: ${text.length} chars, Words: ${text.split(" ").size}")

        // Validate input
        if (text.trim().isEmpty()) {
            return Result.failure(Exception("Please enter text to analyze"))
        }

        return try {
            // Try ChatGPT Detector first (primary)
            val result = tryChatGPTDetector(text)

            if (result.isSuccess) {
                Log.d(TAG, "✅ ChatGPT Detector succeeded")
                return result
            }

            // Fallback to RoBERTa
            Log.w(TAG, "ChatGPT Detector failed, trying RoBERTa fallback")
            delay(1000)
            val fallbackResult = tryRoBERTaDetector(text)

            if (fallbackResult.isSuccess) {
                Log.d(TAG, "✅ RoBERTa fallback succeeded")
                return fallbackResult
            }

            // Ultimate fallback - text pattern analysis only
            Log.w(TAG, "All API calls failed, using pattern analysis fallback")
            Result.success(createPatternBasedResult(text))

        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            Result.failure(e)
        }
    }

    /**
     * Try ChatGPT Detector model (best for modern AI detection)
     */
    private suspend fun tryChatGPTDetector(text: String): Result<Map<String, Any>> {
        try {
            val request = ChatGPTDetectorRequest(
                inputs = text,
                options = mapOf("wait_for_model" to true)
            )

            Log.d(TAG, "Calling ChatGPT Detector API...")
            val response = RetrofitInstance.api.detectWithChatGPTDetector(
                authorization = "Bearer $API_KEY",
                request = request
            )

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val predictions = response.body()!!
                Log.d(TAG, "Received ${predictions.size} predictions")

                // Parse with decisive logic
                val parsedResult = DecisiveResponseParser.parseChatGPTResponse(predictions)

                // Convert to clean JSON map
                val jsonResult = mapOf(
                    "label" to parsedResult.label,
                    "ai_probability" to parsedResult.aiProbability,
                    "human_probability" to parsedResult.humanProbability,
                    "confidence" to parsedResult.confidence,
                    "reasoning" to parsedResult.reasoning,
                    "model_used" to "chatgpt-detector"
                )

                Log.d(TAG, "Result: ${jsonResult["label"]} (AI: ${jsonResult["ai_probability"]}%, Confidence: ${jsonResult["confidence"]}%)")

                return Result.success(jsonResult)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error: ${response.code()} - $errorBody")
                return Result.failure(Exception("API error: ${response.code()}"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "ChatGPT Detector failed", e)
            return Result.failure(e)
        }
    }

    /**
     * Fallback to RoBERTa detector
     */
    private suspend fun tryRoBERTaDetector(text: String): Result<Map<String, Any>> {
        try {
            val request = AIRequest(
                inputs = text,
                options = mapOf("wait_for_model" to true)
            )

            Log.d(TAG, "Calling RoBERTa API...")
            val response = RetrofitInstance.api.detectWithRoBERTa(
                authorization = "Bearer $API_KEY",
                request = request
            )

            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val predictions = response.body()!!
                val parsedResult = DecisiveResponseParser.parseRoBERTaResponse(predictions)

                val jsonResult = mapOf(
                    "label" to parsedResult.label,
                    "ai_probability" to parsedResult.aiProbability,
                    "human_probability" to parsedResult.humanProbability,
                    "confidence" to parsedResult.confidence,
                    "reasoning" to parsedResult.reasoning,
                    "model_used" to "roberta-detector"
                )

                return Result.success(jsonResult)
            } else {
                return Result.failure(Exception("RoBERTa API error: ${response.code()}"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "RoBERTa detector failed", e)
            return Result.failure(e)
        }
    }

    /**
     * Raw API call for debugging/testing purposes
     */
    suspend fun makeAPICall(text: String): Response<List<HuggingFacePrediction>> {
        val request = AIRequest(
            inputs = text,
            options = mapOf("wait_for_model" to true)
        )
        return RetrofitInstance.api.detectWithRoBERTa(
            authorization = "Bearer $API_KEY",
            request = request
        )
    }

    /**
     * Pattern-based analysis as ultimate fallback
     */
    private fun createPatternBasedResult(text: String): Map<String, Any> {
        val features = textAnalyzer.analyze(text)

        // Calculate AI probability based on patterns
        var aiProbability = (
                features.repetitiveness * 0.4f +
                        (1f - features.burstiness) * 0.3f +
                        (1f - features.vocabularyRichness) * 0.3f
                ).coerceIn(0f, 1f)

        // Decisive adjustment - no 50-50
        aiProbability = when {
            aiProbability >= 0.55 -> aiProbability + 0.1f
            aiProbability <= 0.45 -> aiProbability - 0.1f
            aiProbability > 0.5 -> 0.65f  // Push to 65% AI
            else -> 0.35f  // Push to 35% AI (65% Human)
        }

        val aiPercent = (aiProbability * 100).toInt()
        val humanPercent = 100 - aiPercent

        val label = when {
            aiPercent >= 60 -> "AI-generated"
            aiPercent <= 40 -> "Human-written"
            else -> "Mixed"
        }

        val confidence = when {
            abs(aiPercent - 50) >= 30 -> 75
            abs(aiPercent - 50) >= 20 -> 65
            else -> 55
        }

        val reasoning = when (label) {
            "AI-generated" -> "Text shows repetitive patterns and low sentence variation typical of AI-generated content. " +
                    "Limited vocabulary diversity and uniform structure detected."
            "Human-written" -> "Text shows natural variation in sentence structure and personal elements. " +
                    "Rich vocabulary and irregular patterns typical of human writing."
            else -> "Text exhibits mixed characteristics. Some AI-like patterns detected but also human elements. " +
                    "Results based on text pattern analysis due to API unavailability."
        }

        return mapOf(
            "label" to label,
            "ai_probability" to aiPercent,
            "human_probability" to humanPercent,
            "confidence" to confidence,
            "reasoning" to reasoning,
            "model_used" to "pattern-analysis-fallback"
        )
    }
}
