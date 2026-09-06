package com.korczak.control.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class IntegrationItem(val id: String, val name: String, val status: String, val detail: String)

@Composable
fun IntegrationsScreen() {
    val context = LocalContext.current
    val client = remember(context) { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<IntegrationItem>>(emptyList()) }
    var checkedAt by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        error = null
        when (val result = client.get("/api/integrations/status")) {
            is ApiResult.Success -> runCatching {
                val root = JSONObject(result.body)
                val array = root.optJSONArray("items") ?: JSONArray()
                items = List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    IntegrationItem(item.optString("id"), item.optString("name"), item.optString("status"), item.optString("detail"))
                }
                checkedAt = root.optString("checkedAt")
            }.onFailure { error = "Não foi possível interpretar o status das integrações." }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }
    val connected = items.count { it.status == "connected" }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("INTEGRAÇÕES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Serviços conectados", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(if (items.isEmpty()) "Verificando conexões do ambiente." else "$connected de ${items.size} serviços conectados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = { scope.launch { load() } }, enabled = !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, "Atualizar")
            }
        }

        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Não foi possível consultar as integrações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(error.orEmpty())
                    TextButton(onClick = { scope.launch { load() } }) { Text("Tentar novamente") }
                }
            }
            items.isEmpty() -> EmptyIntegrationsState()
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items, key = { it.id }) { IntegrationCard(it) }
                if (checkedAt.isNotBlank()) item {
                    Text("Última verificação: $checkedAt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyIntegrationsState() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.LinkOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Nenhuma integração retornada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("O servidor não informou integrações disponíveis para esta conta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IntegrationCard(item: IntegrationItem) {
    val (label, icon) = when (item.status) {
        "connected" -> "CONECTADO" to Icons.Default.CheckCircle
        "configured" -> "CONFIGURADO" to Icons.Default.Settings
        "unavailable" -> "INDISPONÍVEL" to Icons.Default.WarningAmber
        else -> "NÃO CONFIGURADO" to Icons.Default.ErrorOutline
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (item.detail.isNotBlank()) Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
