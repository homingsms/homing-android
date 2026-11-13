package com.homingsms.app.ui

import com.homingsms.app.core.designsystem.component.AppendState
import com.homingsms.app.feature.home.HomeIssue
import com.homingsms.app.feature.home.MissingPermission
import com.homingsms.app.feature.onboarding.DeviceMaker
import com.homingsms.app.feature.onboarding.OnboardingPermission
import com.homingsms.app.modules.cloud.CloudPollPolicy
import com.homingsms.app.modules.messages.SimCandidate
import com.homingsms.app.modules.messages.SimRouter
import com.homingsms.app.ui.system.AppPermissions
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.app.ui.system.SystemIntents
import com.homingsms.core.model.GatewayState
import com.homingsms.core.model.GatewayStatus
import com.homingsms.core.model.SimSelectionMode
import com.homingsms.core.model.StatusIssue
import com.homingsms.core.model.StatusReason
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MappingAndPolicyTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun everyStatusReasonMapsToAHomeIssue() {
        val expected = mapOf<StatusReason, HomeIssue>(
            StatusReason.Stopped to HomeIssue.ServiceStopped,
            StatusReason.NoModeEnabled to HomeIssue.NotConfigured,
            StatusReason.PermissionsMissing(
                listOf("android.permission.SEND_SMS")
            ) to HomeIssue.PermissionMissing(MissingPermission.SendSms),
            StatusReason.NoSim to HomeIssue.NoSim,
            StatusReason.AirplaneMode to HomeIssue.AirplaneMode,
            StatusReason.NoNetwork to HomeIssue.NoNetwork,
            StatusReason.NoSignal(1) to HomeIssue.SimNoSignal(1),
            StatusReason.BatterySaver to HomeIssue.BatterySaver,
            StatusReason.LowBattery(5) to HomeIssue.LowBattery,
            StatusReason.OutgoingSmsLimit(
                30,
                30,
                1L,
                null
            ) to HomeIssue.OutgoingSmsLimit(StatusReason.OutgoingSmsLimit.SUGGESTED_MAX_COUNT),
            StatusReason.SimDailyLimitReached(0, 10) to HomeIssue.SimCapReached(0),
            StatusReason.CloudUnreachable(null, "timeout") to HomeIssue.CloudUnreachable,
            StatusReason.CloudPaused to HomeIssue.Paused,
            StatusReason.CloudNotPaired to HomeIssue.CloudNotPaired,
            StatusReason.CloudDeviceRemoved to HomeIssue.CloudNotPaired,
            StatusReason.LocalServerDown("port in use") to HomeIssue.LocalServerDown
        )
        expected.forEach { (reason, issue) -> assertEquals(reason.toString(), listOf(issue), UiMappers.issues(reason)) }
        assertEquals(16, expected.keys.map { it::class }.toSet().size)
    }

    @Test
    fun cappedOnEverySimBecomesDeviceCap() {
        val gateway = GatewayState(
            GatewayStatus.Offline,
            listOf(StatusIssue(StatusReason.SimDailyLimitReached(0, 10), true), StatusIssue(StatusReason.SimDailyLimitReached(1, 10), true))
        )
        assertEquals(listOf(HomeIssue.DeviceCapReached), UiMappers.issues(gateway))
    }

    @Test
    fun simRouterModes() {
        val sims = listOf(
            SimCandidate(0, "+2348031234567", sentToday = 40),
            SimCandidate(1, "+2349061234567", sentToday = 5, inService = true)
        )
        assertEquals(0, SimRouter.select(SimSelectionMode.OSDefault, sims, 0, 1L, null, failover = false))
        assertNull(SimRouter.select(SimSelectionMode.OSDefault, sims, null, 1L, null, failover = false))
        assertEquals(1, SimRouter.select(SimSelectionMode.RoundRobin, sims, 0, 3L, null, failover = false))
        assertEquals(1, SimRouter.select(SimSelectionMode.LeastUsed, sims, 0, 1L, null, failover = false))
        assertEquals(1, SimRouter.select(SimSelectionMode.CarrierMatch, sims, 0, 1L, "+2349069999999", failover = false))
        assertEquals(0, SimRouter.select(SimSelectionMode.CarrierMatch, sims, 0, 1L, "+15550100", failover = false))
        assertTrue(SimRouter.select(SimSelectionMode.Random, sims, 0, 1L, null, failover = false, random = Random(1)) in setOf(0, 1))
    }

    @Test
    fun simRouterFailsOverFromSimWithoutService() {
        val sims = listOf(SimCandidate(0, null, 0, inService = false), SimCandidate(1, null, 9, inService = true))
        assertEquals(0, SimRouter.select(SimSelectionMode.OSDefault, sims, 0, 1L, null, failover = false))
        assertEquals(1, SimRouter.select(SimSelectionMode.OSDefault, sims, 0, 1L, null, failover = true))
        val noneInService = sims.map { it.copy(inService = false) }
        assertEquals(0, SimRouter.select(SimSelectionMode.OSDefault, noneInService, 0, 1L, null, failover = true))
    }

    @Test
    fun cloudPollPolicy() {
        assertEquals(15_000L, CloudPollPolicy.intervalMs(5))
        assertEquals(60_000L, CloudPollPolicy.intervalMs(60))
        assertTrue(CloudPollPolicy.needsAlarm(enabled = true, paired = true, pushAvailable = false))
        assertFalse(CloudPollPolicy.needsAlarm(enabled = true, paired = true, pushAvailable = true))
        assertFalse(CloudPollPolicy.needsAlarm(enabled = true, paired = false, pushAvailable = false))
        assertTrue(CloudPollPolicy.shouldPull(now = 100_000L, lastSuccessAt = null, intervalMs = 60_000L))
        assertFalse(CloudPollPolicy.shouldPull(now = 100_000L, lastSuccessAt = 90_000L, intervalMs = 60_000L))
        assertTrue(CloudPollPolicy.shouldPull(now = 200_000L, lastSuccessAt = 90_000L, intervalMs = 60_000L))
    }

    @Test
    fun permissionGroupsAndMakers() {
        assertEquals(emptyList<String>(), AppPermissions.forOnboarding(OnboardingPermission.Notifications, sdkInt = 32))
        assertEquals(
            listOf("android.permission.POST_NOTIFICATIONS"),
            AppPermissions.forOnboarding(OnboardingPermission.Notifications, sdkInt = 33)
        )
        assertEquals(OnboardingPermission.ReceiveSms, AppPermissions.ownerOf("android.permission.RECEIVE_MMS"))
        assertEquals(DeviceMaker.Xiaomi, AppPermissions.makerOf("Xiaomi", "Redmi"))
        assertEquals(DeviceMaker.TecnoInfinix, AppPermissions.makerOf("INFINIX MOBILITY LIMITED", "Infinix"))
        assertEquals(DeviceMaker.OppoRealme, AppPermissions.makerOf("OnePlus", "OnePlus"))
        assertEquals(DeviceMaker.Other, AppPermissions.makerOf("Google", "google"))
        assertTrue(SystemIntents.oemComponents(DeviceMaker.Other).isEmpty())
        assertTrue(SystemIntents.oemComponents(DeviceMaker.Huawei).isNotEmpty())
        assertTrue(SystemIntents.candidates(SystemAction.RequestPermissions(emptyList()), "pkg").isEmpty())
    }

    @Test
    fun cursorPagerLoadsAppendsAndReportsErrors() = runTest {
        val items = (1..5).map { message("msg_$it", createdAt = it.toLong()) }
        var fail = false
        val pager = CursorPager(CoroutineScope(StandardTestDispatcher(testScheduler)), pageSize = 2) { cursor, limit ->
            check(!fail)
            paged(items, cursor, limit) { it.createdAt to it.id }
        }
        pager.refresh()
        advanceUntilIdle()
        assertEquals(InitialLoad.Ready, pager.state.value.initial)
        assertEquals(listOf("msg_5", "msg_4"), pager.state.value.items.map { it.id })

        pager.loadMore()
        advanceUntilIdle()
        assertEquals(4, pager.state.value.items.size)

        fail = true
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(AppendState.Error, pager.state.value.append)
        assertEquals(4, pager.state.value.items.size)

        fail = false
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(AppendState.EndReached, pager.state.value.append)

        pager.refresh(keepLoaded = true)
        advanceUntilIdle()
        assertEquals(5, pager.state.value.items.size)

        fail = true
        pager.refresh()
        advanceUntilIdle()
        assertEquals(InitialLoad.Error, pager.state.value.initial)
    }
}
