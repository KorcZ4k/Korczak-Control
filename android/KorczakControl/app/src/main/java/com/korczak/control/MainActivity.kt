package com.korczak.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager
import com.korczak.control.dashboard.DashboardScreen
import com.korczak.control.modules.ApiDataScreen
import com.korczak.control.settings.SettingsScreen
import com.korczak.control.ui.theme.KorczakControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KorczakControlTheme { ControlApp() } }
    }
}

private data class Destination(val route: String, val label: String, val path: String? = null)
private val destinations = listOf(
    Destination("dashboard", "Início"), Destination("sites", "Sites", "/api/sites"),
    Destination("apis", "APIs", "/api/managed/api"), Destination("apps", "Apps", "/api/managed/app"),
    Destination("databases", "Dados", "/api/databases/stats"), Destination("github", "GitHub", "/api/github/status"),
    Destination("render", "Render", "/api/render/status"), Destination("notifications", "Alertas", "/api/events/unread"),
    Destination("settings", "Ajustes")
)

@Composable
fun ControlApp() {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val client = remember { ApiClient(session) }
    var authenticated by remember { mutableStateOf(session.isAuthenticated()) }
    var validating by remember { mutableStateOf(authenticated) }

    LaunchedEffect(authenticated) {
        if (!authenticated) { validating = false; return@LaunchedEffect }
        validating = true
        when (val result = client.get("/api/auth/me")) {
            is ApiResult.Success -> Unit
            is ApiResult.Failure -> if (result.code == 401) { session.clear(); authenticated = false }
        }
        validating = false
    }

    if (validating) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (!authenticated) {
        LoginScreen { authenticated = true }
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "dashboard"
    Scaffold(bottomBar = {
        NavigationBar(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            destinations.forEach { item ->
                NavigationBarItem(
                    selected = current == item.route,
                    onClick = { navController.navigate(item.route) { launchSingleTop = true; restoreState = true } },
                    icon = { Text("•") }, label = { Text(item.label) }
                )
            }
        }
    }) { padding ->
        NavHost(navController, "dashboard", Modifier.fillMaxSize().padding(padding)) {
            composable("dashboard") { DashboardScreen() }
            destinations.filter { it.route != "dashboard" && it.route != "settings" }.forEach { item ->
                composable(item.route) { ApiDataScreen(item.label, item.path.orEmpty()) }
            }
            composable("settings") { SettingsScreen { authenticated = false } }
        }
    }
}
