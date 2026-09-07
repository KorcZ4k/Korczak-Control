package com.korczak.control.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object ControlApiClient {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val baseUrl: String
        get() = (System.getProperty("CONTROL_API_URL")
            ?: System.getenv("CONTROL_API_URL")
            ?: "https://korczak-control.onrender.com")
            .trim()
            .trimEnd('/')

    suspend fun health(): ApiHealth = withContext(Dispatchers.IO) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/health"))
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("A API respondeu com status ${response.statusCode()}.")
        }
        ApiHealth.fromJson(response.body())
    }
}

data class ApiHealth(
    val status: String,
    val service: String,
    val version: String,
    val environment: String,
    val adminDatabase: Boolean,
    val moonDatabase: Boolean,
    val kzSiteDatabase: Boolean,
    val github: Boolean,
    val render: Boolean,
    val kzSiteApi: Boolean,
    val kzControlApi: Boolean
) {
    companion object {
        private fun stringValue(json: String, key: String): String =
            Regex("\\\"$key\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
                .find(json)?.groupValues?.getOrNull(1).orEmpty()

        private fun booleanValue(json: String, key: String): Boolean =
            Regex("\\\"$key\\\"\\s*:\\s*(true|false)")
                .find(json)?.groupValues?.getOrNull(1)?.toBoolean() ?: false

        fun fromJson(json: String) = ApiHealth(
            status = stringValue(json, "status"),
            service = stringValue(json, "service"),
            version = stringValue(json, "version"),
            environment = stringValue(json, "environment"),
            adminDatabase = booleanValue(json, "KorczakControl"),
            moonDatabase = booleanValue(json, "TensuraMoon"),
            kzSiteDatabase = booleanValue(json, "KorczakTechSite"),
            github = booleanValue(json, "github"),
            render = booleanValue(json, "render"),
            kzSiteApi = booleanValue(json, "kzSiteApi"),
            kzControlApi = booleanValue(json, "kzControlApi")
        )
    }
}
