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
import com.korczak.control.auth.LoginScreen
import com.korczak.control.core.SessionManager
import com.korczak.control.dashboard.DashboardScreen
import com.korczak.control.modules.ApiDataScreen
import com.korczak.control.settings.SettingsScreen
import com.korczak.control.ui.theme.KorczakControlTheme
import com.korczak.control.update.AppUpdate
import com.korczak.control.update.AppUpdateRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KorczakControlTheme { RootApp() } }
    }
}

@Composable
private fun RootApp() {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    var authenticated by remember { mutableStateOf(session.isAuthenticated()) }
    if (session.isApiConfigured() && !authenticated) LoginScreen { authenticated = true }
    else ControlApp(onLogout = { session.clear(); authenticated = false })
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

private fun isAllowed(destination: Destination, permissions: JSONObject, securedMode: Boolean): Boolean {
    if (!securedMode || destination.route in listOf("dashboard", "settings", "notifications")) return true
    return when (destination.route) {
        "sites" -> permissions.optBoolean("sites")
        "apis" -> permissions.optBoolean("apis")
        "apps" -> permissions.optBoolean("applications")
        "databases" -> {
            val mongo = permissions.optJSONObject("mongodb") ?: return false
            mongo.optBoolean("Admin") || mongo.optBoolean("MoonTensura") || mongo.optBoolean("KorczakTechSite")
        }
        "github" -> permissions.optBoolean("github")
        "render" -> permissions.optBoolean("render")
        else -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlApp(onLogout: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "dashboard"
    val currentDestination = destinations.firstOrNull { it.route == current } ?: destinations.first()
    val permissions = session.permissions()
    val securedMode = session.isApiConfigured()
    val visibleDestinations = destinations.filter { isAllowed(it, permissions, securedMode) }
    val preferredBottom = listOf("dashboard", "sites", "github", "notifications", "settings")
        .mapNotNull { route -> visibleDestinations.firstOrNull { it.route == route } }
    val bottomDestinations = if (preferredBottom.size >= 3) preferredBottom else visibleDestinations.take(5)
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<AppUpdate?>(null) }

    LaunchedEffect(Unit) { update = AppUpdateRepository.check(BuildConfig.VERSION_NAME) }

    if (update != null) AlertDialog(
        onDismissRequest = { update = null },
        icon = { Icon(Icons.Default.SystemUpdate, null) },
        title = { Text("Atualização disponível") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Versão ${update!!.version} está disponível.")
            Text(update!!.notes.take(600), style = MaterialTheme.typography.bodySmall)
            Text("Ao tocar em atualizar, o APK será baixado. O Android pedirá a confirmação da instalação.", style = MaterialTheme.typography.labelSmall)
        } },
        confirmButton = { TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update!!.downloadUrl)))
            update = null
        }) { Text("Atualizar") } },
        dismissButton = { TextButton(onClick = { update = null }) { Text("Agora não") } }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("KORCZAK CONTROL", style = MaterialTheme.typography.titleLarge)
                    Text(currentDestination.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } },
                actions = {
                    IconButton(onClick = { scope.launch { update = AppUpdateRepository.check(BuildConfig.VERSION_NAME) } }) {
                        Icon(Icons.Default.SystemUpdate, "Verificar atualizações")
                    }
                    if (securedMode && session.isAuthenticated()) IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Sair") }
                    Spacer(Modifier.width(4.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { item ->
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
            visibleDestinations.filter { it.route != "dashboard" && it.route != "settings" }.forEach { item ->
                composable(item.route) { ApiDataScreen(item.label, item.path.orEmpty()) }
            }
            composable("settings") { SettingsScreen() }
        }
    }
}
