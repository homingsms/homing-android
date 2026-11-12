package com.homingsms.app.modules.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.reflect.KClass
import kotlin.reflect.KType

class PreferencesStorage(
    private val preferences: SharedPreferences,
    private val prefix: String,
    serializer: ValueSerializer? = null
): KeyValueStorage {
    private val serializer = serializer ?: ValueSerializer()

    override fun <T> set(key: String, value: T) {
        preferences.edit {
            if (value == null) {
                remove("${prefix}.${key}")
                return@edit
            }

            when (value) {
                is Long -> putLong("${prefix}.${key}", value)
                is Int -> putInt("${prefix}.${key}", value)
                is String -> putString("${prefix}.${key}", value)
                is Boolean -> putBoolean("${prefix}.${key}", value)
                is Float -> putFloat("${prefix}.${key}", value)
                else -> putString("${prefix}.${key}", serializer.serialize(value))
            }
        }
    }

    override fun <T> get(key: String, type: KType): T? {
        if (!preferences.contains("${prefix}.${key}")) {
            return null
        }

        return try {
            preferences.getString("${prefix}.${key}", null)?.let {
                serializer.deserialize(it, type)
            }
        } catch (th: ClassCastException) {
            getFallback<T>(type, key)
        } catch (th: IllegalArgumentException) {
            if ((type.classifier as? KClass<*>)?.java?.isEnum == true) {
                null
            } else {
                getFallback<T>(type, key)
            }
        }
    }

    override fun remove(key: String) {
        preferences.edit {
            remove("${prefix}.${key}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getFallback(type: KType, key: String) = when (type.classifier) {
        Long::class -> preferences.getLong("${prefix}.${key}", 0) as T
        Int::class -> preferences.getInt("${prefix}.${key}", 0) as T
        String::class -> preferences.getString("${prefix}.${key}", "") as T
        Boolean::class -> preferences.getBoolean(
            "${prefix}.${key}",
            false
        ) as T

        Float::class -> preferences.getFloat("${prefix}.${key}", 0.0f) as T
        else -> throw RuntimeException("Unknown type for key $key")
    }
}
