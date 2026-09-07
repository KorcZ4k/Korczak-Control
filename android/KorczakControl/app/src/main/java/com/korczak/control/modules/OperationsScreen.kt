package com.korczak.control.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private data class MongoTarget(val label: String, val database: String)
private val mongoTargets = listOf(MongoTarget("Korczak Control", "KorczakControl"), MongoTarget("KZ Site", "KorczakTechSite"), MongoTarget("Moon", "TensuraMoon"))

@Composable
fun OperationsScreen(section: String) {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var databases by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedDatabase by remember { mutableStateOf<MongoTarget?>(null) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }
    var documents by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var repository by remember { mutableStateOf("") }
    var workflows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    suspend fun request(path: String, action: String = "GET", body: JSONObject? = null): String? {
        loading = true; error = null
        val result = when (action) { "POST" -> client.post(path, body ?: JSONObject()); "PATCH" -> client.patch(path, body ?: JSONObject()); "DELETE" -> client.delete(path); else -> client.get(path) }
        loading = false
        return when (result) { is ApiResult.Success -> result.body; is ApiResult.Failure -> { error = result.message; null } }
    }
    fun parseItems(body: String): List<JSONObject> = runCatching { val array = JSONObject(body).optJSONArray("items") ?: JSONArray(); List(array.length()) { array.optJSONObject(it) ?: JSONObject() } }.getOrDefault(emptyList())
    suspend fun loadMain() {
        val path = when(section) { "clients" -> "/api/customers"; "apps" -> "/api/applications"; "bots" -> "/api/bots"; "sites" -> "/api/sites"; "apis" -> "/api/managed/api"; else -> "" }
        if (path.isNotBlank()) request(path)?.let { items = parseItems(it) }
    }
    suspend fun loadCollections(target: MongoTarget) { selectedDatabase = target; selectedCollection = null; documents = emptyList(); request("/api/databases/${target.database}/collections")?.let { items = parseItems(it) } }
    suspend fun loadDocuments(name: String) { val target = selectedDatabase ?: return; selectedCollection = name; request("/api/databases/${target.database}/collections/$name/documents?limit=100")?.let { documents = parseItems(it) } }
    suspend fun loadGithub() { val body = request("/api/bots/tensura-moon/workflows") ?: return; val json = JSONObject(body); repository = json.optString("repository"); val array = json.optJSONArray("workflows") ?: JSONArray(); workflows = List(array.length()) { array.getJSONObject(it) } }

    LaunchedEffect(section) { when(section) { "databases" -> request("/api/databases")?.let { databases = parseItems(it) }; "github" -> loadGithub(); else -> loadMain() } }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header(section, loading)
        error?.let { ErrorCard(it) { scope.launch { when(section) { "github" -> loadGithub(); "databases" -> selectedDatabase?.let { loadCollections(it) } ?: request("/api/databases")?.let { databases = parseItems(it) }; else -> loadMain() } } }
        when(section) {
            "databases" -> MongoModule(selectedDatabase, selectedCollection, items, documents, onSelectDatabase = { scope.launch { loadCollections(it) } }, onSelectCollection = { scope.launch { loadDocuments(it) } }, onCreateCollection = { name -> scope.launch { selectedDatabase?.let { target -> request("/api/databases/${target.database}/collections", "POST", JSONObject().put("name", name)); loadCollections(target) } } })
            "clients" -> ResourceModule("Clientes", "Nome e ID são obrigatórios. Os demais campos permanecem opcionais.", items, listOf("Nome", "ID", "E-mail", "Telefone", "Status", "Serviço", "Observações"), onRefresh = { scope.launch { loadMain() } }, onCreate = { data -> scope.launch { request("/api/customers", "POST", JSONObject().put("name", data["Nome"]).put("externalId", data["ID"]).put("email", data["E-mail"]).put("phone", data["Telefone"]).put("status", data["Status"].ifBlank { "active" }).put("service", data["Serviço"]).put("notes", data["Observações"])); loadMain() } }, onDelete = { item -> scope.launch { request("/api/customers/${item.optString("_id")}", "DELETE"); loadMain() } })
            "apps" -> ResourceModule("Aplicações", "Registre cada aplicação com seus vínculos reais.", items, listOf("Nome", "Identificador", "Versão", "Plataformas", "Repositório", "API", "Site", "Banco", "Status", "Observações"), onRefresh = { scope.launch { loadMain() } }, onCreate = { d -> scope.launch { request("/api/applications", "POST", JSONObject().put("name", d["Nome"]).put("slug", slug(d["Identificador"].ifBlank { d["Nome"] })).put("version", d["Versão"]).put("platforms", JSONArray(d["Plataformas"].split(',').map { it.trim() }.filter { it.isNotBlank() })).put("repository", d["Repositório"]).put("apiUrl", d["API"]).put("siteUrl", d["Site"]).put("databaseKey", d["Banco"]).put("status", d["Status"].ifBlank { "unknown" }).put("notes", d["Observações"])); loadMain() } }, onDelete = { item -> scope.launch { request("/api/applications/${item.optString("slug")}", "DELETE"); loadMain() } })
            "bots" -> BotsModule(items, onRefresh = { scope.launch { loadMain() } }, onWorkflows = { bot -> scope.launch { val body = request("/api/bots/${bot.optString("slug")}/workflows") ?: return@launch; val json = JSONObject(body); repository = json.optString("repository"); val array = json.optJSONArray("workflows") ?: JSONArray(); workflows = List(array.length()) { array.getJSONObject(it) } } }, workflows = workflows, repository = repository, onRun = { wf -> scope.launch { val parts = repository.split('/'); if (parts.size == 2) request("/api/github/repos/${parts[0]}/${parts[1]}/workflows/${wf.optString("id")}/dispatch", "POST", JSONObject().put("ref", "main")) } })
            "github" -> GithubModule(repository, workflows, onRefresh = { scope.launch { loadGithub() } }, onRun = { wf -> scope.launch { val parts = repository.split('/'); if (parts.size == 2) request("/api/github/repos/${parts[0]}/${parts[1]}/workflows/${wf.optString("id")}/dispatch", "POST", JSONObject().put("ref", "main")) } })
            "sites" -> ResourceModule("Sites", "Registre URLs reais e acompanhe sua disponibilidade.", items, listOf("Nome", "Identificador", "URL", "Repositório", "Tecnologia", "Status", "Observações"), onRefresh = { scope.launch { loadMain() } }, onCreate = { d -> scope.launch { request("/api/sites", "POST", JSONObject().put("name", d["Nome"]).put("slug", slug(d["Identificador"].ifBlank { d["Nome"] })).put("url", d["URL"]).put("repository", d["Repositório"]).put("technology", d["Tecnologia"]).put("status", d["Status"].ifBlank { "unknown" }).put("notes", d["Observações"])); loadMain() } }, onDelete = null)
            "apis" -> ResourceModule("APIs", "Centralize as APIs e seus vínculos de projeto.", items, listOf("Nome", "Identificador", "URL", "Repositório", "Tecnologia", "Versão", "Status", "Observações"), onRefresh = { scope.launch { loadMain() } }, onCreate = { d -> scope.launch { request("/api/managed/api", "POST", JSONObject().put("name", d["Nome"]).put("slug", slug(d["Identificador"].ifBlank { d["Nome"] })).put("url", d["URL"]).put("repository", d["Repositório"]).put("technology", d["Tecnologia"]).put("version", d["Versão"]).put("status", d["Status"].ifBlank { "unknown" }).put("notes", d["Observações"])); loadMain() } }, onDelete = null)
            else -> InfoCard("Este módulo será carregado pelo serviço correspondente.")
        }
    }
}

