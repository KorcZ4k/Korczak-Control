package com.korczak.control.core

import android.content.Context
import com.korczak.control.BuildConfig
import org.json.JSONObject

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("korczak_control", Context.MODE_PRIVATE)

    /**
     * A API é definida pelo aplicativo no BuildConfig.
     * O usuário não precisa configurar nem alterar uma URL manualmente.
     */
    fun apiUrl(): String = BuildConfig.CONTROL_API_URL.trim().trimEnd('/')

    /**
     * Mantido apenas para compatibilidade com chamadas antigas.
     * A URL manual não é mais salva nem utilizada.
     */
    @Deprecated("A URL da API é configurada automaticamente pelo aplicativo")
    fun setApiUrl(url: String) = Unit

    fun isApiConfigured(): Boolean = true
    fun token(): String? = prefs.getString("access_token", null)?.takeIf { it.isNotBlank() }
    fun isAuthenticated(): Boolean = !token().isNullOrBlank()
    fun permissions(): JSONObject = JSONObject(prefs.getString("permissions", "{}") ?: "{}")
    fun accountName(): String = prefs.getString("account_name", "") ?: ""
    fun accountRole(): String = prefs.getString("account_role", "") ?: ""
    fun accountId(): String = prefs.getString("account_id", "") ?: ""
    fun department(): String = prefs.getString("department", "") ?: ""

    fun saveSession(token: String, user: JSONObject) {
        prefs.edit()
            .putString("access_token", token)
            .putString("permissions", user.optJSONObject("permissions")?.toString() ?: "{}")
            .putString("account_name", user.optString("name"))
            .putString("account_role", user.optString("role"))
            .putString("account_id", user.optString("accountId"))
            .putString("department", user.optString("department"))
            .apply()
    }

    fun updateUser(user: JSONObject) {
        prefs.edit()
            .putString("permissions", user.optJSONObject("permissions")?.toString() ?: "{}")
            .putString("account_name", user.optString("name"))
            .putString("account_role", user.optString("role"))
            .putString("account_id", user.optString("accountId"))
            .putString("department", user.optString("department"))
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove("access_token").remove("permissions").remove("account_name")
            .remove("account_role").remove("account_id").remove("department").apply()
    }

    fun clear() = clearSession()
}
