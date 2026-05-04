package com.example.contentanalyzer.domain.models

data class ImageAnalysisResult(
    // Binary classification result
    val result: String,           // "AI Generated", "Real Image", or "Uncertain ⚠️"
    val confidence: Int,          // 0-100%
    val reasoning: String,        // Detailed explanation

    // Internal scoring (for debugging)
    val aiScore: Float? = null,
    val realScore: Float? = null,
    val margin: Float? = null,
    val metadataFound: Boolean = false,
    val cameraModel: String? = null,
    val modelUsed: String = "hybrid-v2"
)