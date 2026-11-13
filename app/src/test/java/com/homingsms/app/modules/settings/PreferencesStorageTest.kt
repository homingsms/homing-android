package com.homingsms.app.modules.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreferencesStorageTest {
    private val preferences = FakeSharedPreferences()
    private val storage = PreferencesStorage(preferences, "test")

    @Test
    fun rawStringsFallBackToStoredValue() {
        storage.set("start", "09:00")
        storage.set("url", "https://api.example.com/v1")

        assertEquals("09:00", storage.get<String>("start"))
        assertEquals("https://api.example.com/v1", storage.get<String>("url"))
    }

    @Test
    fun numericStringsDecodeAsNumbers() {
        storage.set("days", "30")

        assertEquals(30, storage.get<Int>("days"))
    }

    @Test
    fun primitivesUseTypedFallback() {
        storage.set("enabled", true)
        storage.set("port", 8080)
        storage.set("ttl", 3600L)

        assertEquals(true, storage.get<Boolean>("enabled"))
        assertEquals(8080, storage.get<Int>("port"))
        assertEquals(3600L, storage.get<Long>("ttl"))
    }

    @Test
    fun enumsDecodeByNameAndUnknownIsNull() {
        storage.set("channel", SampleChannel.SSE_ONLY.name)
        storage.set("bogus", "NOT_A_CHANNEL")

        assertEquals(
            SampleChannel.SSE_ONLY,
            storage.get<SampleChannel>("channel")
        )
        assertNull(storage.get<SampleChannel>("bogus"))
    }

    @Test
    fun missingAndRemovedKeysAreNull() {
        assertNull(storage.get<String>("missing"))

        storage.set("value", "x")
        storage.remove("value")

        assertNull(storage.get<String>("value"))
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values

        override fun getString(key: String, defValue: String?): String? = if (values.containsKey(key)) values[key] as String? else defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            values[key] as MutableSet<String>? ?: defValues

        override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue

        override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue

        override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue

        override fun contains(key: String): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()

            override fun putString(key: String, value: String?) = apply { pending[key] = value }

            override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }

            override fun putInt(key: String, value: Int) = apply { pending[key] = value }

            override fun putLong(key: String, value: Long) = apply { pending[key] = value }

            override fun putFloat(key: String, value: Float) = apply { pending[key] = value }

            override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }

            override fun remove(key: String) = apply { removed.add(key) }

            override fun clear() = apply { removed.addAll(values.keys) }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                removed.forEach { values.remove(it) }
                values.putAll(pending)
            }
        }
    }
}

enum class SampleChannel {
    AUTO,
    SSE_ONLY
}
