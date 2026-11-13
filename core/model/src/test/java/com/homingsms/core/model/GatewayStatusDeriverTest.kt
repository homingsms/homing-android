package com.homingsms.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayStatusDeriverTest {
    private val now = 1_731_400_000_000L

    private val healthy = DeviceHealth(
        battery = BatteryInfo(level = 80, charging = true, powerSaver = false),
        network = NetworkType.Wifi,
        sims = listOf(SimHealth(0, "Carrier A", "+15550000001", 4, 3), SimHealth(1, "Carrier B", null, 3, 0)),
        appVersion = "1.0.0",
        airplaneMode = false,
        localQueueSize = 0,
        collectedAt = now
    )

    private val paired = CloudPairingState.Paired("dev_1", "acc_1", "https://api.homingsms.com", now)

    private val base = GatewayInputs(
        running = true,
        localEnabled = true,
        localRunning = true,
        localError = null,
        cloudEnabled = false,
        cloudPairing = CloudPairingState.Unpaired,
        cloudConnection = CloudConnectionState.Idle,
        health = healthy,
        missingPermissions = emptyList(),
        smsLimit = SmsLimitState.Clear,
        sims = emptyList(),
        now = now
    )

    private fun derive(inputs: GatewayInputs) = GatewayStatusDeriver.derive(inputs)

    @Test
    fun stoppedServiceIsOffline() {
        val state = derive(base.copy(running = false))
        assertEquals(GatewayStatus.Offline, state.status)
        assertEquals(listOf(StatusReason.Stopped), state.reasons)
    }

    @Test
    fun noModeEnabledIsOffline() {
        val state = derive(base.copy(localEnabled = false, cloudEnabled = false))
        assertEquals(GatewayStatus.Offline, state.status)
        assertEquals(listOf(StatusReason.NoModeEnabled), state.reasons)
    }

    @Test
    fun healthyLocalGatewayIsOnline() {
        val state = derive(base.copy())
        assertEquals(GatewayStatus.Online, state.status)
        assertTrue(state.reasons.isEmpty())
    }

    @Test
    fun batterySaverDegrades() {
        val state = derive(base.copy(health = healthy.copy(battery = BatteryInfo(50, charging = false, powerSaver = true))))
        assertEquals(GatewayStatus.Degraded, state.status)
        assertEquals(listOf(StatusReason.BatterySaver), state.reasons)
    }

    @Test
    fun lowBatteryWhileUnpluggedDegrades() {
        val state = derive(base.copy(health = healthy.copy(battery = BatteryInfo(9, charging = false, powerSaver = false))))
        assertEquals(GatewayStatus.Degraded, state.status)
        assertEquals(listOf(StatusReason.LowBattery(9)), state.reasons)
    }

    @Test
    fun noSignalOnOneSimDegradesAndOnAllSimsGoesOffline() {
        val oneDown = healthy.copy(sims = listOf(healthy.sims[0], healthy.sims[1].copy(signalLevel = 0)))
        val degraded = derive(base.copy(health = oneDown))
        assertEquals(GatewayStatus.Degraded, degraded.status)
        assertEquals(listOf(StatusReason.NoSignal(1)), degraded.reasons)

        val allDown = healthy.copy(sims = healthy.sims.map { it.copy(inService = false) })
        val offline = derive(base.copy(health = allDown))
        assertEquals(GatewayStatus.Offline, offline.status)
        assertEquals(listOf(StatusReason.NoSignal(0), StatusReason.NoSignal(1)), offline.reasons)
    }

    @Test
    fun noSimAndAirplaneModeAreOffline() {
        assertEquals(listOf(StatusReason.NoSim), derive(base.copy(health = healthy.copy(sims = emptyList()))).reasons)
        val airplane = derive(base.copy(health = healthy.copy(airplaneMode = true)))
        assertEquals(GatewayStatus.Offline, airplane.status)
        assertEquals(listOf(StatusReason.AirplaneMode), airplane.reasons)
    }

    @Test
    fun missingSendPermissionIsOfflineOtherPermissionsDegrade() {
        val send = derive(base.copy(missingPermissions = listOf(GatewayStatusDeriver.SEND_SMS_PERMISSION)))
        assertEquals(GatewayStatus.Offline, send.status)
        val read = derive(base.copy(missingPermissions = listOf("android.permission.READ_SMS")))
        assertEquals(GatewayStatus.Degraded, read.status)
        assertEquals(listOf(StatusReason.PermissionsMissing(listOf("android.permission.READ_SMS"))), read.reasons)
    }

    @Test
    fun outgoingSmsLimitIsReportedWithTheAdbFix() {
        val limit = SmsLimitState(maxCount = 30, windowMs = 1_800_000, sentInWindow = 30, limitHitAt = now - 60_000)
        val state = derive(base.copy(smsLimit = limit))
        assertEquals(GatewayStatus.Degraded, state.status)
        val reason = state.reasons.single() as StatusReason.OutgoingSmsLimit
        assertEquals(30, reason.maxCount)
        assertEquals("adb shell settings put global sms_outgoing_check_max_count 10000", reason.fixCommand)
    }

    @Test
    fun oldLimitHitIsIgnoredOnceBelowLimit() {
        val limit = SmsLimitState(maxCount = 30, windowMs = 1_800_000, sentInWindow = 2, limitHitAt = now - 7_200_000)
        assertEquals(GatewayStatus.Online, derive(base.copy(smsLimit = limit)).status)
    }

    @Test
    fun unreachableCloudIsOfflineWhenItIsTheOnlyMode() {
        val connection = CloudConnectionState.Idle.copy(
            enabled = true,
            consecutiveFailures = 3,
            lastSuccessAt = now - 1000,
            lastError = "timeout"
        )
        val state = derive(base.copy(localEnabled = false, cloudEnabled = true, cloudPairing = paired, cloudConnection = connection))
        assertEquals(GatewayStatus.Offline, state.status)
        assertEquals(listOf(StatusReason.CloudUnreachable(now - 1000, "timeout")), state.reasons)
    }

    @Test
    fun unreachableCloudOnlyDegradesWhenLocalServerWorks() {
        val connection = CloudConnectionState.Idle.copy(enabled = true, consecutiveFailures = 2)
        val state = derive(base.copy(cloudEnabled = true, cloudPairing = paired, cloudConnection = connection))
        assertEquals(GatewayStatus.Degraded, state.status)
    }

    @Test
    fun singleCloudFailureIsTolerated() {
        val connection = CloudConnectionState.Idle.copy(enabled = true, consecutiveFailures = 1)
        val state = derive(base.copy(localEnabled = false, cloudEnabled = true, cloudPairing = paired, cloudConnection = connection))
        assertEquals(GatewayStatus.Online, state.status)
    }

    @Test
    fun cloudStatesMapToReasons() {
        val unpaired = derive(base.copy(localEnabled = false, cloudEnabled = true))
        assertEquals(listOf(StatusReason.CloudNotPaired), unpaired.reasons)
        val removed = derive(base.copy(localEnabled = false, cloudEnabled = true, cloudPairing = CloudPairingState.Removed(now)))
        assertEquals(listOf(StatusReason.CloudDeviceRemoved), removed.reasons)
        val paused = derive(
            base.copy(
                localEnabled = false,
                cloudEnabled = true,
                cloudPairing = paired,
                cloudConnection = CloudConnectionState.Idle.copy(remotePaused = true)
            )
        )
        assertEquals(GatewayStatus.Degraded, paused.status)
        assertEquals(listOf(StatusReason.CloudPaused), paused.reasons)
    }

    @Test
    fun localServerDownIsOfflineWhenItIsTheOnlyMode() {
        val state = derive(base.copy(localRunning = false))
        assertEquals(GatewayStatus.Offline, state.status)
        assertEquals(listOf(StatusReason.LocalServerDown(null)), state.reasons)
    }

    @Test
    fun noNetworkIsOffline() {
        val state = derive(base.copy(health = healthy.copy(network = NetworkType.None)))
        assertEquals(GatewayStatus.Offline, state.status)
        assertTrue(StatusReason.NoNetwork in state.reasons)
    }

    @Test
    fun simDailyLimitDegradesAndAllLimitedGoesOffline() {
        val sims = listOf(
            SimStats(0, "A", null, 4, sentToday = 100, dailyLimit = 100, enabled = true),
            SimStats(1, "B", null, 4, sentToday = 10, dailyLimit = 100, enabled = true)
        )
        val one = derive(base.copy(sims = sims))
        assertEquals(GatewayStatus.Degraded, one.status)
        assertEquals(listOf(StatusReason.SimDailyLimitReached(0, 100)), one.reasons)
        val all = derive(base.copy(sims = sims.map { it.copy(sentToday = 100) }))
        assertEquals(GatewayStatus.Offline, all.status)
    }

    @Test
    fun blockingIssuesComeFirst() {
        val state = derive(
            base.copy(
                health = healthy.copy(battery = BatteryInfo(50, false, powerSaver = true), airplaneMode = true)
            )
        )
        assertTrue(state.issues.first().blocking)
    }
}
