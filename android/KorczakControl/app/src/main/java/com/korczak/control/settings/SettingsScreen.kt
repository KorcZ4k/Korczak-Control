package com.korczak.control.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
        Text("Configure apenas os serviços que a Korczak Technologies já utiliza.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        IntegrationItem("GitHub", "Repositórios, código e projetos", Icons.Default.Code)
        IntegrationItem("Render", "Deploys e serviços hospedados", Icons.Default.Cloud)
        IntegrationItem("MongoDB", "Bancos e collections", Icons.Default.Storage)
        IntegrationItem("Sites Korczak", "Monitoramento dos sites e aplicações", Icons.Default.Language)

        HorizontalDivider()
        Text("Segurança", style = MaterialTheme.typography.titleMedium)
        Text("Tokens e credenciais administrativas não serão inseridos no código do aplicativo. Cada integração deverá usar um método de conexão adequado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IntegrationItem(name: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Ainda não conectado", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
