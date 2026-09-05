package com.korczak.control.dashboard

import android.content.Context
import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager
import org.json.JSONObject

class DashboardRepository(context: Context) {
    private val session = SessionManager(context)
    private val client = ApiClient(session)

    suspend fun load(): DashboardState {
        if (!session.isAuthenticated()) return DashboardState.SetupRequired
        return when (val result = client.get("/api/dashboard/summary")) {
            is ApiResult.Failure -> DashboardState.Error(result.message)
            is ApiResult.Success -> try {
                val json = JSONObject(result.body)
                val resourcesJson = json.optJSONObject("resources") ?: JSONObject()
                val servicesJson = json.optJSONObject("services") ?: JSONObject()
                val integrationsJson = json.optJSONObject("integrations") ?: JSONObject()
                fun ints(source: JSONObject): Map<String, Int> = source.keys().asSequence().associateWith { source.optInt(it, 0) }
                fun bools(source: JSONObject): Map<String, Boolean> = source.keys().asSequence().associateWith { source.optBoolean(it, false) }
                DashboardState.Content(DashboardSummary(
                    generatedAt = json.optString("generatedAt"),
                    resources = ints(resourcesJson),
                    services = ints(servicesJson),
                    integrations = bools(integrationsJson)
                ))
            } catch (error: Exception) { DashboardState.Error("Resposta inválida da API: ${error.message}") }
        }
    }
}
