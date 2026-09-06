package com.korczak.control.settings

import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class IntegrationStatus(val name: String, val detail: String, val status: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var statuses by remember { mutableStateOf<List<IntegrationStatus>>(emptyList()) }

    suspend fun load() {
        loading = true; error = null
        when (val result = client.get("/api/integrations/status")) {
            is ApiResult.Success -> runCatching {
                val array = JSONObject(result.body).optJSONArray("items") ?: JSONArray()
                statuses = List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    val name = item.optString("name")
                    val icon = when (item.optString("id")) {
                        "github" -> Icons.Default.Code
                        "render" -> Icons.Default.Cloud
                        "mongodb" -> Icons.Default.Storage
                        else -> Icons.Default.Link
                    }
                    IntegrationStatus(name, item.optString("detail"), item.optString("status"), icon)
                }
            }.onFailure { error = "Não foi possível interpretar o status das integrações." }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Ajustes", style = MaterialTheme.typography.headlineMedium); Text("Status operacional das conexões do painel.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { scope.launch { load() } }, enabled = !loading) { Icon(Icons.Default.Refresh, "Atualizar") }
        }
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(error!!, Modifier.padding(16.dp)) }
            else -> statuses.forEach { IntegrationItem(it) }
        }
        HorizontalDivider()
        Text("Segurança", style = MaterialTheme.typography.titleMedium)
        Text("Credenciais são configuradas no servidor. O aplicativo apenas consulta o estado das integrações e não expõe tokens ou chaves privadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IntegrationItem(item: IntegrationStatus) {
    val label = when (item.status) { "connected" -> "CONECTADO"; "configured" -> "CONFIGURADO"; "unavailable" -> "INDISPONÍVEL"; else -> "NÃO CONFIGURADO" }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(item.icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}