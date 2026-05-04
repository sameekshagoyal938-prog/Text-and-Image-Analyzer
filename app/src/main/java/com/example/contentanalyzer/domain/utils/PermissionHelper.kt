package com.example.contentanalyzer.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionHelper {

    val CAMERA_PERMISSION = Manifest.permission.CAMERA
    val STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            STORAGE_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }
}