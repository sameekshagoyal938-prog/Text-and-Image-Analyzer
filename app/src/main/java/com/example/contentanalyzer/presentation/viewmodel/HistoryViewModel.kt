package com.example.contentanalyzer.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contentanalyzer.data.repository.HistoryRepository
import com.example.contentanalyzer.domain.models.AnalysisHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val repository = HistoryRepository(application)

    // UI State
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            repository.getAllHistory()
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error occurred"
                        )
                    }
                }
                .collect { historyList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historyList = historyList,
                            isEmpty = historyList.isEmpty(),
                            error = null
                        )
                    }
                }
        }
    }

    fun deleteHistoryItem(historyId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deleteHistory(historyId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deleteAllHistory()
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// UI State data class
data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyList: List<AnalysisHistory> = emptyList(),
    val isEmpty: Boolean = true,
    val error: String? = null
)
