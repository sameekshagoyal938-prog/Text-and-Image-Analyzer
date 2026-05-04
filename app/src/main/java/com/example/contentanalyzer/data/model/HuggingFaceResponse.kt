package com.example.contentanalyzer.data.model

import com.google.gson.annotations.SerializedName

data class HuggingFaceResponse(
    @SerializedName("label")
    val label: String? = null,

    @SerializedName("score")
    val score: Float? = null
)

// Response can be either a single object or list
data class ClassificationResponse(
    val predictions: List<HuggingFaceResponse>? = null,
    val error: String? = null
)

// For zero-shot classification
data class ZeroShotResponse(
    @SerializedName("sequence")
    val sequence: String? = null,

    @SerializedName("labels")
    val labels: List<String>? = null,

    @SerializedName("scores")
    val scores: List<Float>? = null
)

// Proper analysis result with probabilities
data class ModelAnalysisResult(
    val aiProbability: Float,
    val humanProbability: Float,
    val fakeProbability: Float,
    val rawPredictions: Map<String, Float>,
    val modelConfidence: Float
)
