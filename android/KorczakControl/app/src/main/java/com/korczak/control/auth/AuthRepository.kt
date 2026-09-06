package com.korczak.control.auth

import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager
import org.json.JSONObject

class AuthRepository(private val session: SessionManager) {
    private val client = ApiClient(session)

    suspend fun login(email: String, password: String): Result<Unit> {
        if (email.trim().isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Informe e-mail e senha."))
        }
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
            } catch (error: Exception) {
                Result.failure(IllegalStateException("Resposta de autenticação inválida.", error))
            }
            is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun setupRequired(): Result<Boolean> = when (val result = client.get("/api/auth/bootstrap-status")) {
        is ApiResult.Success -> try {
            Result.success(JSONObject(result.body).optBoolean("setupRequired", false))
        } catch (error: Exception) {
            Result.failure(IllegalStateException("Não foi possível verificar a configuração inicial.", error))
        }
        is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
    }

    suspend fun registerFirstAccount(name: String, email: String, password: String): Result<Unit> {
        if (name.trim().length < 2) return Result.failure(IllegalArgumentException("Informe seu nome."))
        if (email.trim().isBlank()) return Result.failure(IllegalArgumentException("Informe seu e-mail."))
        if (password.length < 12) return Result.failure(IllegalArgumentException("A senha deve ter pelo menos 12 caracteres."))

        val body = JSONObject()
            .put("name", name.trim())
            .put("email", email.trim())
            .put("password", password)

        return when (val result = client.post("/api/auth/register", body)) {
            is ApiResult.Success -> Result.success(Unit)
            is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun validateSession(): Result<Unit> {
        if (!session.isAuthenticated()) return Result.failure(IllegalStateException("Sessão indisponível."))
        return when (val result = client.get("/api/auth/me")) {
            is ApiResult.Success -> try {
                val user = JSONObject(result.body).optJSONObject("user")
                    ?: return Result.failure(IllegalStateException("A API não retornou a conta."))
                session.updateUser(user)
                Result.success(Unit)
            } catch (error: Exception) { Result.failure(error) }
            is ApiResult.Failure -> {
                if (result.code == 401 || result.code == 403) session.clearSession()
                Result.failure(IllegalStateException(result.message))
            }
        }
    }
}
