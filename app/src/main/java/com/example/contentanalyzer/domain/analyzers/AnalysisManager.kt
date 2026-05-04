package com.example.contentanalyzer.domain.analyzers

import android.content.ContentResolver
import android.net.Uri
import com.example.contentanalyzer.domain.models.AnalysisResult
import com.example.contentanalyzer.domain.utils.PRNUAnalyzer

class AnalysisManager {
    private val textAnalyzer = TextAnalyzer()
    private val prnuAnalyzer = PRNUAnalyzer()

    suspend fun analyzeText(text: String): AnalysisResult {
        return textAnalyzer.analyzeText(text)
    }

    suspend fun analyzeImage(contentResolver: ContentResolver, uri: Uri): AnalysisResult {
        val prnuResult = prnuAnalyzer.analyze(contentResolver, uri)
        
        return AnalysisResult(
            type = AnalysisResult.AnalysisType.IMAGE,
            classification = prnuResult.interpretation.uppercase(),
            confidence = prnuResult.confidence.toInt(),
            details = "${prnuResult.status}: ${prnuResult.analysis}",
            rawScore = prnuResult.prnuValue.toFloat()
        )
    }
}
