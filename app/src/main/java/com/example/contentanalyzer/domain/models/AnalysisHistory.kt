package com.example.contentanalyzer.domain.models

import com.example.contentanalyzer.data.local.entity.AnalysisHistoryEntity
import java.util.Date

data class AnalysisHistory(
    val id: Long,
    val inputType: String,
    val inputPreview: String,
    val result: String,
    val confidence: Int,
    val timestamp: Long,
    val aiPercentage: Int? = null,
    val humanPercentage: Int? = null,
    val realPercentage: Int? = null,
    val metadataFound: Boolean = false
) {
    val formattedDate: String
        get() = android.text.format.DateFormat.format("MMM dd, yyyy - HH:mm", Date(timestamp)).toString()

    val formattedTime: String
        get() = android.text.format.DateFormat.format("HH:mm", Date(timestamp)).toString()

    val confidenceColor: Int
        get() = when {
            confidence >= 70 -> 0xFF4CAF50.toInt()
            confidence >= 50 -> 0xFFFFC107.toInt()
            else -> 0xFFF44336.toInt()
        }
}

fun AnalysisHistoryEntity.toDomainModel(): AnalysisHistory {
    return AnalysisHistory(
        id = id,
        inputType = inputType,
        inputPreview = inputPreview,
        result = result,
        confidence = confidence,
        timestamp = timestamp,
        aiPercentage = aiPercentage,
        humanPercentage = humanPercentage,
        realPercentage = realPercentage,
        metadataFound = metadataFound
    )
}

fun AnalysisHistory.toEntity(): AnalysisHistoryEntity {
    return AnalysisHistoryEntity(
        id = id,
        inputType = inputType,
        inputPreview = inputPreview,
        result = result,
        confidence = confidence,
        timestamp = timestamp,
        aiPercentage = aiPercentage,
        humanPercentage = humanPercentage,
        realPercentage = realPercentage,
        metadataFound = metadataFound
    )
}
