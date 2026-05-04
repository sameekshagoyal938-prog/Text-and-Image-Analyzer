package com.example.contentanalyzer.domain.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object ArtifactDetector {

    data class ArtifactResult(
        val artifactLevel: Float,  // 0-100, higher = more artifacts
        val hasCompressionArtifacts: Boolean,
        val hasAIDiffusionArtifacts: Boolean,
        val analysis: String
    )

    suspend fun detectArtifacts(uri: Uri, contentResolver: android.content.ContentResolver): ArtifactResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext createDefaultResult()
                }

                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

                val compressionArtifacts = detectCompressionArtifacts(scaledBitmap)
                val aiDiffusionArtifacts = detectAIDiffusionArtifacts(scaledBitmap)

                var artifactLevel = 0f
                if (compressionArtifacts) artifactLevel += 30f
                if (aiDiffusionArtifacts) artifactLevel += 40f
                artifactLevel = artifactLevel.coerceIn(0f, 100f)

                val analysis = buildString {
                    if (compressionArtifacts) {
                        append("Compression artifacts detected. ")
                    }
                    if (aiDiffusionArtifacts) {
                        append("AI diffusion artifacts observed. ")
                    }
                    if (!compressionArtifacts && !aiDiffusionArtifacts) {
                        append("No significant digital artifacts detected. ")
                    }
                }

                scaledBitmap.recycle()
                bitmap.recycle()

                ArtifactResult(
                    artifactLevel = artifactLevel,
                    hasCompressionArtifacts = compressionArtifacts,
                    hasAIDiffusionArtifacts = aiDiffusionArtifacts,
                    analysis = analysis
                )
            } catch (e: Exception) {
                Log.e("ArtifactDetector", "Error detecting artifacts", e)
                createDefaultResult()
            }
        }
    }

    private fun detectCompressionArtifacts(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var blockinessScore = 0
        val blockSize = 8

        for (y in 0 until height - blockSize step blockSize) {
            for (x in 0 until width - blockSize step blockSize) {
                // Check for block boundaries
                val rightEdge = x + blockSize - 1
                val bottomEdge = y + blockSize - 1

                if (rightEdge + 1 < width) {
                    for (dy in 0 until blockSize) {
                        val leftPixel = pixels[(y + dy) * width + rightEdge]
                        val rightPixel = pixels[(y + dy) * width + rightEdge + 1]
                        val leftBright = ((leftPixel shr 16 and 0xFF) + (leftPixel shr 8 and 0xFF) + (leftPixel and 0xFF)) / 3
                        val rightBright = ((rightPixel shr 16 and 0xFF) + (rightPixel shr 8 and 0xFF) + (rightPixel and 0xFF)) / 3
                        if (abs(leftBright - rightBright) > 30) {
                            blockinessScore++
                        }
                    }
                }
            }
        }

        return blockinessScore > 100
    }

    private fun detectAIDiffusionArtifacts(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var smoothScore = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = pixels[y * width + x]
                val centerBright = ((center shr 16 and 0xFF) + (center shr 8 and 0xFF) + (center and 0xFF)) / 3

                var neighborSum = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dy == 0 && dx == 0) continue
                        val neighbor = pixels[(y + dy) * width + (x + dx)]
                        val neighborBright = ((neighbor shr 16 and 0xFF) + (neighbor shr 8 and 0xFF) + (neighbor and 0xFF)) / 3
                        neighborSum += neighborBright
                    }
                }

                val avgNeighbor = neighborSum / 8
                if (abs(centerBright - avgNeighbor) < 10) {
                    smoothScore++
                }
            }
        }

        val smoothRatio = smoothScore.toFloat() / ((height - 2) * (width - 2))
        return smoothRatio > 0.7f
    }

    private fun createDefaultResult(): ArtifactResult {
        return ArtifactResult(
            artifactLevel = 20f,
            hasCompressionArtifacts = false,
            hasAIDiffusionArtifacts = false,
            analysis = "Artifact detection completed with default values."
        )
    }
}
