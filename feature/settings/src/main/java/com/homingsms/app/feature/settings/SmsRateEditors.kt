package com.homingsms.app.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingEditorSheet
import com.homingsms.app.core.designsystem.component.HomingNavigationRow
import com.homingsms.app.core.designsystem.component.HomingTextField
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.currencyOrNull
import com.homingsms.app.core.designsystem.util.currencySymbol
import com.homingsms.app.core.designsystem.util.currentLocale
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatMoney
import com.homingsms.app.core.designsystem.util.simLabel
import java.math.BigDecimal
import java.util.Currency

internal data object SpendCurrencyEditor : SettingsEditor

internal data class SmsRateEditor(val slot: Int, val current: Long?) : SettingsEditor

internal object RateInput {
    const val SCALE = 6
    const val MAX_UNITS = 100_000L
    private const val MICROS_PER_UNIT = 1_000_000L
    private val pattern = Regex("^(\\d{0,6})(?:[.,](\\d{0,6}))?$")

    fun parse(text: String): Long? {
        val match = pattern.matchEntire(text.trim()) ?: return null
        val whole = match.groupValues[1]
        val fraction = match.groupValues[2]
        if (whole.isEmpty() && fraction.isEmpty()) return null
        val micros = whole.ifEmpty { "0" }.toLong() * MICROS_PER_UNIT + fraction.padEnd(SCALE, '0').toLong()
        return micros.takeIf { it <= MAX_UNITS * MICROS_PER_UNIT }
    }

    fun format(micros: Long): String = BigDecimal.valueOf(micros, SCALE).stripTrailingZeros().toPlainString()

    fun clean(text: String): String = text.filter { it.isDigit() || it == '.' || it == ',' }
}

internal object CurrencyInput {
    private const val LENGTH = 3

    fun clean(text: String): String = text.filter { it in 'a'..'z' || it in 'A'..'Z' }.take(LENGTH).uppercase()

    fun resolve(code: String): Currency? = code.takeIf { it.length == LENGTH }?.let(::currencyOrNull)?.takeIf { it.currencyCode == code }
}

internal fun LazyListScope.spendSection(spend: SpendSettingsUi, open: (SettingsEditor) -> Unit) {
    section("spend", R.string.settings_group_spend) {
        val locale = currentLocale()
        val currency = currencyOrNull(spend.currencyCode)
        HomingNavigationRow(
            title = stringResource(R.string.settings_currency),
            value = currency?.let { stringResource(R.string.settings_currency_value, it.getDisplayName(locale), it.currencyCode) }
                ?: spend.currencyCode,
            onClick = { open(SpendCurrencyEditor) }
        )
        spend.rates.forEach { sim ->
            HomingDivider(inset = true)
            HomingNavigationRow(
                title = stringResource(R.string.settings_sms_rate, simLabel(sim.slot)),
                value = sim.rateMicros?.let { micros ->
                    val rate = formatMoney(micros, spend.currencyCode, maxFractionDigits = RateInput.SCALE)
                    stringResource(R.string.settings_sms_rate_value, rate)
                } ?: stringResource(R.string.settings_sms_rate_not_set),
                onClick = { open(SmsRateEditor(sim.slot, sim.rateMicros)) }
            )
        }
    }
}

@Composable
internal fun SpendEditorHost(editor: SettingsEditor, spend: SpendSettingsUi, apply: (SettingsChange) -> Unit, onClose: () -> Unit) {
    when (editor) {
        SpendCurrencyEditor -> CurrencyEditorSheet(
            initial = spend.currencyCode,
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.SpendCurrency(it)) }
        )

        is SmsRateEditor -> SmsRateEditorSheet(
            title = stringResource(R.string.settings_sms_rate, simLabel(editor.slot)),
            currencyCode = spend.currencyCode,
            initialMicros = editor.current,
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.SmsRate(editor.slot, it)) }
        )

        else -> Unit
    }
}

@Composable
internal fun SmsRateEditorSheet(
    title: String,
    currencyCode: String,
    initialMicros: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialMicros?.let(RateInput::format).orEmpty()) }
    val parsed = RateInput.parse(text)
    val blank = text.isBlank()
    val valid = blank || parsed != null
    HomingEditorSheet(
        title = title,
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_save),
        confirmEnabled = valid && !(blank && initialMicros == null),
        onConfirm = { onConfirm(if (blank) null else parsed) }
    ) {
        Text(
            text = stringResource(R.string.settings_sms_rate_help),
            style = MaterialTheme.typography.bodyMedium,
            color = HomingTheme.colors.mutedForeground
        )
        HomingTextField(
            value = text,
            onValueChange = { text = RateInput.clean(it) },
            label = stringResource(R.string.settings_sms_rate_label),
            supportingText = stringResource(R.string.settings_sms_rate_clear),
            errorText = if (valid) {
                null
            } else {
                stringResource(R.string.settings_sms_rate_invalid, formatCount(RateInput.MAX_UNITS), formatCount(RateInput.SCALE))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = {
                Text(
                    text = currencySymbol(currencyCode, currentLocale()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = HomingTheme.colors.mutedForeground
                )
            }
        )
    }
}

@Composable
internal fun CurrencyEditorSheet(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val currency = CurrencyInput.resolve(text)
    val locale = currentLocale()
    HomingEditorSheet(
        title = stringResource(R.string.settings_currency),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_save),
        confirmEnabled = currency != null,
        onConfirm = { currency?.let { onConfirm(it.currencyCode) } }
    ) {
        HomingTextField(
            value = text,
            onValueChange = { text = CurrencyInput.clean(it) },
            label = stringResource(R.string.settings_currency_label),
            supportingText = currency?.let {
                stringResource(R.string.settings_currency_value, it.getDisplayName(locale), it.getSymbol(locale))
            } ?: stringResource(R.string.settings_currency_help),
            errorText = if (text.length == CURRENCY_CODE_LENGTH && currency == null) {
                stringResource(R.string.settings_currency_invalid)
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii)
        )
    }
}

private const val CURRENCY_CODE_LENGTH = 3
