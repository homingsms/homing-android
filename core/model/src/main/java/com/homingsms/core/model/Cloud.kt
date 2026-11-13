package com.homingsms.core.model

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

sealed interface CloudPairingState {
    data object Unpaired : CloudPairingState

    data class Pairing(val serverUrl: String) : CloudPairingState

    data class Paired(val deviceId: String, val accountId: String, val serverUrl: String, val pairedAt: Long) : CloudPairingState

    data class Failed(val reason: PairingFailure, val message: String?) : CloudPairingState

    data class Removed(val at: Long) : CloudPairingState
}

enum class PairingFailure {
    InvalidCode,
    InvalidLink,
    Network,
    RateLimited,
    Server
}

data class CloudConnectionState(
    val enabled: Boolean,
    val lastHeartbeatAt: Long?,
    val lastSuccessAt: Long?,
    val lastFailureAt: Long?,
    val lastError: String?,
    val consecutiveFailures: Int,
    val pushAvailable: Boolean,
    val pendingStateReports: Int,
    val pendingInboxUploads: Int,
    val remotePaused: Boolean
) {
    val reachable: Boolean
        get() = consecutiveFailures == 0

    companion object {
        val Idle = CloudConnectionState(
            enabled = false,
            lastHeartbeatAt = null,
            lastSuccessAt = null,
            lastFailureAt = null,
            lastError = null,
            consecutiveFailures = 0,
            pushAvailable = false,
            pendingStateReports = 0,
            pendingInboxUploads = 0,
            remotePaused = false
        )
    }
}

data class PairingLink(val serverUrl: String, val code: String) {
    companion object {
        const val SCHEME = "homing"
        const val HOST = "pair"
        private val codePattern = Regex("^[A-Za-z0-9-]{4,16}$")

        fun parse(link: String, allowCleartext: Boolean = false): PairingLink? {
            val uri = parseUri(link.trim()) ?: return null
            val isPairLink = uri.scheme.equals(SCHEME, ignoreCase = true) && uri.host.equals(HOST, ignoreCase = true)
            val params = if (isPairLink) queryParams(uri) else emptyMap()
            val code = params["code"]?.trim()?.takeIf(::isValidCode)
            val server = params["server"]?.let { normalizeServerUrl(it, allowCleartext) }
            return if (code != null && server != null) PairingLink(server, code) else null
        }

        private fun parseUri(value: String): URI? = try {
            URI(value)
        } catch (_: URISyntaxException) {
            null
        }

        private fun queryParams(uri: URI): Map<String, String> = uri.rawQuery.orEmpty().split('&')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) null else decode(part.substring(0, separator)) to decode(part.substring(separator + 1))
            }
            .toMap()

        fun isValidCode(code: String): Boolean = codePattern.matches(code.trim())

        fun normalizeServerUrl(value: String, allowCleartext: Boolean = false): String? {
            val trimmed = value.trim().trimEnd('/')
            val uri = parseUri(trimmed) ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            if (uri.host.isNullOrBlank()) return null
            val allowed = scheme == "https" || (allowCleartext && scheme == "http")
            return trimmed.takeIf { allowed && uri.rawQuery == null && uri.rawFragment == null }
        }

        private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())
    }
}
