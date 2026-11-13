package com.homingsms.app.modules.cloud

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import com.homingsms.core.data.cloud.CloudSyncEngine
import com.homingsms.core.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object CloudPollPolicy {
    const val MIN_INTERVAL_SECONDS = 15
    const val WAKE_LOCK_TIMEOUT_MS = 60_000L
    private const val MILLIS = 1000L

    fun intervalMs(intervalSeconds: Int): Long = intervalSeconds.coerceAtLeast(MIN_INTERVAL_SECONDS) * MILLIS

    fun needsAlarm(enabled: Boolean, paired: Boolean, pushAvailable: Boolean): Boolean = enabled && paired && !pushAvailable

    fun shouldPull(now: Long, lastSuccessAt: Long?, intervalMs: Long): Boolean = lastSuccessAt == null || now - lastSuccessAt >= intervalMs
}

fun Context.cloudWakeLock(tag: String): PowerManager.WakeLock =
    (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)

object CloudPollAlarm {
    private const val REQUEST_CODE = 7101

    fun schedule(context: Context, intervalSeconds: Int) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val at = SystemClock.elapsedRealtime() + CloudPollPolicy.intervalMs(intervalSeconds)
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        manager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, CloudPollReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class CloudPollReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val engine: CloudSyncEngine by inject()
    private val settings: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val cloud = settings.current.cloud
        val pushAvailable = engine.connection.value.pushAvailable
        if (!CloudPollPolicy.needsAlarm(cloud.enabled, engine.isPaired, pushAvailable)) return
        val pending = goAsync()
        val wakeLock = appContext.cloudWakeLock(WAKE_LOCK_TAG)
        wakeLock.acquire(CloudPollPolicy.WAKE_LOCK_TIMEOUT_MS)
        scope.launch {
            try {
                val interval = CloudPollPolicy.intervalMs(cloud.effectivePullIntervalSeconds)
                if (CloudPollPolicy.shouldPull(System.currentTimeMillis(), engine.connection.value.lastSuccessAt, interval)) {
                    engine.heartbeat()
                }
            } finally {
                CloudPollAlarm.schedule(appContext, settings.current.cloud.effectivePullIntervalSeconds)
                if (wakeLock.isHeld) wakeLock.release()
                pending.finish()
            }
        }
    }

    private companion object {
        const val WAKE_LOCK_TAG = "homing:cloud-poll"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
