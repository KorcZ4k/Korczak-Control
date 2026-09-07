package com.korczak.control.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private data class Module(
    val id: String,
    val title: String,
    val description: String
)

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
    Module("settings", "Ajustes", "Configuração do aplicativo."),
)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Korczak Control"
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme()
        ) {
            DesktopControlApp()
        }
    }
}

@Composable
private fun DesktopControlApp() {
    var selected by remember { mutableStateOf(modules.first()) }

    Row(Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight().width(250.dp),
            header = {
                Column(Modifier.padding(20.dp)) {
                    Text("KORCZAK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("CONTROL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(24.dp))
                }
            }
        ) {
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

        HorizontalDivider()

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
                    Spacer(Modifier.height(6.dp))
                    Text(selected.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = {}) { Text("Atualizar") }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Módulo ${selected.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("A interface desktop está preparada como módulo independente. A próxima camada conecta este módulo à mesma API e aos mesmos dados utilizados pelo aplicativo Android.")
                }
            }

            when (selected.id) {
                "databases" -> DatabaseView()
                else -> PlaceholderView(selected)
            }
        }
    }
}

@Composable
private fun DatabaseView() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bases de dados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DatabaseCard("Korczak Control", "KorczakControl")
            DatabaseCard("KZ Site", "KorczakTechSite")
            DatabaseCard("Moon", "TensuraMoon")
        }
    }
}

@Composable
private fun DatabaseCard(title: String, database: String) {
    ElevatedCard(Modifier.width(220.dp).height(130.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(database, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaceholderView(module: Module) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text("Área de trabalho", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Os dados deste módulo serão exibidos em uma interface desktop estruturada e conectada aos mesmos serviços do Korczak Control.")
        }
    }
}
