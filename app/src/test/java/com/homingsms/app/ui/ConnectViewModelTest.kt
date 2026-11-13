package com.homingsms.app.ui

import app.cash.turbine.test
import com.homingsms.app.feature.connect.ApiScope
import com.homingsms.app.feature.connect.CloudPairing
import com.homingsms.app.feature.connect.ConnectNotice
import com.homingsms.app.feature.connect.PairingError
import com.homingsms.app.ui.connect.ConnectViewModel
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.navigation.WebhooksRoute
import com.homingsms.core.model.ApiKeyInfo
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.LocalServerInfo
import com.homingsms.core.model.PairingFailure
import com.homingsms.core.model.Scope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConnectViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val status = FakeAppStatus()
    private val control = FakeGatewayControl(status)
    private val links = PairLinkInbox()

    private fun viewModel() = ConnectViewModel(status, control, links)

    @Test
    fun mapsLocalServerKeysAndPairing() {
        status.localServerInfo.value = LocalServerInfo(
            enabled = true,
            running = true,
            port = 8080,
            addresses = emptyList(),
            deviceId = "dev",
            keys = listOf(ApiKeyInfo("key_1", "CI", "hk_live_ab", setOf(Scope.MessagesSend, Scope.Admin), 5L, 6L)),
            lastError = null
        )
        status.settings.value = status.settings.value.copy(cloud = status.settings.value.cloud.copy(deviceName = "Pixel"))
        status.cloudPairing.value = CloudPairingState.Paired("dev_1", "acc_1", "https://api.example.test", 7L)

        val state = viewModel().state.value

        assertEquals("http://127.0.0.1:8080/api/v1", state.local.apiBaseUrl)
        assertTrue(state.local.running)
        assertEquals(setOf(ApiScope.MessagesSend, ApiScope.Admin), state.keys.single().scopes)
        assertEquals(CloudPairing.Paired("Pixel", "acc_1", 7L, "https://api.example.test"), state.cloud)
    }

    @Test
    fun failedPairingMapsToTypedError() {
        status.cloudPairing.value = CloudPairingState.Failed(PairingFailure.Network, "offline")
        assertEquals(CloudPairing.Failed(PairingError.Network), viewModel().state.value.cloud)
    }

    @Test
    fun apiKeySecretIsShownOnceThenCleared() {
        val viewModel = viewModel()
        viewModel.actions.onCreateKeyClick()
        viewModel.actions.onCreateKeyNameChange("Server")
        viewModel.actions.onCreateKeyScopeToggle(ApiScope.MessagesRead)
        viewModel.actions.onCreateKeyScopeToggle(ApiScope.InboxRead)
        viewModel.actions.onCreateKeyConfirm()

        val created = viewModel.state.value
        assertNull(created.createKey)
        assertEquals("hk_live_secret1", created.createdSecret?.secret)
        assertEquals(listOf("createKey:Server:messages:send,inbox:read"), control.calls)
        assertEquals(1, created.keys.size)

        viewModel.actions.onSecretSaved()
        assertNull(viewModel.state.value.createdSecret)

        viewModel.actions.onRevokeKey("key_1")
        assertEquals(ConnectNotice.KeyRevoked, viewModel.state.value.notice)
        assertTrue(viewModel.state.value.keys.isEmpty())
    }

    @Test
    fun pairingCodeUsesDefaultServer() {
        val viewModel = viewModel()
        viewModel.actions.pairing.onPairingCodeChange("ab12cd")
        viewModel.actions.pairing.onSubmitPairingCode()

        assertEquals(listOf("pair:ab12cd:https://api.example.test"), control.calls)
        assertEquals(ConnectNotice.Paired, viewModel.state.value.notice)
        assertEquals("", viewModel.state.value.pairingCode)
    }

    @Test
    fun deepLinkPrefillsCodeAndWaitsForConfirmation() {
        val link = "homing://pair?server=https%3A%2F%2Fcloud.example.test&code=WXYZ12"
        links.offer(link)
        val viewModel = viewModel()

        val pending = viewModel.pendingLink.value
        assertEquals("cloud.example.test", pending?.serverHost)
        assertEquals("WXYZ12", viewModel.state.value.pairingCode)
        assertTrue(control.calls.isEmpty())
        assertNull(links.links.value)

        viewModel.confirmPairLink()
        assertEquals(listOf("pairLink:$link"), control.calls)
        assertNull(viewModel.pendingLink.value)
    }

    @Test
    fun scannedLinkPairsDirectlyAndUnpairNotifies() {
        val viewModel = viewModel()
        viewModel.actions.pairing.onPairingScanned("homing://pair?server=https://cloud.example.test&code=QR1234")
        assertEquals("pairLink:homing://pair?server=https://cloud.example.test&code=QR1234", control.calls.single())

        viewModel.actions.pairing.onUnpair()
        assertEquals(ConnectNotice.Unpaired, viewModel.state.value.notice)
        assertEquals(CloudPairing.NotPaired, viewModel.state.value.cloud)
    }

    @Test
    fun localToggleAndWebhooksNavigation() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.actions.onLocalServerToggle(true)
            viewModel.actions.onOpenWebhooks()
            assertEquals(UiEffect.Navigate(WebhooksRoute), awaitItem())
        }
        assertEquals(listOf("local:true"), control.calls)
        assertTrue(viewModel.state.value.local.enabled)
    }
}
