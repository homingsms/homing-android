package com.homingsms.app.core.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val MICROS_SCALE = 6

fun currencyOrNull(code: String): Currency? = try {
    Currency.getInstance(code)
} catch (_: IllegalArgumentException) {
    null
}

fun formatMoney(micros: Long, currencyCode: String, locale: Locale, maxFractionDigits: Int? = null): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    currencyOrNull(currencyCode)?.let { currency ->
        format.currency = currency
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
    }
    if (maxFractionDigits != null) {
        format.maximumFractionDigits = maxOf(format.minimumFractionDigits, maxFractionDigits.coerceAtMost(MICROS_SCALE))
    }
    format.roundingMode = RoundingMode.HALF_UP
    return format.format(BigDecimal.valueOf(micros, MICROS_SCALE))
}

fun currencySymbol(currencyCode: String, locale: Locale): String = currencyOrNull(currencyCode)?.getSymbol(locale) ?: currencyCode

@Composable
@ReadOnlyComposable
fun formatMoney(micros: Long, currencyCode: String, maxFractionDigits: Int? = null): String =
    formatMoney(micros, currencyCode, currentLocale(), maxFractionDigits)
