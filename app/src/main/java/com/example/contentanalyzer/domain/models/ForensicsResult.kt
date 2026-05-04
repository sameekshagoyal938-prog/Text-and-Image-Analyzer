package com.example.contentanalyzer.domain.models

import org.json.JSONObject

data class ForensicsResult(
    val classification: String,
    val confidenceScore: Int,
    val metadataStatus: String,
    val prnuStatus: String,
    val metadataAnalysis: String,
    val prnuAnalysis: String,
    val compressionIntegrity: String,
    val finalJustification: String,
    val biasDeclaration: String
) {
    fun toJsonString(): String {
        return JSONObject().apply {
            put("classification", classification)
            put("confidenceScore", confidenceScore)
            put("metadataStatus", metadataStatus)
            put("prnuStatus", prnuStatus)
            put("metadataAnalysis", metadataAnalysis)
            put("prnuAnalysis", prnuAnalysis)
            put("compressionIntegrity", compressionIntegrity)
            put("finalJustification", finalJustification)
            put("biasDeclaration", biasDeclaration)
        }.toString(4)
    }
}
