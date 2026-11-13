package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone

enum class GatewayHealth { Online, Degraded, Offline }

fun GatewayHealth.tone(): StatusTone = when (this) {
    GatewayHealth.Online -> StatusTone.Success
    GatewayHealth.Degraded -> StatusTone.Warning
    GatewayHealth.Offline -> StatusTone.Neutral
}

@Composable
fun GatewayHealth.label(): String = when (this) {
    GatewayHealth.Online -> stringResource(R.string.homing_status_online)
    GatewayHealth.Degraded -> stringResource(R.string.homing_status_degraded)
    GatewayHealth.Offline -> stringResource(R.string.homing_status_offline)
}
