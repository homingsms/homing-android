package com.homingsms.app.feature.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.homingsms.app.core.designsystem.component.ConfirmationDialog
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingTextField
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatFullDateTime

@Composable
fun CloudPairingPanel(
    cloud: CloudPairing,
    pairingCode: String,
    actions: PairingActions,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    var scanning by rememberSaveable { mutableStateOf(false) }
    var confirmUnpair by rememberSaveable { mutableStateOf(false) }
    HomingPanel(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        if (showHeader) {
            SectionHeader(
                title = stringResource(R.string.connect_cloud_title),
                trailing = {
                    StatusBadge(
                        label = stringResource(
                            if (cloud is CloudPairing.Paired) R.string.connect_cloud_paired else R.string.connect_cloud_not_paired
                        ),
                        tone = if (cloud is CloudPairing.Paired) StatusTone.Success else StatusTone.Neutral
                    )
                }
            )
        }
        if (cloud is CloudPairing.Paired) {
            PairedDetails(cloud)
            HomingButton(
                text = stringResource(R.string.connect_unpair),
                onClick = { confirmUnpair = true },
                style = HomingButtonStyle.Outline
            )
        } else {
            PairingForm(cloud, pairingCode, actions, onScan = { scanning = true })
        }
    }
    if (scanning) {
        QrScannerDialog(
            onScanned = {
                scanning = false
                actions.onPairingScanned(it)
            },
            onDismiss = { scanning = false }
        )
    }
    if (confirmUnpair) {
        ConfirmationDialog(
            title = stringResource(R.string.connect_unpair_title),
            message = stringResource(R.string.connect_unpair_body),
            confirmLabel = stringResource(R.string.connect_unpair),
            destructive = true,
            onConfirm = {
                confirmUnpair = false
                actions.onUnpair()
            },
            onDismiss = { confirmUnpair = false }
        )
    }
}

@Composable
private fun PairedDetails(cloud: CloudPairing.Paired) {
    val rows = listOfNotNull(
        cloud.accountName?.let { stringResource(R.string.connect_cloud_account) to it },
        stringResource(R.string.connect_cloud_device) to cloud.deviceName,
        stringResource(R.string.connect_cloud_since) to formatFullDateTime(cloud.pairedAtMillis),
        stringResource(R.string.connect_cloud_server) to cloud.serverUrl
    )
    rows.forEach { (label, value) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = HomingTheme.colors.mutedForeground,
                modifier = Modifier.weight(1f)
            )
            Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun PairingForm(cloud: CloudPairing, pairingCode: String, actions: PairingActions, onScan: () -> Unit) {
    val pairing = cloud is CloudPairing.Pairing
    Text(text = stringResource(R.string.connect_cloud_body), style = MaterialTheme.typography.bodyMedium)
    HomingButton(
        text = stringResource(R.string.connect_scan_qr),
        onClick = onScan,
        icon = HomingIcons.QrScan,
        enabled = !pairing,
        modifier = Modifier.fillMaxWidth()
    )
    HomingTextField(
        value = pairingCode,
        onValueChange = actions.onPairingCodeChange,
        label = stringResource(R.string.connect_pairing_code),
        supportingText = stringResource(R.string.connect_pairing_code_help),
        errorText = (cloud as? CloudPairing.Failed)?.let { pairingErrorText(it.error) },
        mono = true,
        enabled = !pairing,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { if (pairingCode.isNotBlank()) actions.onSubmitPairingCode() })
    )
    HomingButton(
        text = stringResource(R.string.connect_pair),
        onClick = actions.onSubmitPairingCode,
        style = HomingButtonStyle.Secondary,
        enabled = pairingCode.isNotBlank(),
        loading = pairing,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun pairingErrorText(error: PairingError): String = stringResource(
    when (error) {
        PairingError.InvalidCode -> R.string.connect_pairing_error_invalid
        PairingError.Expired -> R.string.connect_pairing_error_expired
        PairingError.Network -> R.string.connect_pairing_error_network
        PairingError.Unknown -> R.string.connect_pairing_error_unknown
    }
)
