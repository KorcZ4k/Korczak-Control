package com.korczak.control.dashboard

data class DashboardSummary(
    val generatedAt: String,
    val resources: Map<String, Int>,
    val services: Map<String, Int>,
    val integrations: Map<String, Boolean>
)

sealed interface DashboardState {
    data object Loading : DashboardState
    data class Content(val summary: DashboardSummary) : DashboardState
    data class Error(val message: String) : DashboardState
    data object SetupRequired : DashboardState
}
