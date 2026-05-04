package com.example.contentanalyzer.data.api

import android.util.Log
import com.example.contentanalyzer.data.api.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

object ResponseParser {

    private const val TAG = "ResponseParser"
    private val gson = Gson()

    fun parseBinaryClassification(
        predictions: List<HuggingFacePrediction>,
        text: String
    ): ParsedAnalysisResult {
        if (predictions.isEmpty()) {
            return ParsedAnalysisResult(0.5f, 0.5f, emptyMap(), 0f, "binary", false)
        }

        val rawPredictions = predictions.associate { it.label to it.score }
        
        var aiProb = 0f
        var humanProb = 0f

        predictions.forEach { 
            val label = it.label.uppercase()
            if (label.contains("LABEL_1") || label.contains("AI") || label.contains("FAKE") || label.contains("GENERATED")) {
                aiProb = it.score
            } else if (label.contains("LABEL_0") || label.contains("HUMAN") || label.contains("REAL") || label.contains("ORIGINAL")) {
                humanProb = it.score
            }
        }

        // Handle case where labels don't match our heuristics
        if (aiProb == 0f && humanProb == 0f) {
            val highest = predictions.maxByOrNull { it.score }
            if (highest != null) {
                // If it's something unknown, we can't be sure, but let's assume the highest score is what it's predicting
                aiProb = highest.score
                humanProb = 1f - aiProb
            }
        } else if (aiProb > 0f && humanProb == 0f) {
            humanProb = 1f - aiProb
        } else if (humanProb > 0f && aiProb == 0f) {
            aiProb = 1f - humanProb
        }

        val confidence = abs(aiProb - humanProb)

        return ParsedAnalysisResult(
            aiProbability = aiProb,
            humanProbability = humanProb,
            rawPredictions = rawPredictions,
            modelConfidence = confidence,
            responseType = "binary",
            isValid = true
        )
    }

    fun parseZeroShotClassification(
        responses: List<ZeroShotResponse>,
        text: String
    ): ParsedAnalysisResult {
        if (responses.isEmpty()) {
            return ParsedAnalysisResult(0.5f, 0.5f, emptyMap(), 0f, "zero_shot", false)
        }

        val response = responses[0]
        val labels = response.labels
        val scores = response.scores

        val rawPredictions = labels.zip(scores).toMap()
        
        var aiProb = 0.5f
        var humanProb = 0.5f

        labels.forEachIndexed { index, label ->
            val score = scores.getOrNull(index) ?: 0f
            if (label.contains("AI", ignoreCase = true) || label.contains("generated", ignoreCase = true)) {
                aiProb = score
            } else if (label.contains("human", ignoreCase = true) || label.contains("written", ignoreCase = true)) {
                humanProb = score
            }
        }

        val confidence = abs(aiProb - humanProb)

        return ParsedAnalysisResult(
            aiProbability = aiProb,
            humanProbability = humanProb,
            rawPredictions = rawPredictions,
            modelConfidence = confidence,
            responseType = "zero_shot",
            isValid = true
        )
    }

    fun validateAndSanitize(result: ParsedAnalysisResult): ParsedAnalysisResult {
        return result.copy(
            aiProbability = result.aiProbability.coerceIn(0f, 1f),
            humanProbability = result.humanProbability.coerceIn(0f, 1f),
            modelConfidence = result.modelConfidence.coerceIn(0f, 1f)
        )
    }

    fun parseRawResponse(json: String?): ApiResponse {
        if (json.isNullOrBlank()) return ApiResponse.Empty

        return try {
            if (json.contains("\"error\"")) {
                val error = gson.fromJson(json, HuggingFaceError::class.java)
                ApiResponse.Error(error)
            } else if (json.trim().startsWith("[")) {
                // Determine if it's binary or zero-shot
                if (json.contains("\"sequence\"") && json.contains("\"labels\"")) {
                    val type = object : TypeToken<List<ZeroShotResponse>>() {}.type
                    val responses = gson.fromJson<List<ZeroShotResponse>>(json, type)
                    ApiResponse.ZeroShotSuccess(responses)
                } else {
                    val type = object : TypeToken<List<HuggingFacePrediction>>() {}.type
                    val predictions = gson.fromJson<List<HuggingFacePrediction>>(json, type)
                    ApiResponse.Success(predictions)
                }
            } else {
                ApiResponse.Empty
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse raw response: $json", e)
            ApiResponse.Empty
        }
    }

    fun parseError(error: HuggingFaceError): String {
        return error.error ?: "Unknown API error"
    }
}
