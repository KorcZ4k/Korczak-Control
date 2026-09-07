package com.korczak.control.desktop

import androidx.compose.foundation.layout.*
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
    Module("dashboard", "Painel", "Visão geral operacional do sistema."),
    Module("integrations", "Integrações", "Status dos serviços conectados."),
    Module("databases", "MongoDB", "Bases de dados e conectividade."),
    Module("profile", "Perfil", "Informações da conta e sessão."),
    Module("events", "Eventos", "Notificações e atividade recente."),
    Module("settings", "Ajustes", "Configurações do aplicativo.")
)

private object DesktopSession {
    private val preferences = Preferences.userRoot().node("com/korczak/control/desktop")
    fun token(): String = preferences.get("token", "")
    fun save(token: String) = preferences.put("token", token)
    fun clear() = preferences.remove("token")
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Korczak Control") {
        MaterialTheme(colorScheme = darkColorScheme()) { DesktopControlApp() }
    }
}

@Composable
private fun DesktopControlApp() {
    var token by remember { mutableStateOf(DesktopSession.token()) }
    var profile by remember { mutableStateOf<AccountProfile?>(null) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var loadingSession by remember { mutableStateOf(token.isNotBlank()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadSession() {
        if (token.isBlank()) return
        scope.launch {
            loadingSession = true
            error = null
            runCatching {
                val account = ControlApiClient.me(token)
                val dashboard = ControlApiClient.dashboard(token)
                account to dashboard
            }.onSuccess { (account, dashboard) ->
                profile = account
                summary = dashboard
            }.onFailure {
                if (it is ApiException && (it.message?.contains("Session", true) == true || it.message?.contains("token", true) == true)) {
                    DesktopSession.clear()
                    token = ""
                    profile = null
                    summary = null
                }
                error = it.message ?: "Não foi possível carregar a sessão."
            }
            loadingSession = false
        }
    }

    LaunchedEffect(token) { if (token.isNotBlank()) loadSession() }

    if (token.isBlank()) {
        LoginView(
            error = error,
            onLogin = { email, password ->
                scope.launch {
                    loadingSession = true
                    error = null
                    runCatching { ControlApiClient.login(email, password) }
                        .onSuccess { session ->
                            DesktopSession.save(session.token)
                            token = session.token
                            profile = session.profile
                        }
                        .onFailure { error = it.message ?: "Não foi possível iniciar a sessão." }
                    loadingSession = false
                }
            },
            loading = loadingSession
        )
    } else {
        AuthenticatedShell(
            profile = profile,
            summary = summary,
            error = error,
            loading = loadingSession,
            onRefresh = { loadSession() },
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
private fun LoginView(error: String?, onLogin: (String, String) -> Unit, loading: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            Column(Modifier.padding(36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !loading
                )
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
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
    profile: AccountProfile?,
    summary: DashboardSummary?,
    error: String?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    var selected by remember { mutableStateOf(modules.first()) }

    Row(Modifier.fillMaxSize()) {
        NavigationRail(modifier = Modifier.fillMaxHeight().width(250.dp), header = {
            Column(Modifier.padding(20.dp)) {
                Text("KORCZAK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("CONTROL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
            }
        }) {
            modules.forEach { module ->
                NavigationRailItem(
                    selected = selected.id == module.id,
                    onClick = { selected = module },
                    icon = { Text("•") },
                    label = { Text(module.title) }
                )
            }
            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                selected = false,
                onClick = onLogout,
                icon = { Text("↪") },
                label = { Text("Sair") }
            )
        }

        VerticalDivider()

        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(selected.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(selected.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    profile?.let { Text(it.name.ifBlank { it.email }, style = MaterialTheme.typography.labelLarge) }
                    FilledTonalButton(onClick = onRefresh, enabled = !loading) {
                        Text(if (loading) "Atualizando" else "Atualizar")
                    }
                }
            }

            if (!error.isNullOrBlank()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Atualização indisponível", fontWeight = FontWeight.Bold)
                        Text(error)
                    }
                }
            }

            when (selected.id) {
                "dashboard" -> DashboardView(summary, loading)
                "integrations" -> IntegrationsView(summary)
                "databases" -> DatabaseView(summary)
                "profile" -> ProfileView(profile)
                "events" -> EventsView(summary)
                else -> SettingsView()
            }
        }
    }
}

@Composable
private fun DashboardView(summary: DashboardSummary?, loading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Resumo operacional", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (loading) "Consultando informações atualizadas." else "Dados carregados da API Korczak Control.")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard("Sites", summary?.sites ?: 0)
            MetricCard("APIs", summary?.apis ?: 0)
            MetricCard("Aplicações", summary?.apps ?: 0)
            MetricCard("Serviços operacionais", summary?.online ?: 0)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusCard("Requer atenção", summary?.attention ?: 0)
            StatusCard("Indisponíveis", summary?.unavailable ?: 0)
            StatusCard("Notificações", summary?.unread ?: 0)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: Int) {
    ElevatedCard(Modifier.width(190.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusCard(title: String, value: Int) = MetricCard(title, value)

@Composable
private fun IntegrationsView(summary: DashboardSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IntegrationRow("GitHub", summary?.github == true)
        IntegrationRow("Render", summary?.render == true)
        IntegrationRow("MongoDB", summary?.mongodb == true)
    }
}

@Composable
private fun IntegrationRow(name: String, connected: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(if (connected) "Conectado" else "Não configurado", color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DatabaseView(summary: DashboardSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bases configuradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(if (summary?.mongodb == true) "A integração MongoDB está conectada à API." else "A integração MongoDB não está disponível.")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DatabaseCard("Korczak Control", "KorczakControl", summary?.mongodb == true)
            DatabaseCard("KZ Site", "KorczakTechSite", summary?.mongodb == true)
            DatabaseCard("Moon", "TensuraMoon", summary?.mongodb == true)
        }
    }
}

@Composable
private fun DatabaseCard(title: String, database: String, connected: Boolean) {
    ElevatedCard(Modifier.width(220.dp).height(150.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(database, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (connected) "Conectada" else "Indisponível", color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileView(profile: AccountProfile?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(profile?.name ?: "Conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ProfileField("E-mail", profile?.email)
            ProfileField("Função", profile?.role)
            ProfileField("Departamento", profile?.department)
            ProfileField("ID da conta", profile?.accountId)
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.ifBlank { "Não informado" } ?: "Não informado")
    }
}

@Composable
private fun EventsView(summary: DashboardSummary?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Notificações pendentes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${summary?.unread ?: 0} notificações aguardam leitura.")
        }
    }
}

@Composable
private fun SettingsView() {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Configuração", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("O endereço da API pode ser definido pela variável CONTROL_API_URL antes da inicialização do aplicativo.")
        }
    }
}
