package com.example.contentanalyzer.data.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.InputStream

/**
 * Image utility functions for Bitmap-Mat conversion
 */
object ImageUtils {

    /**
     * Convert Android Bitmap to OpenCV Mat
     */
    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    /**
     * Convert OpenCV Mat to Android Bitmap
     */
    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    /**
     * Load Bitmap from URI
     */
    fun loadBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load and resize image for PRNU analysis
     */
    fun loadResizedMat(contentResolver: ContentResolver, uri: Uri, maxSize: Int = 800): Mat? {
        val bitmap = loadBitmapFromUri(contentResolver, uri) ?: return null

        // Resize if too large
        val width = bitmap.width
        val height = bitmap.height
        val maxDimension = maxOf(width, height)

        val resizedBitmap = if (maxDimension > maxSize) {
            val scale = maxSize.toFloat() / maxDimension
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val mat = bitmapToMat(resizedBitmap)

        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        bitmap.recycle()

        return mat
    }

    /**
     * Convert to grayscale Mat
     */
    fun toGrayscale(mat: Mat): Mat {
        val gray = Mat()
        if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            mat.copyTo(gray)
        }
        return gray
    }
}