package com.korczak.control.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.People
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
import org.json.JSONObject

data class OrganizationAccount(
    val accountId: String,
    val name: String,
    val role: String,
    val department: String,
    val active: Boolean,
    val permissions: JSONObject
)

private val permissionLabels = listOf(
    "github" to "GitHub",
    "render" to "Render",
    "bots" to "Bots",
    "sites" to "Sites",
    "applications" to "Aplicações",
    "apis" to "APIs"
)

@Composable
fun OrganizationScreen() {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<OrganizationAccount>>(emptyList()) }
    var selected by remember { mutableStateOf<OrganizationAccount?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true; error = null
        when (val result = client.get("/api/accounts")) {
            is ApiResult.Success -> try {
                val array = JSONObject(result.body).optJSONArray("accounts")
                accounts = buildList {
                    if (array != null) for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        add(OrganizationAccount(item.optString("accountId"), item.optString("name"), item.optString("role"), item.optString("department"), item.optBoolean("active", true), item.optJSONObject("permissions") ?: JSONObject()))
                    }
                }
            } catch (_: Exception) { error = "Resposta de contas inválida." }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    if (selected != null) {
        AccountAdministrationScreen(selected!!, onBack = { selected = null }, onSaved = { updated ->
            accounts = accounts.map { if (it.accountId == updated.accountId) updated else it }
            selected = updated
        })
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column { Text("Organização", style = MaterialTheme.typography.headlineSmall); Text("Korczak Technologies · Moon Roleplaying", style = MaterialTheme.typography.labelMedium) }
        }
        Text("Selecione uma conta abaixo da sua hierarquia para visualizar o perfil e administrar suas permissões.", style = MaterialTheme.typography.bodySmall)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.accountId }) { account ->
                    ElevatedCard(Modifier.fillMaxWidth().clickable { selected = account }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(account.name, style = MaterialTheme.typography.titleMedium); Text("${account.role} · ${account.department.ifBlank { "Sem departamento" }}", style = MaterialTheme.typography.labelSmall) }
                            AssistChip(onClick = { selected = account }, label = { Text(if (account.active) "ATIVO" else "INATIVO") })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountAdministrationScreen(account: OrganizationAccount, onBack: () -> Unit, onSaved: (OrganizationAccount) -> Unit) {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var permissions by remember { mutableStateOf(JSONObject(account.permissions.toString())) }
    var active by remember { mutableStateOf(account.active) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun mongoEnabled(key: String) = permissions.optJSONObject("mongodb")?.optBoolean(key, false) ?: false
    fun setMongo(key: String, value: Boolean) {
        val mongo = permissions.optJSONObject("mongodb") ?: JSONObject()
        mongo.put(key, value); permissions.put("mongodb", mongo)
        permissions = JSONObject(permissions.toString())
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Column { Text(account.name, style = MaterialTheme.typography.headlineSmall); Text("${account.role} · ${account.accountId}", style = MaterialTheme.typography.labelSmall) }
        }
        Text("Controle administrativo", style = MaterialTheme.typography.titleMedium)
        Text("Altere apenas permissões que este nível hierárquico está autorizado a administrar.", style = MaterialTheme.typography.bodySmall)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            item {
                PermissionSwitch("Conta ativa", active) { active = it }
                permissionLabels.forEach { (key, label) -> PermissionSwitch(label, permissions.optBoolean(key, false)) { value -> permissions.put(key, value); permissions = JSONObject(permissions.toString()) } }
                Text("MongoDB", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                PermissionSwitch("KorczakControl", mongoEnabled("KorczakControl")) { setMongo("KorczakControl", it) }
                PermissionSwitch("MoonTensura", mongoEnabled("MoonTensura")) { setMongo("MoonTensura", it) }
                PermissionSwitch("KorczakTechSite", mongoEnabled("KorczakTechSite")) { setMongo("KorczakTechSite", it) }
            }
            message?.let { item { Text(it, color = if (it.startsWith("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } }
        }

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
        }) { if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Salvar alterações") }
    }
}

@Composable
private fun PermissionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
