package com.korczak.control.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.korczak.control.ui.components.MetricCard
import com.korczak.control.ui.components.StatusCard

@Composable
fun DashboardScreen() {
    val integrations = listOf(
        Triple("GitHub", "Repositórios e código", Icons.Default.Code),
        Triple("Render", "Sites, APIs e serviços", Icons.Default.Cloud),
        Triple("MongoDB", "Bancos de dados", Icons.Default.Storage),
        Triple("Korczak Technologies", "Sites e produtos", Icons.Default.Language)
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Centro de comando", style = MaterialTheme.typography.headlineSmall)
                        Text("Controle os serviços da Korczak Technologies em um único lugar.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(Icons.Default.Dashboard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
                Text("Nenhum servidor adicional é necessário para abrir o painel. Conecte apenas os serviços que você já utiliza.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Serviços", integrations.size.toString(), Modifier.weight(1f))
            MetricCard("Conectados", "0", Modifier.weight(1f))
        }

        StatusCard("Pronto para configurar", "Conecte GitHub, Render, MongoDB e os serviços Korczak quando desejar.", "As integrações serão adicionadas nas próximas configurações.")

        Text("Serviços disponíveis", style = MaterialTheme.typography.titleLarge)
        integrations.forEach { (name, description, icon) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Não conectado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}
