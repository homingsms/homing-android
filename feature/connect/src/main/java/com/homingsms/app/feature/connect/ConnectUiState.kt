package com.homingsms.app.feature.connect

import androidx.compose.runtime.Immutable

enum class ApiScope(val wireName: String) {
    MessagesSend("messages:send"),
    MessagesRead("messages:read"),
    InboxRead("inbox:read"),
    WebhooksManage("webhooks:manage"),
    DevicesManage("devices:manage"),
    Admin("admin")
}

@Immutable
data class ApiKeyUi(
    val id: String,
    val name: String,
    val prefix: String,
    val scopes: Set<ApiScope>,
    val createdAtMillis: Long,
    val lastUsedAtMillis: Long? = null
)

@Immutable
data class LocalApiUi(val enabled: Boolean = false, val running: Boolean = false, val apiBaseUrl: String? = null)

@Immutable
data class CreateKeyDraft(
    val name: String = "",
    val scopes: Set<ApiScope> = setOf(ApiScope.MessagesSend, ApiScope.MessagesRead),
    val isCreating: Boolean = false
) {
    val canCreate: Boolean get() = name.isNotBlank() && scopes.isNotEmpty() && !isCreating
}

@Immutable
data class CreatedKeySecret(val keyId: String, val name: String, val secret: String)

enum class PairingError { InvalidCode, Expired, Network, Unknown }

sealed interface CloudPairing {
    data object NotPaired : CloudPairing
    data object Pairing : CloudPairing
    data class Failed(val error: PairingError) : CloudPairing
    data class Paired(val deviceName: String, val accountName: String?, val pairedAtMillis: Long, val serverUrl: String) : CloudPairing
}

enum class ConnectNotice { KeyRevoked, Unpaired, Paired, ActionFailed }

@Immutable
data class ConnectUiState(
    val local: LocalApiUi = LocalApiUi(),
    val keys: List<ApiKeyUi> = emptyList(),
    val keysLoading: Boolean = false,
    val createKey: CreateKeyDraft? = null,
    val createdSecret: CreatedKeySecret? = null,
    val cloud: CloudPairing = CloudPairing.NotPaired,
    val pairingCode: String = "",
    val notice: ConnectNotice? = null
)

@Immutable
data class PairingActions(
    val onPairingCodeChange: (String) -> Unit = {},
    val onSubmitPairingCode: () -> Unit = {},
    val onPairingScanned: (String) -> Unit = {},
    val onUnpair: () -> Unit = {}
)

@Immutable
data class ConnectActions(
    val onLocalServerToggle: (Boolean) -> Unit = {},
    val onCreateKeyClick: () -> Unit = {},
    val onCreateKeyNameChange: (String) -> Unit = {},
    val onCreateKeyScopeToggle: (ApiScope) -> Unit = {},
    val onCreateKeyConfirm: () -> Unit = {},
    val onCreateKeyDismiss: () -> Unit = {},
    val onSecretSaved: () -> Unit = {},
    val onRevokeKey: (String) -> Unit = {},
    val onOpenWebhooks: () -> Unit = {},
    val onNoticeShown: () -> Unit = {},
    val pairing: PairingActions = PairingActions()
)

fun curlSnippet(apiBaseUrl: String): String =
    """
    curl -X POST ${apiBaseUrl.trimEnd('/')}/messages \
      -H "Authorization: Bearer ${'$'}HOMING_API_KEY" \
      -H "Content-Type: application/json" \
      -d '{"to":"+15550100","type":"sms","text":"Hello from Homing"}'
    """.trimIndent()
