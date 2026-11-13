package com.homingsms.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatMoney
import com.homingsms.app.core.designsystem.util.simLabel

private const val TABULAR = "tnum"
private const val BREAKDOWN_MIN_SIMS = 2

@Composable
fun SpendPanel(spend: SpendUiState, onSetSmsRate: () -> Unit, modifier: Modifier = Modifier) {
    HomingPanel(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        SectionHeader(
            title = stringResource(R.string.home_spend_title),
            trailing = if (spend.hasRate) {
                {
                    HomingButton(
                        text = stringResource(R.string.home_spend_change_rate),
                        onClick = onSetSmsRate,
                        style = HomingButtonStyle.Text
                    )
                }
            } else {
                null
            }
        )
        if (spend.hasRate) RatedSpend(spend) else UnratedSpend(spend, onSetSmsRate)
        if (spend.sims.size >= BREAKDOWN_MIN_SIMS) {
            HomingDivider()
            spend.sims.forEach { SimSpendRow(it, spend.currencyCode, spend.hasRate) }
        }
    }
}

@Composable
private fun RatedSpend(spend: SpendUiState) {
    PrimaryFigure(
        value = formatMoney(spend.monthMicros, spend.currencyCode),
        caption = pluralStringResource(R.plurals.home_spend_month_estimate, spend.monthParts.quantity(), formatCount(spend.monthParts))
    )
    SpendLine(
        label = stringResource(R.string.home_spend_all_time),
        value = pluralStringResource(
            R.plurals.home_spend_cost_for_parts,
            spend.allTimeParts.quantity(),
            formatCount(spend.allTimeParts),
            formatMoney(spend.allTimeMicros, spend.currencyCode)
        )
    )
    if (spend.unpricedMonthParts > 0) {
        Text(
            text = pluralStringResource(
                R.plurals.home_spend_unpriced,
                spend.unpricedMonthParts.quantity(),
                formatCount(spend.unpricedMonthParts)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = HomingTheme.colors.mutedForeground
        )
    }
}

@Composable
private fun UnratedSpend(spend: SpendUiState, onSetSmsRate: () -> Unit) {
    PrimaryFigure(value = formatCount(spend.monthParts), caption = stringResource(R.string.home_spend_parts_month))
    SpendLine(
        label = stringResource(R.string.home_spend_all_time),
        value = pluralStringResource(R.plurals.home_spend_parts, spend.allTimeParts.quantity(), formatCount(spend.allTimeParts))
    )
    Text(
        text = stringResource(R.string.home_spend_no_rate_body),
        style = MaterialTheme.typography.bodyMedium,
        color = HomingTheme.colors.mutedForeground
    )
    HomingButton(
        text = stringResource(R.string.home_spend_set_rate),
        onClick = onSetSmsRate,
        style = HomingButtonStyle.Secondary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PrimaryFigure(value: String, caption: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(text = value, style = HomingTheme.text.heroNumber, color = HomingTheme.colors.foreground)
        Text(text = caption, style = MaterialTheme.typography.bodyMedium, color = HomingTheme.colors.mutedForeground)
    }
}

@Composable
private fun SpendLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = HomingTheme.colors.mutedForeground,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = tabular(MaterialTheme.typography.bodyMedium), color = HomingTheme.colors.foreground)
    }
}

@Composable
private fun SimSpendRow(sim: SimSpend, currencyCode: String, hasRate: Boolean) {
    val name = sim.carrier?.let { stringResource(R.string.home_spend_sim_carrier, simLabel(sim.slot), it) } ?: simLabel(sim.slot)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HomingTheme.spacing.xxs)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, color = HomingTheme.colors.foreground)
            Text(
                text = pluralStringResource(R.plurals.home_spend_parts, sim.monthParts.quantity(), formatCount(sim.monthParts)),
                style = tabular(MaterialTheme.typography.bodySmall),
                color = HomingTheme.colors.mutedForeground
            )
        }
        if (hasRate) {
            val cost = sim.monthMicros
            Text(
                text = if (cost != null) formatMoney(cost, currencyCode) else stringResource(R.string.home_spend_no_rate),
                style = tabular(MaterialTheme.typography.bodyMedium),
                color = if (cost != null) HomingTheme.colors.foreground else HomingTheme.colors.mutedForeground
            )
        }
    }
}

private fun tabular(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = TABULAR)

private fun Long.quantity(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
