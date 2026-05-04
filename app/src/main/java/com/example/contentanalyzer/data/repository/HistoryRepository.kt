package com.example.contentanalyzer.data.repository

import android.content.Context
import com.example.contentanalyzer.data.local.AppDatabase
import com.example.contentanalyzer.data.local.dao.AnalysisHistoryDao
import com.example.contentanalyzer.data.local.entity.AnalysisHistoryEntity
import com.example.contentanalyzer.domain.models.AnalysisHistory
import com.example.contentanalyzer.domain.models.toDomainModel
import com.example.contentanalyzer.domain.models.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoryRepository @Inject constructor(
    private val historyDao: AnalysisHistoryDao
) {
    // Secondary constructor for manual instantiation without Hilt
    constructor(context: Context) : this(AppDatabase.getDatabase(context).historyDao())

    fun getAllHistory(): Flow<List<AnalysisHistory>> {
        return historyDao.getAllHistory()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Emit empty list on error instead of crashing
                emit(emptyList())
            }
    }

    suspend fun insertHistory(history: AnalysisHistory): Result<Unit> {
        return try {
            historyDao.insertHistory(history.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHistory(id: Long): Result<Unit> {
        return try {
            historyDao.deleteHistoryById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllHistory(): Result<Unit> {
        return try {
            historyDao.deleteAllHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertAnalysisResult(
        inputType: String,
        inputPreview: String,
        result: String,
        confidence: Int,
        aiPercentage: Int? = null,
        humanPercentage: Int? = null,
        realPercentage: Int? = null,
        metadataFound: Boolean = false
    ) {
        val history = AnalysisHistory(
            id = 0, // Room will auto-generate
            inputType = inputType,
            inputPreview = inputPreview,
            result = result,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            aiPercentage = aiPercentage,
            humanPercentage = humanPercentage,
            realPercentage = realPercentage,
            metadataFound = metadataFound
        )
        insertHistory(history)
    }
}