@Composable private fun Header(section: String, loading: Boolean) { val title = mapOf("databases" to "MongoDB", "clients" to "Clientes", "apps" to "Aplicações", "bots" to "Bots", "github" to "GitHub e Workflows", "sites" to "Sites", "apis" to "APIs")[section] ?: section; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) } }

@Composable private fun MongoModule(target: MongoTarget?, selected: String?, collections: List<JSONObject>, documents: List<JSONObject>, onSelectDatabase: (MongoTarget) -> Unit, onSelectCollection: (String) -> Unit, onCreateCollection: (String) -> Unit) {
    if (target == null) { Text("Selecione o banco", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); mongoTargets.forEach { db -> ElevatedCard(onClick = { onSelectDatabase(db) }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Storage, null); Spacer(Modifier.width(12.dp)); Column { Text(db.label, fontWeight = FontWeight.Bold); Text(db.database, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
    else if (selected == null) { var name by remember(target.database) { mutableStateOf("") }; Text(target.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); OutlinedTextField(name, { name = it }, label = { Text("Nova collection") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Button(onClick = { if (name.isNotBlank()) { onCreateCollection(name.trim()); name = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Criar collection") }; if (collections.isEmpty()) InfoCard("Nenhuma collection encontrada.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(collections, key = { it.optString("name") }) { item -> ElevatedCard(onClick = { onSelectCollection(item.optString("name")) }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.optString("name"), fontWeight = FontWeight.Bold); Text("${item.optLong("estimatedDocumentCount")} documentos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } } } } }
    else { Text(selected, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (documents.isEmpty()) InfoCard("Nenhum documento encontrado.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(documents, key = { it.optString("_id", it.hashCode().toString()) }) { item -> ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { DetailRows(item) } } } } }
}

@Composable private fun ResourceModule(title: String, description: String, items: List<JSONObject>, fields: List<String>, onRefresh: () -> Unit, onCreate: (Map<String,String>) -> Unit, onDelete: ((JSONObject) -> Unit)?) {
    var creating by remember { mutableStateOf(false) }; var values by remember { mutableStateOf(fields.associateWith { "" }) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(description, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Row { IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Atualizar") }; IconButton(onClick = { values = fields.associateWith { "" }; creating = true }) { Icon(Icons.Default.Add, "Adicionar") } } }
    if (creating) ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Adicionar $title", fontWeight = FontWeight.Bold); fields.forEach { field -> OutlinedTextField(values[field].orEmpty(), { values = values.toMutableMap().apply { put(field, it) } }, label = { Text(field) }, modifier = Modifier.fillMaxWidth()) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onCreate(values); creating = false }) { Text("Salvar") }; TextButton(onClick = { creating = false }) { Text("Cancelar") } } } }
    if (items.isEmpty()) InfoCard("Nenhum registro encontrado.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items, key = { it.optString("_id", it.optString("slug", it.optString("name"))) }) { item -> ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(item.optString("name", title.dropLast(1)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); DetailRows(item, setOf("_id", "name", "history", "createdAt", "updatedAt")); onDelete?.let { remove -> TextButton(onClick = { remove(item) }) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Excluir") } } } } } }
}

@Composable private fun BotsModule(items: List<JSONObject>, onRefresh: () -> Unit, onWorkflows: (JSONObject) -> Unit, workflows: List<JSONObject>, repository: String, onRun: (JSONObject) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onRefresh) { Text("Atualizar") } }; if (items.isEmpty()) InfoCard("Nenhum bot registrado.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items, key = { it.optString("slug") }) { bot -> ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(bot.optString("name"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); DetailRows(bot, setOf("_id", "name", "notes", "createdAt", "updatedAt")); TextButton(onClick = { onWorkflows(bot) }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Workflows do projeto") } } } }; if (workflows.isNotEmpty()) { Text("Workflows de $repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); workflows.forEach { wf -> ElevatedCard { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(wf.optString("name"), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold); IconButton(onClick = { onRun(wf) }) { Icon(Icons.Default.PlayArrow, "Executar") } } } } } } }

@Composable private fun GithubModule(repository: String, workflows: List<JSONObject>, onRefresh: () -> Unit, onRun: (JSONObject) -> Unit) { Text(repository.ifBlank { "Repositório não configurado" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); TextButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Atualizar workflows") }; if (workflows.isEmpty()) InfoCard("Nenhum workflow foi retornado para o projeto configurado.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(workflows, key = { it.optString("id") }) { wf -> ElevatedCard { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(wf.optString("name"), fontWeight = FontWeight.Bold); Text(wf.optString("path"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick = { onRun(wf) }) { Icon(Icons.Default.PlayArrow, "Executar workflow") } } } } } }

@Composable private fun DetailRows(json: JSONObject, excluded: Set<String> = emptySet()) { json.keys().asSequence().toList().filter { it !in excluded }.forEach { key -> val value = json.opt(key); if (value !is JSONObject && value !is JSONArray) { Text(pretty(key), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium); Text(humanValue(value), color = MaterialTheme.colorScheme.onSurfaceVariant) } else if (value is JSONArray && value.length() > 0) { Text(pretty(key), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium); (0 until value.length()).forEach { index -> Text("• ${humanValue(value.opt(index))}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } else if (value is JSONObject) { Text(pretty(key), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium); value.keys().asSequence().toList().forEach { child -> Text("${pretty(child)}: ${humanValue(value.opt(child))}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
private fun pretty(value: String): String = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ').trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
private fun humanValue(value: Any?): String = when(value) { null, JSONObject.NULL -> "Não informado"; is Boolean -> if (value) "Sim" else "Não"; else -> value.toString().replace("operational", "Operacional").replace("unknown", "Desconhecido").replace("active", "Ativo").replace("inactive", "Inativo") }
private fun slug(value: String): String = value.lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "recurso" }
@Composable private fun InfoCard(text: String) { ElevatedCard(modifier = Modifier.fillMaxWidth()) { Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ErrorCard(text: String, retry: () -> Unit) { ElevatedCard(modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ErrorOutline, null); Spacer(Modifier.width(10.dp)); Text(text, Modifier.weight(1f)); TextButton(onClick = retry) { Text("Tentar novamente") } } } }
