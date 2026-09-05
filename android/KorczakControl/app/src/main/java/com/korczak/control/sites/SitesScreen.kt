package com.korczak.control.sites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.korczak.control.ui.components.EmptyState
import com.korczak.control.ui.components.StatusCard

@Composable
fun SitesScreen(items: List<ManagedSite>, onSelect: (ManagedSite) -> Unit) {
    if (items.isEmpty()) {
        EmptyState("Nenhum site cadastrado", "Os sites administrados pela Korczak Technologies aparecerão aqui.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Sites", style = MaterialTheme.typography.headlineMedium) }
        items(items, key = { it.slug }) { site ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(site) }) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(site.name, style = MaterialTheme.typography.titleLarge)
                    Text(site.url, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Status: ${site.status}", color = MaterialTheme.colorScheme.primary)
                    if (site.repository.isNotBlank()) Text("Repositório: ${site.repository}")
                    if (site.technology.isNotBlank()) Text(site.technology, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
