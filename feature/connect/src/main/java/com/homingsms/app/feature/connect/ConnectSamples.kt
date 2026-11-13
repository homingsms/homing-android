package com.homingsms.app.feature.connect

import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS

internal object ConnectSamples {
    private const val HOUR = 3_600_000L

    private val keys = listOf(
        ApiKeyUi(
            "key_1",
            "Order service",
            "hk_live_4f9a",
            setOf(ApiScope.MessagesSend, ApiScope.MessagesRead),
            PREVIEW_NOW_MILLIS - 240 * HOUR,
            PREVIEW_NOW_MILLIS - 2 * HOUR
        ),
        ApiKeyUi(
            "key_2",
            "Support inbox",
            "hk_live_b21c",
            setOf(ApiScope.InboxRead, ApiScope.WebhooksManage),
            PREVIEW_NOW_MILLIS - 30 * HOUR,
            null
        )
    )

    val connected = ConnectUiState(
        local = LocalApiUi(enabled = true, running = true, apiBaseUrl = "http://192.168.1.20:8080/api/v1"),
        keys = keys,
        cloud = CloudPairing.Paired(
            deviceName = "Pixel 8 front desk",
            accountName = "Acme Clinics",
            pairedAtMillis = PREVIEW_NOW_MILLIS - 72 * HOUR,
            serverUrl = "https://api.homingsms.com"
        )
    )

    val notPaired = ConnectUiState(
        local = LocalApiUi(enabled = false),
        cloud = CloudPairing.Failed(PairingError.Expired),
        pairingCode = "K7P-4QX"
    )

    val createKey = connected.copy(createKey = CreateKeyDraft(name = "Billing"))

    val secretOnce = connected.copy(
        createdSecret = CreatedKeySecret("key_3", "Billing", "hk_live_9c2e1f7a4b8d6e3c5a0b9f1d")
    )
}
