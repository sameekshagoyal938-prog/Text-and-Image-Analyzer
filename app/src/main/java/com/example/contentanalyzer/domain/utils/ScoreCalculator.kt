package com.example.contentanalyzer.domain.utils

import android.util.Log
import com.example.contentanalyzer.data.model.HuggingFaceResponse
import com.example.contentanalyzer.data.model.ZeroShotResponse
import com.example.contentanalyzer.domain.models.AnalysisResult
import kotlin.math.*

class ScoreCalculator {

    companion object {
        private const val TAG = "ScoreCalculator"

        fun calculateHuggingFaceScores(
            predictions: List<HuggingFaceResponse>,
            text: String
        ): AnalysisResult {
            val probabilities = mutableMapOf<String, Float>()
            predictions.forEach { 
                probabilities[it.label ?: "unknown"] = it.score ?: 0f
            }

            val aiScore = probabilities["ChatGPT"] ?: probabilities["label"] ?: probabilities["LABEL_1"] ?: 0.5f
            val humanScore = probabilities["Human"] ?: probabilities["LABEL_0"] ?: (1f - aiScore)
            
            val aiPercentage = (aiScore * 100).toInt()
            val humanPercentage = (humanScore * 100).toInt()
            
            val confidenceCalculator = ConfidenceCalculator()
            val confidenceResult = confidenceCalculator.calculateConfidence(aiScore, humanScore)

            val classification = AIThresholdClassifier.classify(aiScore, text, confidenceResult.margin)

            return AnalysisResult(
                resultText = classification.message,
                confidenceScore = confidenceResult.confidencePercentage,
                confidenceLevel = confidenceResult.confidenceLevel,
                isCertain = confidenceResult.isCertain,
                aiPercentage = aiPercentage,
                humanPercentage = humanPercentage,
                classificationType = if (aiScore > humanScore) "AI Generated" else "Human Written",
                recommendation = classification.recommendation
            )
        }

        fun calculateZeroShotScores(
            response: List<ZeroShotResponse>,
            text: String
        ): AnalysisResult {
            if (response.isEmpty()) return calculateHeuristicScores(text)
            
            val first = response[0]
            val labels = first.labels ?: emptyList()
            val scores = first.scores ?: emptyList()
            
            val results = labels.zip(scores).toMap()
            
            val aiScore = results["AI generated text"] ?: 0.5f
            val humanScore = results["Human written text"] ?: 0.5f

            val aiPercentage = (aiScore * 100).toInt()
            val humanPercentage = (humanScore * 100).toInt()
            
            val confidenceCalculator = ConfidenceCalculator()
            val confidenceResult = confidenceCalculator.calculateConfidence(aiScore, humanScore)

            val classification = AIThresholdClassifier.classify(aiScore, text, confidenceResult.margin)

            return AnalysisResult(
                resultText = classification.message,
                confidenceScore = confidenceResult.confidencePercentage,
                confidenceLevel = confidenceResult.confidenceLevel,
                isCertain = confidenceResult.isCertain,
                aiPercentage = aiPercentage,
                humanPercentage = humanPercentage,
                classificationType = if (aiScore > humanScore) "AI Generated" else "Human Written",
                recommendation = classification.recommendation
            )
        }

        fun calculateHeuristicScores(text: String): AnalysisResult {
            val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.isEmpty()) {
                return AnalysisResult(
                    resultText = "No text provided for analysis.",
                    confidenceScore = 100,
                    confidenceLevel = ConfidenceCalculator.ConfidenceLevel.VERY_HIGH,
                    isCertain = true,
                    aiPercentage = 0,
                    humanPercentage = 100,
                    classificationType = "Human Written",
                    recommendation = "Please provide some text to analyze."
                )
            }
            
            val avgWordLength = words.map { it.length }.average()
            val uniqueWords = words.distinct().size.toDouble() / words.size
            
            // Simple heuristic
            val aiProb = ((1.0 - uniqueWords) * 0.5 + (if (avgWordLength > 5) 0.2 else 0.0)).coerceIn(0.0, 1.0).toFloat()
            val humanProb = 1f - aiProb
            
            val confidenceCalculator = ConfidenceCalculator()
            val confidenceResult = confidenceCalculator.calculateConfidence(aiProb, humanProb)
            
            val classification = AIThresholdClassifier.classify(aiProb, text, confidenceResult.margin)
            
            return AnalysisResult(
                resultText = classification.message,
                confidenceScore = confidenceResult.confidencePercentage,
                confidenceLevel = confidenceResult.confidenceLevel,
                isCertain = confidenceResult.isCertain,
                aiPercentage = (aiProb * 100).toInt(),
                humanPercentage = (humanProb * 100).toInt(),
                classificationType = if (aiProb > humanProb) "AI Generated" else "Human Written",
                recommendation = classification.recommendation
            )
        }
    }
}
