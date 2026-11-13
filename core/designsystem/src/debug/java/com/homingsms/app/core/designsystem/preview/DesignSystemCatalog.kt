package com.homingsms.app.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homingsms.app.core.designsystem.component.CodeBlock
import com.homingsms.app.core.designsystem.component.EmptyState
import com.homingsms.app.core.designsystem.component.FilterChipRow
import com.homingsms.app.core.designsystem.component.FilterMenuChip
import com.homingsms.app.core.designsystem.component.FilterOption
import com.homingsms.app.core.designsystem.component.GatewayHealth
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingFilterChip
import com.homingsms.app.core.designsystem.component.HomingListRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingSwitchRow
import com.homingsms.app.core.designsystem.component.HomingTimeline
import com.homingsms.app.core.designsystem.component.HomingWordmark
import com.homingsms.app.core.designsystem.component.HourlyActivityChart
import com.homingsms.app.core.designsystem.component.HourlyBar
import com.homingsms.app.core.designsystem.component.QrCode
import com.homingsms.app.core.designsystem.component.SecretField
import com.homingsms.app.core.designsystem.component.SimCard
import com.homingsms.app.core.designsystem.component.StatTile
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.component.StatusHero
import com.homingsms.app.core.designsystem.component.TimelineEntry
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone

internal val CatalogHourly = List(24) { hour ->
    HourlyBar(delivered = (hour * 7) % 13, sentWithoutReport = hour % 3, failed = if (hour % 9 == 0) 1 else 0)
}

@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(HomingTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
    ) {
        HomingWordmark()
        Text("Tokens and components the app is built from.", style = MaterialTheme.typography.bodyMedium)
        StatusHero(
            health = GatewayHealth.Degraded,
            reason = "SIM 2 has no signal.",
            fixLabel = "Open SIM settings",
            onFix = {}
        )
        HomingPanel {
            Row(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.xxl)) {
                StatTile(label = "Sent", value = 1284, tone = StatusTone.Brand)
                StatTile(label = "Delivered", value = 1203, tone = StatusTone.Success)
                StatTile(label = "Failed", value = null, tone = StatusTone.Danger)
            }
        }
        HourlyActivityChart(bars = CatalogHourly)
        SimCard(
            slot = 0,
            carrier = "Vodafone",
            phoneNumber = "+44 7700 900123",
            signalLevel = 3,
            enabled = true,
            sentToday = 412,
            dailyCap = 500
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
            HomingButton(text = "Primary", onClick = {})
            HomingButton(text = "Secondary", onClick = {}, style = HomingButtonStyle.Secondary)
            HomingButton(text = "Outline", onClick = {}, style = HomingButtonStyle.Outline)
            HomingButton(text = "Delete", onClick = {}, style = HomingButtonStyle.Destructive)
        }
        FilterChipRow(contentPadding = androidx.compose.foundation.layout.PaddingValues()) {
            FilterMenuChip(
                label = "State",
                options = listOf(FilterOption(1, "Failed"), FilterOption(2, "Delivered")),
                selected = setOf(1),
                onToggle = {}
            )
            HomingFilterChip(label = "SMS", selected = false, onClick = {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
            StatusBadge("Delivered", StatusTone.Success)
            StatusBadge("Queued", StatusTone.Info)
            StatusBadge("Failed", StatusTone.Danger)
            StatusBadge("Expired", StatusTone.Warning)
        }
        HomingPanel(contentPadding = androidx.compose.foundation.layout.PaddingValues()) {
            HomingListRow(headline = "+1 555 0100", supporting = "Your code is 481 223", onClick = {})
            HomingDivider()
            HomingSwitchRow(title = "Local server", checked = true, onCheckedChange = {})
        }
        HomingTimeline(
            entries = listOf(
                TimelineEntry("Queued", "14:02", StatusTone.Info),
                TimelineEntry("Sent", "14:02", StatusTone.Brand),
                TimelineEntry("Failed", "14:03", StatusTone.Danger, detail = "generic_failure: Radio off")
            )
        )
        SecretField(label = "API key", secret = "hk_live_4f9a2b7c1d8e", visiblePrefixLength = 8)
        CodeBlock(code = "adb shell settings put global sms_outgoing_check_max_count 1000", copyLabel = "Command")
        QrCode(content = "http://192.168.1.20:8080/api/v1", contentDescription = "Local API address")
        EmptyState(icon = HomingIcons.Inbox, title = "No messages yet", body = "Received messages show up here.")
        EmptyState(icon = HomingIcons.Messages, title = "Nothing sent", actionLabel = "Send a test", onAction = {})
    }
}

@HomingPreviews
@Composable
private fun DesignSystemCatalogPreview() {
    HomingPreviewSurface { DesignSystemCatalog() }
}
