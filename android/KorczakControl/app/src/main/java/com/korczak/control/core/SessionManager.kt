package com.korczak.control.core

import android.content.Context
import com.korczak.control.BuildConfig

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("korczak_control", Context.MODE_PRIVATE)

    fun apiUrl(): String = prefs.getString("api_url", null)?.trim()?.trimEnd('/')
        ?.takeIf { it.isNotBlank() }
        ?: BuildConfig.CONTROL_API_URL.trim().trimEnd('/')

    fun token(): String? = prefs.getString("access_token", null)?.takeIf { it.isNotBlank() }
    fun isAuthenticated(): Boolean = !token().isNullOrBlank() && apiUrl().isNotBlank()

    fun saveSession(apiUrl: String, token: String) {
        prefs.edit().putString("api_url", apiUrl.trim().trimEnd('/')).putString("access_token", token).apply()
    }

    fun saveApiUrl(apiUrl: String) { prefs.edit().putString("api_url", apiUrl.trim().trimEnd('/')).apply() }
    fun clear() { prefs.edit().remove("access_token").apply() }
}
