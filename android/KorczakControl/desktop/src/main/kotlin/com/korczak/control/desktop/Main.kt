package com.korczak.control.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

private data class Module(val id: String, val title: String, val description: String)

private val modules = listOf(
    Module("dashboard", "Painel", "Visão geral do sistema e serviços."),
    Module("integrations", "Integrações", "GitHub, Render e serviços conectados."),
    Module("organization", "Equipe", "Contas, acessos e organização."),
    Module("github", "GitHub", "Repositórios, workflows e atividades."),
    Module("render", "Render", "Serviços, implantações e status."),
    Module("databases", "MongoDB", "Korczak Control, KZ Site e Moon."),
    Module("bots", "Bots", "Bots e workflows vinculados."),
    Module("apis", "APIs", "Serviços e endpoints disponíveis."),
    Module("apps", "Apps", "Aplicações vinculadas ao controle."),
    Module("sites", "Sites", "Sites e serviços publicados."),
    Module("clients", "Clientes", "Clientes e orçamentos cadastrados."),
    Module("profile", "Perfil", "Informações da conta e sessão."),
    Module("events", "Eventos", "Atividade recente do sistema."),
    Module("settings", "Ajustes", "Configuração do aplicativo.")
)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Korczak Control") {
        MaterialTheme(colorScheme = darkColorScheme()) { DesktopControlApp() }
    }
}

@Composable
private fun DesktopControlApp() {
    var selected by remember { mutableStateOf(modules.first()) }
    var health by remember { mutableStateOf<ApiHealth?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            runCatching { ControlApiClient.health() }
                .onSuccess { health = it }
                .onFailure { error = it.message ?: "Não foi possível conectar à API." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Row(Modifier.fillMaxSize()) {
        NavigationRail(modifier = Modifier.fillMaxHeight().width(250.dp), header = {
            Column(Modifier.padding(20.dp)) {
                Text("KORCZAK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("CONTROL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
            }
        }) {
            LazyColumn {
                items(modules) { module ->
                    NavigationRailItem(
                        selected = selected.id == module.id,
                        onClick = { selected = module },
                        icon = { Text("•") },
                        label = { Text(module.title) }
                    )
                }
            }
        }

        VerticalDivider()

        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(selected.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(selected.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = { refresh() }, enabled = !loading) {
                    Text(if (loading) "Atualizando" else "Atualizar")
                }
            }

            if (error != null) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Conexão indisponível", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(error ?: "")
                    }
                }
            }

            when (selected.id) {
                "dashboard" -> DashboardView(health, loading)
                "databases" -> DatabaseView(health)
                "integrations" -> IntegrationsView(health)
                else -> PlaceholderView(selected, health)
            }
        }
    }
}

@Composable
private fun DashboardView(health: ApiHealth?, loading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (loading) "Verificando serviços" else health?.service ?: "Korczak Control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(when {
                    loading -> "Consultando a API principal."
                    health?.status == "ok" -> "API conectada e respondendo normalmente."
                    else -> "Status da API indisponível."
                })
                health?.let { Text("Versão ${it.version} • Ambiente ${it.environment}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusCard("API", health?.status == "ok")
            StatusCard("GitHub", health?.github == true)
            StatusCard("Render", health?.render == true)
        }
    }
}

@Composable
private fun IntegrationsView(health: ApiHealth?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IntegrationRow("GitHub", health?.github == true)
        IntegrationRow("Render", health?.render == true)
        IntegrationRow("KZ Site API", health?.kzSiteApi == true)
        IntegrationRow("Korczak Control API", health?.kzControlApi == true)
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
private fun StatusCard(title: String, online: Boolean) {
    ElevatedCard(Modifier.width(190.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(if (online) "Operacional" else "Indisponível", color = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DatabaseView(health: ApiHealth?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bases de dados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DatabaseCard("Korczak Control", "KorczakControl", health?.adminDatabase == true)
            DatabaseCard("KZ Site", "KorczakTechSite", health?.kzSiteDatabase == true)
            DatabaseCard("Moon", "TensuraMoon", health?.moonDatabase == true)
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
private fun PlaceholderView(module: Module, health: ApiHealth?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${module.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Este módulo está integrado à estrutura desktop do Korczak Control. A próxima implementação conectará seus dados específicos aos endpoints correspondentes da API.")
            if (health?.status == "ok") Text("A conexão com a API principal está ativa.", color = MaterialTheme.colorScheme.primary)
        }
    }
}
