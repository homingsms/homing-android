package com.homingsms.app.ui

import app.cash.turbine.test
import com.homingsms.app.feature.webhooks.DeliveryStatus
import com.homingsms.app.feature.webhooks.TestEventState
import com.homingsms.app.feature.webhooks.WebhookEvent
import com.homingsms.app.feature.webhooks.WebhookNotice
import com.homingsms.app.feature.webhooks.WebhookUrlError
import com.homingsms.app.feature.webhooks.WebhooksLoadState
import com.homingsms.app.ui.navigation.WebhookDetailRoute
import com.homingsms.app.ui.navigation.WebhookEditorRoute
import com.homingsms.app.ui.navigation.WebhooksRoute
import com.homingsms.app.ui.webhooks.WebhookDetailViewModel
import com.homingsms.app.ui.webhooks.WebhookEditorViewModel
import com.homingsms.app.ui.webhooks.WebhookMapping
import com.homingsms.app.ui.webhooks.WebhooksViewModel
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEventType
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WebhooksViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val status = FakeAppStatus()
    private val control = FakeGatewayControl(status)
    private val store = FakeWebhookStore()

    @Test
    fun listShowsEndpointsWithLastDelivery() = runTest {
        store.endpointList.value = listOf(endpoint("whk_1", events = setOf(WebhookEventType.MessageSent, WebhookEventType.InboxReceived)))
        store.deliveryList.value = listOf(
            delivery("whd_old", "whk_1", WebhookDeliveryStatus.Failed, createdAt = 1L, code = 500),
            delivery("whd_new", "whk_1", WebhookDeliveryStatus.Succeeded, createdAt = 2L, code = 204)
        )
        val viewModel = WebhooksViewModel(store)
        keepCollecting(viewModel.state)

        val state = viewModel.state.value
        assertEquals(WebhooksLoadState.Ready, state.loadState)
        val endpoint = state.endpoints.single()
        assertEquals(setOf(WebhookEvent.MessageSent, WebhookEvent.InboxReceived), endpoint.events)
        assertEquals(DeliveryStatus.Succeeded, endpoint.lastDelivery?.status)
        assertEquals(204, endpoint.lastDelivery?.statusCode)

        viewModel.actions.onEnabledChange("whk_1", false)
        assertFalse(viewModel.state.value.endpoints.single().enabled)
    }

    @Test
    fun listNavigation() = runTest {
        val viewModel = WebhooksViewModel(store)
        viewModel.effects.test {
            viewModel.actions.onAdd()
            assertEquals(UiEffect.Navigate(WebhookEditorRoute()), awaitItem())
            viewModel.actions.onEndpointClick("whk_1")
            assertEquals(UiEffect.Navigate(WebhookDetailRoute("whk_1")), awaitItem())
        }
    }

    @Test
    fun urlValidation() {
        assertEquals(WebhookUrlError.Empty, WebhookMapping.validateUrl(" ", allowCleartext = false))
        assertEquals(WebhookUrlError.Invalid, WebhookMapping.validateUrl("not a url", allowCleartext = false))
        assertEquals(WebhookUrlError.Invalid, WebhookMapping.validateUrl("ftp://host.test/x", allowCleartext = false))
        assertEquals(WebhookUrlError.NotHttps, WebhookMapping.validateUrl("http://hooks.example.test", allowCleartext = false))
        assertNull(WebhookMapping.validateUrl("http://hooks.example.test", allowCleartext = true))
        assertNull(WebhookMapping.validateUrl("http://127.0.0.1:9000/hook", allowCleartext = false))
        assertNull(WebhookMapping.validateUrl("https://hooks.example.test/a", allowCleartext = false))
    }

    @Test
    fun editorCreatesAndOpensDetail() = runTest {
        val viewModel = WebhookEditorViewModel(null, store, control)
        viewModel.effects.test {
            viewModel.actions.onUrlChange("http://hooks.example.test")
            viewModel.actions.onToggleEvent(WebhookEvent.InboxReceived)
            viewModel.actions.onSave()
            assertEquals(WebhookUrlError.NotHttps, viewModel.state.value.urlError)

            viewModel.actions.onUrlChange("https://hooks.example.test")
            assertNull(viewModel.state.value.urlError)
            viewModel.actions.onSave()
            assertEquals(UiEffect.BackTo(WebhooksRoute), awaitItem())
            assertEquals(UiEffect.Navigate(WebhookDetailRoute("whk_new")), awaitItem())
        }
        assertEquals(setOf(WebhookEventType.InboxReceived), store.endpointList.value.single().events)
    }

    @Test
    fun editorLoadsUpdatesAndDeletes() = runTest {
        store.endpointList.value = listOf(endpoint("whk_1"))
        val viewModel = WebhookEditorViewModel("whk_1", store, control)
        assertFalse(viewModel.state.value.isNew)
        assertEquals("https://hooks.example.test/a", viewModel.state.value.url)
        assertEquals(setOf(WebhookEvent.MessageSent), viewModel.state.value.events)

        viewModel.effects.test {
            viewModel.actions.onEnabledChange(false)
            viewModel.actions.onSave()
            assertEquals(UiEffect.Back, awaitItem())
            viewModel.actions.onDelete()
            assertEquals(UiEffect.BackTo(WebhooksRoute), awaitItem())
        }
        assertEquals(listOf("update:whk_1:https://hooks.example.test/a:false", "delete:whk_1"), store.calls)
    }

    @Test
    fun detailLoadsSecretAndDeliveries() = runTest {
        store.endpointList.value = listOf(endpoint("whk_1"))
        store.deliveryList.value = listOf(delivery("whd_1", "whk_1", WebhookDeliveryStatus.Retrying, code = 502))
        val viewModel = WebhookDetailViewModel("whk_1", store)

        val state = viewModel.state.value
        assertEquals(WebhooksLoadState.Ready, state.loadState)
        assertEquals("whsec_first", state.signingSecret)
        val row = state.deliveries.single()
        assertEquals(DeliveryStatus.Retrying, row.status)
        assertEquals(502, row.statusCode)
        assertEquals("{\"id\":\"evt_whd_1\"}", row.requestBody)
    }

    @Test
    fun detailTestEventFollowsDelivery() = runTest {
        store.endpointList.value = listOf(endpoint("whk_1"))
        val viewModel = WebhookDetailViewModel("whk_1", store)

        viewModel.actions.onSendTest()
        assertEquals(TestEventState.Sending, viewModel.state.value.testState)

        store.deliveryList.update { list ->
            list.map { if (it.id == "whd_test") it.copy(status = WebhookDeliveryStatus.Succeeded, lastStatusCode = 200) else it }
        }
        assertEquals(TestEventState.Delivered(200), viewModel.state.value.testState)
        assertEquals(listOf("test:whk_1"), store.calls)
    }

    @Test
    fun detailRotatesSecretAndRedelivers() = runTest {
        store.endpointList.value = listOf(endpoint("whk_1"))
        store.deliveryList.value = listOf(delivery("whd_1", "whk_1", WebhookDeliveryStatus.Failed))
        val viewModel = WebhookDetailViewModel("whk_1", store)

        viewModel.actions.onRotateSecret()
        assertEquals("whsec_rotated", viewModel.state.value.signingSecret)
        assertEquals(WebhookNotice.SecretRotated, viewModel.state.value.notice)
        viewModel.actions.onNoticeShown()

        viewModel.actions.onRetryDelivery("whd_1")
        assertEquals(WebhookNotice.RetryQueued, viewModel.state.value.notice)
        assertTrue("redeliver:whd_1" in store.calls)
        assertFalse(viewModel.state.value.deliveries.single().isRetrying)
    }

    @Test
    fun detailMissingEndpointIsAnError() = runTest {
        val viewModel = WebhookDetailViewModel("missing", store)
        assertEquals(WebhooksLoadState.Error, viewModel.state.value.loadState)
    }
}
