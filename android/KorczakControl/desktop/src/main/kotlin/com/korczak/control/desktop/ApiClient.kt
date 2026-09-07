package com.korczak.control.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object ControlApiClient {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val baseUrl: String
        get() = (System.getProperty("CONTROL_API_URL") ?: System.getenv("CONTROL_API_URL") ?: "https://korczak-control.onrender.com").trim().trimEnd('/')

    private fun request(path: String, token: String? = null): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder().uri(URI.create("$baseUrl$path")).timeout(Duration.ofSeconds(25)).header("Accept", "application/json")
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        return builder
    }

    private suspend fun send(path: String, token: String, method: String = "GET", body: String? = null): String = withContext(Dispatchers.IO) {
        val builder = request(path, token).header("Content-Type", "application/json")
        when (method) {
            "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body ?: ""))
            else -> builder.GET()
        }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw ApiException.from(response.statusCode(), response.body())
        response.body()
    }

    suspend fun login(email: String, password: String): AuthSession {
        val body = "{\"email\":\"${email.jsonEscape()}\",\"password\":\"${password.jsonEscape()}\"}"
        return AuthSession.fromJson(sendPublic("/api/auth/login", "POST", body))
    }

    private suspend fun sendPublic(path: String, method: String = "GET", body: String? = null): String = withContext(Dispatchers.IO) {
        val builder = request(path).header("Content-Type", "application/json")
        if (method == "POST") builder.POST(HttpRequest.BodyPublishers.ofString(body ?: "")) else builder.GET()
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw ApiException.from(response.statusCode(), response.body())
        response.body()
    }

    suspend fun me(token: String): AccountProfile = AccountProfile.fromJson(send("/api/auth/me", token))
    suspend fun dashboard(token: String): DashboardSummary = DashboardSummary.fromJson(send("/api/dashboard/summary", token))
    suspend fun renderServices(token: String): List<DisplayRecord> = records(send("/api/render/services", token))
    suspend fun sites(token: String): List<DisplayRecord> = records(send("/api/sites", token))
    suspend fun applications(token: String): List<DisplayRecord> = records(send("/api/apps", token))
    suspend fun clients(token: String): List<DisplayRecord> = records(send("/api/clients", token))
    suspend fun apis(token: String): List<DisplayRecord> = records(send("/api/apis", token))

    suspend fun collections(token: String, database: String): List<DisplayRecord> = records(send("/api/databases/$database/collections", token))
    suspend fun documents(token: String, database: String, collection: String): List<DisplayRecord> = records(send("/api/databases/$database/collections/$collection/documents?limit=50", token))
    suspend fun createCollection(token: String, database: String, name: String) { send("/api/databases/$database/collections", token, "POST", "{\"name\":\"${name.jsonEscape()}\"}") }

    suspend fun workflows(token: String): BotWorkflows {
        val json = send("/api/github/bots/tensura-moon/workflows", token)
        return BotWorkflows(JsonReader.stringValue(json, "repository"), JsonReader.arrayRecords(json, "workflows"))
    }

    suspend fun runWorkflow(token: String, repository: String, workflowId: String) {
        val parts = repository.split('/').filter { it.isNotBlank() }
        require(parts.size == 2) { "Repositório inválido." }
        send("/api/github/repos/${parts[0]}/${parts[1]}/workflows/$workflowId/dispatch", token, "POST", "{\"ref\":\"main\"}")
    }

    private fun records(json: String): List<DisplayRecord> = JsonReader.arrayRecords(json, "items")
}

private fun String.jsonEscape(): String = buildString { this@jsonEscape.forEach { append(if (it == '\\') "\\\\" else if (it == '"') "\\\"" else it) } }

class ApiException(message: String) : IllegalStateException(message) {
    companion object { fun from(status: Int, body: String): ApiException = ApiException(JsonReader.stringValue(body, "error").ifBlank { JsonReader.stringValue(body, "message") }.ifBlank { "A API respondeu com status $status." }) }
}

