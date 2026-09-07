package com.korczak.control.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.util.prefs.Preferences

private data class Module(val id: String, val title: String, val description: String)

private val modules = listOf(
    Module("dashboard", "Painel", "Visão geral operacional"),
    Module("databases", "MongoDB", "Bases de dados e collections"),
    Module("render", "Render", "Serviços e implantações"),
    Module("bots", "Bots", "Workflows do Tensura Moon"),
    Module("sites", "Sites", "Sites registrados"),
    Module("apps", "Aplicações", "Aplicações cadastradas"),
    Module("apis", "APIs", "Serviços de API"),
    Module("clients", "Clientes", "Clientes registrados"),
    Module("integrations", "Integrações", "Status das conexões"),
    Module("profile", "Perfil", "Informações da conta")
)

private val databases = listOf(
    "Korczak Control" to "KorczakControl",
    "KZ Site" to "KorczakTechSite",
    "Moon" to "TensuraMoon"
)

private object DesktopSession {
    private val preferences = Preferences.userRoot().node("com/korczak/control/desktop")
    fun token(): String = preferences.get("token", "")
    fun save(token: String) = preferences.put("token", token)
    fun clear() = preferences.remove("token")
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Korczak Control") {
        MaterialTheme(colorScheme = darkColorScheme()) {
            DesktopControlApp()
        }
    }
}

