package com.homingsms.app.ui.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.homingsms.app.R

class InboxSettingsFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setSettingsResource(R.xml.inbox_preferences, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == "incoming.lifetime_days") numericInput(preference as EditTextPreference)
        super.onDisplayPreferenceDialog(preference)
    }
}
