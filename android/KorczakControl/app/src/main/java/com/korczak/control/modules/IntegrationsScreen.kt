package com.korczak.control.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
        loading = true
        error = null
        when (val result = client.get("/api/integrations/status")) {
            is ApiResult.Success -> {
                try {
                    val root = JSONObject(result.body)
                    val array = root.optJSONArray("items") ?: JSONArray()
                    items = buildList {
                        for (index in 0 until array.length()) {
                            val item = array.getJSONObject(index)
                            add(IntegrationItem(
                                id = item.optString("id"),
                                name = item.optString("name"),
                                status = item.optString("status"),
                                detail = item.optString("detail")
                            ))
                        }
                    }
                    checkedAt = root.optString("checkedAt")
                } catch (exception: Exception) {
                    error = "Resposta de integrações inválida."
                }
            }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Integrações", style = MaterialTheme.typography.headlineSmall)
                    Text("Status verificado diretamente pela Korczak Control API", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { scope.launch { load() } }, enabled = !loading) { Icon(Icons.Default.Refresh, "Atualizar") }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Falha ao verificar integrações", style = MaterialTheme.typography.titleMedium)
                    Text(error!!)
                    Button(onClick = { scope.launch { load() } }) { Text("Tentar novamente") }
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item -> IntegrationCard(item) }
                if (checkedAt.isNotBlank()) item { Text("Última verificação: $checkedAt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun IntegrationCard(item: IntegrationItem) {
    val (label, icon) = when (item.status) {
        "connected" -> "Conectado" to Icons.Default.Link
        "configured" -> "Configurado" to Icons.Default.Settings
        "unavailable" -> "Indisponível" to Icons.Default.WarningAmber
        else -> "Não configurado" to Icons.Default.ErrorOutline
    }

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = {}, enabled = false, label = { Text(label) })
        }
    }
}
