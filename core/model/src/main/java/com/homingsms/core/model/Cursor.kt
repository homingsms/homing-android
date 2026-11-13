package com.homingsms.core.model

import kotlin.io.encoding.Base64

data class Cursor(val timestamp: Long, val id: String) {
    fun encode(): String = codec.encode("$timestamp:$id".toByteArray(Charsets.UTF_8))

    companion object {
        private val codec = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

        fun decode(value: String): Cursor? {
            val decoded = runCatching {
                String(codec.decode(value), Charsets.UTF_8)
            }.getOrNull() ?: return null
            val separator = decoded.indexOf(':')
            if (separator <= 0 || separator == decoded.lastIndex) return null
            val timestamp = decoded.substring(0, separator).toLongOrNull() ?: return null
            return Cursor(timestamp, decoded.substring(separator + 1))
        }
    }
}
