package com.korczak.control.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium)
        Text("Entre com sua conta autorizada para acessar os serviços permitidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Senha") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(enabled = !loading && session.isApiConfigured(), modifier = Modifier.fillMaxWidth(), onClick = {
            loading = true
            error = null
            scope.launch {
                repository.login(email, password).onSuccess { onLoggedIn() }.onFailure { error = it.message ?: "Não foi possível entrar." }
                loading = false
            }
        }) { Text(if (loading) "Entrando..." else "Entrar") }

        if (!session.isApiConfigured()) {
            Text("A autenticação será ativada automaticamente quando o serviço seguro da Korczak Technologies for conectado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
