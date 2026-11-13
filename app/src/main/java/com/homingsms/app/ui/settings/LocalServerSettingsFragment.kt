package com.homingsms.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.homingsms.app.R
import com.homingsms.app.databinding.DialogApiKeyBinding
import com.homingsms.app.modules.localserver.LocalServerService
import com.homingsms.core.model.ApiKeyInfo
import com.homingsms.core.model.Scope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LocalServerSettingsFragment : BasePreferenceFragment() {
    private val localServer by inject<LocalServerService>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setSettingsResource(R.xml.local_server_preferences, rootKey)

        findPreference<Preference>("transient.device_id")?.summary = localServer.ensureDeviceId()
        findPreference<Preference>("transient.create_api_key")?.setOnPreferenceClickListener {
            showCreateKeyDialog()
            true
        }
        findPreference<EditTextPreference>(PORT)?.setOnPreferenceChangeListener { _, newValue ->
            val value = (newValue as? String)?.toIntOrNull()
            val valid = value != null && value in LocalServerService.MIN_PORT..LocalServerService.MAX_PORT
            if (!valid) showToast(getString(R.string.is_not_a_valid_port_must_be_between_1024_and_65535, newValue))
            valid
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                localServer.info.collect { renderKeys(it.keys) }
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == PORT) numericInput(preference as EditTextPreference)
        super.onDisplayPreferenceDialog(preference)
    }

    private fun renderKeys(keys: List<ApiKeyInfo>) {
        val category = findPreference<PreferenceCategory>("api_keys") ?: return
        for (index in category.preferenceCount - 1 downTo 0) {
            val preference = category.getPreference(index)
            if (preference.key?.startsWith(KEY_PREFIX) == true) category.removePreference(preference)
        }
        keys.forEach { key ->
            category.addPreference(
                Preference(requireContext()).apply {
                    this.key = KEY_PREFIX + key.id
                    isPersistent = false
                    title = key.name
                    summary = key.prefix + "… · " + key.scopes.joinToString { it.value }
                    setOnPreferenceClickListener {
                        confirmRevoke(key)
                        true
                    }
                }
            )
        }
    }

    private fun confirmRevoke(key: ApiKeyInfo) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.revoke_api_key, key.name))
            .setPositiveButton(R.string.confirm) { _, _ -> lifecycleScope.launch { localServer.revokeKey(key.id) } }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCreateKeyDialog() {
        val binding = DialogApiKeyBinding.inflate(layoutInflater)
        val scopes = Scope.entries.toTypedArray()
        val checked = BooleanArray(scopes.size) { scopes[it] == Scope.MessagesSend || scopes[it] == Scope.MessagesRead }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_api_key)
            .setView(binding.root)
            .setMultiChoiceItems(scopes.map { it.value }.toTypedArray(), checked) { _, index, value -> checked[index] = value }
            .setPositiveButton(R.string.confirm) { _, _ ->
                val name = binding.editKeyName.text.toString().ifBlank { getString(R.string.api_keys) }
                val selected = scopes.filterIndexed { index, _ -> checked[index] }.toSet().ifEmpty { setOf(Scope.MessagesSend) }
                lifecycleScope.launch { showCreatedKey(localServer.createKey(name, selected).secret) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCreatedKey(secret: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.api_key_created)
            .setMessage(secret)
            .setPositiveButton(R.string.copy_key) { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("API key", secret))
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    private companion object {
        const val PORT = "localserver.PORT"
        const val KEY_PREFIX = "transient.key."
    }
}
