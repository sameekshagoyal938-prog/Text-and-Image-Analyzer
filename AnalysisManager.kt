package com.example.contentanalyzer.domain.analyzers

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.example.contentanalyzer.domain.models.AnalysisResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central manager that routes analysis to appropriate analyzer
 * Ensures text and image analysis never interfere
 */
class AnalysisManager {

    private val textAnalyzer = TextAnalyzer()
    private val imageAnalyzer = ImageAnalyzer()

    /**
     * Analyze text input only
     */
    suspend fun analyzeText(text: String): AnalysisResult {
        return withContext(Dispatchers.Default) {
            textAnalyzer.analyzeText(text)
        }
    }

    /**
     * Analyze image input only
     */
    suspend fun analyzeImage(
        contentResolver: ContentResolver,
        uri: Uri
    ): AnalysisResult {
        return withContext(Dispatchers.IO) {
            imageAnalyzer.analyzeImage(contentResolver, uri)
        }
    }

    /**
     * Smart routing based on input type
     * Decides which analyzer to call
     */
    suspend fun analyze(
        text: String? = null,
        imageUri: Uri? = null,
        contentResolver: ContentResolver? = null
    ): AnalysisResult {
        return when {
            // Text analysis takes priority when text is provided
            !text.isNullOrEmpty() && text.trim().isNotEmpty() -> {
                analyzeText(text)
            }
            // Image analysis when image is provided
            imageUri != null && contentResolver != null -> {
                analyzeImage(contentResolver, imageUri)
            }
            // No valid input
            else -> {
                AnalysisResult(
                    type = AnalysisResult.AnalysisType.NONE,
                    classification = "NO INPUT",
                    confidence = 0,
                    details = "Please provide either text or an image for analysis",
                    rawScore = 0.5f
                )
            }
        }
    }

    /**
     * Non-suspending version with callback for UI
     */
    fun analyzeAsync(
        text: String? = null,
        imageUri: Uri? = null,
        contentResolver: ContentResolver? = null,
        onResult: (AnalysisResult) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = analyze(text, imageUri, contentResolver)
            onResult(result)
        }
    }
}