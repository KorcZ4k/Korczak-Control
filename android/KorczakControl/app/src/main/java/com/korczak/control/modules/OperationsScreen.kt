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

private data class OperationCard(val title: String, val subtitle: String, val actions: List<String> = emptyList())

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
        val result = when (method) {
            "POST" -> client.post(path, body ?: JSONObject())
            "PATCH" -> client.patch(path, body ?: JSONObject())
            "DELETE" -> client.delete(path)
            else -> client.get(path)
        }
        resultText = when (result) {
            is ApiResult.Success -> result.body
            is ApiResult.Failure -> "ERRO: ${result.message}"
        }
        loading = false
    }

    suspend fun loadCollections() {
        loading = true
        when (val result = client.get("/api/databases/collections")) {
            is ApiResult.Success -> {
                val root = JSONObject(result.body)
                val array = root.optJSONArray("items") ?: JSONArray()
                mongoCollections = List(array.length()) { array.getJSONObject(it) }
                resultText = "Banco de dados: ${root.optString("database")}" 
                selectedCollection = null
                documents = emptyList()
            }
            is ApiResult.Failure -> resultText = "ERRO: ${result.message}"
        }
        loading = false
    }

    suspend fun loadDocuments(name: String) {
        selectedCollection = name
        loading = true
        when (val result = client.get("/api/databases/collections/$name/documents?limit=100")) {
            is ApiResult.Success -> {
                val array = JSONObject(result.body).optJSONArray("items") ?: JSONArray()
                documents = List(array.length()) { array.getJSONObject(it) }
            }
            is ApiResult.Failure -> resultText = "ERRO: ${result.message}"
        }
        loading = false
    }

    LaunchedEffect(section) {
        when (section) {
            "github" -> request("/api/github/repos/KorcZ4k/Korczak-Control")
            "render" -> request("/api/render/services")
            "databases" -> loadCollections()
            "apis" -> request("/api/managed/api")
            "apps" -> request("/api/managed/app")
            "sites" -> request("/api/sites")
            "profile" -> request("/api/accounts/me")
        }
    }

    if (showCreateCollection) {
        AlertDialog(
            onDismissRequest = { showCreateCollection = false },
            title = { Text("Criar collection") },
            text = { OutlinedTextField(value = newCollectionName, onValueChange = { newCollectionName = it }, label = { Text("Nome da collection") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                val name = newCollectionName.trim()
                if (name.isNotBlank()) {
                    showCreateCollection = false
                    scope.launch { request("/api/databases/collections", "POST", JSONObject().put("name", name)); loadCollections() }
                }
            }) { Text("Criar") } },
            dismissButton = { TextButton(onClick = { showCreateCollection = false }) { Text("Cancelar") } }
        )
    }

    if (showDocumentEditor) {
        AlertDialog(
            onDismissRequest = { showDocumentEditor = false },
            title = { Text("Novo documento") },
            text = { OutlinedTextField(value = newDocumentJson, onValueChange = { newDocumentJson = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), label = { Text("Dados do documento") }) },
            confirmButton = { TextButton(onClick = {
                val collection = selectedCollection ?: return@TextButton
                try {
                    val document = JSONObject(newDocumentJson)
                    showDocumentEditor = false
                    scope.launch { request("/api/databases/collections/$collection/documents", "POST", JSONObject().put("document", document)); loadDocuments(collection) }
                } catch (_: Exception) { resultText = "ERRO: Os dados informados não são válidos." }
            }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { showDocumentEditor = false }) { Text("Cancelar") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(title, section, loading)
        when (section) {
            "github" -> GitHubPanel(resultText) { action -> scope.launch {
                when (action) {
                    "Informações" -> request("/api/github/repos/KorcZ4k/Korczak-Control")
                    "Código" -> request("/api/github/repos/KorcZ4k/Korczak-Control/code")
                    "Workflows" -> request("/api/github/repos/KorcZ4k/Korczak-Control/workflows")
                    "Codespace" -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/codespaces/new?repo=KorcZ4k/Korczak-Control")))
                }
            } }
            "render" -> RenderPanel(resultText) { serviceId -> scope.launch { request("/api/render/services/$serviceId/deploys", "POST", JSONObject().put("clearCache", false)) } }
            "databases" -> MongoPanel(mongoCollections, selectedCollection, documents, { scope.launch { loadCollections() } }, { showCreateCollection = true }, { scope.launch { loadDocuments(it) } }, { showDocumentEditor = true }, { id -> val collection = selectedCollection ?: return@MongoPanel; scope.launch { request("/api/databases/collections/$collection/documents/$id", "DELETE"); loadDocuments(collection) } })
            "bots" -> StaticPanel(listOf(OperationCard("Tensura Moon", "Gerenciamento operacional do bot.", listOf("Informações", "Executar", "Codespace"))), resultText) { action -> resultText = when (action) { "Codespace" -> { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/codespaces"))); "Abrindo o Codespaces." }; "Executar" -> "A execução precisa estar vinculada a um workflow configurado no servidor."; else -> "As informações do bot serão exibidas quando a integração estiver disponível." } }
            "apis" -> StaticPanel(listOf(OperationCard("Korczak Tech Site", "Serviços e documentação da API.", listOf("Informações")), OperationCard("Korczak Control", "API administrativa do ecossistema.", listOf("Informações"))), resultText) { scope.launch { request(if (it == "Korczak Control") "/health" else "/api/managed/api") } }
            "apps" -> StaticPanel(listOf(OperationCard("Korczak Control", "Aplicativo administrativo Android.", listOf("Informações"))), resultText) { scope.launch { request("/api/managed/app") } }
            "sites" -> StaticPanel(listOf(OperationCard("Sites", "Serviços web administrados pela Korczak Technologies.", listOf("Informações"))), resultText) { scope.launch { request("/api/sites") } }
            "profile" -> StaticPanel(listOf(OperationCard("Perfil", "Informações da conta administrativa.", listOf("Atualizar informações"))), resultText) { scope.launch { request("/api/accounts/me") } }
            "clients" -> ClientsPanel()
        }
        if (section !in listOf("databases", "clients") && resultText.isNotBlank()) ResponseCard(resultText)
    }
}

@Composable
private fun SectionHeader(title: String, section: String, loading: Boolean) {
    ElevatedCard {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(when(section) { "github" -> Icons.Default.Code; "render" -> Icons.Default.Cloud; "databases" -> Icons.Default.Storage; "bots" -> Icons.Default.SmartToy; "apis" -> Icons.Default.Api; "apps" -> Icons.Default.Apps; "clients" -> Icons.Default.People; else -> Icons.Default.Info }, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("CENTRAL OPERACIONAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable private fun GitHubPanel(result: String, onAction: (String) -> Unit) = StaticPanel(listOf(OperationCard("Korczak Control", "Repositório principal da plataforma administrativa.", listOf("Informações", "Workflows", "Codespace", "Código"))), result, onAction)

@Composable
private fun RenderPanel(result: String, onDeploy: (String) -> Unit) {
    val services = remember(result) { runCatching { val array = JSONObject(result).optJSONArray("items") ?: JSONArray(); List(array.length()) { array.getJSONObject(it) } }.getOrDefault(emptyList()) }
    if (services.isEmpty()) StaticPanel(listOf(OperationCard("KZSite", "Serviço hospedado no Render."), OperationCard("KZControl", "Serviço administrativo hospedado no Render.")), result) {}
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(services, key = { it.optString("id") }) { service -> ElevatedCard { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(service.optString("service", service.optString("name", "Serviço Render")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("Status e operações do serviço", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = { onDeploy(service.optString("id")) }) { Icon(Icons.Default.RocketLaunch, null); Spacer(Modifier.width(8.dp)); Text("Iniciar deploy") } } } } }
}

@Composable
private fun MongoPanel(collections: List<JSONObject>, selected: String?, documents: List<JSONObject>, onRefresh: () -> Unit, onCreate: () -> Unit, onOpen: (String) -> Unit, onAddDocument: () -> Unit, onDeleteDocument: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Atualizar") }
            OutlinedButton(onClick = onCreate, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Nova collection") }
        }
        if (selected == null) collections.forEach { item -> ElevatedCard(onClick = { onOpen(item.optString("name")) }) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(item.optString("name"), fontWeight = FontWeight.SemiBold); Text("${item.optLong("estimatedDocumentCount")} documentos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } } }
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(selected, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Button(onClick = onAddDocument) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Novo documento") } }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) { items(documents, key = { it.optJSONObject("_id")?.optString("\$oid") ?: it.opt("_id").toString() }) { doc -> ElevatedCard { Column(Modifier.padding(16.dp)) { StructuredJson(doc, 0); val id = doc.optJSONObject("_id")?.optString("\$oid") ?: doc.optString("_id"); if (id.isNotBlank()) TextButton(onClick = { onDeleteDocument(id) }) { Text("Excluir documento") } } } } }
        }
    }
}

@Composable private fun StaticPanel(cards: List<OperationCard>, result: String, onAction: (String) -> Unit) { LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(cards) { card -> ElevatedCard { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(card.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant); if (card.actions.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { card.actions.forEach { action -> OutlinedButton(onClick = { onAction(if (card.title == "Korczak Control" && action == "Informações") "Korczak Control" else action) }) { Text(action) } } } } } }; if (result.isNotBlank()) item { ResponseCard(result) } } }

@Composable private fun ClientsPanel() { LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Clientes e orçamentos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; item { ElevatedCard { Column(Modifier.padding(18.dp)) { Text("Clientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("Dados dos clientes serão exibidos aqui quando a integração da base estiver disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; item { ElevatedCard { Column(Modifier.padding(18.dp)) { Text("Orçamentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("Solicitações e histórico de orçamentos serão exibidos aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }

@Composable
private fun ResponseCard(text: String) {
    ElevatedCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Informações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (text.startsWith("ERRO:")) Text(text.removePrefix("ERRO:").trim(), color = MaterialTheme.colorScheme.error)
            else runCatching { StructuredJson(JSONObject(text), 0) }.getOrElse {
                runCatching { StructuredArray(JSONArray(text)) }.getOrElse { Text(text) }
            }
        }
    }
}

@Composable
private fun StructuredJson(value: JSONObject, depth: Int) {
    val keys = value.keys().asSequence().toList()
    keys.forEach { key ->
        val item = value.opt(key)
        Column(Modifier.fillMaxWidth().padding(start = (depth * 8).dp, top = 4.dp, bottom = 4.dp)) {
            Text(prettyKey(key), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            when (item) {
                is JSONObject -> StructuredJson(item, depth + 1)
                is JSONArray -> StructuredArray(item)
                else -> Text(prettyValue(item), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (key != keys.last()) HorizontalDivider()
    }
}

@Composable
private fun StructuredArray(value: JSONArray) {
    if (value.length() == 0) Text("Nenhuma informação disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (index in 0 until value.length()) {
            ElevatedCard { Column(Modifier.padding(12.dp)) {
                when (val item = value.opt(index)) {
                    is JSONObject -> StructuredJson(item, 0)
                    is JSONArray -> StructuredArray(item)
                    else -> Text(prettyValue(item))
                }
            } }
        }
    }
}

private fun prettyKey(key: String): String = key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
private fun prettyValue(value: Any?): String = when (value) { null, JSONObject.NULL -> "Não informado"; true -> "Sim"; false -> "Não"; else -> value.toString() }
