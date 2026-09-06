package com.korczak.control.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import org.json.JSONObject

data class OrganizationAccount(val accountId: String, val name: String, val role: String, val department: String, val active: Boolean, val permissions: JSONObject)

private val permissionLabels = listOf("github" to "GitHub", "render" to "Render", "bots" to "Bots", "sites" to "Sites", "applications" to "Aplicações", "apis" to "APIs")

@Composable
fun OrganizationScreen() {
    val client = remember { ApiClient(SessionManager(LocalContext.current)) }
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<OrganizationAccount>>(emptyList()) }
    var selected by remember { mutableStateOf<OrganizationAccount?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true; error = null
        when (val result = client.get("/api/accounts")) {
            is ApiResult.Success -> runCatching {
                val array = JSONObject(result.body).optJSONArray("accounts")
                accounts = buildList {
                    if (array != null) for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        add(OrganizationAccount(item.optString("accountId"), item.optString("name"), item.optString("role"), item.optString("department"), item.optBoolean("active", true), item.optJSONObject("permissions") ?: JSONObject()))
                    }
                }
            }.onFailure { error = "Não foi possível interpretar a lista de contas." }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }
    selected?.let { account -> AccountAdministrationScreen(account, { selected = null }) { updated -> accounts = accounts.map { if (it.accountId == updated.accountId) updated else it }; selected = updated }; return }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ORGANIZAÇÃO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Contas e acessos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Gerencie contas dentro do seu nível de autorização.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) { Text(error.orEmpty(), Modifier.padding(18.dp)) }
            accounts.isEmpty() -> Text("Nenhuma conta foi retornada para administração.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(accounts, key = { it.accountId }) { account ->
                    ElevatedCard(Modifier.fillMaxWidth().clickable { selected = account }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Person, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(listOf(account.role, account.department.ifBlank { "Sem departamento" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(if (account.active) "ATIVA" else "INATIVA", style = MaterialTheme.typography.labelSmall, color = if (account.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountAdministrationScreen(account: OrganizationAccount, onBack: () -> Unit, onSaved: (OrganizationAccount) -> Unit) {
    val client = remember { ApiClient(SessionManager(LocalContext.current)) }
    val scope = rememberCoroutineScope()
    var permissions by remember { mutableStateOf(JSONObject(account.permissions.toString())) }
    var active by remember { mutableStateOf(account.active) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun mongoEnabled(key: String) = permissions.optJSONObject("mongodb")?.optBoolean(key, false) ?: false
    fun setMongo(key: String, value: Boolean) { val mongo = permissions.optJSONObject("mongodb") ?: JSONObject(); mongo.put(key, value); permissions.put("mongodb", mongo); permissions = JSONObject(permissions.toString()) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            Column { Text(account.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(account.accountId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text("PERMISSÕES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("As alterações são enviadas ao servidor e ficam sujeitas às regras de autorização.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            item {
                PermissionSwitch("Conta ativa", active) { active = it }
                permissionLabels.forEach { (key, label) -> PermissionSwitch(label, permissions.optBoolean(key, false)) { value -> permissions.put(key, value); permissions = JSONObject(permissions.toString()) } }
                Text("MONGODB", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                PermissionSwitch("Korczak Control", mongoEnabled("KorczakControl")) { setMongo("KorczakControl", it) }
                PermissionSwitch("Moon Tensura", mongoEnabled("MoonTensura")) { setMongo("MoonTensura", it) }
                PermissionSwitch("Korczak Tech Site", mongoEnabled("KorczakTechSite")) { setMongo("KorczakTechSite", it) }
            }
        }
        message?.let { Text(it, color = if (it.startsWith("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        Button(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = {
            saving = true; message = null
            scope.launch {
                val body = JSONObject().put("permissions", permissions).put("active", active)
                when (val result = client.patch("/api/accounts/${account.accountId}/permissions", body)) {
                    is ApiResult.Success -> { message = "Permissões atualizadas."; onSaved(account.copy(active = active, permissions = permissions)) }
                    is ApiResult.Failure -> message = "Erro: ${result.message}"
                }
                saving = false
            }
        }) { if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Salvar alterações") }
    }
}

@Composable
private fun PermissionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
