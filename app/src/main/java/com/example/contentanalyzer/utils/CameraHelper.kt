package com.example.contentanalyzer.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.contentanalyzer.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CameraHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val PHOTO_FILE_PREFIX = "JPEG_"
    private const val PHOTO_FILE_SUFFIX = ".jpg"
    private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Create a temporary file for camera capture
     */
    fun createTempImageFile(context: Context): File? {
        return try {
            val timeStamp = DATE_FORMAT.format(Date())
            val fileName = "$PHOTO_FILE_PREFIX$timeStamp$PHOTO_FILE_SUFFIX"
            val storageDir = context.cacheDir
            File(storageDir, fileName).apply {
                createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get URI for the captured image file
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}$AUTHORITY_SUFFIX",
            file
        )
    }

    /**
     * Delete temporary image file
     */
    fun deleteTempImageFile(file: File?) {
        try {
            file?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get image size in bytes
     */
    fun getImageSize(contentResolver: ContentResolver, uri: Uri): Long {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.available().toLong()
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}