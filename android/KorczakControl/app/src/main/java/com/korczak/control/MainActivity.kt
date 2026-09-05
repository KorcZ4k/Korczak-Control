package com.korczak.control

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.korczak.control.dashboard.DashboardScreen
import com.korczak.control.modules.ApiDataScreen
import com.korczak.control.settings.SettingsScreen
import com.korczak.control.ui.theme.KorczakControlTheme
import com.korczak.control.update.AppUpdate
import com.korczak.control.update.AppUpdateRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KorczakControlTheme { ControlApp() } }
    }
}

private data class Destination(val route: String, val label: String, val path: String? = null)
private val destinations = listOf(
    Destination("dashboard", "Painel"),
    Destination("sites", "Sites", "/api/sites"),
    Destination("apis", "APIs", "/api/managed/api"),
    Destination("apps", "Apps", "/api/managed/app"),
    Destination("databases", "MongoDB", "/api/databases/stats"),
    Destination("github", "GitHub", "/api/github/status"),
    Destination("render", "Render", "/api/render/status"),
    Destination("notifications", "Eventos", "/api/events/unread"),
    Destination("settings", "Ajustes")
)

@Composable
private fun DestinationIcon(route: String) {
    val icon = when (route) {
        "dashboard" -> Icons.Default.Home
        "sites" -> Icons.Default.Language
        "apis" -> Icons.Default.Api
        "apps" -> Icons.Default.Apps
        "databases" -> Icons.Default.Storage
        "github" -> Icons.Default.Code
        "render" -> Icons.Default.Cloud
        "notifications" -> Icons.Default.Notifications
        else -> Icons.Default.Settings
    }
    Icon(icon, contentDescription = null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "dashboard"
    val currentDestination = destinations.firstOrNull { it.route == current } ?: destinations.first()
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<AppUpdate?>(null) }
    var updateChecked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        update = AppUpdateRepository.check(BuildConfig.VERSION_NAME)
        updateChecked = true
    }

    if (update != null) {
        AlertDialog(
            onDismissRequest = { update = null },
            icon = { Icon(Icons.Default.SystemUpdate, null) },
            title = { Text("Atualização disponível") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Versão ${update!!.version} está disponível.")
                    Text(update!!.notes.take(600), style = MaterialTheme.typography.bodySmall)
                    Text("Ao tocar em atualizar, o APK será baixado. O Android pedirá a confirmação da instalação.", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update!!.downloadUrl)))
                    update = null
                }) { Text("Atualizar") }
            },
            dismissButton = { TextButton(onClick = { update = null }) { Text("Agora não") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("KORCZAK CONTROL", style = MaterialTheme.typography.titleLarge)
                        Text(currentDestination.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            update = AppUpdateRepository.check(BuildConfig.VERSION_NAME)
                            updateChecked = true
                        }
                    }) { Icon(Icons.Default.SystemUpdate, "Verificar atualizações") }
                    AssistChip(
                        onClick = { navController.navigate("dashboard") { launchSingleTop = true } },
                        label = { Text("Centro de controle") },
                        leadingIcon = { Icon(Icons.Default.Shield, null, Modifier.size(16.dp)) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(destinations[0], destinations[1], destinations[5], destinations[7], destinations[8]).forEach { item ->
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = { navController.navigate(item.route) { launchSingleTop = true; restoreState = true } },
                        icon = { DestinationIcon(item.route) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, "dashboard", Modifier.fillMaxSize().padding(padding)) {
            composable("dashboard") { DashboardScreen() }
            destinations.filter { it.route != "dashboard" && it.route != "settings" }.forEach { item ->
                composable(item.route) { ApiDataScreen(item.label, item.path.orEmpty()) }
            }
            composable("settings") { SettingsScreen() }
        }
    }
}
