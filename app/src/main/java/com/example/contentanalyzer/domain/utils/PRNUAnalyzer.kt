package com.example.contentanalyzer.domain.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Scientific PRNU-based Image Authenticity Analyzer
 */
class PRNUAnalyzer {

    companion object {
        private const val TAG = "PRNUAnalyzer"
    }

    data class PRNUResult(
        val prnuValue: Double,
        val interpretation: String,
        val status: String,
        val confidence: Double,
        val methodUsed: String,
        val fingerprintAvailable: Boolean,
        val analysis: String = ""
    )

    private var referenceFingerprint: org.opencv.core.Mat? = null

    suspend fun setReferenceFingerprint(bitmaps: List<Bitmap>) {
        withContext(Dispatchers.IO) {
            try {
                referenceFingerprint = PRNUDetector.generateFingerprint(bitmaps)
                Log.d(TAG, "Reference fingerprint ${if (referenceFingerprint != null) "generated" else "failed"}")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating fingerprint", e)
            }
        }
    }

    suspend fun analyze(
        contentResolver: ContentResolver,
        uri: Uri
    ): PRNUResult {
        return withContext(Dispatchers.IO) {
            val bitmap = ImageUtils.loadBitmapFromUri(contentResolver, uri)
            if (bitmap == null) {
                return@withContext createErrorResult("Failed to load image")
            }

            val result = if (referenceFingerprint != null) {
                analyzeWithPRNU(bitmap)
            } else {
                analyzeWithFallback(bitmap)
            }

            bitmap.recycle()
            result
        }
    }

    private fun analyzeWithPRNU(bitmap: Bitmap): PRNUResult {
        return try {
            val nccScore: Double = PRNUDetector.computeNCC(bitmap, referenceFingerprint!!)
            
            val interpretation: String
            val status: String
            val confidence: Double

            when {
                nccScore >= 0.015 -> {
                    interpretation = "Human"
                    status = "MATCH FOUND"
                    confidence = (65.0 + (nccScore - 0.015) * 80.0).coerceIn(65.0, 95.0)
                }
                nccScore >= 0.008 -> {
                    interpretation = "Inconclusive"
                    status = "WEAK MATCH"
                    confidence = (50.0 + ((nccScore - 0.008) / 0.007 * 20.0)).coerceIn(50.0, 70.0)
                }
                nccScore > 0.001 -> {
                    interpretation = "AI"
                    status = "NO MATCH"
                    confidence = (65.0 + (0.008 - nccScore) * 200.0).coerceIn(60.0, 85.0)
                }
                else -> {
                    interpretation = "No Pattern"
                    status = "NOT DETECTED"
                    confidence = 90.0
                }
            }

            PRNUResult(
                prnuValue = nccScore,
                interpretation = interpretation,
                status = status,
                confidence = confidence,
                methodUsed = "PRNU",
                fingerprintAvailable = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in PRNU analysis", e)
            createErrorResult("PRNU Error: ${e.message}")
        }
    }

    private fun analyzeWithFallback(bitmap: Bitmap): PRNUResult {
        return try {
            val mat = ImageUtils.bitmapToMat(bitmap)
            val resultArr = PRNUDetector.nativeFallbackAnalysis(mat.nativeObjAddr)
            mat.release()

            val confidence = resultArr?.getOrNull(0) ?: 50.0

            val (interpretation, status) = when {
                confidence >= 65.0 -> Pair("Human", "LIKELY HUMAN")
                confidence <= 40.0 -> Pair("AI", "LIKELY AI")
                else -> Pair("Inconclusive", "UNCERTAIN")
            }

            PRNUResult(
                prnuValue = 0.0,
                interpretation = interpretation,
                status = status,
                confidence = confidence,
                methodUsed = "FALLBACK",
                fingerprintAvailable = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback analysis", e)
            createErrorResult("Fallback Error: ${e.message}")
        }
    }

    private fun createErrorResult(message: String): PRNUResult {
        return PRNUResult(
            prnuValue = 0.0,
            interpretation = "Inconclusive",
            status = "ERROR",
            confidence = 0.0,
            methodUsed = "ERROR",
            fingerprintAvailable = false,
            analysis = message
        )
    }

    fun toJson(result: PRNUResult): String {
        val json = JSONObject()
        json.put("classification", result.interpretation)
        json.put("ncc_score", result.prnuValue)
        json.put("confidence_score", result.confidence)
        json.put("method_used", result.methodUsed)
        return json.toString(2)
    }
}