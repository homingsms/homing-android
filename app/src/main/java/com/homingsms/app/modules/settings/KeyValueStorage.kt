package com.homingsms.app.modules.settings

import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface KeyValueStorage {
    fun <T> set(key: String, value: T)
    fun <T> get(key: String, type: KType): T?
    fun remove(key: String)
}

inline fun <reified T> KeyValueStorage.get(key: String): T? = get<T>(key, typeOf<T>())
