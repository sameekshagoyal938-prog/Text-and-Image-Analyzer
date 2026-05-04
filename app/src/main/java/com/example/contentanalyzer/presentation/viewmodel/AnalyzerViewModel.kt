package com.example.contentanalyzer.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contentanalyzer.data.repository.AnalyzerRepository
import com.example.contentanalyzer.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

sealed class TextAnalysisUiState {
    object Idle : TextAnalysisUiState()
    object Loading : TextAnalysisUiState()
    data class Success(val result: JSONObject) : TextAnalysisUiState()
    data class Error(val message: String) : TextAnalysisUiState()
}

@HiltViewModel
class AnalyzerViewModel @Inject constructor(
    application: Application,
    private val repository: AnalyzerRepository,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<TextAnalysisUiState>(TextAnalysisUiState.Idle)
    val uiState: StateFlow<TextAnalysisUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
        if (_uiState.value is TextAnalysisUiState.Success || _uiState.value is TextAnalysisUiState.Error) {
            _uiState.value = TextAnalysisUiState.Idle
        }
    }

    fun analyzeText() {
        val text = _inputText.value.trim()

        if (text.isEmpty()) {
            _uiState.value = TextAnalysisUiState.Error("Please enter text to analyze")
            return
        }

        viewModelScope.launch {
            _uiState.value = TextAnalysisUiState.Loading

            val result = repository.analyzeText(text)

            result.fold(
                onSuccess = { mapResult ->
                    val jsonResult = JSONObject(mapResult)
                    _uiState.value = TextAnalysisUiState.Success(jsonResult)

                    // Save to history
                    saveToHistory(text, jsonResult)
                },
                onFailure = { exception ->
                    _uiState.value = TextAnalysisUiState.Error(
                        "Analysis failed: ${exception.message ?: "Unknown error"}"
                    )
                }
            )
        }
    }

    private suspend fun saveToHistory(text: String, result: JSONObject) {
        try {
            historyRepository.insertAnalysisResult(
                inputType = "Text",
                inputPreview = text,
                result = result.getString("label"),
                confidence = result.getInt("confidence"),
                aiPercentage = result.optInt("ai_probability"),
                humanPercentage = result.optInt("human_probability")
            )
        } catch (e: Exception) {
            // Silently fail history saving to not interrupt user experience
        }
    }

    fun reset() {
        _uiState.value = TextAnalysisUiState.Idle
        _inputText.value = ""
    }
}
