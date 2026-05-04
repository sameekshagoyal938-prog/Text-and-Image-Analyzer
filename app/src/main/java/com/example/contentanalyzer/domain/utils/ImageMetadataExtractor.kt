package com.example.contentanalyzer.domain.utils

import android.content.ContentResolver
import android.media.ExifInterface
import android.net.Uri
import android.util.Log

object MetadataExtractor {

    private const val TAG = "MetadataExtractor"

    data class MetadataInfo(
        val hasExif: Boolean,
        val cameraModel: String?,
        val cameraMake: String?,
        val software: String?,
        val editingSoftwareDetected: Boolean,
        val dateTime: String?,
        val hasGPS: Boolean,
        val isRealPhoto: Boolean,      // New: strong real photo indicator
        val realPhotoConfidence: Float  // New: 0-1 confidence for real photo
    )

    suspend fun extractAndAnalyze(
        contentResolver: ContentResolver,
        uri: Uri
    ): MetadataInfo {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.w(TAG, "Cannot open input stream")
                return createEmptyMetadata()
            }

            val exif = ExifInterface(inputStream)
            inputStream.close()

            val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
            val cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE)
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)

            val hasExif = cameraModel != null || cameraMake != null || software != null || dateTime != null

            val hasGPS = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null &&
                    exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null

            val editingSoftwareDetected = software != null &&
                    !software.contains("Camera", ignoreCase = true) &&
                    !software.contains("Android", ignoreCase = true)

            // Determine if this is a real photo based on strong indicators
            val isRealPhoto = hasStrongRealPhotoIndicators(cameraModel, cameraMake, editingSoftwareDetected)

            // Calculate real photo confidence
            val realPhotoConfidence = calculateRealPhotoConfidence(
                cameraModel = cameraModel,
                cameraMake = cameraMake,
                editingSoftwareDetected = editingSoftwareDetected,
                hasGPS = hasGPS,
                hasExif = hasExif
            )

            Log.d(TAG, "Metadata: hasExif=$hasExif, camera=$cameraModel, make=$cameraMake, software=$software, isRealPhoto=$isRealPhoto, realConfidence=${"%.2f".format(realPhotoConfidence)}")

            MetadataInfo(
                hasExif = hasExif,
                cameraModel = cameraModel,
                cameraMake = cameraMake,
                software = software,
                editingSoftwareDetected = editingSoftwareDetected,
                dateTime = dateTime,
                hasGPS = hasGPS,
                isRealPhoto = isRealPhoto,
                realPhotoConfidence = realPhotoConfidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata", e)
            createEmptyMetadata()
        }
    }

    private fun hasStrongRealPhotoIndicators(
        cameraModel: String?,
        cameraMake: String?,
        editingSoftwareDetected: Boolean
    ): Boolean {
        // Real camera detected
        val hasRealCamera = cameraModel != null || cameraMake != null

        return hasRealCamera && !editingSoftwareDetected
    }

    private fun calculateRealPhotoConfidence(
        cameraModel: String?,
        cameraMake: String?,
        editingSoftwareDetected: Boolean,
        hasGPS: Boolean,
        hasExif: Boolean
    ): Float {
        var confidence = 0.5f

        // Strong boost for camera metadata
        if (cameraModel != null) {
            confidence += 0.35f
            Log.d(TAG, "Camera model present: +0.35")
        }
        if (cameraMake != null) {
            confidence += 0.20f
            Log.d(TAG, "Camera make present: +0.20")
        }

        // GPS boost
        if (hasGPS) {
            confidence += 0.15f
            Log.d(TAG, "GPS present: +0.15")
        }

        // Penalize editing software
        if (editingSoftwareDetected) {
            confidence -= 0.25f
            Log.d(TAG, "Editing software detected: -0.25")
        }

        // Penalize missing EXIF
        if (!hasExif) {
            confidence -= 0.20f
            Log.d(TAG, "Missing EXIF: -0.20")
        }

        return confidence.coerceIn(0f, 1f)
    }

    private fun createEmptyMetadata(): MetadataInfo {
        return MetadataInfo(
            hasExif = false,
            cameraModel = null,
            cameraMake = null,
            software = null,
            editingSoftwareDetected = false,
            dateTime = null,
            hasGPS = false,
            isRealPhoto = false,
            realPhotoConfidence = 0.3f
        )
    }

    fun calculateMetadataScore(metadata: MetadataInfo): Float {
        // Lower score = more likely real photo
        // Higher score = more likely AI
        return 1f - metadata.realPhotoConfidence
    }
}
