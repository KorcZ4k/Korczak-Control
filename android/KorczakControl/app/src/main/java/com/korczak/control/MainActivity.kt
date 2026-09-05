package com.korczak.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.korczak.control.dashboard.DashboardScreen
import com.korczak.control.ui.theme.KorczakControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KorczakControlTheme { ControlApp() } }
    }
}

private data class Destination(val route: String, val label: String)
private val destinations = listOf(
    Destination("dashboard", "Início"), Destination("sites", "Sites"),
    Destination("apis", "APIs"), Destination("apps", "Apps"),
    Destination("databases", "Dados"), Destination("infrastructure", "Infra"),
    Destination("notifications", "Alertas"), Destination("settings", "Ajustes")
)

@Composable
fun ControlApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "dashboard"

    Scaffold(bottomBar = {
        NavigationBar {
            destinations.forEach { item ->
                NavigationBarItem(
                    selected = current == item.route,
                    onClick = { navController.navigate(item.route) { launchSingleTop = true; restoreState = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } },
                    icon = { Text("•") }, label = { Text(item.label) }
                )
            }
        }
    }) { padding ->
        NavHost(navController, "dashboard", Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen() }
            destinations.filter { it.route != "dashboard" }.forEach { item -> composable(item.route) { PlaceholderScreen(item.label) } }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize().padding(androidx.compose.ui.unit.dp(24f))) {
        Text(title)
    }
}
