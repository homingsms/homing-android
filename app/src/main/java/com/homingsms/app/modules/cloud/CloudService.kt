package com.homingsms.app.modules.cloud

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.homingsms.app.R
import com.homingsms.app.modules.notifications.NotificationsService
import com.homingsms.core.data.cloud.CloudSyncEngine
import com.homingsms.core.data.cloud.SyncOutcome
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.model.RemoteDeviceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CloudService : Service() {
    private val engine: CloudSyncEngine by inject()
    private val settings: SettingsRepository by inject()
    private val notifications: NotificationsService by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch { heartbeatLoop() }
        scope.launch { pullLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifications.makeNotification(
            this,
            NotificationsService.NOTIFICATION_ID_CLOUD_SERVICE,
            getString(R.string.cloud_connected_notification)
        )
        ServiceCompat.startForeground(this, NotificationsService.NOTIFICATION_ID_CLOUD_SERVICE, notification, foregroundType())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun heartbeatLoop() {
        while (scope.isActive) {
            when (engine.heartbeat()) {
                SyncOutcome.NotPaired, SyncOutcome.Unpaired -> {
                    stopSelf()
                    return
                }

                else -> Unit
            }
            delay(remote().heartbeatIntervalSeconds.coerceAtLeast(MIN_INTERVAL_SECONDS) * MILLIS)
        }
    }

    private suspend fun pullLoop() {
        while (scope.isActive) {
            val base = remote().pullIntervalSeconds.coerceAtLeast(MIN_INTERVAL_SECONDS)
            val interval = if (engine.connection.value.pushAvailable) maxOf(base * PUSH_FACTOR, PUSH_MIN_SECONDS) else base
            delay(interval * MILLIS)
            engine.pull()
        }
    }

    private fun remote(): RemoteDeviceSettings = settings.current.cloud.remote ?: RemoteDeviceSettings()

    private fun foregroundType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
    } else {
        0
    }

    companion object {
        private const val MILLIS = 1000L
        private const val MIN_INTERVAL_SECONDS = 15
        private const val PUSH_FACTOR = 5
        private const val PUSH_MIN_SECONDS = 300

        fun start(context: Context) {
            val intent = Intent(context, CloudService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CloudService::class.java))
        }
    }
}
