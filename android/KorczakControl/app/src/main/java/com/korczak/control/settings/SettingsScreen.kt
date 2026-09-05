package com.korczak.control.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.korczak.control.core.SessionManager

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    var apiUrl by remember { mutableStateOf(session.apiUrl()) }
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
        Text("Conexão e sessão", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(apiUrl, { apiUrl = it; saved = false }, label = { Text("URL da Korczak Control API") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { session.saveApiUrl(apiUrl); saved = true }) { Text("Salvar URL") }
        if (saved) Text("URL salva.")
        HorizontalDivider()
        Text("Sessão", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { session.clear(); onLogout() }) { Text("Sair da conta") }
    }
}