@Composable
private fun DesktopControlApp() {
    var token by remember { mutableStateOf(DesktopSession.token()) }
    var profile by remember { mutableStateOf<AccountProfile?>(null) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refreshSession() {
        if (token.isBlank()) return
        scope.launch {
            loading = true
            error = null
            runCatching {
                ControlApiClient.me(token) to ControlApiClient.dashboard(token)
            }.onSuccess {
                profile = it.first
                summary = it.second
            }.onFailure {
                error = it.message ?: "Não foi possível atualizar as informações."
            }
            loading = false
        }
    }

    LaunchedEffect(token) {
        if (token.isNotBlank()) refreshSession()
    }

    if (token.isBlank()) {
        LoginView(
            error = error,
            loading = loading,
            onLogin = { email, password ->
                scope.launch {
                    loading = true
                    error = null
                    runCatching { ControlApiClient.login(email, password) }
                        .onSuccess { session ->
                            DesktopSession.save(session.token)
                            token = session.token
                            profile = session.profile
                        }
                        .onFailure { throwable ->
                            error = throwable.message ?: "Não foi possível iniciar a sessão."
                        }
                    loading = false
                }
            }
        )
    } else {
        AuthenticatedShell(
            token = token,
            profile = profile,
            summary = summary,
            loading = loading,
            error = error,
            onRefresh = ::refreshSession,
            onLogout = {
                DesktopSession.clear()
                token = ""
                profile = null
                summary = null
                error = null
            }
        )
    }
}

@Composable
private fun LoginView(
    error: String?,
    loading: Boolean,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Acesso ao painel administrativo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("E-mail") },
                    singleLine = true,
                    enabled = !loading
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Senha") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let { Notice("Acesso indisponível", it) }
                Button(
                    onClick = { onLogin(email.trim(), password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank() && password.isNotBlank() && !loading
                ) {
                    Text(if (loading) "Verificando acesso" else "Entrar")
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(
    token: String,
    profile: AccountProfile?,
    summary: DashboardSummary?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    var selected by remember { mutableStateOf(modules.first()) }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(modifier = Modifier.fillMaxHeight().width(240.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("KORCZAK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("CONTROL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            modules.forEach { module ->
                NavigationRailItem(
                    selected = selected.id == module.id,
                    onClick = { selected = module },
                    icon = { Text("•") },
                    label = { Text(module.title) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            NavigationRailItem(
                selected = false,
                onClick = onLogout,
                icon = { Text("↪") },
                label = { Text("Sair") }
            )
        }

        VerticalDivider()

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(selected.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(selected.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    profile?.let { Text(it.name.ifBlank { it.email }) }
                    FilledTonalButton(onClick = onRefresh, enabled = !loading) {
                        Text(if (loading) "Atualizando" else "Atualizar")
                    }
                }
            }

            error?.let { Notice("Atualização indisponível", it) }

            when (selected.id) {
                "dashboard" -> DashboardView(summary, loading)
                "databases" -> MongoDesktop(token)
                "render" -> RecordsModule("Serviços Render", token, ControlApiClient::renderServices)
                "bots" -> BotsModule(token)
                "sites" -> RecordsModule("Sites", token, ControlApiClient::sites)
                "apps" -> RecordsModule("Aplicações", token, ControlApiClient::applications)
                "apis" -> RecordsModule("APIs", token, ControlApiClient::apis)
                "clients" -> RecordsModule("Clientes", token, ControlApiClient::clients)
                "integrations" -> IntegrationsView(summary)
                "profile" -> ProfileView(profile)
            }
        }
    }
}

@Composable
private fun DashboardView(summary: DashboardSummary?, loading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Resumo operacional", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (loading) "Consultando informações atualizadas." else "Dados carregados do Korczak Control.")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric("Sites", summary?.sites ?: 0)
            Metric("APIs", summary?.apis ?: 0)
            Metric("Aplicações", summary?.apps ?: 0)
            Metric("Serviços", summary?.online ?: 0)
        }
    }
}

@Composable
private fun Metric(title: String, value: Int) {
    ElevatedCard(modifier = Modifier.width(180.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MongoDesktop(token: String) {
    var selectedDb by remember { mutableStateOf<Pair<String, String>?>(null) }
    var collections by remember { mutableStateOf<List<DisplayRecord>>(emptyList()) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }
    var documents by remember { mutableStateOf<List<DisplayRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newCollection by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadCollections() {
        val database = selectedDb ?: return
        scope.launch {
            loading = true
            error = null
            runCatching { ControlApiClient.collections(token, database.second) }
                .onSuccess { collections = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    fun loadDocuments(name: String) {
        val database = selectedDb ?: return
        scope.launch {
            loading = true
            error = null
            runCatching { ControlApiClient.documents(token, database.second, name) }
                .onSuccess {
                    documents = it
                    selectedCollection = name
                }
                .onFailure { error = it.message }
            loading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        error?.let { Notice("MongoDB", it) }

        when {
            selectedDb == null -> {
                Text("Selecione uma base de dados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                databases.forEach { database ->
                    ElevatedCard(
                        onClick = {
                            selectedDb = database
                            loadCollections()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(database.first, fontWeight = FontWeight.Bold)
                            Text(database.second, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            selectedCollection == null -> {
                Text(selectedDb!!.first, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCollection,
                        onValueChange = { newCollection = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Nova collection") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val database = selectedDb!!
                            if (newCollection.isNotBlank()) {
                                scope.launch {
                                    loading = true
                                    runCatching {
                                        ControlApiClient.createCollection(token, database.second, newCollection.trim())
                                        ControlApiClient.collections(token, database.second)
                                    }.onSuccess {
                                        collections = it
                                        newCollection = ""
                                    }.onFailure { error = it.message }
                                    loading = false
                                }
                            }
                        },
                        enabled = !loading
                    ) { Text("Criar") }
                }
                TextButton(onClick = ::loadCollections, enabled = !loading) { Text("Atualizar") }
                if (collections.isEmpty()) {
                    EmptyState("Nenhuma collection encontrada.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(collections) { item ->
                            val name = item.value("name")
                            ElevatedCard(onClick = { loadDocuments(name) }, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(name.ifBlank { "Collection" }, fontWeight = FontWeight.SemiBold)
                                    val count = item.value("estimatedDocumentCount")
                                    if (count.isNotBlank()) Text("$count documentos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = {
                        selectedCollection = null
                        documents = emptyList()
                    }) { Text("Voltar") }
                    Text(selectedCollection!!, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                RecordsList(documents)
            }
        }
    }
}

@Composable
private fun RecordsModule(
    title: String,
    token: String,
    loader: suspend (String) -> List<DisplayRecord>
) {
    var records by remember { mutableStateOf<List<DisplayRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            runCatching { loader(token) }
                .onSuccess { records = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(title) { refresh() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = ::refresh, enabled = !loading) { Text("Atualizar") }
        }
        error?.let { Notice(title, it) }
        if (loading && records.isEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            RecordsList(records)
        }
    }
}

@Composable
private fun BotsModule(token: String) {
    var data by remember { mutableStateOf<BotWorkflows?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            message = null
            runCatching { ControlApiClient.workflows(token) }
                .onSuccess { data = it }
                .onFailure { message = it.message }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tensura Moon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        data?.repository?.takeIf { it.isNotBlank() }?.let {
            Text("Repositório vinculado: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = ::refresh, enabled = !loading) { Text("Atualizar workflows") }
        message?.let { Notice("Bots", it) }
        if (loading && data == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (data?.workflows.isNullOrEmpty()) {
            EmptyState("Nenhum workflow foi retornado para o repositório configurado.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(data!!.workflows) { workflow ->
                    val name = workflow.value("name", "workflowName").ifBlank { "Workflow" }
                    val path = workflow.value("path")
                    val id = workflow.value("id")
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            if (path.isNotBlank()) Text(path, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = {
                                    scope.launch {
                                        runCatching { ControlApiClient.runWorkflow(token, data!!.repository, id) }
                                            .onSuccess { message = "Execução solicitada ao GitHub Actions." }
                                            .onFailure { message = it.message }
                                    }
                                },
                                enabled = id.isNotBlank()
                            ) { Text("Executar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsList(records: List<DisplayRecord>) {
    if (records.isEmpty()) {
        EmptyState("Nenhum registro foi encontrado.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(records) { record ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val title = record.value("name", "title", "email", "id", "accountId").ifBlank { "Registro" }
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        record.fields
                            .filterKeys { it !in setOf("name", "title") }
                            .forEach { (key, value) ->
                                if (value.isNotBlank()) Field(pretty(key), human(value))
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrationsView(summary: DashboardSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Integration("GitHub", summary?.github == true)
        Integration("Render", summary?.render == true)
        Integration("MongoDB", summary?.mongodb == true)
    }
}

@Composable
private fun Integration(name: String, connected: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(if (connected) "Conectado" else "Não configurado")
        }
    }
}

@Composable
private fun ProfileView(profile: AccountProfile?) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(profile?.name ?: "Conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Field("E-mail", profile?.email.orEmpty())
            Field("Função", profile?.role.orEmpty())
            Field("Departamento", profile?.department.orEmpty())
            Field("ID da conta", profile?.accountId.orEmpty())
        }
    }
}

@Composable
private fun Notice(title: String, text: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "Não informado" })
    }
}

private fun pretty(value: String): String = value
    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun human(value: String): String = when (value.lowercase()) {
    "true" -> "Sim"
    "false" -> "Não"
    "null" -> "Não informado"
    else -> value
}
