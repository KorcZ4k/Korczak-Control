package com.korczak.control.update

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val version: String,
    val notes: String,
    val downloadUrl: String
)

object AppUpdateRepository {
    private const val RELEASE_URL = "https://api.github.com/repos/KorcZ4k/Korczak-Control/releases/latest"

    suspend fun check(currentVersion: String): AppUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Korczak-Control-Android/${Build.VERSION.SDK_INT}")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(body)
            val version = release.optString("tag_name").removePrefix("v")
            if (version.isBlank() || version == currentVersion) return@withContext null
            val assets = release.optJSONArray("assets") ?: return@withContext null
            var downloadUrl = ""
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (downloadUrl.isBlank()) return@withContext null
            AppUpdate(version, release.optString("body", "Nova versão disponível."), downloadUrl)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
