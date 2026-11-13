package com.homingsms.core.model.api

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebhookSignature {
    const val HEADER = "Homing-Signature"
    const val EVENT_ID_HEADER = "Homing-Event-Id"
    const val EVENT_TYPE_HEADER = "Homing-Event-Type"
    const val TOLERANCE_SECONDS = 300L
    private const val ALGORITHM = "HmacSHA256"

    fun sign(secret: String, timestampSeconds: Long, body: String): String =
        "t=$timestampSeconds,v1=${hmacHex(secret, "$timestampSeconds.$body")}"

    fun verify(header: String, body: String, secret: String, nowSeconds: Long): Boolean {
        val parts = header.split(',').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index).trim() to part.substring(index + 1).trim()
        }
        val timestamp = parts.firstOrNull { it.first == "t" }?.second?.toLongOrNull() ?: return false
        if (kotlin.math.abs(nowSeconds - timestamp) > TOLERANCE_SECONDS) return false
        val expected = hmacHex(secret, "$timestamp.$body").toByteArray(Charsets.UTF_8)
        return parts.filter { it.first == "v1" }.any { MessageDigest.isEqual(it.second.toByteArray(Charsets.UTF_8), expected) }
    }

    fun hmacHex(secret: String, payload: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), ALGORITHM))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).toHex()
    }
}

object ApiKeyFormat {
    const val PREFIX = "hk_live_"
    const val SECRET_LENGTH = 32
    const val DISPLAY_PREFIX_LENGTH = 12
    private const val ALGORITHM = "HmacSHA256"

    fun displayPrefix(key: String): String = key.take(DISPLAY_PREFIX_LENGTH)

    fun looksValid(key: String): Boolean = key.startsWith(PREFIX) && key.length == PREFIX.length + SECRET_LENGTH

    fun hash(pepper: ByteArray, key: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(pepper, ALGORITHM))
        return mac.doFinal(key.toByteArray(Charsets.UTF_8)).toHex()
    }

    fun matches(pepper: ByteArray, key: String, storedHash: String): Boolean = MessageDigest.isEqual(
        hash(pepper, key).toByteArray(Charsets.UTF_8),
        storedHash.toByteArray(Charsets.UTF_8)
    )
}

object WebhookSecretFormat {
    const val PREFIX = "whsec_"
    const val SECRET_LENGTH = 32

    private const val PREVIEW_CHARS = 4

    fun preview(secret: String): String = secret.take(PREFIX.length + PREVIEW_CHARS) + "…"
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
