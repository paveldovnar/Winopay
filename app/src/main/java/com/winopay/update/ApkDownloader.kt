package com.winopay.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads APK files from URL with progress tracking.
 */
object ApkDownloader {
    private const val TAG = "ApkDownloader"

    sealed class DownloadProgress {
        data object Started : DownloadProgress()
        data class Progress(val downloaded: Long, val total: Long) : DownloadProgress()
        data class Success(val file: File) : DownloadProgress()
        data class Error(val message: String) : DownloadProgress()
    }

    /**
     * Download APK from URL.
     *
     * @param context Android context
     * @param url Download URL
     * @param fileName Output file name
     * @return Flow of download progress
     */
    fun downloadApk(
        context: Context,
        url: String,
        fileName: String = "update.apk"
    ): Flow<DownloadProgress> = flow {
        try {
            Log.i(TAG, "DOWNLOAD|START|url=$url")
            emit(DownloadProgress.Started)

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != 200) {
                val error = "HTTP ${connection.responseCode}"
                Log.e(TAG, "DOWNLOAD|HTTP_ERROR|$error")
                emit(DownloadProgress.Error(error))
                return@flow
            }

            val totalSize = connection.contentLength.toLong()
            Log.i(TAG, "DOWNLOAD|SIZE|$totalSize bytes")

            // Save to app cache directory
            val outputFile = File(context.cacheDir, fileName)
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var downloaded: Long = 0
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                emit(DownloadProgress.Progress(downloaded, totalSize))
            }

            outputStream.close()
            inputStream.close()

            Log.i(TAG, "DOWNLOAD|SUCCESS|file=${outputFile.absolutePath}|size=${outputFile.length()}")
            emit(DownloadProgress.Success(outputFile))

        } catch (e: Exception) {
            val error = "${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "DOWNLOAD|ERROR|$error", e)
            emit(DownloadProgress.Error(error))
        }
    }.flowOn(Dispatchers.IO)
}
