package com.homingsms.core.data.settings

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.homingsms.core.data.FakeSecrets
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.model.LimitPeriod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    @Before
    fun clean() {
        context.dataStoreFile(SettingsRepository.FILE_NAME).delete()
        preferences.edit(commit = true) { clear() }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun migratesSharedPreferencesOnceAndMovesThePassphrase() = runBlocking {
        preferences.edit(commit = true) {
            putString("messages.limit_period", "PerDay")
            putString("messages.limit_value", "500")
            putBoolean("localserver.ENABLED", true)
            putString("localserver.PORT", "8181")
            putString("encryption.passphrase", "secret phrase")
            putString("app.language", "ru")
            putLong("receiver.state.last_processed_id", 9)
        }
        val secrets = FakeSecrets()
        val repository = SettingsRepository.create(context, secrets, scope)

        val settings = repository.current
        assertEquals(LimitPeriod.PerDay, settings.messages.limitPeriod)
        assertEquals(500, settings.messages.limitValue)
        assertTrue(settings.localServer.enabled)
        assertEquals(8181, settings.localServer.port)
        assertEquals("secret phrase", secrets.get(SecretStore.PASSPHRASE))
        assertFalse(preferences.contains("messages.limit_period"))
        assertFalse(preferences.contains("encryption.passphrase"))
        assertEquals("ru", preferences.getString("app.language", null))
        assertEquals(9L, preferences.getLong("receiver.state.last_processed_id", 0))
    }

    @Test
    fun updatesPersistAndAreObservable() = runBlocking {
        val repository = SettingsRepository.create(context, FakeSecrets(), scope)
        repository.update { it.copy(localServer = it.localServer.copy(port = 9999)) }
        assertEquals(9999, repository.current.localServer.port)
        assertTrue(context.dataStoreFile(SettingsRepository.FILE_NAME).readText().contains("9999"))
    }
}
