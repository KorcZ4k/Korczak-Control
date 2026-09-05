package com.korczak.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.korczak.control.ui.theme.KorczakControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { KorczakControlTheme { ControlApp() } } }
}

@Composable
fun ControlApp() {
    val navController = rememberNavController()
    Scaffold { padding ->
        NavHost(navController = navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(onOpenSettings = { navController.navigate("settings") }) }
            composable("settings") { PlaceholderScreen("Configurações") }
            composable("sites") { PlaceholderScreen("Sites") }
            composable("apis") { PlaceholderScreen("APIs") }
            composable("apps") { PlaceholderScreen("Aplicativos") }
            composable("databases") { PlaceholderScreen("Databases") }
            composable("infrastructure") { PlaceholderScreen("Infraestrutura") }
            composable("notifications") { PlaceholderScreen("Notificações") }
        }
    }
}

@Composable fun DashboardScreen(onOpenSettings: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("KORCZAK CONTROL", style = MaterialTheme.typography.headlineMedium); Text("Centro administrativo da Korczak Technologies", color = MaterialTheme.colorScheme.onSurfaceVariant); Card { Column(Modifier.padding(20.dp)) { Text("Status do sistema"); Text("Integrações serão conectadas nas próximas etapas.") } }; Button(onClick = onOpenSettings) { Text("Configurações") } } }
@Composable fun PlaceholderScreen(title: String) { Box(Modifier.fillMaxSize().padding(24.dp)) { Text(title, style = MaterialTheme.typography.headlineMedium) } }
