package com.homingsms.core.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.homingsms.app.helpers.SubscriptionsHelper
import com.homingsms.core.model.BatteryInfo
import com.homingsms.core.model.DeviceHealth
import com.homingsms.core.model.NetworkType
import com.homingsms.core.model.SimHealth

class DeviceHealthReader(private val context: Context, private val appVersion: String) {
    fun read(sentToday: Map<Int, Int>, localQueueSize: Int, now: Long = System.currentTimeMillis()): DeviceHealth = DeviceHealth(
        battery = battery(),
        network = network(),
        sims = sims(sentToday),
        appVersion = appVersion,
        airplaneMode = airplaneMode(),
        localQueueSize = localQueueSize,
        collectedAt = now
    )

    fun simCards(): List<SimCard> = SubscriptionsHelper.getActiveSimCards(context)

    fun missingPermissions(permissions: List<String>): List<String> = permissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    private fun battery(): BatteryInfo {
        val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val percent = if (level >= 0 && scale > 0) level * PERCENT / scale else 0
        val charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return BatteryInfo(percent.coerceIn(0, PERCENT), charging, powerManager?.isPowerSaveMode == true)
    }

    @SuppressLint("MissingPermission")
    private fun network(): NetworkType {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return NetworkType.Unknown
        val capabilities = try {
            manager.activeNetwork?.let(manager::getNetworkCapabilities)
        } catch (_: SecurityException) {
            return NetworkType.Unknown
        } ?: return NetworkType.None
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular
            else -> NetworkType.Unknown
        }
    }

    private fun airplaneMode(): Boolean = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    private fun sims(sentToday: Map<Int, Int>): List<SimHealth> = simCards().map { card ->
        val signal = signal(card.slotIndex)
        SimHealth(
            slot = card.slotIndex,
            carrier = card.carrierName,
            phoneNumber = card.phoneNumber,
            signalLevel = signal.first,
            sentToday = sentToday[card.slotIndex] ?: 0,
            inService = signal.second
        )
    }

    @SuppressLint("MissingPermission")
    private fun signal(slot: Int): Pair<Int?, Boolean> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null to true
        val subscriptionId = SubscriptionsHelper.getSubscriptionId(context, slot) ?: return null to true
        val base = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return null to true
        return try {
            val manager = base.createForSubscriptionId(subscriptionId)
            val level = manager.signalStrength?.level
            val inService = manager.serviceState?.state?.let { it == ServiceState.STATE_IN_SERVICE } ?: true
            level to inService
        } catch (_: SecurityException) {
            null to true
        }
    }

    companion object {
        private const val PERCENT = 100
    }
}

object SmsOutgoingLimits {
    private const val MAX_COUNT_KEY = "sms_outgoing_check_max_count"
    private const val INTERVAL_KEY = "sms_outgoing_check_interval_ms"

    fun maxCount(context: Context, fallback: Int): Int = try {
        Settings.Global.getInt(context.contentResolver, MAX_COUNT_KEY, fallback)
    } catch (_: SecurityException) {
        fallback
    }

    fun intervalMs(context: Context, fallback: Long): Long = try {
        Settings.Global.getLong(context.contentResolver, INTERVAL_KEY, fallback)
    } catch (_: SecurityException) {
        fallback
    }
}
