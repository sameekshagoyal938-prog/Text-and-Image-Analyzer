package com.example.contentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class AnalysisHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputType: String,
    val inputPreview: String,
    val result: String,
    val confidence: Int,
    val timestamp: Long,
    val aiPercentage: Int? = null,
    val humanPercentage: Int? = null,
    val realPercentage: Int? = null,
    val metadataFound: Boolean = false
)
