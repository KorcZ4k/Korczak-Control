package com.korczak.control.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.korczak.control.core.SessionManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val repository = remember { AuthRepository(session) }
    val scope = rememberCoroutineScope()
    var apiUrl by remember { mutableStateOf(session.apiUrl()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium)
        Text("Acesso à infraestrutura da Korczak Technologies", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(apiUrl, { apiUrl = it }, label = { Text("URL da API") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Senha") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !loading, modifier = Modifier.fillMaxWidth(), onClick = {
            loading = true; error = null
            scope.launch {
                repository.login(apiUrl, email, password).onSuccess { onLoggedIn() }.onFailure { error = it.message ?: "Não foi possível entrar." }
                loading = false
            }
        }) { Text(if (loading) "Entrando..." else "Entrar") }
    }
}
