package com.homingsms.app.core.designsystem

import com.homingsms.app.core.designsystem.util.currencySymbol
import com.homingsms.app.core.designsystem.util.formatMoney
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormattingTest {
    @Test
    fun formatsMicrosWithTheCurrencyDefaultPrecision() {
        assertEquals("$12.40", formatMoney(12_397_500, "USD", Locale.US))
        assertEquals("$0.00", formatMoney(0, "USD", Locale.US))
        assertEquals("$0.01", formatMoney(5_000, "USD", Locale.US))
        assertEquals("¥3", formatMoney(3_000_000, "JPY", Locale.US))
        assertEquals("1.234,57 €", formatMoney(1_234_567_890, "EUR", Locale.GERMANY))
    }

    @Test
    fun ratesKeepUpToSixDecimals() {
        assertEquals("$0.0075", formatMoney(7_500, "USD", Locale.US, maxFractionDigits = 6))
        assertEquals("$0.000001", formatMoney(1, "USD", Locale.US, maxFractionDigits = 6))
        assertEquals("$4.00", formatMoney(4_000_000, "USD", Locale.US, maxFractionDigits = 6))
    }

    @Test
    fun unknownCurrencyFallsBackToTheLocaleCurrency() {
        assertEquals("$1.00", formatMoney(1_000_000, "XYZ", Locale.US))
        assertEquals("XYZ", currencySymbol("XYZ", Locale.US))
        assertEquals("₦", currencySymbol("NGN", Locale("en", "NG")))
    }
}
