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

private data class MongoTarget(val label: String, val database: String)
private val mongoTargets = listOf(
    MongoTarget("Korczak Control", "KorczakControl"),
    MongoTarget("KZ Site", "KorczakTechSite"),
    MongoTarget("Moon", "TensuraMoon")
)

@Composable
fun OperationsScreen(section: String) {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var renderServices by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var mongoTarget by remember { mutableStateOf<MongoTarget?>(null) }
    var collections by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }
    var documents by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var sites by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var profile by remember { mutableStateOf<JSONObject?>(null) }
    var workflows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var workflowRepository by remember { mutableStateOf("") }

    suspend fun get(path: String): String? {
        loading = true
        message = null
        val result = client.get(path)
        loading = false
        return when (result) {
            is ApiResult.Success -> result.body
            is ApiResult.Failure -> { message = result.message; null }
        }
    }

    suspend fun loadRender() {
        val body = get("/api/render/services") ?: return
        renderServices = runCatching {
            val array = JSONObject(body).optJSONArray("items") ?: JSONArray()
            List(array.length()) { array.getJSONObject(it) }
        }.getOrElse { message = "Não foi possível interpretar os serviços do Render."; emptyList() }
    }

    suspend fun loadCollections(target: MongoTarget) {
        mongoTarget = target
        selectedCollection = null
        documents = emptyList()
        val body = get("/api/databases/${target.database}/collections") ?: return
        collections = runCatching {
            val array = JSONObject(body).optJSONArray("items") ?: JSONArray()
            List(array.length()) { array.getJSONObject(it) }
        }.getOrElse { message = "Não foi possível carregar as collections."; emptyList() }
    }

    suspend fun loadDocuments(name: String) {
        val target = mongoTarget ?: return
        selectedCollection = name
        val body = get("/api/databases/${target.database}/collections/$name/documents?limit=50") ?: return
        documents = runCatching {
            val array = JSONObject(body).optJSONArray("items") ?: JSONArray()
            List(array.length()) { array.getJSONObject(it) }
        }.getOrElse { message = "Não foi possível carregar os documentos."; emptyList() }
    }

    suspend fun createCollection(name: String) {
        val target = mongoTarget ?: return
        loading = true
        message = null
        when (val result = client.post("/api/databases/${target.database}/collections", JSONObject().put("name", name))) {
            is ApiResult.Success -> loadCollections(target)
            is ApiResult.Failure -> { loading = false; message = result.message }
        }
    }

    suspend fun loadSites() {
        val body = get("/api/sites") ?: return
        sites = runCatching {
            val array = JSONObject(body).optJSONArray("items") ?: JSONArray()
            List(array.length()) { array.getJSONObject(it) }
        }.getOrElse { message = "Não foi possível carregar os sites."; emptyList() }
    }

    suspend fun loadProfile() {
        val body = get("/api/accounts/me") ?: return
        profile = runCatching { JSONObject(body).optJSONObject("account") }.getOrElse {
            message = "Não foi possível carregar as informações da conta."
            null
        }
    }

    suspend fun loadWorkflows() {
        val body = get("/api/github/bots/tensura-moon/workflows") ?: return
        val json = JSONObject(body)
        workflowRepository = json.optString("repository")
        workflows = runCatching {
            val array = json.optJSONArray("workflows") ?: JSONArray()
            List(array.length()) { array.getJSONObject(it) }
        }.getOrElse { message = "Não foi possível carregar os workflows do Tensura Moon."; emptyList() }
    }

    suspend fun runWorkflow(workflow: JSONObject) {
        val repo = workflowRepository.split('/').filter { it.isNotBlank() }
        val id = workflow.optString("id")
        if (repo.size != 2 || id.isBlank()) {
            message = "Não foi possível identificar o repositório ou o workflow."
            return
        }
        loading = true
        message = null
        when (val result = client.post("/api/github/repos/${repo[0]}/${repo[1]}/workflows/$id/dispatch", JSONObject().put("ref", "main"))) {
            is ApiResult.Success -> message = "Execução solicitada ao GitHub Actions."
            is ApiResult.Failure -> message = result.message
        }
        loading = false
    }

    LaunchedEffect(section) {
        when (section) {
            "render" -> loadRender()
            "sites" -> loadSites()
            "profile" -> loadProfile()
            "bots" -> loadWorkflows()
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header(section, loading)
        message?.let { MessageCard(it) }
        when (section) {
            "render" -> RenderModule(renderServices) { scope.launch { loadRender() } }
            "databases" -> MongoModule(mongoTarget, collections, selectedCollection, documents,
                onDatabase = { scope.launch { loadCollections(it) } },
                onCollection = { scope.launch { loadDocuments(it) } },
                onCreateCollection = { scope.launch { createCollection(it) } })
            "bots" -> BotModule(workflows, workflowRepository, onRefresh = { scope.launch { loadWorkflows() } }, onRun = { scope.launch { runWorkflow(it) } })
            "sites" -> SitesModule(sites) { scope.launch { loadSites() } }
            "profile" -> ProfileModule(profile) { scope.launch { loadProfile() } }
            "github" -> SimpleModule("GitHub", "Repositório e automações", listOf("Abrir repositório")) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/KorcZ4k/Korczak-Control")))
            }
            else -> SimpleModule("Módulo em desenvolvimento", "Esta área será vinculada ao serviço correspondente do Korczak Control.", emptyList()) {}
        }
    }
}

