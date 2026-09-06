package com.korczak.control.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.korczak.control.core.SessionManager

@Composable
fun DashboardScreen() {
    val session = SessionManager(LocalContext.current)
    val permissions = session.permissions()
    val mongo = permissions.optJSONObject("mongodb")
    val services = listOfNotNull(
        if (permissions.optBoolean("github")) Service("GitHub", "Repositórios e automações", Icons.Default.Code) else null,
        if (permissions.optBoolean("render")) Service("Render", "Infraestrutura e serviços", Icons.Default.Cloud) else null,
        if (mongo?.length() ?: 0 > 0) Service("MongoDB", "Acesso a bases autorizadas", Icons.Default.Storage) else null,
        if (permissions.optBoolean("bots")) Service("Bots", "Serviços automatizados", Icons.Default.SmartToy) else null,
        if (permissions.optBoolean("apis")) Service("APIs", "Interfaces de serviço", Icons.Default.Api) else null
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("VISÃO GERAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Centro de controle", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Monitore serviços, acessos e operações da sua organização.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(session.accountName().ifBlank { "Sessão administrativa" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(session.accountRole(), session.department()).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Acesso autenticado" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Text("SESSÃO ATIVA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Text("ESTADO DO AMBIENTE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Serviços autorizados", services.size.toString(), Modifier.weight(1f))
            SummaryCard("Sessão", "Ativa", Modifier.weight(1f))
        }

        Text("SERVIÇOS DISPONÍVEIS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        if (services.isEmpty()) {
            Text("Nenhum serviço adicional está disponível para esta conta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else services.forEach { service ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(service.icon, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(service.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("DISPONÍVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class Service(val name: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
