package com.winopay.update

import android.content.Context
import android.util.Log
import com.winopay.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_BASE = "https://api.github.com"
    private const val GITHUB_REPO = "paveldovnar/Winopay"

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val fileSize: Long,
        val releaseNotes: String
    )

    suspend fun checkForUpdate(
        botToken: String,
        chatUsername: String
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "UPDATE|CHECK|START|current=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")

            val latestRelease = getLatestGitHubRelease() ?: run {
                Log.i(TAG, "UPDATE|CHECK|NONE|no_release_found")
                return@withContext null
            }

            if (latestRelease.versionCode > BuildConfig.VERSION_CODE) {
                Log.i(TAG, "UPDATE|CHECK|AVAILABLE|new=${latestRelease.versionName}(${latestRelease.versionCode})")
                return@withContext latestRelease
            } else {
                Log.i(TAG, "UPDATE|CHECK|UP_TO_DATE")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(TAG, "UPDATE|CHECK|ERROR|${e.javaClass.simpleName}|${e.message}", e)
            return@withContext null
        }
    }

    private fun getLatestGitHubRelease(): UpdateInfo? {
        try {
            Log.d(TAG, "UPDATE|GITHUB|fetching_releases")
            val url = "$GITHUB_API_BASE/repos/$GITHUB_REPO/releases"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "WinoPay-UpdateChecker")

            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val releases = org.json.JSONArray(response)

            Log.d(TAG, "UPDATE|GITHUB|found_releases=${releases.length()}")

            // Build variant name: e.g., "mainnetDebug" or "devnetDebug"
            val currentVariant = BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() }
            Log.d(TAG, "UPDATE|GITHUB|looking_for=$currentVariant")

            // Find latest release matching current build variant
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val tag = release.getString("tag_name")
                val releaseName = release.optString("name", "")
                val body = release.optString("body", "")

                Log.d(TAG, "UPDATE|GITHUB|checking|tag=$tag|name=$releaseName")

                // Skip if not matching current build variant
                if (!tag.contains(currentVariant) && !releaseName.contains(currentVariant)) {
                    Log.d(TAG, "UPDATE|GITHUB|skip|not_$currentVariant")
                    continue
                }

                // Parse version from tag or name
                val versionInfo = parseVersionFromText("$tag $releaseName $body")
                if (versionInfo == null) {
                    Log.w(TAG, "UPDATE|GITHUB|skip|no_version")
                    continue
                }

                // Find APK asset
                val assets = release.getJSONArray("assets")
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val assetName = asset.getString("name")

                    if (assetName.endsWith(".apk")) {
                        val downloadUrl = asset.getString("browser_download_url")
                        val fileSize = asset.getLong("size")

                        Log.i(TAG, "UPDATE|GITHUB|found|version=${versionInfo.first}(${versionInfo.second})|url=$downloadUrl")

                        return UpdateInfo(
                            versionName = versionInfo.first,
                            versionCode = versionInfo.second,
                            downloadUrl = downloadUrl,
                            fileSize = fileSize,
                            releaseNotes = body
                        )
                    }
                }
            }

            Log.w(TAG, "UPDATE|GITHUB|no_matching_release")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "UPDATE|GITHUB|ERROR|${e.message}", e)
            return null
        }
    }

    private fun parseVersionFromText(text: String): Pair<String, Int>? {
        try {
            val versionNameRegex = """v([\d.]+)""".toRegex()
            val versionCodeRegex = """\((\d+)\)""".toRegex()

            val versionName = versionNameRegex.find(text)?.groupValues?.get(1) ?: return null
            val versionCode = versionCodeRegex.find(text)?.groupValues?.get(1)?.toInt() ?: return null

            return versionName to versionCode

        } catch (e: Exception) {
            return null
        }
    }
}
