package com.homingsms.app.ui.connect

import com.homingsms.app.feature.connect.PairingActions
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.PairingFailure
import com.homingsms.core.model.PairingLink
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairLinkInbox {
    private val pending = MutableStateFlow<String?>(null)

    val links: StateFlow<String?> = pending.asStateFlow()

    fun offer(link: String) {
        pending.value = link
    }

    fun consume(): String? = pending.value.also { pending.value = null }
}

data class PendingPairLink(val link: String, val serverHost: String, val code: String)

class PairingCoordinator(
    private val scope: CoroutineScope,
    private val control: GatewayControl,
    private val onResult: (CloudPairingState) -> Unit,
    private val onUnpaired: () -> Unit = {}
) {
    private val code = MutableStateFlow("")
    private val pending = MutableStateFlow<PendingPairLink?>(null)

    val pairingCode: StateFlow<String> = code.asStateFlow()
    val pendingLink: StateFlow<PendingPairLink?> = pending.asStateFlow()

    val actions = PairingActions(
        onPairingCodeChange = { code.value = it.take(MAX_CODE) },
        onSubmitPairingCode = ::submitCode,
        onPairingScanned = ::scanned,
        onUnpair = ::unpair
    )

    fun offerLink(link: String): Boolean {
        val parsed = PairingLink.parse(link, control.allowCleartext) ?: run {
            onResult(CloudPairingState.Failed(PairingFailure.InvalidLink, null))
            return false
        }
        code.value = parsed.code
        pending.value = PendingPairLink(link, hostOf(parsed.serverUrl), parsed.code)
        return true
    }

    fun confirmLink() {
        val link = pending.value ?: return
        pending.value = null
        pair { control.pairWithLink(link.link) }
    }

    fun dismissLink() {
        pending.value = null
    }

    private fun submitCode() {
        val value = code.value.trim()
        if (value.isEmpty()) return
        pair { control.pair(value, control.defaultCloudUrl) }
    }

    private fun scanned(text: String) {
        val value = text.trim()
        val link = PairingLink.parse(value, control.allowCleartext)
        when {
            link != null -> {
                code.value = link.code
                pair { control.pairWithLink(value) }
            }

            PairingLink.isValidCode(value) -> {
                code.value = value.uppercase()
                submitCode()
            }

            else -> onResult(CloudPairingState.Failed(PairingFailure.InvalidLink, null))
        }
    }

    private fun unpair() {
        scope.launch {
            if (attempt { control.unpair() }.isSuccess) {
                code.value = ""
                onUnpaired()
            }
        }
    }

    private fun pair(block: suspend () -> CloudPairingState) {
        scope.launch {
            val result = attempt { block() }.getOrElse { CloudPairingState.Failed(PairingFailure.Network, it.message) }
            if (result is CloudPairingState.Paired) code.value = ""
            onResult(result)
        }
    }

    private companion object {
        const val MAX_CODE = 16

        fun hostOf(serverUrl: String): String = runCatching { URI(serverUrl).host }.getOrNull() ?: serverUrl
    }
}
