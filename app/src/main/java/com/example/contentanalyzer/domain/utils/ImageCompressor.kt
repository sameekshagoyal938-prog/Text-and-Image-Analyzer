package com.example.contentanalyzer.domain.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageCompressor {

    private const val MAX_IMAGE_SIZE_MB = 5
    private const val MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024 * 1024
    private const val TARGET_IMAGE_SIZE_PX = 1024

    suspend fun compressImage(
        contentResolver: ContentResolver,
        uri: Uri
    ): ByteArray {
        val inputStream: InputStream = contentResolver.openInputStream(uri) ?: throw Exception("Unable to open image")
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Scale down if needed
        val scaledBitmap = scaleBitmap(originalBitmap)

        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        var quality = 90
        var compressed = false

        while (!compressed && quality > 50) {
            outputStream.reset()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            if (outputStream.size() <= MAX_IMAGE_SIZE_BYTES) {
                compressed = true
            } else {
                quality -= 10
            }
        }

        scaledBitmap.recycle()
        originalBitmap.recycle()

        return outputStream.toByteArray()
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= TARGET_IMAGE_SIZE_PX && height <= TARGET_IMAGE_SIZE_PX) {
            return bitmap
        }

        val scale = minOf(
            TARGET_IMAGE_SIZE_PX.toFloat() / width,
            TARGET_IMAGE_SIZE_PX.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}