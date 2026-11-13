package com.homingsms.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.homingsms.app.BuildConfig
import com.homingsms.app.R
import com.homingsms.app.databinding.DialogPairBinding
import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.core.model.CloudPairingState
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CloudServerSettingsFragment : BasePreferenceFragment() {
    private val cloud by inject<CloudController>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setSettingsResource(R.xml.cloud_server_preferences, rootKey)
        findPreference<Preference>("transient.pair")?.setOnPreferenceClickListener {
            showPairDialog()
            true
        }
        findPreference<Preference>("transient.unpair")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_unregister_device)
                .setPositiveButton(R.string.confirm) { _, _ -> lifecycleScope.launch { cloud.unpair() } }
                .setNegativeButton(R.string.cancel, null)
                .show()
            true
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cloud.pairing.collect(::render)
            }
        }
    }

    private fun render(pairing: CloudPairingState) {
        val paired = pairing as? CloudPairingState.Paired
        findPreference<Preference>("transient.cloud_server")?.summary = paired?.serverUrl ?: getString(R.string.n_a)
        findPreference<Preference>("transient.device_id")?.summary = paired?.deviceId ?: getString(R.string.not_registered)
        findPreference<Preference>("transient.pair")?.apply {
            isVisible = paired == null
            summary = (pairing as? CloudPairingState.Failed)?.let { getString(R.string.pairing_failed, it.message ?: it.reason.name) }
        }
        findPreference<Preference>("transient.unpair")?.isVisible = paired != null
    }

    private fun showPairDialog() {
        val binding = DialogPairBinding.inflate(layoutInflater)
        binding.editServerUrl.setText(BuildConfig.CLOUD_URL)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.pair_device)
            .setView(binding.root)
            .setPositiveButton(R.string.btn_continue) { _, _ ->
                val code = binding.editPairingCode.text.toString()
                val server = binding.editServerUrl.text.toString()
                lifecycleScope.launch { cloud.pair(code, server) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
