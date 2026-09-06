package com.korczak.control.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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

    var setupMode by remember { mutableStateOf(false) }
    var setupChecked by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.setupRequired()
            .onSuccess { setupMode = it }
            .onFailure { error = it.message ?: "Não foi possível conectar à API." }
        setupChecked = true
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (setupMode) "Crie a primeira conta administrativa para inicializar o Korczak Control."
            else "Entre com sua conta para acessar os serviços autorizados.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (setupMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
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

        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            enabled = setupChecked && !loading && email.isNotBlank() && password.isNotBlank() && (!setupMode || name.trim().length >= 2),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                loading = true
                error = null
                message = null
                scope.launch {
                    if (setupMode) {
                        repository.registerFirstAccount(name, email.trim(), password)
                            .onSuccess {
                                message = "Conta criada. Faça login para continuar."
                                password = ""
                                setupMode = false
                            }
                            .onFailure { error = it.message ?: "Não foi possível criar a conta." }
                    } else {
                        repository.login(email.trim(), password)
                            .onSuccess { onLoggedIn() }
                            .onFailure { error = it.message ?: "Não foi possível entrar." }
                    }
                    loading = false
                }
            }
        ) {
            Text(
                if (loading) "Processando..."
                else if (setupMode) "Criar primeira conta"
                else "Entrar"
            )
        }
    }
}
