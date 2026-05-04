package com.example.contentanalyzer.domain.models

data class MultiClassAnalysisResult(
    val aiPercentage: Int,
    val humanPercentage: Int,
    val fakePercentage: Int,
    val confidenceScore: Float,
    val explanation: String,
    val dominantClass: String
)