package com.korczak.control.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        if (permissions.optBoolean("github")) Triple("GitHub", "Código e repositórios", Icons.Default.Code) else null,
        if (permissions.optBoolean("render")) Triple("Render", "Serviços e infraestrutura", Icons.Default.Cloud) else null,
        if (mongo?.optBoolean("KorczakControl") == true || mongo?.optBoolean("MoonTensura") == true || mongo?.optBoolean("KorczakTechSite") == true) Triple("MongoDB", "Dados autorizados", Icons.Default.Storage) else null,
        if (permissions.optBoolean("sites")) Triple("Sites", "Produtos web", Icons.Default.Language) else null,
        if (permissions.optBoolean("bots")) Triple("Bots", "Serviços automatizados", Icons.Default.SmartToy) else null,
        if (permissions.optBoolean("applications")) Triple("Aplicativos", "Apps conectados", Icons.Default.Apps) else null,
        if (permissions.optBoolean("apis")) Triple("APIs", "Serviços de integração", Icons.Default.Api) else null
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("VISÃO GERAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("KORCZAK CONTROL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Olá, ${session.accountName().ifBlank { "usuário" }}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("${session.accountRole().ifBlank { "Conta" }}${session.department().takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("SERVIÇOS", integrations.size.toString(), Modifier.weight(1f))
            MetricCard("SESSÃO", "ATIVA", Modifier.weight(1f))
        }

        StatusCard("Sistema operacional", "Sua sessão está protegida e as permissões foram carregadas.", "Acesse Integrações para verificar o estado dos serviços.")

        Text("SERVIÇOS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        if (integrations.isEmpty()) Text("Nenhum serviço adicional foi autorizado para esta conta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        integrations.forEach { (name, description, icon) ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(46.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("AUTORIZADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
