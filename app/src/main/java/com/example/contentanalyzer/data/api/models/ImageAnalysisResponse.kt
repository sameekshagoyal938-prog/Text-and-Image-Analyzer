package com.example.contentanalyzer.data.api.models

import com.google.gson.annotations.SerializedName

data class ImageAnalysisResponse(
    @SerializedName("classification")
    val classification: String,

    @SerializedName("confidence_score")
    val confidenceScore: String,

    @SerializedName("analysis")
    val analysis: AnalysisDetails,

    @SerializedName("final_justification")
    val finalJustification: String,

    @SerializedName("bias_declaration")
    val biasDeclaration: String
)

data class AnalysisDetails(
    @SerializedName("metadata_analysis")
    val metadataAnalysis: String,

    @SerializedName("reverse_image_indicators")
    val reverseImageIndicators: String,

    @SerializedName("unusual_inconsistencies")
    val unusualInconsistencies: String,

    @SerializedName("texture_pattern_analysis")
    val texturePatternAnalysis: String,

    @SerializedName("lighting_shadow_consistency")
    val lightingShadowConsistency: String,

    @SerializedName("background_anomalies")
    val backgroundAnomalies: String,

    @SerializedName("facial_feature_analysis")
    val facialFeatureAnalysis: String,

    @SerializedName("contextual_errors")
    val contextualErrors: String,

    @SerializedName("text_label_inspection")
    val textLabelInspection: String,

    @SerializedName("digital_artifacts")
    val digitalArtifacts: String,

    @SerializedName("emotional_consistency")
    val emotionalConsistency: String,

    @SerializedName("text_analysis")
    val textAnalysis: String
)
