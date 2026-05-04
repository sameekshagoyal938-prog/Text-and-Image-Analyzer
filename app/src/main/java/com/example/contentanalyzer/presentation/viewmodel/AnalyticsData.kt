package com.example.contentanalyzer.presentation.viewmodel

data class AnalyticsData(
    val totalCount: Int = 0,
    val aiCount: Int = 0,
    val realCount: Int = 0,
    val uncertainCount: Int = 0,
    val textCount: Int = 0,
    val imageCount: Int = 0,
    val averageConfidence: Float = 0f
)
