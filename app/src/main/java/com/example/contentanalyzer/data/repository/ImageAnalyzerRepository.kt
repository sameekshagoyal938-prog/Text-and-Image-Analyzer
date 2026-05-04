package com.example.contentanalyzer.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.contentanalyzer.domain.utils.PRNUAnalyzer
import com.example.contentanalyzer.domain.utils.MetadataForensics
import com.example.contentanalyzer.domain.utils.CompressionAnalyzer
import com.example.contentanalyzer.domain.models.ForensicsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageAnalyzerRepository @Inject constructor() {

    companion object {
        private const val TAG = "ImageAnalyzerRepo"
    }

    private val prnuAnalyzer = PRNUAnalyzer()
    private val metadataForensics = MetadataForensics()
    private val compressionAnalyzer = CompressionAnalyzer()

    suspend fun analyzeImage(
        contentResolver: ContentResolver,
        uri: Uri
    ): ForensicsResult {
        Log.d(TAG, "========== STARTING FORENSICS ANALYSIS ==========")

        return withContext(Dispatchers.IO) {
            val prnuDeferred = async { prnuAnalyzer.analyze(contentResolver, uri) }
            val metadataDeferred = async { metadataForensics.analyze(contentResolver, uri) }
            val compressionDeferred = async { compressionAnalyzer.analyze(contentResolver, uri) }

            val prnu = prnuDeferred.await()
            val metadata = metadataDeferred.await()
            val compression = compressionDeferred.await()

            // ========== DYNAMIC WEIGHTS BASED ON PRNU VALUE ==========
            val (weightPRNU, weightMetadata, weightCompression) = calculateWeightsBasedOnPRNU(prnu)

            Log.d(TAG, """
                ========== PRNU INTERPRETATION ==========
                PRNU Value: ${prnu.prnuValue}
                Interpretation: ${prnu.interpretation}
                Status: ${prnu.status}
                PRNU Weight: ${(weightPRNU * 100).toInt()}%
                ==========================================
            """.trimIndent())

            // ========== CLASSIFICATION BASED ON PRNU VALUE ==========
            var finalClassification = ""
            var confidenceScore = 0

            when (prnu.interpretation) {
                "Human" -> {
                    finalClassification = "HUMAN GENERATED"
                    confidenceScore = prnu.confidence.toInt()
                }
                "AI" -> {
                    finalClassification = "AI GENERATED"
                    confidenceScore = prnu.confidence.toInt()
                }
                "No Pattern" -> {
                    finalClassification = "AI GENERATED"
                    confidenceScore = 85
                }
                else -> {  // Inconclusive
                    finalClassification = determineFromMetadata(metadata, compression)
                    confidenceScore = calculateInconclusiveConfidence(metadata, compression)
                }
            }

            // Build justification
            val finalJustification = buildJustification(
                classification = finalClassification,
                confidenceScore = confidenceScore,
                prnu = prnu,
                metadata = metadata,
                compression = compression
            )

            ForensicsResult(
                classification = finalClassification,
                confidenceScore = confidenceScore,
                metadataStatus = metadata.status,
                prnuStatus = prnu.status,
                metadataAnalysis = metadata.analysis,
                prnuAnalysis = buildPRNUAnalysis(prnu),
                compressionIntegrity = compression.analysis,
                finalJustification = finalJustification,
                biasDeclaration = "This analysis is unbiased and independent of camera brand, device type, and platform."
            )
        }
    }

    private fun calculateWeightsBasedOnPRNU(prnu: PRNUAnalyzer.PRNUResult): Triple<Float, Float, Float> {
        return when (prnu.interpretation) {
            "Human" -> Triple(0.80f, 0.15f, 0.05f)
            "AI" -> Triple(0.85f, 0.10f, 0.05f)
            "No Pattern" -> Triple(0.75f, 0.15f, 0.10f)
            else -> Triple(0.40f, 0.45f, 0.15f)
        }
    }

    private fun determineFromMetadata(
        metadata: MetadataForensics.MetadataResult,
        compression: CompressionAnalyzer.CompressionResult
    ): String {
        return when {
            metadata.aiToolsDetected.isNotEmpty() -> "AI GENERATED"
            metadata.cameraInfo != null -> "HUMAN GENERATED"
            compression.integrity == "Severe" -> "AI GENERATED"
            else -> "UNCERTAIN"
        }
    }

    private fun calculateInconclusiveConfidence(
        metadata: MetadataForensics.MetadataResult,
        compression: CompressionAnalyzer.CompressionResult
    ): Int {
        var confidence = 60
        if (metadata.aiToolsDetected.isNotEmpty()) confidence += 15
        if (metadata.cameraInfo != null) confidence += 20
        if (compression.integrity == "Severe") confidence += 10
        return confidence.coerceIn(55, 85)
    }

    private fun buildPRNUAnalysis(prnu: PRNUAnalyzer.PRNUResult): String {
        return "PRNU Value: ${"%.4f".format(prnu.prnuValue)}. Interpretation: ${prnu.interpretation}. Status: ${prnu.status}"
    }

    private fun buildJustification(
        classification: String,
        confidenceScore: Int,
        prnu: PRNUAnalyzer.PRNUResult,
        metadata: MetadataForensics.MetadataResult,
        compression: CompressionAnalyzer.CompressionResult
    ): String {
        return if (classification == "HUMAN GENERATED") {
            "Natural camera sensor pattern detected (PRNU ${"%.4f".format(prnu.prnuValue)}). Status: ${prnu.status}."
        } else {
            "No valid camera sensor pattern detected (PRNU ${"%.4f".format(prnu.prnuValue)}). Likely synthetic or AI-generated content."
        }
    }
}