package com.korczak.control.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
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
    val client = remember { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<IntegrationItem>>(emptyList()) }
    var checkedAt by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true; error = null
        when (val result = client.get("/api/integrations/status")) {
            is ApiResult.Success -> try {
                val root = JSONObject(result.body)
                val array = root.optJSONArray("items") ?: JSONArray()
                items = buildList { for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(IntegrationItem(item.optString("id"), item.optString("name"), item.optString("status"), item.optString("detail")))
                } }
                checkedAt = root.optString("checkedAt")
            } catch (_: Exception) { error = "Resposta de integrações inválida." }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }
    val connected = items.count { it.status == "connected" }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Integrações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${connected}/${items.size} conectadas", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { scope.launch { load() } }, enabled = !loading) { Icon(Icons.Default.Refresh, "Atualizar") }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Falha ao verificar", fontWeight = FontWeight.Bold)
                    Text(error!!)
                    TextButton(onClick = { scope.launch { load() } }) { Text("TENTAR NOVAMENTE") }
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items, key = { it.id }) { IntegrationCard(it) }
                if (checkedAt.isNotBlank()) item {
                    Text("Verificado: $checkedAt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun IntegrationCard(item: IntegrationItem) {
    val (label, icon) = when (item.status) {
        "connected" -> "ONLINE" to Icons.Default.CheckCircle
        "configured" -> "CONFIGURADO" to Icons.Default.Settings
        "unavailable" -> "INDISPONÍVEL" to Icons.Default.WarningAmber
        else -> "NÃO CONFIGURADO" to Icons.Default.ErrorOutline
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(icon, null, modifier = Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
