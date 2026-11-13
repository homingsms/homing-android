package com.homingsms.app.helpers

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.homingsms.app.receivers.BootReceiver
import com.homingsms.core.data.settings.SettingsRepository

class SettingsHelper(private val context: Context, private val repository: SettingsRepository? = null) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var autostart: Boolean
        get() = repository?.current?.general?.autostart ?: false
        set(value) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, BootReceiver::class.java),
                if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            repository?.updateBlocking { it.copy(general = it.general.copy(autostart = value)) }
        }

    var language: String
        get() = when (val stored = preferences.getString(APP_LANGUAGE, "")) {
            null, "system" -> ""
            else -> stored
        }
        set(value) = preferences.edit(true) { putString(APP_LANGUAGE, value) }

    companion object {
        const val APP_LANGUAGE = "app.language"
    }
}
