package com.homingsms.app.modules.encryption.providers

import com.homingsms.app.modules.settings.KeyValueStorage
import java.lang.reflect.Type

internal class FakeStorage(private val passphrase: String) : KeyValueStorage {
    override fun <T> set(key: String, value: T) {}
    override fun <T> get(key: String, typeOfT: Type): T? {
        @Suppress("UNCHECKED_CAST")
        return when (key) {
            "passphrase" -> passphrase as T
            else -> null
        }
    }

    override fun remove(key: String) {}
}
