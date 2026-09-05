package com.korczak.control.dashboard

import android.content.Context
import com.korczak.control.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DashboardRepository(private val context: Context) {
    suspend fun load(): DashboardState = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.CONTROL_API_URL.trim().trimEnd('/')
        val token = context.getSharedPreferences("korczak_control", Context.MODE_PRIVATE)
            .getString("access_token", null)

        if (baseUrl.isBlank() || token.isNullOrBlank()) return@withContext DashboardState.SetupRequired

        try {
            val connection = (URL("$baseUrl/api/dashboard/summary").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val message = if (responseCode == 401) "Sua sessão expirou ou não é válida." else "Não foi possível carregar o dashboard. Código HTTP: $responseCode"
                connection.disconnect()
                return@withContext DashboardState.Error(message)
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = JSONObject(body)
            val resourcesJson = json.getJSONObject("resources")
            val servicesJson = json.getJSONObject("services")
            val integrationsJson = json.getJSONObject("integrations")

            fun ints(source: JSONObject): Map<String, Int> = source.keys().asSequence().associateWith { source.optInt(it, 0) }
            fun bools(source: JSONObject): Map<String, Boolean> = source.keys().asSequence().associateWith { source.optBoolean(it, false) }

            DashboardState.Content(
                DashboardSummary(
                    generatedAt = json.optString("generatedAt"),
                    resources = ints(resourcesJson),
                    services = ints(servicesJson),
                    integrations = bools(integrationsJson)
                )
            )
        } catch (error: Exception) {
            DashboardState.Error(error.message ?: "Erro desconhecido ao comunicar com o servidor.")
        }
    }
}
