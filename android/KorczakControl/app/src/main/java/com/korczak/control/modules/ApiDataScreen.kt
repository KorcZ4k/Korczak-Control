package com.korczak.control.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.korczak.control.core.ApiClient
import com.korczak.control.core.ApiResult
import com.korczak.control.core.SessionManager

@Composable
fun ApiDataScreen(title: String, path: String, subtitle: String = "Dados reais da Korczak Control API") {
    val context = LocalContext.current
    val client = remember { ApiClient(SessionManager(context)) }
    var state by remember { mutableStateOf<Any>("loading") }

    suspend fun load() {
        state = "loading"
        state = when (val result = client.get(path)) {
            is ApiResult.Success -> result.body
            is ApiResult.Failure -> result
        }
    }

    LaunchedEffect(path) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (val current = state) {
            "loading" -> CircularProgressIndicator()
            is ApiResult.Failure -> {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { state = "loading" }) { Text("Atualizar") }
            }
            is String -> Card(Modifier.fillMaxWidth().weight(1f, false)) {
                Text(current, modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = { state = "loading" }) { Text("Atualizar") }
        if (state == "loading") LaunchedEffect("reload-$path") { load() }
    }
}