@Composable private fun Header(section: String, loading: Boolean) {
    val title = mapOf("render" to "Render", "databases" to "MongoDB", "bots" to "Bots", "sites" to "Sites", "profile" to "Perfil", "github" to "GitHub")[section] ?: section
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Central operacional", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable private fun MessageCard(text: String) {
    Card { Text(text, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable private fun RenderModule(services: List<JSONObject>, refresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = refresh) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Atualizar") }
    }
    if (services.isEmpty()) InfoCard("Nenhum serviço foi retornado pelo Render. Verifique a configuração da integração no servidor.")
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(services, key = { it.optString("id", it.optString("name")) }) { service ->
            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(service.optString("name", "Serviço Render"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailRows(service, excluded = setOf("id", "name"))
                }
            }
        }
    }
}

@Composable private fun MongoModule(target: MongoTarget?, collections: List<JSONObject>, selected: String?, documents: List<JSONObject>, onDatabase: (MongoTarget) -> Unit, onCollection: (String) -> Unit, onCreateCollection: (String) -> Unit) {
    if (target == null) {
        Text("Selecione um banco de dados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        mongoTargets.forEach { item ->
            ElevatedCard(onClick = { onDatabase(item) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, null)
                    Spacer(Modifier.width(14.dp))
                    Column { Text(item.label, fontWeight = FontWeight.SemiBold); Text(item.database, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    } else if (selected == null) {
        var newCollection by remember(target.database) { mutableStateOf("") }
        Text(target.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(target.database, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = newCollection, onValueChange = { newCollection = it }, label = { Text("Nova collection") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { if (newCollection.isNotBlank()) { onCreateCollection(newCollection.trim()); newCollection = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Criar collection") }
        if (collections.isEmpty()) InfoCard("Nenhuma collection foi encontrada neste banco de dados.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(collections, key = { it.optString("name") }) { collection ->
                ElevatedCard(onClick = { onCollection(collection.optString("name")) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(collection.optString("name"), fontWeight = FontWeight.SemiBold)
                            Text("${collection.optLong("estimatedDocumentCount")} documentos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    } else {
        Text(selected, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (documents.isEmpty()) InfoCard("Nenhum documento foi encontrado nesta collection.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(documents, key = { it.optString("_id", it.hashCode().toString()) }) { document ->
                ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { DetailRows(document) } }
            }
        }
    }
}

@Composable private fun BotModule(workflows: List<JSONObject>, repository: String, onRefresh: () -> Unit, onRun: (JSONObject) -> Unit) {
    Text("Tensura Moon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (repository.isNotBlank()) Text("Repositório vinculado: $repository", color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = onRefresh) { Text("Atualizar workflows") }
    if (workflows.isEmpty()) InfoCard("Nenhum workflow foi retornado para o repositório configurado do Tensura Moon.")
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(workflows, key = { it.optString("id") }) { workflow ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(workflow.optString("name", "Workflow"), fontWeight = FontWeight.SemiBold)
                    workflow.optString("path").takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { onRun(workflow) }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Executar") }
                }
            }
        }
    }
}

@Composable private fun SitesModule(sites: List<JSONObject>, refresh: () -> Unit) {
    TextButton(onClick = refresh) { Text("Atualizar") }
    if (sites.isEmpty()) InfoCard("Nenhum site foi registrado.")
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sites, key = { it.optString("_id", it.optString("slug", it.optString("name"))) }) { site ->
            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(site.optString("name", "Site"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailRows(site, excluded = setOf("_id", "name"))
                }
            }
        }
    }
}

@Composable private fun ProfileModule(profile: JSONObject?, refresh: () -> Unit) {
    TextButton(onClick = refresh) { Text("Atualizar informações") }
    if (profile == null) InfoCard("Informações da conta indisponíveis.")
    else ElevatedCard { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { DetailRows(profile) } }
}

@Composable private fun DetailRows(json: JSONObject, excluded: Set<String> = emptySet()) {
    json.keys().asSequence().toList().filter { it !in excluded }.forEach { key ->
        Field(pretty(key), humanValue(json.opt(key)))
    }
}

private fun humanValue(value: Any?): String = when (value) {
    null, JSONObject.NULL -> "Não informado"
    is JSONObject -> value.keys().asSequence().toList().joinToString("\n") { key -> "${pretty(key)}: ${humanValue(value.opt(key))}" }
    is JSONArray -> if (value.length() == 0) "Nenhum item" else (0 until value.length()).joinToString("\n") { index -> "• ${humanValue(value.opt(index))}" }
    is Boolean -> if (value) "Sim" else "Não"
    else -> value.toString()
}

@Composable private fun Field(label: String, value: String) {
    Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    Text(value.ifBlank { "Não informado" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun InfoCard(text: String) { ElevatedCard(modifier = Modifier.fillMaxWidth()) { Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SimpleModule(title: String, description: String, actions: List<String>, onAction: () -> Unit) { ElevatedCard(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant); actions.forEach { Button(onClick = onAction) { Text(it) } } } } }
private fun pretty(value: String): String = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ').replaceFirstChar { it.uppercase() }
