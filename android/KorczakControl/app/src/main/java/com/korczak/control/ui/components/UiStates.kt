package com.korczak.control.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun LoadingState(message: String = "Carregando...") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(); Text(message) } } }
@Composable fun EmptyState(title: String, message: String) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable fun ErrorState(message: String, onRetry: () -> Unit) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Ocorreu um erro", style = MaterialTheme.typography.titleLarge); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onRetry) { Text("Tentar novamente") } } } }
@Composable fun StatusCard(title: String, status: String, detail: String) { Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(status, color = MaterialTheme.colorScheme.primary); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable fun MetricCard(label: String, value: String) { Card { Column(Modifier.padding(20.dp)) { Text(value, style = MaterialTheme.typography.headlineSmall); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
