package com.homingsms.app.feature.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpendPanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(spend: SpendUiState, onSetSmsRate: () -> Unit = {}) {
        composeRule.setContent { HomingTestContent { SpendPanel(spend, onSetSmsRate) } }
    }

    @Test
    fun noRateShowsPartsAndAsksForARateWithoutAnyAmount() {
        var opened = 0
        show(HomeSamples.spendNoRate) { opened++ }

        composeRule.onNodeWithText("1,653").assertIsDisplayed()
        composeRule.onNodeWithText("SMS parts sent this month").assertIsDisplayed()
        composeRule.onNodeWithText("11,212 parts").assertIsDisplayed()
        composeRule.onAllNodesWithText("£", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("No rate").assertCountEquals(0)
        composeRule.onNodeWithText("SIM 2 · EE").assertIsDisplayed()
        composeRule.onNodeWithText("Set SMS rate").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun zeroWithARateShowsCurrencyZero() {
        show(HomeSamples.spendZero)

        composeRule.onNodeWithText("£0.00").assertIsDisplayed()
        composeRule.onNodeWithText("Estimated this month from 0 parts").assertIsDisplayed()
        composeRule.onNodeWithText("£0.00 for 0 parts").assertIsDisplayed()
        composeRule.onAllNodesWithText("Set SMS rate").assertCountEquals(0)
    }

    @Test
    fun singleSimShowsTotalsWithoutBreakdown() {
        show(HomeSamples.spendSingleSim)

        composeRule.onNodeWithText("£7.65").assertIsDisplayed()
        composeRule.onNodeWithText("Estimated this month from 1,020 parts").assertIsDisplayed()
        composeRule.onNodeWithText("£51.75 for 6,900 parts").assertIsDisplayed()
        composeRule.onAllNodesWithText("SIM 1 · Vodafone UK").assertCountEquals(0)
    }

    @Test
    fun multiSimShowsPerSimBreakdownAndChangeRate() {
        var opened = 0
        show(HomeSamples.spendMultiSim) { opened++ }

        composeRule.onNodeWithText("£13.98").assertIsDisplayed()
        composeRule.onNodeWithText("SIM 1 · Vodafone UK").assertIsDisplayed()
        composeRule.onNodeWithText("£7.65").assertIsDisplayed()
        composeRule.onNodeWithText("SIM 2 · EE").assertIsDisplayed()
        composeRule.onNodeWithText("633 parts").assertIsDisplayed()
        composeRule.onNodeWithText("£6.33").assertIsDisplayed()
        composeRule.onNodeWithText("Change rate").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun simWithoutRateIsMarkedAndNotCounted() {
        show(HomeSamples.spendPartlyPriced)

        composeRule.onNodeWithText("No rate").assertIsDisplayed()
        composeRule.onNodeWithText("633 parts this month have no rate and aren't counted.").assertIsDisplayed()
    }

    @Test
    fun homeShowsSpendAfterTodayAndRoutesTheAction() {
        var opened = 0
        val state = HomeSamples.withSpend.copy(spend = HomeSamples.spendNoRate)
        composeRule.setContent { HomingTestContent { HomeScreen(state, HomeActions(onSetSmsRate = { opened++ })) } }

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Set SMS rate"))
        composeRule.onNodeWithText("SMS spend").assertIsDisplayed()
        composeRule.onNodeWithText("Set SMS rate").performClick()
        assertEquals(1, opened)
    }
}
