package com.example.contentanalyzer.presentation.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contentanalyzer.data.repository.ImageAnalyzerRepository
import com.example.contentanalyzer.domain.models.ForensicsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ImageAnalysisUiState {
    object Idle : ImageAnalysisUiState()
    object ImageSelected : ImageAnalysisUiState()
    object Loading : ImageAnalysisUiState()
    data class Success(val result: ForensicsResult) : ImageAnalysisUiState()
    data class Error(val message: String) : ImageAnalysisUiState()
}

@HiltViewModel
class ImageAnalyzerViewModel @Inject constructor(
    private val repository: ImageAnalyzerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImageAnalysisUiState>(ImageAnalysisUiState.Idle)
    val uiState: StateFlow<ImageAnalysisUiState> = _uiState.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _jsonOutput = MutableStateFlow<String?>(null)
    val jsonOutput: StateFlow<String?> = _jsonOutput.asStateFlow()

    fun selectImage(uri: Uri) {
        _selectedImageUri.value = uri
        _uiState.value = ImageAnalysisUiState.ImageSelected
        _jsonOutput.value = null
    }

    fun analyzeImage(contentResolver: ContentResolver) {
        val uri = _selectedImageUri.value
        if (uri == null) {
            _uiState.value = ImageAnalysisUiState.Error("No image selected")
            return
        }

        viewModelScope.launch {
            _uiState.value = ImageAnalysisUiState.Loading

            val result = repository.analyzeImage(contentResolver, uri)

            _uiState.value = ImageAnalysisUiState.Success(result)
            _jsonOutput.value = result.toJsonString()
        }
    }

    fun clearSelection() {
        _selectedImageUri.value = null
        _jsonOutput.value = null
        _uiState.value = ImageAnalysisUiState.Idle
    }
}
