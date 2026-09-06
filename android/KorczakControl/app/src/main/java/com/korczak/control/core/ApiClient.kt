package com.korczak.control.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface ApiResult {
    data class Success(val body: String, val code: Int) : ApiResult
    data class Failure(val message: String, val code: Int? = null) : ApiResult
}

class ApiClient(private val session: SessionManager) {
    suspend fun get(path: String): ApiResult = request("GET", path)
    suspend fun post(path: String, body: JSONObject): ApiResult = request("POST", path, body.toString())
    suspend fun patch(path: String, body: JSONObject): ApiResult = request("PATCH", path, body.toString())
    suspend fun delete(path: String): ApiResult = request("DELETE", path)

    private suspend fun request(method: String, path: String, body: String? = null): ApiResult = withContext(Dispatchers.IO) {
        val baseUrl = session.apiUrl()
        try {
            val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                session.token()?.let { setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    outputStream.bufferedWriter().use { it.write(body) }
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code in 200..299) ApiResult.Success(text, code) else ApiResult.Failure(extractError(text, "Erro HTTP $code"), code)
        } catch (error: Exception) { ApiResult.Failure(error.message ?: "Falha de comunicação com a API.") }
    }

    private fun extractError(body: String, fallback: String): String = try { JSONObject(body).optString("error").ifBlank { fallback } } catch (_: Exception) { fallback }
}
