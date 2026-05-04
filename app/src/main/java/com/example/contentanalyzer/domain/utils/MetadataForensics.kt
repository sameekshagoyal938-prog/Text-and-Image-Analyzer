package com.example.contentanalyzer.domain.utils

import android.content.ContentResolver
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataForensics @Inject constructor() {

    companion object {
        private const val TAG = "MetadataForensics"

        private val AI_TOOL_SIGNATURES = listOf(
            "Midjourney", "DALL·E", "Stable Diffusion", "Firefly", "Leonardo",
            "RunwayML", "Generative AI", "AI Generated", "Craiyon", "NightCafe"
        )

        private val EDITING_SOFTWARE = listOf(
            "Photoshop", "Lightroom", "GIMP", "Affinity", "Pixelmator", "Capture One"
        )
    }

    data class MetadataResult(
        val status: String,
        val confidence: Float,
        val aiToolsDetected: List<String>,
        val editingSoftwareDetected: List<String>,
        val cameraInfo: String?,
        val hasTimestamp: Boolean,
        val hasGPS: Boolean,
        val analysis: String
    )

    suspend fun analyze(contentResolver: ContentResolver, uri: Uri): MetadataResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext createNotAvailableResult()
                }

                val exif = ExifInterface(inputStream)
                inputStream.close()

                val cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE)
                val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
                val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                val hasGPS = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null &&
                        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null

                val hasAnyMetadata = cameraMake != null || cameraModel != null ||
                        software != null || dateTime != null || hasGPS

                if (!hasAnyMetadata) {
                    return@withContext createNotAvailableResult()
                }

                val aiToolsDetected = mutableListOf<String>()
                val editingSoftwareDetected = mutableListOf<String>()

                if (software != null) {
                    AI_TOOL_SIGNATURES.forEach { tool ->
                        if (software.contains(tool, ignoreCase = true)) {
                            aiToolsDetected.add(tool)
                        }
                    }
                    EDITING_SOFTWARE.forEach { tool ->
                        if (software.contains(tool, ignoreCase = true)) {
                            editingSoftwareDetected.add(tool)
                        }
                    }
                }

                var confidence = 0f
                var confidenceFactors = 0

                val cameraInfo = when {
                    cameraMake != null && cameraModel != null -> "$cameraMake $cameraModel"
                    cameraMake != null -> cameraMake
                    cameraModel != null -> cameraModel
                    else -> null
                }

                if (cameraInfo != null) {
                    confidence += 70f
                    confidenceFactors++
                }
                if (dateTime != null) {
                    confidence += 15f
                    confidenceFactors++
                }
                if (hasGPS) {
                    confidence += 15f
                    confidenceFactors++
                }

                if (aiToolsDetected.isNotEmpty()) {
                    confidence -= 40f
                }

                if (editingSoftwareDetected.isNotEmpty()) {
                    confidence -= 15f
                }

                confidence = if (confidenceFactors > 0) {
                    confidence / confidenceFactors
                } else {
                    0f
                }
                confidence = confidence.coerceIn(0f, 100f)

                val analysis = buildString {
                    if (cameraInfo != null) {
                        append("Camera detected: $cameraInfo. ")
                    }
                    if (dateTime != null) {
                        append("Timestamp: $dateTime. ")
                    }
                    if (hasGPS) {
                        append("GPS coordinates available. ")
                    }
                    if (aiToolsDetected.isNotEmpty()) {
                        append("⚠️ AI generation tools detected: ${aiToolsDetected.joinToString(", ")}. ")
                    }
                    if (editingSoftwareDetected.isNotEmpty()) {
                        append("Editing software detected: ${editingSoftwareDetected.joinToString(", ")}. ")
                    }
                }

                MetadataResult(
                    status = "Available",
                    confidence = confidence,
                    aiToolsDetected = aiToolsDetected,
                    editingSoftwareDetected = editingSoftwareDetected,
                    cameraInfo = cameraInfo,
                    hasTimestamp = dateTime != null,
                    hasGPS = hasGPS,
                    analysis = analysis.ifEmpty { "Basic metadata present." }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Metadata analysis failed", e)
                createNotAvailableResult()
            }
        }
    }

    private fun createNotAvailableResult(): MetadataResult {
        return MetadataResult(
            status = "Not Available",
            confidence = 0f,
            aiToolsDetected = emptyList(),
            editingSoftwareDetected = emptyList(),
            cameraInfo = null,
            hasTimestamp = false,
            hasGPS = false,
            analysis = "No EXIF metadata available."
        )
    }
}
