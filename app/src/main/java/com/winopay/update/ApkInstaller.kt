package com.winopay.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Installs APK files using Android PackageInstaller.
 *
 * Requires REQUEST_INSTALL_PACKAGES permission (Android 8.0+).
 */
object ApkInstaller {
    private const val TAG = "ApkInstaller"

    /**
     * Install APK file.
     *
     * Opens system installer UI for user confirmation.
     *
     * @param context Android context
     * @param apkFile APK file to install
     * @return true if installer was launched successfully
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "INSTALL|ERROR|file_not_found|${apkFile.absolutePath}")
                return false
            }

            Log.i(TAG, "INSTALL|START|file=${apkFile.absolutePath}|size=${apkFile.length()}")

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+: Use FileProvider
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                // Pre-Nougat: Direct file URI
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            Log.i(TAG, "INSTALL|LAUNCHED")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "INSTALL|ERROR|${e.javaClass.simpleName}|${e.message}", e)
            return false
        }
    }

    /**
     * Check if app can install APKs.
     *
     * On Android 8.0+, requires REQUEST_INSTALL_PACKAGES permission.
     */
    fun canInstallApks(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true  // Pre-Oreo: always allowed
        }
    }

    /**
     * Open settings to allow app to install APKs.
     */
    fun openInstallSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
