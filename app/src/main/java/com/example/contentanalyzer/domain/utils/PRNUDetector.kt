package com.example.contentanalyzer.domain.utils

import android.graphics.Bitmap
import org.opencv.core.Mat

/**
 * JNI Bridge for PRNU detection
 */
object PRNUDetector {

    init {
        System.loadLibrary("prnu")
    }

    @JvmStatic
    external fun nativeExtractNoise(matAddr: Long): Double
    
    @JvmStatic
    external fun nativeGenerateFingerprint(matAddrs: LongArray): Long
    
    @JvmStatic
    external fun nativeComputeNCC(noiseAddr: Long, fingerprintAddr: Long): Double
    
    @JvmStatic
    external fun nativeFallbackAnalysis(matAddr: Long): DoubleArray?

    fun generateFingerprint(bitmaps: List<Bitmap>): Mat? {
        val mats = bitmaps.map { ImageUtils.bitmapToMat(it) }
        val addresses = mats.map { it.nativeObjAddr }.toLongArray()
        val fingerprintAddr = nativeGenerateFingerprint(addresses)
        mats.forEach { it.release() }

        return if (fingerprintAddr != 0L) {
            Mat(fingerprintAddr)
        } else {
            null
        }
    }

    fun computeNCC(bitmap: Bitmap, fingerprint: Mat): Double {
        val imageMat = ImageUtils.bitmapToMat(bitmap)
        val noiseAddr = extractNoiseResidual(imageMat)
        val nccScore = nativeComputeNCC(noiseAddr, fingerprint.nativeObjAddr)
        imageMat.release()
        return nccScore
    }

    fun extractNoiseResidual(mat: Mat): Long {
        val gray = Mat()
        if (mat.channels() == 3) {
            org.opencv.imgproc.Imgproc.cvtColor(mat, gray, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY)
        } else {
            mat.copyTo(gray)
        }

        val grayFloat = Mat()
        gray.convertTo(grayFloat, org.opencv.core.CvType.CV_32F)

        val denoised = Mat()
        org.opencv.imgproc.Imgproc.GaussianBlur(grayFloat, denoised, org.opencv.core.Size(7.0, 7.0), 1.5)

        val noise = Mat()
        org.opencv.core.Core.subtract(grayFloat, denoised, noise)

        val mean = org.opencv.core.Core.mean(noise)
        org.opencv.core.Core.subtract(noise, org.opencv.core.Scalar(mean.`val`[0]), noise)

        return noise.nativeObjAddr
    }
}