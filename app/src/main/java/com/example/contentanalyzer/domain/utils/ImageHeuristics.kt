package com.example.contentanalyzer.domain.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow

object ImageHeuristicsAnalyzer {

    private const val TAG = "ImageHeuristics"

    data class VisualPatterns(
        val textureNaturalness: Float,   // 0-1, higher = more natural (real)
        val noiseNaturalness: Float,     // 0-1, higher = more natural (real)
        val artifactLevel: Float,        // 0-1, higher = more artifacts (AI)
        val overallAIScore: Float,       // 0-1, higher = more likely AI
        val overallRealScore: Float      // 0-1, higher = more likely real
    )

    data class HeuristicsScores(
        val textureScore: Float,    // 0-100
        val lightingScore: Float,   // 0-100
        val artifactScore: Float    // 0-100
    )

    suspend fun analyze(contentResolver: ContentResolver, uri: Uri): HeuristicsScores {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext createDefaultScores()
                }

                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap == null) {
                    return@withContext createDefaultScores()
                }

                // Resize for faster analysis
                val bitmap = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true)

                // Calculate all scores
                val textureNaturalness = calculateTextureNaturalness(bitmap)
                val noiseNaturalness = calculateNoiseNaturalness(bitmap)
                val artifactLevel = calculateArtifactLevel(bitmap)
                
                // For lighting score, we'll use a combination of noise naturalness and texture as a proxy 
                // if we don't want to add a full lighting analyzer here, or we can use LightingAnalyzer
                val lightingScore = (noiseNaturalness * 60f + 40f).coerceIn(0f, 100f)

                val scores = HeuristicsScores(
                    textureScore = textureNaturalness * 100f,
                    lightingScore = lightingScore,
                    artifactScore = artifactLevel * 100f
                )

                Log.d(TAG, """
                    🔍 VISUAL PATTERN ANALYSIS
                    Texture Score: ${"%.2f".format(scores.textureScore)}
                    Lighting Score: ${"%.2f".format(scores.lightingScore)}
                    Artifact Score: ${"%.2f".format(scores.artifactScore)}
                """.trimIndent())

                bitmap.recycle()
                originalBitmap.recycle()

                scores

            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing visual patterns", e)
                createDefaultScores()
            }
        }
    }

    private fun calculateTextureNaturalness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalVariance = 0.0
        var count = 0
        val windowSize = 4

        for (y in 0 until height - windowSize step windowSize) {
            for (x in 0 until width - windowSize step windowSize) {
                var sum = 0.0
                var pixelCount = 0

                for (dy in 0 until windowSize) {
                    for (dx in 0 until windowSize) {
                        val pixel = pixels[(y + dy) * width + (x + dx)]
                        val brightness = ((pixel shr 16 and 0xFF) +
                                (pixel shr 8 and 0xFF) +
                                (pixel and 0xFF)) / 3.0
                        sum += brightness
                        pixelCount++
                    }
                }

                val mean = sum / pixelCount
                var variance = 0.0

                for (dy in 0 until windowSize) {
                    for (dx in 0 until windowSize) {
                        val pixel = pixels[(y + dy) * width + (x + dx)]
                        val brightness = ((pixel shr 16 and 0xFF) +
                                (pixel shr 8 and 0xFF) +
                                (pixel and 0xFF)) / 3.0
                        variance += (brightness - mean).pow(2.0)
                    }
                }

                totalVariance += variance / pixelCount
                count++
            }
        }

        val avgVariance = if (count > 0) totalVariance / count else 0.0

        // Higher variance = more texture = more natural
        // Normalize: 0-2000 variance maps to 0-1 naturalness
        val naturalness = (avgVariance / 2000.0).coerceIn(0.2, 0.9).toFloat()
        return naturalness
    }

    private fun calculateNoiseNaturalness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var highFreqSum = 0.0
        var totalPixels = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = pixels[y * width + x]
                val centerBright = ((center shr 16 and 0xFF) +
                        (center shr 8 and 0xFF) +
                        (center and 0xFF)) / 3.0

                var neighborSum = 0.0
                var neighborCount = 0

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dy == 0 && dx == 0) continue
                        val neighbor = pixels[(y + dy) * width + (x + dx)]
                        val neighborBright = ((neighbor shr 16 and 0xFF) +
                                (neighbor shr 8 and 0xFF) +
                                (neighbor and 0xFF)) / 3.0
                        neighborSum += neighborBright
                        neighborCount++
                    }
                }

                val neighborAvg = neighborSum / neighborCount
                val diff = abs(centerBright - neighborAvg)
                highFreqSum += diff
                totalPixels++
            }
        }

        val avgHighFreq = if (totalPixels > 0) highFreqSum / totalPixels else 0.0

        // Higher high-frequency = more noise = more natural
        // Normalize: 0-30 maps to 0-1 naturalness
        val naturalness = (avgHighFreq / 30.0).coerceIn(0.2, 0.9).toFloat()
        return naturalness
    }

    private fun calculateArtifactLevel(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var artifactCount = 0
        val threshold = 30

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val pixel = pixels[y * width + x]
                val pixelBright = ((pixel shr 16 and 0xFF) +
                        (pixel shr 8 and 0xFF) +
                        (pixel and 0xFF)) / 3

                val left = pixels[y * width + (x - 1)]
                val leftBright = ((left shr 16 and 0xFF) +
                        (left shr 8 and 0xFF) +
                        (left and 0xFF)) / 3

                val right = pixels[y * width + (x + 1)]
                val rightBright = ((right shr 16 and 0xFF) +
                        (right shr 8 and 0xFF) +
                        (right and 0xFF)) / 3

                val top = pixels[(y - 1) * width + x]
                val topBright = ((top shr 16 and 0xFF) +
                        (top shr 8 and 0xFF) +
                        (top and 0xFF)) / 3

                val bottom = pixels[(y + 1) * width + x]
                val bottomBright = ((bottom shr 16 and 0xFF) +
                        (bottom shr 8 and 0xFF) +
                        (bottom and 0xFF)) / 3

                if (abs(pixelBright - leftBright) > threshold ||
                    abs(pixelBright - rightBright) > threshold ||
                    abs(pixelBright - topBright) > threshold ||
                    abs(pixelBright - bottomBright) > threshold) {
                    artifactCount++
                }
            }
        }

        val totalPixels = (height - 2) * (width - 2)
        val artifactRatio = if (totalPixels > 0) artifactCount.toFloat() / totalPixels else 0f

        return artifactRatio.coerceIn(0f, 0.5f)
    }

    private fun createDefaultScores(): HeuristicsScores {
        return HeuristicsScores(
            textureScore = 50f,
            lightingScore = 50f,
            artifactScore = 25f
        )
    }
}
