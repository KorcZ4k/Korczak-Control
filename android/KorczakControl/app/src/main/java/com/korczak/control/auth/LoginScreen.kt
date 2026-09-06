package com.korczak.control.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
            .onFailure { error = it.message ?: "Não foi possível conectar ao Korczak Control." }
        setupChecked = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp).size(30.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column {
                            Text(
                                text = "KORCZAK CONTROL",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "KZ TECH • ADMINISTRAÇÃO E CONTROLE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (setupMode) "Inicialização do sistema" else "Acesso administrativo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (setupMode)
                            "Crie a primeira conta administrativa para inicializar o ambiente Korczak Control."
                        else
                            "Informe suas credenciais para acessar os serviços e recursos autorizados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!setupChecked) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Text("Verificando configuração do sistema...")
                            }
                        } else {
                            if (setupMode) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Nome administrativo") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-mail") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Senha") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                singleLine = true,
                                visualTransformation = if (showPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (showPassword) {
                                                "Ocultar senha"
                                            } else {
                                                "Mostrar senha"
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            message?.let {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = it,
                                        modifier = Modifier.padding(14.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            error?.let {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = it,
                                        modifier = Modifier.padding(14.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Button(
                                enabled = !loading && email.isNotBlank() && password.isNotBlank() &&
                                    (!setupMode || name.trim().length >= 2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(14.dp),
                                onClick = {
                                    loading = true
                                    error = null
                                    message = null

                                    scope.launch {
                                        if (setupMode) {
                                            repository.registerFirstAccount(name, email.trim(), password)
                                                .onSuccess {
                                                    message = "Conta administrativa criada. Faça login para continuar."
                                                    password = ""
                                                    setupMode = false
                                                }
                                                .onFailure {
                                                    error = it.message ?: "Não foi possível criar a conta administrativa."
                                                }
                                        } else {
                                            repository.login(email.trim(), password)
                                                .onSuccess { onLoggedIn() }
                                                .onFailure {
                                                    error = it.message ?: "Não foi possível concluir o login."
                                                }
                                        }
                                        loading = false
                                    }
                                }
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(if (setupMode) "CRIAR CONTA ADMINISTRATIVA" else "ENTRAR NO SISTEMA")
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Acesso restrito • Korczak Control",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
