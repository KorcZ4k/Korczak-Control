package com.korczak.control.auth

import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager
import org.json.JSONObject

class AuthRepository(private val session: SessionManager) {
    private val client = ApiClient(session)

    suspend fun login(apiUrl: String, email: String, password: String): Result<Unit> {
        if (apiUrl.trim().isBlank()) return Result.failure(IllegalArgumentException("Informe a URL da API."))
        if (email.trim().isBlank() || password.isBlank()) return Result.failure(IllegalArgumentException("Informe email e senha."))
        session.saveApiUrl(apiUrl)
        return when (val result = client.post("/api/auth/login", JSONObject().put("email", email.trim()).put("password", password))) {
            is ApiResult.Success -> try {
                val token = JSONObject(result.body).optString("token")
                if (token.isBlank()) Result.failure(IllegalStateException("A API não retornou um token de sessão."))
                else { session.saveSession(apiUrl, token); Result.success(Unit) }
            } catch (error: Exception) { Result.failure(error) }
            is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
        }
    }
}
