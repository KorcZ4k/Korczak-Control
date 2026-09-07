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

    private fun request(path: String, token: String? = null): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(25))
            .header("Accept", "application/json")
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        return builder
    }

    suspend fun login(email: String, password: String): AuthSession = withContext(Dispatchers.IO) {
        val body = "{\"email\":\"${email.jsonEscape()}\",\"password\":\"${password.jsonEscape()}\"}"
        val response = client.send(
            request("/api/auth/login")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() !in 200..299) throw ApiException.from(response.statusCode(), response.body())
        AuthSession.fromJson(response.body())
    }

    suspend fun me(token: String): AccountProfile = withContext(Dispatchers.IO) {
        val response = client.send(request("/api/auth/me", token).GET().build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw ApiException.from(response.statusCode(), response.body())
        AccountProfile.fromJson(response.body())
    }

    suspend fun dashboard(token: String): DashboardSummary = withContext(Dispatchers.IO) {
        val response = client.send(request("/api/dashboard/summary", token).GET().build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw ApiException.from(response.statusCode(), response.body())
        DashboardSummary.fromJson(response.body())
    }

    suspend fun health(): ApiHealth = withContext(Dispatchers.IO) {
        val response = client.send(request("/health").GET().build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw ApiException.from(response.statusCode(), response.body())
        ApiHealth.fromJson(response.body())
    }
}

private fun String.jsonEscape(): String = buildString {
    this@jsonEscape.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

class ApiException(message: String) : IllegalStateException(message) {
    companion object {
        fun from(status: Int, body: String): ApiException {
            val error = JsonReader.stringValue(body, "error")
                .ifBlank { JsonReader.stringValue(body, "message") }
                .ifBlank { "A API respondeu com status $status." }
            return ApiException(error)
        }
    }
}

private object JsonReader {
    fun stringValue(json: String, key: String): String =
        Regex("\\\"$key\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"")
            .find(json)?.groupValues?.getOrNull(1)?.replace("\\\"", "\"")?.replace("\\\\", "").orEmpty()

    fun intValue(json: String, key: String): Int =
        Regex("\\\"$key\\\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    fun booleanValue(json: String, key: String): Boolean =
        Regex("\\\"$key\\\"\\s*:\\s*(true|false)").find(json)?.groupValues?.getOrNull(1)?.toBoolean() ?: false

    fun objectValue(json: String, key: String): String {
        val start = Regex("\\\"$key\\\"\\s*:").find(json)?.range?.last?.plus(1) ?: return ""
        val open = json.indexOf('{', start)
        if (open < 0) return ""
        var depth = 0
        for (index in open until json.length) {
            when (json[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return json.substring(open, index + 1)
                }
            }
        }
        return ""
    }
}

data class AuthSession(val token: String, val profile: AccountProfile) {
    companion object {
        fun fromJson(json: String): AuthSession {
            val user = JsonReader.objectValue(json, "user")
            return AuthSession(JsonReader.stringValue(json, "token"), AccountProfile.fromUserJson(user))
        }
    }
}

data class AccountProfile(
    val id: String,
    val accountId: String,
    val name: String,
    val email: String,
    val role: String,
    val department: String
) {
    companion object {
        fun fromJson(json: String): AccountProfile = fromUserJson(JsonReader.objectValue(json, "user"))
        fun fromUserJson(json: String) = AccountProfile(
            id = JsonReader.stringValue(json, "id"),
            accountId = JsonReader.stringValue(json, "accountId"),
            name = JsonReader.stringValue(json, "name"),
            email = JsonReader.stringValue(json, "email"),
            role = JsonReader.stringValue(json, "role"),
            department = JsonReader.stringValue(json, "department")
        )
    }
}

data class DashboardSummary(
    val sites: Int,
    val apis: Int,
    val apps: Int,
    val databases: Int,
    val unread: Int,
    val online: Int,
    val attention: Int,
    val unavailable: Int,
    val github: Boolean,
    val render: Boolean,
    val mongodb: Boolean
) {
    companion object {
        fun fromJson(json: String): DashboardSummary {
            val resources = JsonReader.objectValue(json, "resources")
            val services = JsonReader.objectValue(json, "services")
            val notifications = JsonReader.objectValue(json, "notifications")
            val integrations = JsonReader.objectValue(json, "integrations")
            return DashboardSummary(
                sites = JsonReader.intValue(resources, "sites"),
                apis = JsonReader.intValue(resources, "apis"),
                apps = JsonReader.intValue(resources, "apps"),
                databases = JsonReader.intValue(resources, "databases"),
                unread = JsonReader.intValue(notifications, "unread"),
                online = JsonReader.intValue(services, "online"),
                attention = JsonReader.intValue(services, "attention"),
                unavailable = JsonReader.intValue(services, "unavailable"),
                github = JsonReader.booleanValue(integrations, "github"),
                render = JsonReader.booleanValue(integrations, "render"),
                mongodb = JsonReader.booleanValue(integrations, "mongodb")
            )
        }
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
        fun fromJson(json: String) = ApiHealth(
            status = JsonReader.stringValue(json, "status"),
            service = JsonReader.stringValue(json, "service"),
            version = JsonReader.stringValue(json, "version"),
            environment = JsonReader.stringValue(json, "environment"),
            adminDatabase = JsonReader.booleanValue(json, "KorczakControl"),
            moonDatabase = JsonReader.booleanValue(json, "TensuraMoon"),
            kzSiteDatabase = JsonReader.booleanValue(json, "KorczakTechSite"),
            github = JsonReader.booleanValue(json, "github"),
            render = JsonReader.booleanValue(json, "render"),
            kzSiteApi = JsonReader.booleanValue(json, "kzSiteApi"),
            kzControlApi = JsonReader.booleanValue(json, "kzControlApi")
        )
    }
}
