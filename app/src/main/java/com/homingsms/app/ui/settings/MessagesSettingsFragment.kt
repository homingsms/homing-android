package com.homingsms.app.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.homingsms.app.R
import java.util.Locale

class MessagesSettingsFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setSettingsResource(R.xml.messages_preferences, rootKey)
        val period = findPreference<DropDownPreference>(LIMIT_PERIOD)
        updateLimitValue(period?.value)
        period?.setOnPreferenceChangeListener { _, value ->
            updateLimitValue(value as? String)
            true
        }
    }

    private fun updateLimitValue(period: String?) {
        findPreference<EditTextPreference>("messages.limit_value")?.isEnabled = period != null && period != "Disabled"
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference.key) {
            "messages.limit_value",
            "messages.log_lifetime_days",
            "messages.send_interval_min",
            "messages.send_interval_max"
            -> numericInput(preference as EditTextPreference)

            "messages.work_hours_start", "messages.work_hours_end" -> {
                showTimePicker(preference as EditTextPreference)
                return
            }
        }
        super.onDisplayPreferenceDialog(preference)
    }

    private fun showTimePicker(preference: EditTextPreference) {
        val parts = (preference.text ?: DEFAULT_TIME).split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, MAX_HOUR) ?: DEFAULT_HOUR
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, MAX_MINUTE) ?: 0
        TimePickerDialog(
            requireContext(),
            { _, h, m -> preference.text = String.format(Locale.US, "%02d:%02d", h, m) },
            hour,
            minute,
            true
        ).show()
    }

    private companion object {
        const val LIMIT_PERIOD = "messages.limit_period"
        const val DEFAULT_TIME = "09:00"
        const val DEFAULT_HOUR = 9
        const val MAX_HOUR = 23
        const val MAX_MINUTE = 59
    }
}
