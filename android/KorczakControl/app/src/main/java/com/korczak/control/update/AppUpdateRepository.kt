package com.korczak.control.update

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val version: String, val notes: String, val downloadUrl: String)

object AppUpdateRepository {
    private const val RELEASE_URL = "https://api.github.com/repos/KorcZ4k/Korczak-Control/releases/latest"

    suspend fun check(currentVersion: String): AppUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Korczak-Control-Android/${Build.VERSION.SDK_INT}")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@withContext null

            val version = release.optString("tag_name").removePrefix("v").trim()
            if (version.isBlank() || !isNewerVersion(version, currentVersion)) return@withContext null

            val assets = release.optJSONArray("assets") ?: return@withContext null
            val downloadUrl = (0 until assets.length()).asSequence()
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", true) }
                ?.optString("browser_download_url")
                ?.trim()
                .orEmpty()

            if (downloadUrl.isBlank()) return@withContext null
            AppUpdate(
                version = version,
                notes = release.optString("body", "Nova versão disponível.").trim(),
                downloadUrl = downloadUrl
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewerVersion(candidate: String, current: String): Boolean {
        val candidateParts = candidate.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(candidateParts.size, currentParts.size)
        for (index in 0 until size) {
            val candidateValue = candidateParts.getOrElse(index) { 0 }
            val currentValue = currentParts.getOrElse(index) { 0 }
            if (candidateValue != currentValue) return candidateValue > currentValue
        }
        return false
    }
}
