package com.korczak.control.auth

import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager
import org.json.JSONObject

class AuthRepository(private val session: SessionManager) {
    private val client = ApiClient(session)

    suspend fun login(email: String, password: String): Result<Unit> {
        if (!session.isApiConfigured()) return Result.failure(IllegalStateException("O serviço de autenticação ainda não foi conectado ao aplicativo."))
        if (email.trim().isBlank() || password.isBlank()) return Result.failure(IllegalArgumentException("Informe email e senha."))
        return when (val result = client.post("/api/auth/login", JSONObject().put("email", email.trim()).put("password", password))) {
            is ApiResult.Success -> try {
                val response = JSONObject(result.body)
                val token = response.optString("token")
                val user = response.optJSONObject("user")
                if (token.isBlank() || user == null) Result.failure(IllegalStateException("A API não retornou uma sessão válida."))
                else {
                    session.saveSession(token, user)
                    Result.success(Unit)
                }
            } catch (error: Exception) { Result.failure(error) }
            is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
        }
    }
}
