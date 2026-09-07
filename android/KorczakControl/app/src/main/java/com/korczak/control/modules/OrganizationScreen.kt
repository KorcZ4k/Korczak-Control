@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

data class OrganizationAccount(
    val accountId: String,
    val name: String,
    val email: String,
    val role: String,
    val department: String,
    val active: Boolean,
    val permissions: JSONObject,
    val lastLoginAt: String = "",
    val createdAt: String = ""
)

private val permissionLabels = listOf(
    "github" to "GitHub",
    "render" to "Render",
    "bots" to "Bots",
    "sites" to "Sites",
    "applications" to "Aplicações",
    "apis" to "APIs"
)
private val roleLabels = listOf(
    "DIRECTOR", "MANAGER", "OPERATIONAL", "ADMINISTRATOR", "DEPARTMENT_MANAGER",
    "DEVELOPER", "STAFF", "EMPLOYEE", "VIEWER", "MOON_SUB_OWNER", "MOON_HEAD_ADMIN",
    "MOON_ADMIN", "MOON_MODERATOR", "MOON_HELPER"
)
private val departmentLabels = listOf("", "Korczak Technologies", "Moon Roleplaying", "Desenvolvimento", "Operações", "Tecnologia", "Administrativo")

private fun accountFromJson(item: JSONObject) = OrganizationAccount(
    accountId = item.optString("accountId"),
    name = item.optString("name"),
    email = item.optString("email"),
    role = item.optString("role"),
    department = item.optString("department"),
    active = item.optBoolean("active", true),
    permissions = item.optJSONObject("permissions") ?: JSONObject(),
    lastLoginAt = item.optString("lastLoginAt"),
    createdAt = item.optString("createdAt")
)

