package com.homingsms.app.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmsRateEditorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<SettingsChange>()

    private fun show(state: SettingsUiState = SettingsSamples.typical) {
        composeRule.setContent {
            HomingTestContent { SettingsScreen(state, SettingsActions(onChange = { changes += it })) }
        }
    }

    private fun openRow(title: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(title))
        composeRule.onNodeWithText(title).performClick()
    }

    @Test
    fun parsesNonNegativeAmountsWithUpToSixDecimals() {
        assertEquals(7_500L, RateInput.parse("0.0075"))
        assertEquals(7_500L, RateInput.parse("0,0075"))
        assertEquals(500_000L, RateInput.parse(".5"))
        assertEquals(3_000_000L, RateInput.parse("3."))
        assertEquals(0L, RateInput.parse("0"))
        assertEquals(1L, RateInput.parse("0.000001"))
        assertEquals(100_000_000_000L, RateInput.parse("100000"))
        assertNull(RateInput.parse("100000.01"))
        assertNull(RateInput.parse("0.0000001"))
        assertNull(RateInput.parse("-1"))
        assertNull(RateInput.parse("1.2.3"))
        assertNull(RateInput.parse("."))
        assertNull(RateInput.parse(""))
        assertEquals("0.0075", RateInput.format(7_500))
        assertEquals("4", RateInput.format(4_000_000))
        assertEquals("12.34", RateInput.clean("£12.34 "))
    }

    @Test
    fun currencyInputAcceptsOnlyRealCodes() {
        assertEquals("NGN", CurrencyInput.clean("ngn1"))
        assertEquals("NGN", CurrencyInput.resolve("NGN")?.currencyCode)
        assertNull(CurrencyInput.resolve("ABC"))
        assertNull(CurrencyInput.resolve("US"))
    }

    @Test
    fun rowsShowCurrencyAndRates() {
        show()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Rate for SIM 2"))
        composeRule.onNodeWithText("British Pound (GBP)").assertIsDisplayed()
        composeRule.onNodeWithText("£0.0075 per part").assertIsDisplayed()
        composeRule.onNodeWithText("Not set").assertIsDisplayed()
    }

    @Test
    fun rateEditorSavesMicros() {
        show()
        openRow("Rate for SIM 2")
        composeRule.onNode(hasSetTextAction()).performTextInput("0.012")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(listOf<SettingsChange>(SettingsChange.SmsRate(1, 12_000)), changes)
    }

    @Test
    fun rateEditorRejectsTooManyDecimals() {
        show()
        openRow("Rate for SIM 2")
        composeRule.onNode(hasSetTextAction()).performTextInput("0.0000001")
        composeRule.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Enter an amount up to 100,000 with at most 6 decimal places.").assertIsDisplayed()
    }

    @Test
    fun clearingARateRemovesIt() {
        show()
        openRow("Rate for SIM 1")
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNodeWithText("Save").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf<SettingsChange>(SettingsChange.SmsRate(0, null)), changes)
    }

    @Test
    fun emptyRateWithNothingSavedCannotBeSaved() {
        show()
        openRow("Rate for SIM 2")
        composeRule.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun currencyEditorValidatesAndSavesCode() {
        show()
        openRow("Currency")
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("abc")
        composeRule.onNodeWithText("That isn't a currency code.").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("ngn")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(listOf<SettingsChange>(SettingsChange.SpendCurrency("NGN")), changes)
    }
}
