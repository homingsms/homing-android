package com.homingsms.core.model

data class LocalServerInfo(
    val enabled: Boolean,
    val running: Boolean,
    val port: Int,
    val addresses: List<String>,
    val deviceId: String,
    val keys: List<ApiKeyInfo>,
    val lastError: String?
) {
    val baseUrls: List<String>
        get() = addresses.map { "http://$it:$port/api/v1" }

    companion object {
        const val DEFAULT_PORT = 8080

        val Stopped = LocalServerInfo(
            enabled = false,
            running = false,
            port = DEFAULT_PORT,
            addresses = emptyList(),
            deviceId = "",
            keys = emptyList(),
            lastError = null
        )
    }
}
