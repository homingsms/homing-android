package com.homingsms.core.model

import java.security.SecureRandom

object Ids {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val DEFAULT_LENGTH = 20
    private val random = SecureRandom()

    fun random(length: Int = DEFAULT_LENGTH): String = buildString(length) {
        repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    fun message(): String = "msg_${random()}"

    fun inbox(): String = "inb_${random()}"

    fun webhook(): String = "whk_${random()}"

    fun delivery(): String = "whd_${random()}"

    fun event(): String = "evt_${random()}"

    fun apiKey(): String = "key_${random()}"

    fun device(): String = "dev_${random()}"
}
