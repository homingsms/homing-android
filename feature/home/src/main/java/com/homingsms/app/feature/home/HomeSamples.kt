package com.homingsms.app.feature.home

import com.homingsms.app.core.designsystem.component.GatewayHealth
import com.homingsms.app.core.designsystem.component.HourlyBar
import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS

internal object HomeSamples {
    private val hourly = List(24) { hour ->
        HourlyBar(
            delivered = listOf(0, 0, 0, 0, 0, 2, 6, 14, 22, 31, 28, 24, 26, 30, 27, 25, 29, 33, 21, 16, 11, 7, 4, 3)[hour],
            sentWithoutReport = if (hour > 6) hour % 4 else 0,
            failed = if (hour == 9 || hour == 17) 2 else 0
        )
    }

    private val sims = listOf(
        SimStatus(0, "Vodafone UK", "+44 7700 900123", 4, true, 214, 500),
        SimStatus(1, "EE", "+44 7700 900456", 2, true, 412, 500)
    )

    private val device = DeviceHealth(
        batteryPercent = 86,
        charging = true,
        network = NetworkKind.Wifi,
        lastCloudContactMillis = PREVIEW_NOW_MILLIS - 40_000
    )

    val online = HomeUiState(
        health = GatewayHealth.Online,
        today = TodayCounts(sent = 626, delivered = 571, failed = 4, queued = 12),
        hourly = hourly,
        sims = sims,
        device = device,
        localServer = LocalServerStatus(enabled = true, running = true, address = "http://192.168.1.20:8080"),
        cloud = CloudStatus(enabled = true, paired = true, connected = true, lastContactMillis = PREVIEW_NOW_MILLIS - 40_000)
    )

    val smsLimit = online.copy(
        health = GatewayHealth.Degraded,
        issues = listOf(HomeIssue.OutgoingSmsLimit(), HomeIssue.SimNoSignal(1)),
        sims = listOf(sims[0], sims[1].copy(signalLevel = 0))
    )

    val offline = HomeUiState(
        health = GatewayHealth.Offline,
        issues = listOf(HomeIssue.ServiceStopped),
        today = TodayCounts(sent = 0, delivered = 0, failed = 0, queued = 3),
        hourly = List(24) { HourlyBar(0, 0, 0) },
        sims = emptyList(),
        device = device.copy(batteryPercent = 9, charging = false, network = NetworkKind.None, lastCloudContactMillis = null),
        localServer = LocalServerStatus(enabled = false),
        cloud = CloudStatus(enabled = false, paired = false)
    )

    val loading = HomeUiState(isLoading = true)
}
