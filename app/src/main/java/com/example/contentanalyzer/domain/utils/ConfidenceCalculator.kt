package com.example.contentanalyzer.domain.utils

import android.util.Log
import kotlin.math.*

class ConfidenceCalculator {

    /**
     * Legacy instance method for backward compatibility
     */
    fun calculateConfidence(aiScore: Float, humanScore: Float): ConfidenceResult {
        val probabilities = mapOf("AI" to aiScore, "Human" to humanScore)
        return calculateConfidence(probabilities, "")
    }

    companion object {
        private const val TAG = "ConfidenceCalculator"

        /**
         * Calculate calibrated confidence score from model probabilities
         */
        fun calculateConfidence(
            probabilities: Map<String, Float>,
            text: String
        ): ConfidenceResult {

            val (highestProb, secondHighestProb, margin) = extractProbabilityMetrics(probabilities)
            val numClasses = probabilities.size

            val baseConfidence = if (numClasses == 2) {
                calculateBinaryConfidence(highestProb, margin)
            } else {
                calculateMultiClassConfidence(probabilities)
            }

            val temperature = calculateOptimalTemperature(text)
            val scaledConfidence = temperatureScaling(baseConfidence, temperature)

            val lengthPenalty = calculateLengthPenalty(text)
            val lengthAdjusted = scaledConfidence * (1f - lengthPenalty)

            val marginBoost = calculateMarginBoost(margin)
            val finalConfidence = (lengthAdjusted + marginBoost).coerceIn(0f, 1f)

            return ConfidenceResult(
                confidence = finalConfidence,
                rawProbability = highestProb,
                margin = margin,
                temperatureUsed = temperature,
                calibrationFactors = CalibrationFactors(
                    baseConfidence = baseConfidence,
                    temperatureScaled = scaledConfidence,
                    lengthAdjusted = lengthAdjusted,
                    marginBoost = marginBoost
                )
            )
        }

        private fun calculateBinaryConfidence(highestProb: Float, margin: Float): Float {
            val distanceFromHalf = abs(highestProb - 0.5f) * 2f
            return (highestProb * 0.7f + distanceFromHalf * 0.3f).coerceIn(0f, 1f)
        }

        private fun calculateMultiClassConfidence(probabilities: Map<String, Float>): Float {
            val sorted = probabilities.values.sortedDescending()
            val highest = sorted[0]
            val second = sorted.getOrElse(1) { 0f }
            val margin = highest - second
            return (highest * 0.8f + margin * 0.2f).coerceIn(0f, 1f)
        }

        private fun extractProbabilityMetrics(probabilities: Map<String, Float>): ProbabilityMetrics {
            if (probabilities.isEmpty()) return ProbabilityMetrics(0f, 0f, 0f)
            val sorted = probabilities.values.sortedDescending()
            val highest = sorted[0]
            val secondHighest = sorted.getOrElse(1) { 0f }
            val margin = highest - secondHighest
            return ProbabilityMetrics(highest, secondHighest, margin)
        }

        private fun temperatureScaling(probability: Float, temperature: Float): Float {
            if (temperature <= 0) return probability
            val logit = ln(probability / (1 - probability + 1e-8f))
            val scaledLogit = logit / temperature
            return (1f / (1f + exp(-scaledLogit))).coerceIn(0f, 1f)
        }

        private fun calculateOptimalTemperature(text: String): Float {
            val wordCount = text.split(" ").filter { it.isNotBlank() }.size
            return when {
                wordCount == 0 -> 1.0f
                wordCount < 30 -> 2.0f
                wordCount < 100 -> 1.5f
                wordCount < 300 -> 1.2f
                else -> 1.0f
            }
        }

        private fun calculateLengthPenalty(text: String): Float {
            val wordCount = text.split(" ").filter { it.isNotBlank() }.size
            return when {
                wordCount == 0 -> 0.0f
                wordCount < 20 -> 0.4f
                wordCount < 50 -> 0.2f
                wordCount < 100 -> 0.1f
                else -> 0.0f
            }
        }

        private fun calculateMarginBoost(margin: Float): Float {
            return when {
                margin > 0.8f -> 0.05f
                margin > 0.6f -> 0.03f
                else -> 0.0f
            }
        }

        fun getConfidenceLevel(confidence: Float): ConfidenceLevel {
            return when {
                confidence >= 0.8 -> ConfidenceLevel.HIGH
                confidence >= 0.6 -> ConfidenceLevel.MEDIUM
                confidence >= 0.4 -> ConfidenceLevel.LOW
                else -> ConfidenceLevel.VERY_LOW
            }
        }
    }

    data class ProbabilityMetrics(
        val highestProb: Float,
        val secondHighestProb: Float,
        val margin: Float
    )

    data class ConfidenceResult(
        val confidence: Float,
        val rawProbability: Float,
        val margin: Float,
        val temperatureUsed: Float,
        val calibrationFactors: CalibrationFactors
    ) {
        val confidencePercentage: Int get() = (confidence * 100).toInt()
        val confidenceLevel: ConfidenceLevel get() = ConfidenceCalculator.getConfidenceLevel(confidence)
        val isCertain: Boolean get() = margin > 0.15f
    }

    data class CalibrationFactors(
        val baseConfidence: Float,
        val temperatureScaled: Float,
        val lengthAdjusted: Float,
        val marginBoost: Float
    )

    enum class ConfidenceLevel {
        VERY_HIGH, // Added for compatibility with ScoreCalculator.kt
        HIGH,
        MEDIUM,
        LOW,
        VERY_LOW
    }
}
