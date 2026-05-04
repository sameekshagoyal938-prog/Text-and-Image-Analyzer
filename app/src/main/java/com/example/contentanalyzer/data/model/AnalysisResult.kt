package com.example.contentanalyzer.data.model

import com.example.contentanalyzer.domain.utils.ConfidenceCalculator

data class AnalysisResult(
    val resultText: String,                    // Complete formatted result
    val confidenceScore: Int,                  // 0-100% based on margin
    val confidenceLevel: ConfidenceCalculator.ConfidenceLevel,
    val isCertain: Boolean,                    // True if margin > 15%
    val aiPercentage: Int?,                    // Null if uncertain
    val humanPercentage: Int?,                 // Null if uncertain
    val classificationType: String,            // "AI Generated", "Human Written", or "Uncertain"
    val recommendation: String
)
