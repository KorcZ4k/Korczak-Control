package com.korczak.control.sites

data class ManagedSite(
    val name: String,
    val slug: String,
    val url: String,
    val repository: String = "",
    val technology: String = "",
    val status: String = "unknown"
)
