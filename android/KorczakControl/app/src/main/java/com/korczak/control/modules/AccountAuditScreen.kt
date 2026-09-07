package com.korczak.control.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
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
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AccountAuditActivity(
    val id: String,
    val action: String,
    val actorName: String,
    val actorRole: String,
    val targetName: String,
    val createdAt: String,
    val details: JSONObject
)

private fun auditActivityFromJson(item: JSONObject) = AccountAuditActivity(
    id = item.optString("id"),
    action = item.optString("action"),
    actorName = item.optString("actorName", item.optString("actorAccountId")),
    actorRole = item.optString("actorRole"),
    targetName = item.optString("targetName", item.optString("targetAccountId")),
    createdAt = item.optString("createdAt"),
    details = item.optJSONObject("details") ?: JSONObject()
)

private fun auditActionLabel(action: String) = when (action) {
    "account.registered" -> "Conta registrada"
    "account.created" -> "Conta criada"
    "account.login" -> "Login realizado"
    "account.profile_updated" -> "Perfil atualizado"
    "account.profile_admin_updated" -> "Perfil alterado pela administração"
    "account.access_updated" -> "Acessos alterados"
    "account.password_changed" -> "Senha alterada"
    "account.password_reset" -> "Senha redefinida"
    "account.deactivated" -> "Conta desativada"
    else -> action.replace('.', ' ').replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun auditDate(value: String): String = runCatching {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm")
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(formatter)
}.getOrElse { value.ifBlank { "Data indisponível" } }

private fun auditDetailsText(details: JSONObject): String {
    val keys = details.keys().asSequence().toList()
    if (keys.isEmpty()) return "Nenhuma informação adicional registrada."
    return keys.joinToString(" • ") { key ->
        val value = details.opt(key)
        val label = key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ').replaceFirstChar { it.uppercase() }
        "$label: ${when (value) { is JSONObject -> "Informações registradas" else -> value.toString() }}"
    }
}

@Composable
fun AccountAuditScreen(onBack: () -> Unit, accountId: String? = null, title: String = "Histórico de auditoria") {
    val context = LocalContext.current
    val client = remember(context) { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var activities by remember { mutableStateOf<List<AccountAuditActivity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionFilter by remember { mutableStateOf("Todas") }

    fun load() {
        scope.launch {
            loading = true
            error = null
            val suffix = accountId?.takeIf { it.isNotBlank() }?.let { "?accountId=$it" } ?: ""
            when (val result = client.get("/api/audit$suffix")) {
                is ApiResult.Success -> runCatching {
                    val array = JSONObject(result.body).optJSONArray("activities")
                    activities = buildList {
                        if (array != null) for (index in 0 until array.length()) add(auditActivityFromJson(array.getJSONObject(index)))
                    }
                }.onFailure { error = "Não foi possível interpretar o histórico recebido." }
                is ApiResult.Failure -> error = result.message
            }
            loading = false
        }
    }

    LaunchedEffect(accountId) { load() }
    val actionOptions = listOf("Todas") + activities.map { auditActionLabel(it.action) }.distinct()
    val visible = activities.filter { actionFilter == "Todas" || auditActionLabel(it.action) == actionFilter }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("SEGURANÇA E RASTREABILIDADE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Ações administrativas e acessos registrados em ordem cronológica.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = ::load, enabled = !loading) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Atualizar")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ElevatedCard(Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(activities.size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Eventos carregados", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ElevatedCard(Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(activities.map { it.actorName }.distinct().size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Responsáveis", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (actionOptions.size > 1) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                actionOptions.take(4).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = actionFilter == option,
                        onClick = { actionFilter = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = actionOptions.take(4).size)
                    ) { Text(option, maxLines = 1) }
                }
            }
        }

        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> ElevatedCard(Modifier.fillMaxWidth()) { Text(error.orEmpty(), Modifier.padding(18.dp), color = MaterialTheme.colorScheme.error) }
            visible.isEmpty() -> ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.History, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Nenhum evento encontrado", fontWeight = FontWeight.SemiBold)
                    Text("Os próximos acessos e alterações aparecerão aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(visible, key = { it.id.ifBlank { "${it.action}-${it.createdAt}" } }) { item ->
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Icon(Icons.Default.History, null, Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(auditActionLabel(item.action), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(auditDate(item.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("Responsável: ${item.actorName}${item.actorRole.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
                            Text("Conta afetada: ${item.targetName}", style = MaterialTheme.typography.bodyMedium)
                            Text(auditDetailsText(item.details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
