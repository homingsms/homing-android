package com.homingsms.core.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.model.AppSettings
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SettingsRepository(private val dataStore: DataStore<AppSettings>, private val scope: CoroutineScope) {
    val settings: StateFlow<AppSettings> = dataStore.data.stateIn(
        scope,
        SharingStarted.Eagerly,
        runBlocking { dataStore.data.first() }
    )

    val current: AppSettings
        get() = settings.value

    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings = dataStore.updateData(transform)

    fun updateBlocking(transform: (AppSettings) -> AppSettings): AppSettings = runBlocking { update(transform) }

    fun updateAsync(transform: (AppSettings) -> AppSettings) {
        scope.launch { update(transform) }
    }

    companion object {
        const val FILE_NAME = "settings.json"

        fun create(context: Context, secrets: SecretStore, scope: CoroutineScope): SettingsRepository {
            val preferences = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
            val dataStore = DataStoreFactory.create(
                serializer = AppSettingsSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { AppSettings() },
                migrations = listOf(LegacyPreferencesMigration(preferences, secrets)),
                scope = scope,
                produceFile = { context.dataStoreFile(FILE_NAME) }
            )
            return SettingsRepository(dataStore, scope)
        }
    }
}

object AppSettingsSerializer : Serializer<AppSettings> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    override val defaultValue: AppSettings = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings = try {
        json.decodeFromString(AppSettings.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read settings", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Cannot read settings", e)
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(json.encodeToString(AppSettings.serializer(), t).encodeToByteArray())
    }
}

class LegacyPreferencesMigration(private val preferences: SharedPreferences, private val secrets: SecretStore) :
    DataMigration<AppSettings> {
    override suspend fun shouldMigrate(currentData: AppSettings): Boolean = LegacySettingsMapper.hasLegacyValues(preferences.all)

    override suspend fun migrate(currentData: AppSettings): AppSettings {
        val result = LegacySettingsMapper.map(preferences.all, currentData)
        result.secrets.forEach { (key, value) -> if (secrets.get(key) == null) secrets.put(key, value) }
        return result.settings
    }

    override suspend fun cleanUp() {
        val keys = preferences.all.keys.filter(LegacySettingsMapper::isLegacyKey)
        preferences.edit { keys.forEach(::remove) }
    }
}
