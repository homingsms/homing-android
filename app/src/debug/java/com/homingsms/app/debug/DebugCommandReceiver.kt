package com.homingsms.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.homingsms.app.modules.localserver.LocalServerService
import com.homingsms.app.modules.orchestrator.OrchestratorService
import com.homingsms.app.modules.status.AppStateRepository
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.model.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DebugCommandReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val settings: SettingsRepository by inject()
    private val localServer: LocalServerService by inject()
    private val orchestrator: OrchestratorService by inject()
    private val state: AppStateRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            val result = try {
                handle(context.applicationContext, intent)
            } catch (e: IllegalArgumentException) {
                "error: ${e.message}"
            } catch (e: IllegalStateException) {
                "error: ${e.message}"
            }
            pending.resultCode = 0
            pending.resultData = result
            pending.finish()
        }
    }

    private suspend fun handle(context: Context, intent: Intent): String = when (intent.action) {
        ACTION_ENABLE_LOCAL -> {
            val port = intent.getIntExtra(EXTRA_PORT, settings.current.localServer.port)
            settings.update {
                it.copy(
                    general = it.general.copy(autostart = true),
                    localServer = it.localServer.copy(enabled = true, port = port)
                )
            }
            localServer.ensureDeviceId()
            "ok"
        }

        ACTION_START -> {
            orchestrator.start(context, false)
            "ok"
        }

        ACTION_STOP -> {
            orchestrator.stop(context)
            "ok"
        }

        ACTION_CREATE_API_KEY -> {
            val name = intent.getStringExtra(EXTRA_NAME) ?: "debug"
            val scopes = (intent.getStringExtra(EXTRA_SCOPES) ?: Scope.Admin.value)
                .split(',')
                .mapNotNull { Scope.fromValue(it.trim()) }
                .toSet()
            localServer.createKey(name, scopes).secret
        }

        ACTION_STATUS -> {
            state.refresh()
            val info = localServer.info.value
            "running=${info.running} port=${info.port} deviceId=${info.deviceId} gateway=${state.gateway.value.status}"
        }

        else -> "unknown action"
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        const val ACTION_ENABLE_LOCAL = "com.homingsms.app.debug.ENABLE_LOCAL"
        const val ACTION_START = "com.homingsms.app.debug.START"
        const val ACTION_STOP = "com.homingsms.app.debug.STOP"
        const val ACTION_CREATE_API_KEY = "com.homingsms.app.debug.CREATE_API_KEY"
        const val ACTION_STATUS = "com.homingsms.app.debug.STATUS"
        const val EXTRA_PORT = "port"
        const val EXTRA_NAME = "name"
        const val EXTRA_SCOPES = "scopes"
    }
}
