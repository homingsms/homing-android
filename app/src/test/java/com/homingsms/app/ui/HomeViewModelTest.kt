package com.homingsms.app.ui

import android.Manifest
import app.cash.turbine.test
import com.homingsms.app.core.designsystem.component.GatewayHealth
import com.homingsms.app.core.designsystem.component.HourlyBar
import com.homingsms.app.feature.home.HomeIssue
import com.homingsms.app.feature.home.MissingPermission
import com.homingsms.app.feature.home.NetworkKind
import com.homingsms.app.ui.home.HomeViewModel
import com.homingsms.app.ui.navigation.ConnectRoute
import com.homingsms.app.ui.navigation.MessagesRoute
import com.homingsms.app.ui.navigation.SettingsRoute
import com.homingsms.app.ui.system.PermissionOutcome
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.BatteryInfo
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.DeviceHealth
import com.homingsms.core.model.GatewayState
import com.homingsms.core.model.GatewayStatus
import com.homingsms.core.model.HourlyBucket
import com.homingsms.core.model.LocalServerInfo
import com.homingsms.core.model.NetworkType
import com.homingsms.core.model.SimStats
import com.homingsms.core.model.StatusIssue
import com.homingsms.core.model.StatusReason
import com.homingsms.core.model.TodayCounts
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val status = FakeAppStatus()
    private val control = FakeGatewayControl(status)
    private val platform = FakePlatformState(batteryIgnored = true)

    private fun viewModel() = HomeViewModel(status, control, platform)

    @Test
    fun mapsGatewayDeviceAndActivity() = runTest {
        status.gateway.value = GatewayState(
            GatewayStatus.Degraded,
            listOf(
                StatusIssue(
                    StatusReason.PermissionsMissing(listOf(Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE)),
                    false
                ),
                StatusIssue(StatusReason.NoSignal(1), false),
                StatusIssue(StatusReason.CloudNotPaired, false)
            )
        )
        status.health.value = DeviceHealth(BatteryInfo(64, true, false), NetworkType.Wifi, emptyList(), "1.0", false, 0, 5L)
        status.today.value = TodayCounts(sent = 10, delivered = 7, failed = 2, received = 4, queued = 3)
        status.last24Hours.value = listOf(HourlyBucket(0L, sent = 5, failed = 1, received = 0, delivered = 3))
        status.sims.value = listOf(SimStats(0, "Carrier", "+1555", 3, 12, 100, true))
        status.localServerInfo.value = LocalServerInfo(true, true, 8080, listOf("192.168.1.2"), "dev", emptyList(), null)
        status.settings.value = status.settings.value.copy(localServer = status.settings.value.localServer.copy(enabled = true))
        status.cloudConnection.value = status.cloudConnection.value.copy(lastSuccessAt = 99L)

        val viewModel = viewModel()
        keepCollecting(viewModel.state)
        val state = viewModel.state.value

        assertEquals(GatewayHealth.Degraded, state.health)
        assertEquals(
            listOf(
                HomeIssue.PermissionMissing(MissingPermission.ReceiveSms),
                HomeIssue.PermissionMissing(MissingPermission.PhoneState),
                HomeIssue.SimNoSignal(1),
                HomeIssue.CloudNotPaired
            ),
            state.issues
        )
        assertEquals(10L, state.today?.sent)
        assertEquals(3L, state.today?.queued)
        assertEquals(listOf(HourlyBar(delivered = 3, sentWithoutReport = 2, failed = 1)), state.hourly)
        assertEquals(12, state.sims.single().sentToday)
        assertEquals(100, state.sims.single().dailyCap)
        assertEquals(NetworkKind.Wifi, state.device?.network)
        assertEquals(99L, state.device?.lastCloudContactMillis)
        assertEquals("http://192.168.1.2:8080/api/v1", state.localServer.address)
        assertTrue(state.localServer.running)
        assertFalse(state.cloud.paired)
        assertFalse(state.isLoading)
    }

    @Test
    fun addsBatteryAndSimIssuesThatAreNotStatusReasons() = runTest {
        platform.batteryIgnored = false
        status.sims.value = listOf(SimStats(0, null, null, null, 0, null, enabled = false))
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        assertTrue(HomeIssue.BatteryOptimized in viewModel.state.value.issues)
        assertTrue(HomeIssue.AllSimsDisabled in viewModel.state.value.issues)
        assertTrue(HomeIssue.ServiceStopped in viewModel.state.value.issues)
    }

    @Test
    fun cloudStatusFollowsPairingAndConnection() = runTest {
        status.cloudPairing.value = CloudPairingState.Paired("dev", "acc", "https://x.test", 1L)
        status.settings.value = status.settings.value.copy(cloud = status.settings.value.cloud.copy(enabled = true))
        status.cloudConnection.value = status.cloudConnection.value.copy(lastSuccessAt = 5L, consecutiveFailures = 0)
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        val cloud = viewModel.state.value.cloud
        assertTrue(cloud.enabled && cloud.paired && cloud.connected)
        assertEquals(5L, cloud.lastContactMillis)
    }

    @Test
    fun fixesNavigateOrLaunchSystemScreens() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.fix(HomeIssue.NotConfigured)
            assertEquals(UiEffect.Navigate(ConnectRoute), awaitItem())
            viewModel.fix(HomeIssue.BatteryOptimized)
            assertEquals(UiEffect.System(SystemAction.RequestBatteryExemption), awaitItem())
            viewModel.fix(HomeIssue.SimCapReached(0))
            assertEquals(UiEffect.Navigate(SettingsRoute), awaitItem())
            viewModel.fix(HomeIssue.AirplaneMode)
            assertEquals(UiEffect.System(SystemAction.OpenNetworkSettings), awaitItem())
            viewModel.fix(HomeIssue.BatterySaver)
            assertEquals(UiEffect.System(SystemAction.OpenBatterySaverSettings), awaitItem())
            viewModel.fix(HomeIssue.AllSimsDisabled)
            assertEquals(UiEffect.System(SystemAction.OpenSimSettings), awaitItem())
            viewModel.actions.onOpenMessages()
            assertEquals(UiEffect.Navigate(MessagesRoute), awaitItem())
        }
    }

    @Test
    fun permissionFixRequestsMissingPermissions() = runTest {
        status.gateway.value = GatewayState(
            GatewayStatus.Offline,
            listOf(StatusIssue(StatusReason.PermissionsMissing(listOf(Manifest.permission.SEND_SMS)), true))
        )
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.fix(HomeIssue.PermissionMissing(MissingPermission.SendSms))
            assertEquals(UiEffect.System(SystemAction.RequestPermissions(listOf(Manifest.permission.SEND_SMS))), awaitItem())
            viewModel.onPermissionsResult(listOf(PermissionOutcome(Manifest.permission.SEND_SMS, granted = false, canAskAgain = false)))
            assertEquals(UiEffect.System(SystemAction.OpenAppSettings), awaitItem())
        }
    }

    @Test
    fun restartStartsServiceAndRetryPingsCloud() = runTest {
        platform.granted += listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        status.serviceRunning.value = false
        val viewModel = viewModel()

        viewModel.fix(HomeIssue.ServiceStopped)
        viewModel.fix(HomeIssue.CloudUnreachable)
        viewModel.actions.onLocalServerToggle(true)
        viewModel.actions.onCloudToggle(false)

        assertEquals(listOf("start", "retryCloud", "local:true", "cloud:false"), control.calls)
        assertTrue(status.refreshes > 0)
    }
}
