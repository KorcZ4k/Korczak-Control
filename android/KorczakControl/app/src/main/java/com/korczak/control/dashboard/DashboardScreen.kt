package com.korczak.control.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    LaunchedEffect(state) {
        if (state == DashboardState.Loading) state = repository.load()
    }

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
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium)
        Text("Visão geral da infraestrutura", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Recursos", totalResources.toString(), Modifier.weight(1f))
            MetricCard("Online", summary.services["online"].orEmpty().toString(), Modifier.weight(1f))
        }
        StatusCard(
            "Saúde operacional",
            "${summary.services["attention"].orEmpty()} em atenção · ${summary.services["unavailable"].orEmpty()} indisponíveis",
            "Atualizado pelo Korczak Control API"
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Integrações", style = MaterialTheme.typography.titleMedium)
                summary.integrations.forEach { (name, configured) ->
                    Text("${name.replaceFirstChar { it.uppercase() }}: ${if (configured) "Configurada" else "Não configurada"}")
                }
            }
        }
        Button(onClick = onRefresh) { Text("Atualizar dashboard") }
    }
}

@Composable
private fun DashboardLoading() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium)
        Text("Carregando dados da infraestrutura...")
    }
}

@Composable
private fun DashboardError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Não foi possível carregar o dashboard", style = MaterialTheme.typography.headlineSmall)
        Text(message)
        Button(onClick = onRetry) { Text("Tentar novamente") }
    }
}

@Composable
private fun DashboardSetupRequired() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Dashboard pronto para conexão", style = MaterialTheme.typography.headlineSmall)
        Text("Configure a URL da Korczak Control API e entre com uma conta para visualizar os dados reais.")
    }
}

private fun Int?.orEmpty(): Int = this ?: 0
