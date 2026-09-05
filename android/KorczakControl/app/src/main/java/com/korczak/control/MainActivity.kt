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
import com.korczak.control.ui.components.MetricCard
import com.korczak.control.ui.components.StatusCard
import com.korczak.control.ui.theme.KorczakControlTheme

class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { KorczakControlTheme { ControlApp() } } } }
private data class Destination(val route: String, val label: String)
private val destinations = listOf(Destination("dashboard","Início"),Destination("sites","Sites"),Destination("apis","APIs"),Destination("apps","Apps"),Destination("databases","Dados"),Destination("infrastructure","Infra"),Destination("notifications","Alertas"),Destination("settings","Ajustes"))
@Composable fun ControlApp() { val navController=rememberNavController(); val backStack by navController.currentBackStackEntryAsState(); val current=backStack?.destination?.route ?: "dashboard"; Scaffold(bottomBar={ NavigationBar { destinations.forEach { item -> NavigationBarItem(selected=current==item.route,onClick={navController.navigate(item.route){launchSingleTop=true;restoreState=true;popUpTo(navController.graph.startDestinationId){saveState=true}}},icon={Text("•")},label={Text(item.label)}) } } }) { padding -> NavHost(navController,"dashboard",Modifier.padding(padding)) { composable("dashboard"){DashboardScreen()}; destinations.filter{it.route!="dashboard"}.forEach{item->composable(item.route){PlaceholderScreen(item.label)}} } } }
@Composable fun DashboardScreen() { Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){ Text("KORCZAK CONTROL",style=MaterialTheme.typography.headlineMedium); Text("Centro administrativo da Korczak Technologies",color=MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricCard("Serviços","—");MetricCard("Alertas","—")}; StatusCard("Status do sistema","Aguardando integrações","GitHub, Render e MongoDB serão conectados nas próximas etapas.") } }
@Composable fun PlaceholderScreen(title:String){Box(Modifier.fillMaxSize().padding(24.dp)){Text(title,style=MaterialTheme.typography.headlineMedium)}}
