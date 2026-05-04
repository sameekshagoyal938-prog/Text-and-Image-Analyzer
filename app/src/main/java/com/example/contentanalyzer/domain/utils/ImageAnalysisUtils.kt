package com.example.contentanalyzer.data.utils

import android.util.Log
import kotlin.math.abs

/**
 * BALANCED VISUAL ANALYSIS
 * Proper thresholds for real vs AI detection
 */
object ImageAnalysisUtils {

    private const val TAG = "ImageAnalysisUtils"

    // Balanced weights - favoring real images slightly
    private const val WEIGHT_MODEL = 0.55f
    private const val WEIGHT_HEURISTICS = 0.45f

    // CRITICAL: Proper thresholds for real image detection
    private const val REAL_IMAGE_THRESHOLD = 0.45f   // 45% real confidence = Real Image
    private const val AI_IMAGE_THRESHOLD = 0.60f     // 60% AI confidence = AI Generated
    // 45-60% zone = lean toward Real Image (to avoid false AI detection)

    data class VisualAnalysisResult(
        val result: String,
        val confidence: Int,
        val aiScore: Float,
        val realScore: Float
    )

    fun analyzeVisualOnly(
        modelScore: Float,
        heuristicScore: Float
    ): VisualAnalysisResult {

        // Calculate scores
        var aiScore = (modelScore * WEIGHT_MODEL + heuristicScore * WEIGHT_HEURISTICS)
        aiScore = aiScore.coerceIn(0f, 1f)
        var realScore = 1f - aiScore

        // Apply slight bias toward real images (to fix false AI detection)
        realScore = (realScore + 0.10f).coerceAtMost(0.95f)
        aiScore = 1f - realScore

        val margin = abs(aiScore - realScore)

        // Determine result with proper thresholds
        val result = when {
            realScore >= REAL_IMAGE_THRESHOLD -> "Real Image"
            aiScore >= AI_IMAGE_THRESHOLD -> "AI Generated"
            else -> "Real Image"  // Default to Real when uncertain
        }

        // Calculate confidence
        var confidence = (margin * 100).toInt()
        confidence = confidence.coerceIn(50, 95)

        Log.d(TAG, """
            ========== BALANCED ANALYSIS ==========
            Model Score: ${"%.2f".format(modelScore)}
            Heuristic Score: ${"%.2f".format(heuristicScore)}
            Raw AI: ${"%.2f".format(aiScore)}
            Raw Real: ${"%.2f".format(realScore)}
            Margin: ${"%.2f".format(margin)}
            Result: $result
            Confidence: $confidence%
            =======================================
        """.trimIndent())

        return VisualAnalysisResult(
            result = result,
            confidence = confidence,
            aiScore = aiScore,
            realScore = realScore
        )
    }
}