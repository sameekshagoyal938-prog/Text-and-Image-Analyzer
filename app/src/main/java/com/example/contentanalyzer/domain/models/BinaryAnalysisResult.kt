package com.example.contentanalyzer.domain.models

import com.example.contentanalyzer.domain.utils.AIThresholdClassifier
import com.example.contentanalyzer.domain.utils.ConfidenceCalculator

data class BinaryAnalysisResult(
    val aiPercentage: Int,
    val humanPercentage: Int,
    val confidenceScore: Int,
    val explanation: String,
    val modelCertainty: Float,
    val confidenceLevel: ConfidenceCalculator.ConfidenceLevel,
    val category: AIThresholdClassifier.AICategory,
    val categoryColor: Long,
    val categoryMessage: String,
    val recommendation: String
)