@Composable
fun OrganizationScreen() {
    val context = LocalContext.current
    val client = remember(context) { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<OrganizationAccount>>(emptyList()) }
    var selected by remember { mutableStateOf<OrganizationAccount?>(null) }
    var creating by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Todas") }

    suspend fun load() {
        loading = true
        error = null
        when (val result = client.get("/api/accounts")) {
            is ApiResult.Success -> runCatching {
                val array = JSONObject(result.body).optJSONArray("accounts")
                accounts = buildList { if (array != null) for (i in 0 until array.length()) add(accountFromJson(array.getJSONObject(i))) }
            }.onFailure { error = "Não foi possível interpretar as contas recebidas." }
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    if (creating) {
        CreateEmployeeScreen(
            onBack = { creating = false },
            onCreated = { account -> accounts = listOf(account) + accounts; creating = false; selected = account }
        )
        return
    }
    selected?.let { account ->
        AccountAdministrationScreen(
            account = account,
            onBack = { selected = null },
            onSaved = { updated -> accounts = accounts.map { if (it.accountId == updated.accountId) updated else it }; selected = updated },
            onDeactivated = { updated -> accounts = accounts.map { if (it.accountId == updated.accountId) updated else it }; selected = null }
        )
        return
    }

    val filtered = accounts.filter {
        val matchesSearch = search.isBlank() || "${it.name} ${it.email} ${it.accountId}".contains(search, ignoreCase = true)
        val matchesStatus = statusFilter == "Todas" || (statusFilter == "Ativas" && it.active) || (statusFilter == "Inativas" && !it.active)
        matchesSearch && matchesStatus
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ORGANIZAÇÃO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Contas e acessos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Gerenciamento central de funcionários, cargos e permissões.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = { scope.launch { load() } }, enabled = !loading) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Atualizar") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AccountMetric("Total", accounts.size, Modifier.weight(1f))
            AccountMetric("Ativas", accounts.count { it.active }, Modifier.weight(1f))
            AccountMetric("Inativas", accounts.count { !it.active }, Modifier.weight(1f))
        }
        Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Criar conta") }
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Pesquisar por nome, e-mail ou ID") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Ativas", "Inativas").forEach { label -> FilterChip(selected = statusFilter == label, onClick = { statusFilter = label }, label = { Text(label) }) }
        }
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> ErrorCard(error.orEmpty())
            filtered.isEmpty() -> EmptyAccountsCard()
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.accountId }) { account ->
                    ElevatedCard(Modifier.fillMaxWidth().clickable { selected = account }, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Person, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(listOf(account.role, account.department.ifBlank { "Sem departamento" }).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(account.accountId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AssistChip(onClick = { selected = account }, label = { Text(if (account.active) "Ativa" else "Inativa") }, leadingIcon = { Icon(if (account.active) Icons.Default.CheckCircle else Icons.Default.Block, null, Modifier.size(16.dp)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
private fun CreateEmployeeScreen(onBack: () -> Unit, onCreated: (OrganizationAccount) -> Unit) {
    val context = LocalContext.current
    val client = remember(context) { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("EMPLOYEE") }
    var department by remember { mutableStateOf("Korczak Technologies") }
    var roleExpanded by remember { mutableStateOf(false) }
    var departmentExpanded by remember { mutableStateOf(false) }
    var permissions by remember { mutableStateOf(defaultPermissions()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun mongoEnabled(key: String) = permissions.optJSONObject("mongodb")?.optBoolean(key, false) ?: false
    fun setMongo(key: String, value: Boolean) { val mongo = permissions.optJSONObject("mongodb") ?: JSONObject(); mongo.put(key, value); permissions.put("mongodb", mongo); permissions = JSONObject(permissions.toString()) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }; Column { Text("Nova conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Defina identidade, cargo e acessos", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            item {
                Text("IDENTIDADE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Nome completo") }, singleLine = true)
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("E-mail") }, singleLine = true)
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Senha inicial") }, supportingText = { Text("Mínimo de 12 caracteres") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Text("CARGO E DEPARTAMENTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                ExposedDropdownMenuBox(roleExpanded, { roleExpanded = !roleExpanded }) { OutlinedTextField(role, {}, Modifier.menuAnchor().fillMaxWidth().padding(top = 8.dp), readOnly = true, label = { Text("Cargo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) }); ExposedDropdownMenu(roleExpanded, { roleExpanded = false }) { roleLabels.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { role = value; roleExpanded = false }) } } }
                ExposedDropdownMenuBox(departmentExpanded, { departmentExpanded = !departmentExpanded }) { OutlinedTextField(department.ifBlank { "Sem departamento" }, {}, Modifier.menuAnchor().fillMaxWidth().padding(top = 8.dp), readOnly = true, label = { Text("Departamento") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(departmentExpanded) }); ExposedDropdownMenu(departmentExpanded, { departmentExpanded = false }) { departmentLabels.forEach { value -> DropdownMenuItem(text = { Text(value.ifBlank { "Sem departamento" }) }, onClick = { department = value; departmentExpanded = false }) } } }
                PermissionEditor(permissions, { permissions = it }, ::mongoEnabled, ::setMongo)
            }
        }
        message?.let { Text(it, color = if (it.startsWith("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        Button(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = {
            saving = true; message = null
            scope.launch {
                val body = JSONObject().put("name", name).put("email", email).put("password", password).put("role", role).put("department", department).put("permissions", permissions)
                when (val result = client.post("/api/accounts", body)) {
                    is ApiResult.Success -> runCatching { onCreated(accountFromJson(JSONObject(result.body).getJSONObject("account"))) }.onFailure { message = "Erro: resposta inválida do servidor." }
                    is ApiResult.Failure -> message = "Erro: ${result.message}"
                }
                saving = false
            }
        }) { if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Criar conta") } }
    }
}

@Composable
private fun PermissionEditor(permissions: JSONObject, onPermissionsChanged: (JSONObject) -> Unit, mongoEnabled: (String) -> Boolean, setMongo: (String, Boolean) -> Unit) {
    Text("PERMISSÕES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
    permissionLabels.forEach { (key, label) -> PermissionSwitch(label, permissions.optBoolean(key, false)) { value -> permissions.put(key, value); onPermissionsChanged(JSONObject(permissions.toString())) } }
    Text("BASES DE DADOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
    PermissionSwitch("Korczak Control", mongoEnabled("KorczakControl")) { setMongo("KorczakControl", it) }
    PermissionSwitch("Tensura Moon", mongoEnabled("TensuraMoon")) { setMongo("TensuraMoon", it) }
    PermissionSwitch("Korczak Tech Site", mongoEnabled("KorczakTechSite")) { setMongo("KorczakTechSite", it) }
}

@Composable
private fun AccountAdministrationScreen(account: OrganizationAccount, onBack: () -> Unit, onSaved: (OrganizationAccount) -> Unit, onDeactivated: (OrganizationAccount) -> Unit) {
    val context = LocalContext.current
    val client = remember(context) { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(account.name) }
    var email by remember { mutableStateOf(account.email) }
    var role by remember { mutableStateOf(account.role) }
    var department by remember { mutableStateOf(account.department) }
    var permissions by remember { mutableStateOf(normalizedPermissions(account.permissions)) }
    var active by remember { mutableStateOf(account.active) }
    var saving by remember { mutableStateOf(false) }
    var resetPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDeactivate by remember { mutableStateOf(false) }

    fun mongoEnabled(key: String) = permissions.optJSONObject("mongodb")?.optBoolean(key, false) ?: false
    fun setMongo(key: String, value: Boolean) { val mongo = permissions.optJSONObject("mongodb") ?: JSONObject(); mongo.put(key, value); permissions.put("mongodb", mongo); permissions = JSONObject(permissions.toString()) }
    fun updated() = account.copy(name = name, email = email, role = role, department = department, active = active, permissions = permissions)

    if (confirmDeactivate) AlertDialog(onDismissRequest = { confirmDeactivate = false }, title = { Text("Desativar conta?") }, text = { Text("A conta perderá acesso ao sistema, mas seus dados e histórico serão preservados.") }, confirmButton = { TextButton(onClick = { scope.launch { saving = true; when (val result = client.delete("/api/accounts/${account.accountId}")) { is ApiResult.Success -> { active = false; onDeactivated(updated()) }; is ApiResult.Failure -> { message = "Erro: ${result.message}"; confirmDeactivate = false } }; saving = false } }) { Text("Desativar") } }, dismissButton = { TextButton(onClick = { confirmDeactivate = false }) { Text("Cancelar") } })

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }; Column { Text(account.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(account.accountId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            item {
                Text("INFORMAÇÕES DA CONTA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("E-mail") }, singleLine = true)
                Text("CARGO E ACESSO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Cargo") }, supportingText = { Text("Use os cargos definidos pela organização") }, singleLine = true)
                OutlinedTextField(department, { department = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Departamento") }, singleLine = true)
                PermissionSwitch("Conta ativa", active) { active = it }
                PermissionEditor(permissions, { permissions = it }, ::mongoEnabled, ::setMongo)
                Text("REDEFINIR SENHA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(resetPassword, { resetPassword = it }, Modifier.fillMaxWidth(), label = { Text("Nova senha") }, supportingText = { Text("Mínimo de 12 caracteres") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedButton(onClick = { if (resetPassword.length < 12) { message = "Erro: a nova senha deve possuir pelo menos 12 caracteres."; return@OutlinedButton }; scope.launch { saving = true; when (val result = client.patch("/api/accounts/${account.accountId}/password", JSONObject().put("password", resetPassword))) { is ApiResult.Success -> { resetPassword = ""; message = "Senha redefinida com sucesso." }; is ApiResult.Failure -> message = "Erro: ${result.message}" }; saving = false } }, enabled = !saving, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Redefinir senha") }
                if (account.active) OutlinedButton(onClick = { confirmDeactivate = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), enabled = !saving) { Icon(Icons.Default.Block, null); Spacer(Modifier.width(8.dp)); Text("Desativar conta") }
            }
        }
        message?.let { Text(it, color = if (it.startsWith("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        Button(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = {
            saving = true; message = null
            scope.launch {
                val profileResult = client.patch("/api/accounts/${account.accountId}", JSONObject().put("name", name).put("email", email))
                if (profileResult is ApiResult.Failure) message = "Erro: ${profileResult.message}" else {
                    val body = JSONObject().put("permissions", permissions).put("active", active).put("role", role).put("department", department)
                    when (val result = client.patch("/api/accounts/${account.accountId}/access", body)) { is ApiResult.Success -> { message = "Alterações salvas."; onSaved(updated()) }; is ApiResult.Failure -> message = "Erro: ${result.message}" }
                }
                saving = false
            }
        }) { if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Salvar alterações") } }
    }
}

private fun defaultPermissions(): JSONObject = JSONObject().put("mongodb", JSONObject())
private fun normalizedPermissions(source: JSONObject): JSONObject {
    val result = JSONObject(source.toString())
    val mongo = result.optJSONObject("mongodb") ?: JSONObject()
    if (mongo.has("MoonTensura") && !mongo.has("TensuraMoon")) mongo.put("TensuraMoon", mongo.optBoolean("MoonTensura", false))
    mongo.remove("MoonTensura")
    result.put("mongodb", mongo)
    return result
}

@Composable
private fun PermissionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { ElevatedCard(Modifier.fillMaxWidth().padding(top = 6.dp), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge); Switch(checked = checked, onCheckedChange = onCheckedChange) } } }
@Composable
private fun ErrorCard(message: String) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) { Text(message, Modifier.padding(18.dp)) } }
@Composable
private fun EmptyAccountsCard() { ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.primary); Text("Nenhuma conta encontrada", fontWeight = FontWeight.SemiBold); Text("Crie uma conta ou altere os filtros de pesquisa.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
