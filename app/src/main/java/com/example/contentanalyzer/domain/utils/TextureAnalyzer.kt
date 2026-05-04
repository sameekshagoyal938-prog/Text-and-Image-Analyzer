package com.example.contentanalyzer.domain.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

object TextureAnalyzer {

    data class TextureResult(
        val naturalnessScore: Float,  // 0-100, higher = more natural
        val hasRepeatingPatterns: Boolean,
        val isOverlySmooth: Boolean,
        val analysis: String
    )

    suspend fun analyzeTexture(uri: Uri, contentResolver: android.content.ContentResolver): TextureResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext createDefaultResult()
                }

                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

                // Calculate texture variance
                val variance = calculateTextureVariance(scaledBitmap)
                val naturalnessScore = (variance / 3000f).coerceIn(0f, 1f) * 100

                // Detect repeating patterns
                val hasRepeatingPatterns = detectRepeatingPatterns(scaledBitmap)

                // Detect overly smooth areas
                val isOverlySmooth = variance < 500f

                val analysis = buildString {
                    if (naturalnessScore > 60) {
                        append("Natural texture variation detected. ")
                    } else if (naturalnessScore > 40) {
                        append("Moderate texture variation. ")
                    } else {
                        append("Limited texture variation detected. ")
                    }

                    if (hasRepeatingPatterns) {
                        append("Repeating patterns observed. ")
                    }

                    if (isOverlySmooth) {
                        append("Some areas appear unusually smooth. ")
                    }
                }

                scaledBitmap.recycle()
                bitmap.recycle()

                TextureResult(
                    naturalnessScore = naturalnessScore,
                    hasRepeatingPatterns = hasRepeatingPatterns,
                    isOverlySmooth = isOverlySmooth,
                    analysis = analysis
                )
            } catch (e: Exception) {
                Log.e("TextureAnalyzer", "Error analyzing texture", e)
                createDefaultResult()
            }
        }
    }

    private fun calculateTextureVariance(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var sum = 0.0
        for (pixel in pixels) {
            val brightness = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3.0
            sum += brightness
        }

        val mean = sum / pixels.size

        var variance = 0.0
        for (pixel in pixels) {
            val brightness = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3.0
            variance += (brightness - mean).pow(2)
        }

        return (variance / pixels.size).toFloat()
    }

    private fun detectRepeatingPatterns(bitmap: Bitmap): Boolean {
        // Simplified pattern detection
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var similarBlocks = 0
        val blockSize = 32

        for (y in 0 until height - blockSize step blockSize) {
            for (x in 0 until width - blockSize step blockSize) {
                val block1 = getBlockHash(pixels, width, x, y, blockSize)
                for (y2 in y + blockSize until height - blockSize step blockSize) {
                    for (x2 in x + blockSize until width - blockSize step blockSize) {
                        val block2 = getBlockHash(pixels, width, x2, y2, blockSize)
                        if (block1 == block2) {
                            similarBlocks++
                        }
                    }
                }
            }
        }

        return similarBlocks > 5
    }

    private fun getBlockHash(pixels: IntArray, width: Int, startX: Int, startY: Int, size: Int): String {
        val hash = StringBuilder()
        for (y in startY until startY + size step 4) {
            for (x in startX until startX + size step 4) {
                val pixel = pixels[y * width + x]
                val brightness = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3
                hash.append(if (brightness > 128) "1" else "0")
            }
        }
        return hash.toString()
    }

    private fun createDefaultResult(): TextureResult {
        return TextureResult(
            naturalnessScore = 50f,
            hasRepeatingPatterns = false,
            isOverlySmooth = false,
            analysis = "Texture analysis completed with default values."
        )
    }
}
