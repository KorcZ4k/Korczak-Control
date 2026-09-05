package com.korczak.control.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.korczak.control.ui.components.MetricCard
import com.korczak.control.ui.components.StatusCard

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val repository = remember { DashboardRepository(context) }
    var state by remember { mutableStateOf<DashboardState>(DashboardState.Loading) }
    fun refresh() { state = DashboardState.Loading }
    LaunchedEffect(state) { if (state == DashboardState.Loading) state = repository.load() }
    when (val current = state) {
        DashboardState.Loading -> DashboardLoading()
        is DashboardState.Content -> DashboardContent(current.summary, ::refresh)
        is DashboardState.Error -> DashboardError(current.message, ::refresh)
        DashboardState.SetupRequired -> DashboardSetupRequired()
    }
}

@Composable
private fun DashboardContent(summary: DashboardSummary, onRefresh: () -> Unit) {
    val totalResources = summary.resources.values.sum()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Centro de comando", style = MaterialTheme.typography.headlineSmall)
                        Text("Visão unificada da infraestrutura Korczak Technologies", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(Icons.Default.Dashboard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
                Button(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Atualizar agora") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Recursos", totalResources.toString(), Modifier.weight(1f))
            MetricCard("Online", summary.services["online"].orEmpty().toString(), Modifier.weight(1f))
        }
        StatusCard("Saúde operacional", "${summary.services["attention"].orEmpty()} em atenção · ${summary.services["unavailable"].orEmpty()} indisponíveis", "Dados fornecidos pela Korczak Control API")
        Text("Integrações", style = MaterialTheme.typography.titleLarge)
        summary.integrations.forEach { (name, configured) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium); Text(if (configured) "Conectada ao centro de controle" else "Aguardando configuração", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Icon(if (configured) Icons.Default.CheckCircle else Icons.Default.Schedule, null, tint = if (configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DashboardLoading() { Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() } }

@Composable
private fun DashboardError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text("Não foi possível carregar o painel", style = MaterialTheme.typography.headlineSmall)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry) { Text("Tentar novamente") }
    }
}

@Composable
private fun DashboardSetupRequired() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.LinkOff, null, modifier = Modifier.size(48.dp))
        Text("Conexão necessária", style = MaterialTheme.typography.headlineSmall)
        Text("Configure a URL da Korczak Control API nos Ajustes e entre novamente para visualizar os dados reais.")
    }
}

private fun Int?.orEmpty(): Int = this ?: 0
