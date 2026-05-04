package com.example.contentanalyzer.domain.models

import com.example.contentanalyzer.domain.utils.ConfidenceCalculator
import org.json.JSONObject

/**
 * Universal Analysis Result model for Text and Image forensics.
 * Supports legacy fields for backward compatibility.
 */
data class AnalysisResult(
    // New fields
    val type: AnalysisType = AnalysisType.NONE,
    val classification: String = "",
    val confidence: Int = 0,
    val details: String = "",
    val rawScore: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis(),

    // Legacy fields for compatibility with ScoreCalculator and AnalyzerRepository
    val resultText: String = "",
    val confidenceScore: Int = 0,
    val confidenceLevel: ConfidenceCalculator.ConfidenceLevel = ConfidenceCalculator.ConfidenceLevel.MEDIUM,
    val isCertain: Boolean = false,
    val aiPercentage: Int? = null,
    val humanPercentage: Int? = null,
    val classificationType: String = "",
    val recommendation: String = ""
) {
    enum class AnalysisType {
        TEXT, IMAGE, NONE
    }

    fun toJsonString(): String {
        val json = JSONObject()
        json.put("type", type.name)
        json.put("classification", classification)
        json.put("confidence", confidence)
        json.put("details", details)
        json.put("timestamp", timestamp)
        return json.toString(2)
    }
}
