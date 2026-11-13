package com.homingsms.app.ui.webhooks

import androidx.lifecycle.viewModelScope
import com.homingsms.app.feature.webhooks.DeliverySummary
import com.homingsms.app.feature.webhooks.DeliveryUi
import com.homingsms.app.feature.webhooks.TestEventState
import com.homingsms.app.feature.webhooks.WebhookDetailActions
import com.homingsms.app.feature.webhooks.WebhookDetailUiState
import com.homingsms.app.feature.webhooks.WebhookEditorActions
import com.homingsms.app.feature.webhooks.WebhookEditorUiState
import com.homingsms.app.feature.webhooks.WebhookEndpointUi
import com.homingsms.app.feature.webhooks.WebhookNotice
import com.homingsms.app.feature.webhooks.WebhookUrlError
import com.homingsms.app.feature.webhooks.WebhooksActions
import com.homingsms.app.feature.webhooks.WebhooksLoadState
import com.homingsms.app.feature.webhooks.WebhooksUiState
import com.homingsms.app.ui.CursorPager
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.InitialLoad
import com.homingsms.app.ui.UiMappers
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.data.WebhookStore
import com.homingsms.app.ui.messages.toggle
import com.homingsms.app.ui.navigation.WebhookDetailRoute
import com.homingsms.app.ui.navigation.WebhookEditorRoute
import com.homingsms.app.ui.navigation.WebhooksRoute
import com.homingsms.core.model.Page
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEndpoint
import java.net.URI
import java.net.URISyntaxException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object WebhookMapping {
    private val loopbackHosts = setOf("127.0.0.1", "localhost", "[::1]", "::1")

    fun validateUrl(value: String, allowCleartext: Boolean): WebhookUrlError? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return WebhookUrlError.Empty
        val uri = try {
            URI(trimmed)
        } catch (_: URISyntaxException) {
            return WebhookUrlError.Invalid
        }
        val host = uri.host
        return when {
            host.isNullOrBlank() -> WebhookUrlError.Invalid
            uri.scheme.equals("https", ignoreCase = true) -> null
            uri.scheme.equals("http", ignoreCase = true) -> if (allowCleartext || host in loopbackHosts) null else WebhookUrlError.NotHttps
            else -> WebhookUrlError.Invalid
        }
    }

    fun endpoint(endpoint: WebhookEndpoint, last: WebhookDelivery?) = WebhookEndpointUi(
        id = endpoint.id,
        url = endpoint.url,
        events = endpoint.events.mapNotNull(UiMappers::event).toSet(),
        enabled = endpoint.enabled,
        lastDelivery = last?.let { DeliverySummary(UiMappers.delivery(it.status), it.lastStatusCode, it.updatedAt) }
    )

    fun delivery(delivery: WebhookDelivery, retrying: Boolean) = DeliveryUi(
        id = delivery.id,
        eventType = delivery.eventType.value,
        status = UiMappers.delivery(delivery.status),
        atMillis = delivery.updatedAt,
        attempt = delivery.attempts.coerceAtLeast(1),
        statusCode = delivery.lastStatusCode,
        requestBody = delivery.payload,
        responseBody = delivery.lastResponseBody,
        errorMessage = delivery.lastError,
        nextRetryAtMillis = delivery.nextAttemptAt.takeIf { delivery.status == WebhookDeliveryStatus.Retrying },
        isRetrying = retrying
    )

    fun testState(delivery: WebhookDelivery): TestEventState = when (delivery.status) {
        WebhookDeliveryStatus.Pending -> TestEventState.Sending
        WebhookDeliveryStatus.Succeeded -> TestEventState.Delivered(delivery.lastStatusCode ?: 0)
        WebhookDeliveryStatus.Retrying, WebhookDeliveryStatus.Failed -> TestEventState.Failed(delivery.lastStatusCode)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WebhooksViewModel(private val store: WebhookStore) : EffectViewModel() {
    private val reload = MutableStateFlow(0)

    val state: StateFlow<WebhooksUiState> = reload.flatMapLatest {
        store.endpoints
            .flatMapLatest { endpoints -> withLastDelivery(endpoints) }
            .map { WebhooksUiState(WebhooksLoadState.Ready, it) }
            .catch { emit(WebhooksUiState(WebhooksLoadState.Error)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WebhooksUiState())

    val actions = WebhooksActions(
        onBack = ::back,
        onAdd = { navigate(WebhookEditorRoute()) },
        onEndpointClick = { navigate(WebhookDetailRoute(it)) },
        onEnabledChange = { id, enabled -> viewModelScope.launch { attempt { store.update(id, enabled = enabled) } } },
        onRetryLoad = { reload.update { it + 1 } }
    )

    private fun withLastDelivery(endpoints: List<WebhookEndpoint>): Flow<List<WebhookEndpointUi>> {
        if (endpoints.isEmpty()) return flowOf(emptyList())
        val rows = endpoints.map { endpoint ->
            store.recentDeliveries(endpoint.id, 1).map { WebhookMapping.endpoint(endpoint, it.firstOrNull()) }
        }
        return combine(rows) { it.toList() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

class WebhookEditorViewModel(private val id: String?, private val store: WebhookStore, private val control: GatewayControl) :
    EffectViewModel() {
    private val mutable = MutableStateFlow(WebhookEditorUiState(isNew = id == null))

    val state: StateFlow<WebhookEditorUiState> = mutable.asStateFlow()

    val actions = WebhookEditorActions(
        onBack = ::back,
        onUrlChange = { url -> mutable.update { it.copy(url = url, urlError = null) } },
        onToggleEvent = { event -> mutable.update { it.copy(events = it.events.toggle(event)) } },
        onEnabledChange = { enabled -> mutable.update { it.copy(enabled = enabled) } },
        onSave = ::save,
        onDelete = ::delete
    )

    init {
        if (id != null) {
            viewModelScope.launch {
                attempt { store.get(id) }.getOrNull()?.let { endpoint ->
                    mutable.update {
                        it.copy(
                            url = endpoint.url,
                            events = endpoint.events.mapNotNull(UiMappers::event).toSet(),
                            enabled = endpoint.enabled
                        )
                    }
                }
            }
        }
    }

    private fun save() {
        val current = mutable.value
        if (!current.canSave) return
        val error = WebhookMapping.validateUrl(current.url, control.allowCleartext)
        if (error != null) {
            mutable.update { it.copy(urlError = error) }
            return
        }
        mutable.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val events = current.events.map(UiMappers::event).toSet()
            val url = current.url.trim()
            val result = attempt {
                if (id == null) store.create(url, events, current.enabled) else store.update(id, url, events, current.enabled)
            }.getOrNull()
            mutable.update { it.copy(isSaving = false) }
            when {
                result == null -> mutable.update { it.copy(urlError = WebhookUrlError.Invalid) }

                id == null -> {
                    backTo(WebhooksRoute)
                    navigate(WebhookDetailRoute(result.id))
                }

                else -> back()
            }
        }
    }

    private fun delete() {
        val target = id ?: return back()
        mutable.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            attempt { store.delete(target) }
            mutable.update { it.copy(isDeleting = false) }
            backTo(WebhooksRoute)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WebhookDetailViewModel(private val id: String, private val store: WebhookStore) : EffectViewModel() {
    private data class Local(
        val secret: String? = null,
        val testDeliveryId: String? = null,
        val testFallback: TestEventState = TestEventState.Idle,
        val rotating: Boolean = false,
        val retrying: Set<String> = emptySet(),
        val notice: WebhookNotice? = null
    )

    private val local = MutableStateFlow(Local())
    private val reload = MutableStateFlow(0)
    private var testJob: Job? = null
    private val pager = CursorPager(viewModelScope) { cursor, limit -> store.deliveries(id, cursor, limit) ?: Page(emptyList(), null) }

    private val endpoint = reload.flatMapLatest {
        store.endpoints.map { list -> list.firstOrNull { it.id == id } }
    }
    private val recent = store.recentDeliveries(id, RECENT_LIMIT)

    val state: StateFlow<WebhookDetailUiState> = combine(endpoint, recent, pager.state, local) { found, latest, page, extra ->
        val loadState = when {
            found == null && page.initial == InitialLoad.Ready -> WebhooksLoadState.Error
            page.initial == InitialLoad.Error -> WebhooksLoadState.Error
            found == null || page.initial == InitialLoad.Loading -> WebhooksLoadState.Loading
            else -> WebhooksLoadState.Ready
        }
        val test = extra.testDeliveryId?.let { testId -> latest.firstOrNull { it.id == testId } }
        WebhookDetailUiState(
            loadState = loadState,
            endpoint = found?.let { WebhookMapping.endpoint(it, latest.firstOrNull()) },
            signingSecret = extra.secret,
            deliveries = page.items.map { WebhookMapping.delivery(it, it.id in extra.retrying) },
            deliveriesAppendState = page.append,
            testState = test?.let(WebhookMapping::testState) ?: extra.testFallback,
            isRotatingSecret = extra.rotating,
            notice = extra.notice
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WebhookDetailUiState())

    val actions = WebhookDetailActions(
        onBack = ::back,
        onEdit = { navigate(WebhookEditorRoute(id)) },
        onSendTest = ::sendTest,
        onRotateSecret = ::rotate,
        onRetryDelivery = ::redeliver,
        onLoadMoreDeliveries = pager::loadMore,
        onRetryLoad = {
            reload.update { it + 1 }
            pager.refresh()
        },
        onNoticeShown = { local.update { it.copy(notice = null) } }
    )

    init {
        pager.refresh()
        recent.drop(1).onEach { pager.refresh(keepLoaded = true) }.launchIn(viewModelScope)
        viewModelScope.launch {
            val secret = attempt { store.secret(id) }.getOrNull()
            local.update { it.copy(secret = secret) }
        }
    }

    private fun sendTest() {
        if (testJob?.isActive == true) return
        local.update { it.copy(testDeliveryId = null, testFallback = TestEventState.Sending) }
        testJob = viewModelScope.launch {
            val delivery = attempt { store.sendTest(id) }.getOrNull()
            if (delivery == null) {
                local.update { it.copy(testFallback = TestEventState.Failed(null), notice = WebhookNotice.ActionFailed) }
                return@launch
            }
            local.update { it.copy(testDeliveryId = delivery.id) }
        }
    }

    private fun rotate() {
        if (local.value.rotating) return
        local.update { it.copy(rotating = true) }
        viewModelScope.launch {
            val secret = attempt { store.rotateSecret(id) }.getOrNull()
            local.update {
                if (secret == null) {
                    it.copy(rotating = false, notice = WebhookNotice.ActionFailed)
                } else {
                    it.copy(rotating = false, secret = secret, notice = WebhookNotice.SecretRotated)
                }
            }
        }
    }

    private fun redeliver(deliveryId: String) {
        if (deliveryId in local.value.retrying) return
        local.update { it.copy(retrying = it.retrying + deliveryId) }
        viewModelScope.launch {
            val result = attempt { store.redeliver(id, deliveryId) }.getOrNull()
            local.update {
                it.copy(
                    retrying = it.retrying - deliveryId,
                    notice = if (result != null) WebhookNotice.RetryQueued else WebhookNotice.ActionFailed
                )
            }
            pager.refresh(keepLoaded = true)
        }
    }

    private companion object {
        const val RECENT_LIMIT = 50
    }
}