private object JsonReader {
    fun stringValue(json: String, key: String): String = Regex("\\\"$key\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").find(json)?.groupValues?.getOrNull(1)?.replace("\\\"", "\"").orEmpty()
    fun intValue(json: String, key: String): Int = Regex("\\\"$key\\\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    fun booleanValue(json: String, key: String): Boolean = Regex("\\\"$key\\\"\\s*:\\s*(true|false)").find(json)?.groupValues?.getOrNull(1)?.toBoolean() ?: false
    fun objectValue(json: String, key: String): String = extractDelimited(json, key, '{', '}')
    private fun extractDelimited(json: String, key: String, openChar: Char, closeChar: Char): String {
        val start = Regex("\\\"$key\\\"\\s*:").find(json)?.range?.last?.plus(1) ?: return ""
        val open = json.indexOf(openChar, start); if (open < 0) return ""
        var depth = 0
        for (i in open until json.length) { if (json[i] == openChar) depth++; if (json[i] == closeChar && --depth == 0) return json.substring(open, i + 1) }
        return ""
    }
    fun arrayRecords(json: String, key: String): List<DisplayRecord> {
        val raw = extractDelimited(json, key, '[', ']'); if (raw.isBlank()) return emptyList()
        val objects = mutableListOf<String>(); var depth = 0; var start = -1
        raw.forEachIndexed { i, c -> if (c == '{') { if (depth == 0) start = i; depth++ }; if (c == '}') { depth--; if (depth == 0 && start >= 0) objects += raw.substring(start, i + 1) } }
        return objects.map { record ->
            val pairs = Regex("\\\"([^\\\"]+)\\\"\\s*:\\s*(\\\"((?:\\\\.|[^\\\"])*)\\\"|-?\\d+|true|false|null)").findAll(record).associate { match -> match.groupValues[1] to match.groupValues[3].ifBlank { match.groupValues[2] } }
            DisplayRecord(pairs)
        }
    }
}

data class DisplayRecord(val fields: Map<String, String>) { fun value(vararg names: String): String = names.firstNotNullOfOrNull { fields[it]?.takeIf(String::isNotBlank) }.orEmpty() }
data class BotWorkflows(val repository: String, val workflows: List<DisplayRecord>)
data class AuthSession(val token: String, val profile: AccountProfile) { companion object { fun fromJson(json: String): AuthSession { val user = JsonReader.objectValue(json, "user"); return AuthSession(JsonReader.stringValue(json, "token"), AccountProfile.fromUserJson(user)) } } }
data class AccountProfile(val id: String, val accountId: String, val name: String, val email: String, val role: String, val department: String) { companion object { fun fromJson(json: String): AccountProfile = fromUserJson(JsonReader.objectValue(json, "user")); fun fromUserJson(json: String) = AccountProfile(JsonReader.stringValue(json, "id"), JsonReader.stringValue(json, "accountId"), JsonReader.stringValue(json, "name"), JsonReader.stringValue(json, "email"), JsonReader.stringValue(json, "role"), JsonReader.stringValue(json, "department")) } }
data class DashboardSummary(val sites: Int, val apis: Int, val apps: Int, val databases: Int, val unread: Int, val online: Int, val attention: Int, val unavailable: Int, val github: Boolean, val render: Boolean, val mongodb: Boolean) { companion object { fun fromJson(json: String): DashboardSummary { val resources=JsonReader.objectValue(json,"resources"); val services=JsonReader.objectValue(json,"services"); val notifications=JsonReader.objectValue(json,"notifications"); val integrations=JsonReader.objectValue(json,"integrations"); return DashboardSummary(JsonReader.intValue(resources,"sites"),JsonReader.intValue(resources,"apis"),JsonReader.intValue(resources,"apps"),JsonReader.intValue(resources,"databases"),JsonReader.intValue(notifications,"unread"),JsonReader.intValue(services,"online"),JsonReader.intValue(services,"attention"),JsonReader.intValue(services,"unavailable"),JsonReader.booleanValue(integrations,"github"),JsonReader.booleanValue(integrations,"render"),JsonReader.booleanValue(integrations,"mongodb")) } } }
