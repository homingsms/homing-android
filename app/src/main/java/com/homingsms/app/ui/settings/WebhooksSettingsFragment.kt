package com.homingsms.app.ui.settings

import android.os.Bundle
import com.homingsms.app.R

class WebhooksSettingsFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setSettingsResource(R.xml.webhooks_preferences, rootKey)
    }
}
