package com.homingsms.app.modules.localserver

import android.content.Context
import com.homingsms.core.data.repository.ApiKeyRepository
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.model.ApiKeyInfo
import com.homingsms.core.model.CreatedApiKey
import com.homingsms.core.model.Ids
import com.homingsms.core.model.LocalServerInfo
import com.homingsms.core.model.Scope
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LocalServerService(private val settings: SettingsRepository, private val keys: ApiKeyRepository, scope: CoroutineScope) {
    private data class Runtime(val running: Boolean, val addresses: List<String>, val error: String?)

    private val runtime = MutableStateFlow(Runtime(running = false, addresses = emptyList(), error = null))

    val info: StateFlow<LocalServerInfo> = combine(settings.settings, runtime, keys.keys) { current, state, keyList ->
        LocalServerInfo(
            enabled = current.localServer.enabled,
            running = state.running,
            port = current.localServer.port,
            addresses = state.addresses,
            deviceId = current.localServer.deviceId.orEmpty(),
            keys = keyList,
            lastError = state.error
        )
    }.stateIn(scope, SharingStarted.Eagerly, LocalServerInfo.Stopped)

    fun ensureDeviceId(): String = settings.current.localServer.deviceId ?: Ids.device().also { id ->
        settings.updateBlocking { it.copy(localServer = it.localServer.copy(deviceId = it.localServer.deviceId ?: id)) }
    }

    fun start(context: Context) {
        ensureDeviceId()
        if (!settings.current.localServer.enabled) return
        WebService.start(context)
    }

    fun stop(context: Context) {
        WebService.stop(context)
    }

    suspend fun setEnabled(enabled: Boolean) {
        settings.update { it.copy(localServer = it.localServer.copy(enabled = enabled)) }
    }

    suspend fun setPort(port: Int) {
        require(port in MIN_PORT..MAX_PORT) { "Port must be between $MIN_PORT and $MAX_PORT" }
        settings.update { it.copy(localServer = it.localServer.copy(port = port)) }
    }

    suspend fun createKey(name: String, scopes: Set<Scope>): CreatedApiKey = keys.create(name, scopes)

    suspend fun revokeKey(id: String): Boolean = keys.revoke(id)

    suspend fun listKeys(): List<ApiKeyInfo> = keys.list()

    internal fun onStarted() {
        runtime.value = Runtime(running = true, addresses = localAddresses(), error = null)
    }

    internal fun onStopped(error: String?) {
        runtime.value = Runtime(running = false, addresses = emptyList(), error = error)
    }

    fun refreshAddresses() {
        runtime.value = runtime.value.copy(addresses = if (runtime.value.running) localAddresses() else emptyList())
    }

    companion object {
        const val MIN_PORT = 1024
        const val MAX_PORT = 65535

        fun localAddresses(): List<String> = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .distinct()
        } catch (_: SocketException) {
            emptyList()
        }
    }
}
