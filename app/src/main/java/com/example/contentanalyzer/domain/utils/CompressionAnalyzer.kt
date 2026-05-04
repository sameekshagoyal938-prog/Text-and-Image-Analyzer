package com.example.contentanalyzer.domain.utils

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompressionAnalyzer @Inject constructor() {

    companion object {
        private const val TAG = "CompressionAnalyzer"
    }

    data class CompressionResult(
        val integrity: String,
        val confidence: Float,
        val artifactsLevel: String,
        val analysis: String,
        val compressionRatio: Float,
        val estimatedQuality: Int
    )

    suspend fun analyze(contentResolver: ContentResolver, uri: Uri): CompressionResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream = contentResolver.openInputStream(uri)
                    ?: return@withContext createDefaultResult()

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                val width = options.outWidth
                val height = options.outHeight
                val mimeType = options.outMimeType

                val megapixels = (width * height) / 1000000f

                val compressionRatio = when {
                    megapixels < 0.5f -> 0.85f
                    megapixels < 1.0f -> 0.70f
                    megapixels < 5.0f -> 0.50f
                    else -> 0.30f
                }

                val estimatedQuality = when {
                    mimeType == "image/jpeg" -> (85 - (megapixels * 2)).toInt().coerceIn(40, 90)
                    mimeType == "image/png" -> 95
                    else -> 75
                }

                val (integrity, artifactsLevel, confidence) = when {
                    compressionRatio > 0.7f && estimatedQuality > 80 -> Triple("Good", "Minimal", 85f)
                    compressionRatio > 0.5f && estimatedQuality > 60 -> Triple("Good", "Minimal", 75f)
                    compressionRatio > 0.3f -> Triple("Degraded", "Moderate", 55f)
                    else -> Triple("Severe", "High", 35f)
                }

                val analysis = buildString {
                    append("Image: ${width}x${height} (${"%.1f".format(megapixels)}MP). ")
                    append("Format: ${mimeType ?: "Unknown"}. ")
                    append("Estimated quality: $estimatedQuality%. ")
                    append("Compression integrity: $integrity. ")
                }

                CompressionResult(
                    integrity = integrity,
                    confidence = confidence,
                    artifactsLevel = artifactsLevel,
                    analysis = analysis,
                    compressionRatio = compressionRatio,
                    estimatedQuality = estimatedQuality
                )

            } catch (e: Exception) {
                Log.e(TAG, "Compression analysis failed", e)
                createDefaultResult()
            }
        }
    }

    private fun createDefaultResult(): CompressionResult {
        return CompressionResult(
            integrity = "Unknown",
            confidence = 50f,
            artifactsLevel = "Unknown",
            analysis = "Compression analysis could not be completed.",
            compressionRatio = 0.5f,
            estimatedQuality = 70
        )
    }
}
