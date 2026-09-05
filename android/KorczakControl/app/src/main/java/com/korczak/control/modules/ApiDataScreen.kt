package com.korczak.control.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager

@Composable
fun ApiDataScreen(title: String, path: String, subtitle: String = "Informações sincronizadas pela Korczak Control API") {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    var state by remember { mutableStateOf<Any>("loading") }
    suspend fun load() {
        state = "loading"
        state = when (val result = client.get(path)) {
            is ApiResult.Success -> result.body
            is ApiResult.Failure -> result
        }
    }
    LaunchedEffect(path) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall)
                        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        when (val current = state) {
            "loading" -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            is ApiResult.Failure -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Falha na comunicação", style = MaterialTheme.typography.titleMedium)
                    Text(current.message)
                    Button(onClick = { state = "loading" }) { Text("Tentar novamente") }
                }
            }
            is String -> Card(Modifier.fillMaxWidth().weight(1f, false)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resposta do sistema", style = MaterialTheme.typography.titleMedium)
                    Text(current, modifier = Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(onClick = { state = "loading" }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Sincronizar dados") }
        if (state == "loading") LaunchedEffect("reload-$path") { load() }
    }
}
