package com.homingsms.app.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.homingsms.app.BuildConfig
import com.homingsms.app.R
import com.homingsms.app.databinding.DialogPairBinding
import com.homingsms.app.databinding.FragmentHomeBinding
import com.homingsms.app.helpers.SettingsHelper
import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.app.modules.localserver.LocalServerService
import com.homingsms.app.modules.orchestrator.OrchestratorService
import com.homingsms.app.modules.status.AppStateRepository
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.GatewayState
import com.homingsms.core.model.GatewayStatus
import com.homingsms.core.model.LocalServerInfo
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val state: AppStateRepository by inject()
    private val settingsHelper: SettingsHelper by inject()
    private val localServer: LocalServerService by inject()
    private val cloud: CloudController by inject()
    private val orchestrator: OrchestratorService by inject()

    private val permissionsRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        start()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchAutostart.isChecked = settingsHelper.autostart
        binding.switchAutostart.setOnCheckedChangeListener { _, checked -> settingsHelper.autostart = checked }
        binding.switchUseLocalServer.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) viewLifecycleOwner.lifecycleScope.launch { localServer.setEnabled(checked) }
        }
        binding.switchUseRemoteServer.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) viewLifecycleOwner.lifecycleScope.launch { cloud.setEnabled(checked) }
        }
        binding.buttonPair.setOnClickListener { onPairClicked() }
        binding.buttonStart.setOnClickListener {
            if (state.serviceRunning.value) orchestrator.stop(requireContext()) else requestPermissionsAndStart()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { state.gateway.collect(::renderGateway) }
                launch { state.localServerInfo.collect(::renderLocalServer) }
                launch { state.cloudPairing.collect(::renderCloud) }
                launch { state.serviceRunning.collect(::renderRunning) }
                launch { state.settings.collect { binding.switchUseRemoteServer.isChecked = it.cloud.enabled } }
                launch {
                    state.today.collect {
                        binding.textToday.text = getString(R.string.today_counts, it.sent, it.delivered, it.failed, it.received, it.queued)
                    }
                }
            }
        }
    }

    private fun renderGateway(gateway: GatewayState) {
        binding.textStatus.setText(
            when (gateway.status) {
                GatewayStatus.Online -> R.string.gateway_status_online
                GatewayStatus.Degraded -> R.string.gateway_status_degraded
                GatewayStatus.Offline -> R.string.gateway_status_offline
            }
        )
        binding.textStatusReasons.text = gateway.reasons.joinToString("\n") { StatusText.describe(it) }
        binding.textStatusReasons.isVisible = gateway.reasons.isNotEmpty()
    }

    private fun renderLocalServer(info: LocalServerInfo) {
        binding.switchUseLocalServer.isChecked = info.enabled
        val lines = buildList {
            addAll(info.baseUrls)
            if (info.running && info.baseUrls.isEmpty()) add("http://127.0.0.1:${info.port}/api/v1")
            add(getString(R.string.device_id) + ": " + info.deviceId.ifEmpty { getString(R.string.n_a) })
            add(getString(R.string.api_keys) + ": " + info.keys.size)
            info.lastError?.let(::add)
        }
        binding.textLocalServer.text = lines.joinToString("\n")
    }

    private fun renderCloud(pairing: CloudPairingState) {
        binding.textCloud.text = when (pairing) {
            is CloudPairingState.Paired -> getString(R.string.paired_as, pairing.deviceId) + "\n" + pairing.serverUrl
            is CloudPairingState.Pairing -> getString(R.string.dots)
            is CloudPairingState.Failed -> getString(R.string.pairing_failed, pairing.message ?: pairing.reason.name)
            is CloudPairingState.Removed -> getString(R.string.device_unregistered)
            CloudPairingState.Unpaired -> getString(R.string.not_registered)
        }
        binding.buttonPair.setText(if (pairing is CloudPairingState.Paired) R.string.unregister_device else R.string.pair_device)
    }

    private fun renderRunning(running: Boolean) {
        binding.buttonStart.setText(if (running) R.string.button_stop_service else R.string.button_start_service)
    }

    private fun onPairClicked() {
        if (cloud.pairing.value is CloudPairingState.Paired) {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_unregister_device)
                .setPositiveButton(R.string.confirm) { _, _ -> viewLifecycleOwner.lifecycleScope.launch { cloud.unpair() } }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }
        val dialogBinding = DialogPairBinding.inflate(layoutInflater)
        dialogBinding.editServerUrl.setText(state.settings.value.cloud.serverUrl ?: BuildConfig.CLOUD_URL)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.pair_device)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_continue) { _, _ ->
                val code = dialogBinding.editPairingCode.text.toString()
                val server = dialogBinding.editServerUrl.text.toString()
                viewLifecycleOwner.lifecycleScope.launch { cloud.pair(code, server) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun requestPermissionsAndStart() {
        val missing = AppStateRepository.requiredPermissions().filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) start() else permissionsRequest.launch(missing.toTypedArray())
    }

    private fun start() {
        val context = context ?: return
        orchestrator.start(context.applicationContext, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}
