package com.korczak.control.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ControlApiClient {
    private val baseUrl: String
        get() = (System.getProperty("CONTROL_API_URL") ?: System.getenv("CONTROL_API_URL") ?: "https://korczak-control.onrender.com")
            .trim()
            .trimEnd('/')

    private suspend fun send(path: String, token: String? = null, method: String = "GET", body: String? = null): String = withContext(Dispatchers.IO) {
        val url = URI.create("$baseUrl$path").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw ApiException.from(status, response)
            response
        } finally {
            connection.disconnect()
        }
    }

    suspend fun login(email: String, password: String): AuthSession {
        val body = "{\"email\":\"${email.jsonEscape()}\",\"password\":\"${password.jsonEscape()}\"}"
        return AuthSession.fromJson(send("/api/auth/login", method = "POST", body = body))
    }

    suspend fun me(token: String): AccountProfile = AccountProfile.fromJson(send("/api/auth/me", token))
    suspend fun dashboard(token: String): DashboardSummary = DashboardSummary.fromJson(send("/api/dashboard/summary", token))
    suspend fun renderServices(token: String): List<DisplayRecord> = records(send("/api/render/services", token))
    suspend fun sites(token: String): List<DisplayRecord> = records(send("/api/sites", token))
    suspend fun applications(token: String): List<DisplayRecord> = records(send("/api/apps", token))
    suspend fun clients(token: String): List<DisplayRecord> = records(send("/api/clients", token))
    suspend fun apis(token: String): List<DisplayRecord> = records(send("/api/apis", token))

    suspend fun collections(token: String, database: String): List<DisplayRecord> = records(send("/api/databases/${database.encodePath()}/collections", token))
    suspend fun documents(token: String, database: String, collection: String): List<DisplayRecord> = records(send("/api/databases/${database.encodePath()}/collections/${collection.encodePath()}/documents?limit=50", token))
    suspend fun createCollection(token: String, database: String, name: String) {
        send("/api/databases/${database.encodePath()}/collections", token, "POST", "{\"name\":\"${name.jsonEscape()}\"}")
    }

    suspend fun workflows(token: String): BotWorkflows {
        val json = send("/api/github/bots/tensura-moon/workflows", token)
        return BotWorkflows(JsonReader.stringValue(json, "repository"), JsonReader.arrayRecords(json, "workflows"))
    }

    suspend fun runWorkflow(token: String, repository: String, workflowId: String) {
        val parts = repository.split('/').filter { it.isNotBlank() }
        require(parts.size == 2) { "Repositório inválido." }
        send("/api/github/repos/${parts[0].encodePath()}/${parts[1].encodePath()}/workflows/${workflowId.encodePath()}/dispatch", token, "POST", "{\"ref\":\"main\"}")
    }

    private fun records(json: String): List<DisplayRecord> = JsonReader.arrayRecords(json, "items")
}

private fun String.encodePath(): String = URLEncoder.encode(this, StandardCharsets.UTF_8).replace("+", "%20")
private fun String.jsonEscape(): String = buildString {
    this@jsonEscape.forEach {
        when (it) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(it)
        }
    }
}

class ApiException(message: String) : IllegalStateException(message) {
    companion object {
        fun from(status: Int, body: String): ApiException {
            val detail = JsonReader.stringValue(body, "error")
                .ifBlank { JsonReader.stringValue(body, "message") }
                .ifBlank { "A API respondeu com status $status." }
            return ApiException(detail)
        }
    }
}

private object JsonReader {
    fun stringValue(json: String, key: String): String = Regex("\\\"$key\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"")
        .find(json)?.groupValues?.getOrNull(1)
        ?.replace("\\\"", "\"")
        ?.replace("\\\\", "\\")
        .orEmpty()

    fun intValue(json: String, key: String): Int = Regex("\\\"$key\\\"\\s*:\\s*(-?\\d+)")
        .find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    fun booleanValue(json: String, key: String): Boolean = Regex("\\\"$key\\\"\\s*:\\s*(true|false)")
        .find(json)?.groupValues?.getOrNull(1)?.toBoolean() ?: false

    fun objectValue(json: String, key: String): String = extractDelimited(json, key, '{', '}')

    private fun extractDelimited(json: String, key: String, openChar: Char, closeChar: Char): String {
        val start = Regex("\\\"$key\\\"\\s*:").find(json)?.range?.last?.plus(1) ?: return ""
        val open = json.indexOf(openChar, start)
        if (open < 0) return ""
        var depth = 0
        var quoted = false
        var escaped = false
        for (i in open until json.length) {
            val c = json[i]
            if (quoted) {
                if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
                continue
            }
            if (c == '"') { quoted = true; continue }
            if (c == openChar) depth++
            if (c == closeChar) {
                depth--
                if (depth == 0) return json.substring(open, i + 1)
            }
        }
        return ""
    }

    fun arrayRecords(json: String, key: String): List<DisplayRecord> {
        val raw = extractDelimited(json, key, '[', ']')
        if (raw.isBlank()) return emptyList()
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        var quoted = false
        var escaped = false
        raw.forEachIndexed { i, c ->
            if (quoted) {
                if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
            } else {
                if (c == '"') quoted = true
                else if (c == '{') { if (depth == 0) start = i; depth++ }
                else if (c == '}') { depth--; if (depth == 0 && start >= 0) objects += raw.substring(start, i + 1) }
            }
        }
        return objects.map { record ->
            val pairs = Regex("\\\"([^\\\"]+)\\\"\\s*:\\s*(\\\"((?:\\\\.|[^\\\"])*)\\\"|-?\\d+|true|false|null)")
                .findAll(record)
                .associate { match ->
                    match.groupValues[1] to match.groupValues[3].ifBlank { match.groupValues[2] }
                }
            DisplayRecord(pairs)
        }
    }
}

data class DisplayRecord(val fields: Map<String, String>) {
    fun value(vararg names: String): String = names.firstNotNullOfOrNull { fields[it]?.takeIf(String::isNotBlank) }.orEmpty()
}

data class BotWorkflows(val repository: String, val workflows: List<DisplayRecord>)

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
            JsonReader.stringValue(json, "id"),
            JsonReader.stringValue(json, "accountId"),
            JsonReader.stringValue(json, "name"),
            JsonReader.stringValue(json, "email"),
            JsonReader.stringValue(json, "role"),
            JsonReader.stringValue(json, "department")
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
                JsonReader.intValue(resources, "sites"),
                JsonReader.intValue(resources, "apis"),
                JsonReader.intValue(resources, "apps"),
                JsonReader.intValue(resources, "databases"),
                JsonReader.intValue(notifications, "unread"),
                JsonReader.intValue(services, "online"),
                JsonReader.intValue(services, "attention"),
                JsonReader.intValue(services, "unavailable"),
                JsonReader.booleanValue(integrations, "github"),
                JsonReader.booleanValue(integrations, "render"),
                JsonReader.booleanValue(integrations, "mongodb")
            )
        }
    }
}
