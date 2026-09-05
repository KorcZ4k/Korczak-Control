package com.korczak.control.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.korczak.control.core.SessionManager
import com.korczak.control.ui.components.MetricCard
import com.korczak.control.ui.components.StatusCard

@Composable
fun DashboardScreen() {
    val session = SessionManager(LocalContext.current)
    val permissions = session.permissions()
    val mongo = permissions.optJSONObject("mongodb")
    val integrations = listOfNotNull(
        if (permissions.optBoolean("github")) Triple("GitHub", "Repositórios e código", Icons.Default.Code) else null,
        if (permissions.optBoolean("render")) Triple("Render", "Sites, APIs e serviços", Icons.Default.Cloud) else null,
        if (mongo?.optBoolean("KorczakControl") == true || mongo?.optBoolean("MoonTensura") == true || mongo?.optBoolean("KorczakTechSite") == true) Triple("MongoDB", "Bancos autorizados para sua conta", Icons.Default.Storage) else null,
        if (permissions.optBoolean("sites")) Triple("Sites", "Sites e produtos autorizados", Icons.Default.Language) else null,
        if (permissions.optBoolean("bots")) Triple("Bots", "Bots autorizados", Icons.Default.SmartToy) else null,
        if (permissions.optBoolean("applications")) Triple("Aplicativos", "Aplicativos autorizados", Icons.Default.Apps) else null,
        if (permissions.optBoolean("apis")) Triple("APIs", "APIs autorizadas", Icons.Default.Api) else null
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bem-vindo, ${session.accountName().ifBlank { "usuário" }}", style = MaterialTheme.typography.headlineSmall)
                Text("${session.accountRole().ifBlank { "Conta" }}${session.department().takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                session.accountId().takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Serviços autorizados", integrations.size.toString(), Modifier.weight(1f))
            MetricCard("Sessão", "Ativa", Modifier.weight(1f))
        }

        StatusCard("Permissões carregadas", "Este painel mostra somente os serviços autorizados para sua conta.", "As permissões são verificadas pela API e atualizadas ao validar a sessão.")

        Text("Seus serviços", style = MaterialTheme.typography.titleLarge)
        if (integrations.isEmpty()) Text("Nenhum serviço adicional foi autorizado para esta conta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        integrations.forEach { (name, description, icon) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Autorizado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
