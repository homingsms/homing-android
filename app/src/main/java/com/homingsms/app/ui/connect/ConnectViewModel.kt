package com.homingsms.app.ui.connect

import androidx.lifecycle.viewModelScope
import com.homingsms.app.feature.connect.ApiKeyUi
import com.homingsms.app.feature.connect.ConnectActions
import com.homingsms.app.feature.connect.ConnectNotice
import com.homingsms.app.feature.connect.ConnectUiState
import com.homingsms.app.feature.connect.CreateKeyDraft
import com.homingsms.app.feature.connect.CreatedKeySecret
import com.homingsms.app.feature.connect.LocalApiUi
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.UiMappers
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.messages.toggle
import com.homingsms.app.ui.navigation.WebhooksRoute
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.LocalServerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConnectViewModel(private val status: AppStatus, private val control: GatewayControl, links: PairLinkInbox) : EffectViewModel() {
    private data class Local(val draft: CreateKeyDraft? = null, val secret: CreatedKeySecret? = null, val notice: ConnectNotice? = null)

    private val local = MutableStateFlow(Local())

    private val pairing = PairingCoordinator(
        scope = viewModelScope,
        control = control,
        onResult = ::onPairingResult,
        onUnpaired = { notice(ConnectNotice.Unpaired) }
    )

    val pendingLink: StateFlow<PendingPairLink?> = pairing.pendingLink

    val state: StateFlow<ConnectUiState> = combine(
        status.settings,
        status.localServerInfo,
        status.cloudPairing,
        pairing.pairingCode,
        local
    ) { settings, info, cloud, code, extra ->
        ConnectUiState(
            local = LocalApiUi(enabled = settings.localServer.enabled, running = info.running, apiBaseUrl = baseUrl(info)),
            keys = info.keys.map { key ->
                ApiKeyUi(key.id, key.name, key.prefix, key.scopes.map(UiMappers::scope).toSet(), key.createdAt, key.lastUsedAt)
            },
            createKey = extra.draft,
            createdSecret = extra.secret,
            cloud = UiMappers.pairing(cloud, settings.cloud.deviceName),
            pairingCode = code,
            notice = extra.notice
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConnectUiState())

    val actions = ConnectActions(
        onLocalServerToggle = { enabled -> launchOrFail { control.setLocalEnabled(enabled) } },
        onCreateKeyClick = { local.update { it.copy(draft = CreateKeyDraft()) } },
        onCreateKeyNameChange = { name -> updateDraft { it.copy(name = name) } },
        onCreateKeyScopeToggle = { scope -> updateDraft { it.copy(scopes = it.scopes.toggle(scope)) } },
        onCreateKeyConfirm = ::createKey,
        onCreateKeyDismiss = { local.update { it.copy(draft = null) } },
        onSecretSaved = { local.update { it.copy(secret = null) } },
        onRevokeKey = { id -> launchOrFail { if (control.revokeApiKey(id)) notice(ConnectNotice.KeyRevoked) } },
        onOpenWebhooks = { navigate(WebhooksRoute) },
        onNoticeShown = { local.update { it.copy(notice = null) } },
        pairing = pairing.actions
    )

    init {
        links.links.filterNotNull().onEach { links.consume()?.let(pairing::offerLink) }.launchIn(viewModelScope)
    }

    fun confirmPairLink() = pairing.confirmLink()

    fun dismissPairLink() = pairing.dismissLink()

    private fun createKey() {
        val draft = local.value.draft?.takeIf { it.canCreate } ?: return
        local.update { it.copy(draft = draft.copy(isCreating = true)) }
        viewModelScope.launch {
            attempt { control.createApiKey(draft.name.trim(), draft.scopes.map(UiMappers::scope).toSet()) }
                .onSuccess { created ->
                    local.update { it.copy(draft = null, secret = CreatedKeySecret(created.info.id, created.info.name, created.secret)) }
                }
                .onFailure { local.update { it.copy(draft = draft.copy(isCreating = false), notice = ConnectNotice.ActionFailed) } }
        }
    }

    private fun updateDraft(transform: (CreateKeyDraft) -> CreateKeyDraft) {
        local.update { current -> current.draft?.let { current.copy(draft = transform(it)) } ?: current }
    }

    private fun onPairingResult(result: CloudPairingState) {
        when {
            result is CloudPairingState.Paired -> notice(ConnectNotice.Paired)

            result is CloudPairingState.Failed && status.cloudPairing.value !is CloudPairingState.Failed -> notice(
                ConnectNotice.ActionFailed
            )
        }
    }

    private fun notice(value: ConnectNotice) = local.update { it.copy(notice = value) }

    private fun launchOrFail(block: suspend () -> Unit) {
        viewModelScope.launch { attempt { block() }.onFailure { notice(ConnectNotice.ActionFailed) } }
    }

    companion object {
        fun baseUrl(info: LocalServerInfo): String? =
            info.baseUrls.firstOrNull() ?: if (info.running) "http://127.0.0.1:${info.port}/api/v1" else null
    }
}
