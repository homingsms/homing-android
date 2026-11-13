package com.homingsms.app.modules.localserver

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.homingsms.app.BuildConfig
import com.homingsms.app.R
import com.homingsms.app.modules.notifications.NotificationsService
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.server.LocalApiConfig
import com.homingsms.core.server.installLocalApi
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import java.io.IOException
import kotlin.concurrent.thread
import org.koin.android.ext.android.inject

class WebService : Service() {
    private val settings: SettingsRepository by inject()
    private val notificationsService: NotificationsService by inject()
    private val localServer: LocalServerService by inject()
    private val config: LocalApiConfig by inject()

    private var server: EmbeddedServer<*, *>? = null
    private var port: Int = 0

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
    }

    @Suppress("DEPRECATION")
    private val wifiLock: WifiManager.WifiLock by lazy {
        (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            javaClass.name
        )
    }

    override fun onCreate() {
        super.onCreate()
        port = settings.current.localServer.port
        try {
            server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                installLocalApi(config, BuildConfig.VERSION_NAME)
            }.start(wait = false)
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
            wifiLock.acquire()
            localServer.onStarted()
        } catch (e: IOException) {
            localServer.onStopped(e.message ?: "Cannot listen on port $port")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationsService.makeNotification(
            this,
            NotificationsService.NOTIFICATION_ID_LOCAL_SERVICE,
            getString(R.string.sms_gateway_is_running_on_port, port)
        )
        ServiceCompat.startForeground(
            this,
            NotificationsService.NOTIFICATION_ID_LOCAL_SERVICE,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        if (wifiLock.isHeld) wifiLock.release()
        if (wakeLock.isHeld) wakeLock.release()
        server?.let { running -> thread { running.stop(STOP_GRACE_MS, STOP_TIMEOUT_MS) } }
        server = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        localServer.onStopped(null)
        super.onDestroy()
    }

    companion object {
        private const val STOP_GRACE_MS = 500L
        private const val STOP_TIMEOUT_MS = 2_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 24L * 60 * 60 * 1000

        fun start(context: Context) {
            val intent = Intent(context, WebService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WebService::class.java))
        }
    }
}
