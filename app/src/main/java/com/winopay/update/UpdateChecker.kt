package com.winopay.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.winopay.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * In-app update checker for debug/internal builds.
 *
 * SECURITY:
 * - Only enabled for debug builds (BuildConfig.DEBUG)
 * - Downloads APK and verifies SHA-256 before install
 * - Uses Android system installer (user confirmation required)
 * - APK signature must match installed app (enforced by Android)
 *
 * FLOW:
 * 1. Fetch latest.json from self-hosted endpoint
 * 2. Compare versionCode with BuildConfig.VERSION_CODE
 * 3. Download APK to cache directory
 * 4. Verify SHA-256 hash
 * 5. Launch system installer with FileProvider URI
 *
 * LOGGING: UPDATE|CHECK, UPDATE|AVAILABLE, UPDATE|DOWNLOAD, UPDATE|VERIFY_OK, UPDATE|INSTALL_INTENT
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"

    /** Timeout for network requests (ms) */
    private const val NETWORK_TIMEOUT_MS = 10_000L

    /** Retry delay (ms) */
    private const val RETRY_DELAY_MS = 2_000L

    /**
     * Update manifest from endpoint.
     */
    data class UpdateManifest(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val sha256: String
    )

    /**
     * Result of update check.
     */
    sealed class UpdateCheckResult {
        data class UpdateAvailable(val manifest: UpdateManifest) : UpdateCheckResult()
        data object NoUpdateAvailable : UpdateCheckResult()
        data class Error(val message: String) : UpdateCheckResult()
    }

    /**
     * Result of APK download.
     */
    sealed class DownloadResult {
        data class Success(val apkFile: File) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    /**
     * Check if updater is enabled.
     * Only enabled for debug builds.
     */
    fun isUpdateCheckEnabled(): Boolean {
        return BuildConfig.DEBUG
    }

    /**
     * Get update endpoint URL from BuildConfig.
     * Falls back to empty string if not configured.
     */
    private fun getUpdateEndpoint(): String {
        return try {
            // BuildConfig.UPDATE_ENDPOINT will be added in build.gradle
            val field = BuildConfig::class.java.getDeclaredField("UPDATE_ENDPOINT")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Check for updates.
     *
     * @param context Application context
     * @return UpdateCheckResult
     */
    suspend fun checkForUpdate(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isUpdateCheckEnabled()) {
            Log.d(TAG, "UPDATE|CHECK|disabled (not debug build)")
            return@withContext UpdateCheckResult.Error("Update checker disabled")
        }

        val endpoint = getUpdateEndpoint()
        if (endpoint.isBlank()) {
            Log.e(TAG, "UPDATE|CHECK|error=no_endpoint_configured")
            return@withContext UpdateCheckResult.Error("Update endpoint not configured")
        }

        Log.i(TAG, "UPDATE|CHECK|endpoint=$endpoint|currentVersion=${BuildConfig.VERSION_CODE}")

        try {
            // Fetch with timeout and retry
            val manifest = fetchManifestWithRetry(endpoint)

            // Compare version codes
            if (manifest.versionCode > BuildConfig.VERSION_CODE) {
                Log.i(TAG, "UPDATE|AVAILABLE|new=${manifest.versionCode}|current=${BuildConfig.VERSION_CODE}|name=${manifest.versionName}")
                return@withContext UpdateCheckResult.UpdateAvailable(manifest)
            } else {
                Log.d(TAG, "UPDATE|NO_UPDATE|latest=${manifest.versionCode}|current=${BuildConfig.VERSION_CODE}")
                return@withContext UpdateCheckResult.NoUpdateAvailable
            }
        } catch (e: Exception) {
            Log.e(TAG, "UPDATE|CHECK|error=${e.message}", e)
            return@withContext UpdateCheckResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Fetch manifest from endpoint with one retry.
     */
    private suspend fun fetchManifestWithRetry(endpoint: String): UpdateManifest {
        return try {
            fetchManifest(endpoint)
        } catch (e: Exception) {
            Log.w(TAG, "UPDATE|CHECK|fetch_failed, retrying after ${RETRY_DELAY_MS}ms: ${e.message}")
            kotlinx.coroutines.delay(RETRY_DELAY_MS)
            fetchManifest(endpoint)
        }
    }

    /**
     * Fetch and parse manifest JSON.
     */
    private suspend fun fetchManifest(endpoint: String): UpdateManifest = withTimeout(NETWORK_TIMEOUT_MS) {
        val url = URL("$endpoint/releases/latest.json")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = NETWORK_TIMEOUT_MS.toInt()
        connection.readTimeout = NETWORK_TIMEOUT_MS.toInt()

        try {
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                throw Exception("HTTP $responseCode")
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            parseManifest(json)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parse manifest JSON.
     */
    private fun parseManifest(json: String): UpdateManifest {
        val obj = JSONObject(json)
        return UpdateManifest(
            versionCode = obj.getInt("versionCode"),
            versionName = obj.getString("versionName"),
            apkUrl = obj.getString("apkUrl"),
            sha256 = obj.getString("sha256")
        )
    }

    /**
     * Download APK and verify SHA-256.
     *
     * @param context Application context
     * @param manifest Update manifest
     * @param onProgress Progress callback (0-100)
     * @return DownloadResult
     */
    suspend fun downloadAndVerifyApk(
        context: Context,
        manifest: UpdateManifest,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "UPDATE|DOWNLOAD|url=${manifest.apkUrl}|version=${manifest.versionName}")

        try {
            // Download to cache directory
            val cacheDir = File(context.cacheDir, "updates")
            cacheDir.mkdirs()

            val apkFile = File(cacheDir, "update-${manifest.versionName}.apk")

            // Download with progress
            val url = URL(manifest.apkUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            try {
                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    throw Exception("HTTP $responseCode")
                }

                val contentLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = apkFile.outputStream()

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        onProgress(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.i(TAG, "UPDATE|DOWNLOAD|complete|size=${apkFile.length()}")
            } finally {
                connection.disconnect()
            }

            // Verify SHA-256
            Log.i(TAG, "UPDATE|VERIFY|start")
            val actualHash = calculateSha256(apkFile)
            val expectedHash = manifest.sha256.lowercase()

            if (actualHash != expectedHash) {
                Log.e(TAG, "UPDATE|VERIFY|error=hash_mismatch|expected=$expectedHash|actual=$actualHash")
                apkFile.delete()
                return@withContext DownloadResult.Error("SHA-256 verification failed")
            }

            Log.i(TAG, "UPDATE|VERIFY_OK|sha256=$actualHash")
            return@withContext DownloadResult.Success(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "UPDATE|DOWNLOAD|error=${e.message}", e)
            return@withContext DownloadResult.Error(e.message ?: "Download failed")
        }
    }

    /**
     * Calculate SHA-256 hash of file.
     */
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }

        inputStream.close()

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Launch system installer for APK.
     *
     * SECURITY: Uses FileProvider to expose APK to system installer.
     * User must confirm installation (Android enforces signature match).
     *
     * @param context Application context
     * @param apkFile APK file to install
     */
    fun launchInstaller(context: Context, apkFile: File) {
        Log.i(TAG, "UPDATE|INSTALL_INTENT|file=${apkFile.name}")

        try {
            // Use FileProvider to expose APK
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            // Create install intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "UPDATE|INSTALL_INTENT|error=${e.message}", e)
        }
    }

    /**
     * Clean up old downloaded APKs.
     */
    fun cleanupOldApks(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "updates")
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("update-") && file.name.endsWith(".apk")) {
                    file.delete()
                    Log.d(TAG, "UPDATE|CLEANUP|deleted=${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "UPDATE|CLEANUP|error=${e.message}")
        }
    }
}
