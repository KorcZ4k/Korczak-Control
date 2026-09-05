package com.korczak.control.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.korczak.control.core.SessionManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val repository = remember { AuthRepository(session) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var apiUrl by remember { mutableStateOf(session.apiUrl()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showApiConfig by remember { mutableStateOf(!session.isApiConfigured()) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium)
        Text("Entre com sua conta autorizada para acessar os serviços permitidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (showApiConfig) {
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("URL da Korczak Control API") },
                supportingText = { Text("Use a URL pública da API. Credenciais do MongoDB nunca são inseridas no aplicativo.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { session.setApiUrl(apiUrl); showApiConfig = false }) { Text("Salvar URL") }
        } else {
            TextButton(onClick = { showApiConfig = true }) { Text("Alterar conexão da API") }
        }

        OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            enabled = !loading && session.isApiConfigured(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                loading = true
                error = null
                scope.launch {
                    repository.login(email, password)
                        .onSuccess { onLoggedIn() }
                        .onFailure { error = it.message ?: "Não foi possível entrar." }
                    loading = false
                }
            }
        ) { Text(if (loading) "Entrando..." else "Entrar") }

        if (!session.isApiConfigured()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("A API precisa ter uma URL pública para o aplicativo instalado no celular conseguir fazer login.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
