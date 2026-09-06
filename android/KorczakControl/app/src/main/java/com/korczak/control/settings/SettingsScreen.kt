package com.korczak.control.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val client = remember(context) { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var statuses by remember { mutableStateOf<List<IntegrationStatus>>(emptyList()) }

    suspend fun load() {
        loading = true
        error = null
        when (val result = client.get("/api/integrations/status")) {
            is ApiResult.Success -> runCatching {
                val array = JSONObject(result.body).optJSONArray("items") ?: JSONArray()
                statuses = List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    val icon = when (item.optString("id")) {
                        "github" -> Icons.Default.Code
                        "render" -> Icons.Default.Cloud
                        "mongodb" -> Icons.Default.Storage
                        else -> Icons.Default.Link
                    }
                    IntegrationStatus(item.optString("name"), item.optString("detail"), item.optString("status"), icon)
                }
            }.onFailure { error = "Não foi possível interpretar o status das conexões." }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AJUSTES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Configuração e segurança", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Informações operacionais do aplicativo e das conexões.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = { scope.launch { load() } }, enabled = !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, "Atualizar")
            }
        }

        Text("CONEXÕES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(error.orEmpty(), Modifier.padding(18.dp)) }
            statuses.isEmpty() -> Text("Nenhum status de integração foi informado pelo servidor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> statuses.forEach { IntegrationRow(it) }
        }

        Text("SEGURANÇA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        ElevatedCard(shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Credenciais protegidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Tokens, senhas e chaves privadas permanecem no ambiente do servidor. O aplicativo recebe apenas o estado operacional necessário.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun IntegrationRow(item: IntegrationStatus) {
    val label = when (item.status) { "connected" -> "CONECTADO"; "configured" -> "CONFIGURADO"; "unavailable" -> "INDISPONÍVEL"; else -> "NÃO CONFIGURADO" }
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (item.detail.isNotBlank()) Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
