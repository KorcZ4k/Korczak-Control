package com.korczak.control.modules

import android.content.Intent
import android.net.Uri
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

private data class OperationCard(val title: String, val subtitle: String, val endpoint: String? = null, val actions: List<String> = emptyList())

@Composable
fun OperationsScreen(section: String) {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    val title = mapOf("github" to "GitHub", "render" to "Render", "databases" to "MongoDB", "bots" to "Bots", "apis" to "APIs", "apps" to "Aplicações", "sites" to "Sites", "profile" to "Perfil", "clients" to "Korczak Technologies")[section] ?: section
    var resultText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var mongoCollections by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var showCreateCollection by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf<String?>(null) }
    var documents by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var showDocumentEditor by remember { mutableStateOf(false) }
    var newDocumentJson by remember { mutableStateOf("{\n  \n}") }

    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null) {
        loading = true
        val result = when (method) { "POST" -> client.post(path, body ?: JSONObject()); "PATCH" -> client.patch(path, body ?: JSONObject()); "DELETE" -> client.delete(path); else -> client.get(path) }
        resultText = when (result) { is ApiResult.Success -> result.body; is ApiResult.Failure -> "ERRO: ${result.message}" }
        loading = false
    }
    suspend fun loadCollections() {
        loading = true
        when (val result = client.get("/api/databases/collections")) {
            is ApiResult.Success -> {
                val root = JSONObject(result.body); val array = root.optJSONArray("items") ?: JSONArray()
                mongoCollections = List(array.length()) { array.getJSONObject(it) }
                resultText = "Banco: ${root.optString("database")}"; selectedCollection = null; documents = emptyList()
            }
            is ApiResult.Failure -> resultText = "ERRO: ${result.message}"
        }; loading = false
    }
    suspend fun loadDocuments(name: String) {
        selectedCollection = name; loading = true
        when (val result = client.get("/api/databases/collections/$name/documents?limit=100")) {
            is ApiResult.Success -> { val array = JSONObject(result.body).optJSONArray("items") ?: JSONArray(); documents = List(array.length()) { array.getJSONObject(it) } }
            is ApiResult.Failure -> resultText = "ERRO: ${result.message}"
        }; loading = false
    }

    LaunchedEffect(section) {
        when (section) { "github" -> request("/api/github/repos/KorcZ4k/Korczak-Control"); "render" -> request("/api/render/services"); "databases" -> loadCollections(); "apis" -> request("/api/managed/api"); "apps" -> request("/api/managed/app"); "sites" -> request("/api/sites"); "profile" -> request("/api/accounts/me") }
    }

    if (showCreateCollection) AlertDialog(onDismissRequest = { showCreateCollection = false }, title = { Text("Nova collection") }, text = { OutlinedTextField(value = newCollectionName, onValueChange = { newCollectionName = it }, label = { Text("Nome") }, singleLine = true) }, confirmButton = { TextButton(onClick = { val name = newCollectionName.trim(); if (name.isNotBlank()) { showCreateCollection = false; scope.launch { request("/api/databases/collections", "POST", JSONObject().put("name", name)); loadCollections() } } }) { Text("Criar") } }, dismissButton = { TextButton(onClick = { showCreateCollection = false }) { Text("Cancelar") } })
    if (showDocumentEditor) AlertDialog(onDismissRequest = { showDocumentEditor = false }, title = { Text("Novo documento") }, text = { OutlinedTextField(value = newDocumentJson, onValueChange = { newDocumentJson = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), label = { Text("JSON") }) }, confirmButton = { TextButton(onClick = { val collection = selectedCollection ?: return@TextButton; try { val document = JSONObject(newDocumentJson); showDocumentEditor = false; scope.launch { request("/api/databases/collections/$collection/documents", "POST", JSONObject().put("document", document)); loadDocuments(collection) } } catch (_: Exception) { resultText = "ERRO: JSON inválido." } }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { showDocumentEditor = false }) { Text("Cancelar") } })

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title, section, loading)
        when (section) {
            "github" -> GitHubPanel(resultText, onAction = { action -> scope.launch { when (action) { "Infos" -> request("/api/github/repos/KorcZ4k/Korczak-Control"); "Código" -> request("/api/github/repos/KorcZ4k/Korczak-Control/code"); "Workflow" -> request("/api/github/repos/KorcZ4k/Korczak-Control/workflows"); "Codespace" -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/codespaces/new?repo=KorcZ4k/Korczak-Control"))) } } })
            "render" -> RenderPanel(resultText, onDeploy = { serviceId -> scope.launch { request("/api/render/services/$serviceId/deploys", "POST", JSONObject().put("clearCache", false)) } })
            "databases" -> MongoPanel(mongoCollections, selectedCollection, documents, onRefresh = { scope.launch { loadCollections() } }, onCreate = { showCreateCollection = true }, onOpen = { scope.launch { loadDocuments(it) } }, onAddDocument = { showDocumentEditor = true }, onDeleteDocument = { id -> val collection = selectedCollection ?: return@MongoPanel; scope.launch { request("/api/databases/collections/$collection/documents/$id", "DELETE"); loadDocuments(collection) } })
            "bots" -> StaticPanel(listOf(OperationCard("Tensura Moon", "Bot operacional hospedado por GitHub Actions.", actions = listOf("Infos", "Run", "Codespace"))), resultText) { action -> if (action == "Codespace") context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/codespaces"))) else resultText = if (action == "Run") "Selecione o workflow do repositório TensuraMoon para executar." else "Tensura Moon: informações operacionais serão carregadas pela integração GitHub." }
            "apis" -> StaticPanel(listOf(OperationCard("Korczak Tech Site", "API do ecossistema Korczak Technologies.", actions = listOf("Infos")), OperationCard("Korczak Control", "API administrativa principal.", actions = listOf("Infos"))), resultText) { scope.launch { request(if (it == "Korczak Control") "/health" else "/api/managed/api") } }
            "apps" -> StaticPanel(listOf(OperationCard("Korczak Control", "Aplicativo administrativo Android.", actions = listOf("Infos"))), resultText) { scope.launch { request("/api/managed/app") } }
            "sites" -> StaticPanel(listOf(OperationCard("Sites", "Informações dos sites administrados pela KZ TECH.", actions = listOf("Infos"))), resultText) { scope.launch { request("/api/sites") } }
            "profile" -> StaticPanel(listOf(OperationCard("Perfil", "Informações da sua conta administrativa.", actions = listOf("Infos"))), resultText) { scope.launch { request("/api/accounts/me") } }
            "clients" -> ClientsPanel()
        }
        if (section !in listOf("databases", "clients") && resultText.isNotBlank()) ResponseCard(resultText)
    }
}

@Composable private fun SectionHeader(title: String, section: String, loading: Boolean) { Card { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(when(section) { "github" -> Icons.Default.Code; "render" -> Icons.Default.Cloud; "databases" -> Icons.Default.Storage; "bots" -> Icons.Default.SmartToy; "apis" -> Icons.Default.Api; "apps" -> Icons.Default.Apps; "clients" -> Icons.Default.People; else -> Icons.Default.Info }, null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("KORCZAK CONTROL · CENTRAL OPERACIONAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) } } }

@Composable private fun GitHubPanel(result: String, onAction: (String) -> Unit) { StaticPanel(listOf(OperationCard("Korczak-Control", "Repositório principal da administração KZ TECH.", actions = listOf("Infos", "Workflow", "Codespace", "Código"))), result, onAction) }

@Composable private fun RenderPanel(result: String, onDeploy: (String) -> Unit) { val services = remember(result) { runCatching { val array = JSONObject(result).optJSONArray("items") ?: JSONArray(); List(array.length()) { array.getJSONObject(it) } }.getOrDefault(emptyList()) }; if (services.isEmpty()) StaticPanel(listOf(OperationCard("KZSite", "Serviço Render · informações e deploy."), OperationCard("KZControl", "Serviço Render · informações e deploy.")), result) { } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(services, key = { it.optString("id") }) { service -> Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(service.optString("service", service.optString("name", "Render Service")), style = MaterialTheme.typography.titleMedium); Text("ID: ${service.optString("id")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Row { OutlinedButton(onClick = {}) { Text("Infos") }; Spacer(Modifier.width(8.dp)); Button(onClick = { onDeploy(service.optString("id")) }) { Text("Deploy") } } } } } } }

@Composable private fun MongoPanel(collections: List<JSONObject>, selected: String?, documents: List<JSONObject>, onRefresh: () -> Unit, onCreate: () -> Unit, onOpen: (String) -> Unit, onAddDocument: () -> Unit, onDeleteDocument: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Atualizar DB") }; OutlinedButton(onClick = onCreate, modifier = Modifier.weight(1f)) { Text("Criar collection") } }; if (selected == null) { collections.forEach { item -> Card(onClick = { onOpen(item.optString("name")) }) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.optString("name"), fontWeight = FontWeight.SemiBold); Text("${item.optLong("estimatedDocumentCount")} documentos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } } } } else { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(selected, style = MaterialTheme.typography.titleMedium); Button(onClick = onAddDocument) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Documento") } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) { items(documents, key = { it.optJSONObject("_id")?.optString("\$oid") ?: it.opt("_id").toString() }) { doc -> Card { Column(Modifier.padding(12.dp)) { Text(doc.toString(2), style = MaterialTheme.typography.bodySmall); val id = doc.optJSONObject("_id")?.optString("\$oid") ?: doc.optString("_id"); if (id.isNotBlank()) TextButton(onClick = { onDeleteDocument(id) }) { Text("Excluir") } } } } } } } }

@Composable private fun StaticPanel(cards: List<OperationCard>, result: String, onAction: (String) -> Unit) { LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(cards) { card -> Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(card.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); if (card.actions.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { card.actions.forEach { action -> AssistChip(onClick = { onAction(if (card.title == "Korczak Control") "Korczak Control" else action) }, label = { Text(action) }) } } } } }; if (result.isNotBlank()) item { ResponseCard(result) } } }

@Composable private fun ClientsPanel() { val cards = listOf("KorczakTechSite (DB)" to "users · collection: Usuários", "Orçamentos (DB)" to "collection: Users"); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Clientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(cards) { (name, detail) -> Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(name, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }

@Composable private fun ResponseCard(text: String) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text(text.take(12000), Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall) } }
