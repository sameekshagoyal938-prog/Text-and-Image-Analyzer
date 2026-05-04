package com.example.contentanalyzer.domain.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object LightingAnalyzer {

    data class LightingResult(
        val consistencyScore: Float,  // 0-100, higher = more consistent
        val hasUnnaturalShadows: Boolean,
        val analysis: String
    )

    suspend fun analyzeLighting(uri: Uri, contentResolver: android.content.ContentResolver): LightingResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext createDefaultResult()
                }

                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

                // Calculate lighting distribution
                val histogram = calculateBrightnessHistogram(scaledBitmap)
                val consistencyScore = calculateConsistencyScore(histogram)
                val hasUnnaturalShadows = detectUnnaturalShadows(scaledBitmap)

                val analysis = buildString {
                    if (consistencyScore > 70) {
                        append("Lighting appears natural and consistent. ")
                    } else if (consistencyScore > 50) {
                        append("Lighting shows reasonable consistency. ")
                    } else {
                        append("Lighting patterns show some inconsistency. ")
                    }

                    if (hasUnnaturalShadows) {
                        append("Unusual shadow patterns detected. ")
                    }
                }

                scaledBitmap.recycle()
                bitmap.recycle()

                LightingResult(
                    consistencyScore = consistencyScore,
                    hasUnnaturalShadows = hasUnnaturalShadows,
                    analysis = analysis
                )
            } catch (e: Exception) {
                Log.e("LightingAnalyzer", "Error analyzing lighting", e)
                createDefaultResult()
            }
        }
    }

    private fun calculateBrightnessHistogram(bitmap: Bitmap): IntArray {
        val histogram = IntArray(256)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val brightness = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3
            histogram[brightness]++
        }

        return histogram
    }

    private fun calculateConsistencyScore(histogram: IntArray): Float {
        // Calculate smoothness of histogram (natural images have smoother distributions)
        var smoothness = 0.0
        for (i in 1 until histogram.size - 1) {
            val diff = abs(histogram[i] - (histogram[i - 1] + histogram[i + 1]) / 2.0)
            smoothness += diff
        }

        val maxPossibleDiff = histogram.sum() * 0.5
        val consistencyScore = (1.0 - (smoothness / maxPossibleDiff)) * 100

        return consistencyScore.toFloat().coerceIn(0f, 100f)
    }

    private fun detectUnnaturalShadows(bitmap: Bitmap): Boolean {
        // Simplified shadow detection
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var shadowCount = 0
        val threshold = 50

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = pixels[y * width + x]
                val centerBright = ((center shr 16 and 0xFF) + (center shr 8 and 0xFF) + (center and 0xFF)) / 3

                val top = pixels[(y - 1) * width + x]
                val topBright = ((top shr 16 and 0xFF) + (top shr 8 and 0xFF) + (top and 0xFF)) / 3

                if (abs(centerBright - topBright) > threshold) {
                    shadowCount++
                }
            }
        }

        val shadowRatio = shadowCount.toFloat() / ((height - 2) * (width - 2))
        return shadowRatio > 0.3f
    }

    private fun createDefaultResult(): LightingResult {
        return LightingResult(
            consistencyScore = 50f,
            hasUnnaturalShadows = false,
            analysis = "Lighting analysis completed with default values."
        )
    }
}
