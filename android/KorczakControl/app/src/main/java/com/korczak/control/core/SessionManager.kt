package com.korczak.control.core

import android.content.Context
import com.korczak.control.BuildConfig
import org.json.JSONObject

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("korczak_control", Context.MODE_PRIVATE)

    fun apiUrl(): String = BuildConfig.CONTROL_API_URL.trim().trimEnd('/')
    fun isApiConfigured(): Boolean = apiUrl().isNotBlank()
    fun token(): String? = prefs.getString("access_token", null)?.takeIf { it.isNotBlank() }
    fun isAuthenticated(): Boolean = !token().isNullOrBlank() && isApiConfigured()
    fun permissions(): JSONObject = JSONObject(prefs.getString("permissions", "{}") ?: "{}")
    fun accountName(): String = prefs.getString("account_name", "") ?: ""
    fun accountRole(): String = prefs.getString("account_role", "") ?: ""

    fun saveSession(token: String, user: JSONObject) {
        prefs.edit()
            .putString("access_token", token)
            .putString("permissions", user.optJSONObject("permissions")?.toString() ?: "{}")
            .putString("account_name", user.optString("name"))
            .putString("account_role", user.optString("role"))
            .apply()
    }

    fun clear() {
        prefs.edit().remove("access_token").remove("permissions").remove("account_name").remove("account_role").apply()
    }
}
