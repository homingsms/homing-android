package com.homingsms.app.feature.settings

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<SettingsChange>()
    private val destinations = mutableListOf<SettingsDestination>()

    private fun show(state: SettingsUiState = SettingsSamples.typical) {
        composeRule.setContent {
            HomingTestContent {
                SettingsScreen(state, SettingsActions(onChange = { changes += it }, onNavigate = { destinations += it }))
            }
        }
    }

    @Test
    fun rateLimitEditorSavesNewValue() {
        show()
        composeRule.onNodeWithText("Sending rate").performClick()
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("45")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(listOf<SettingsChange>(SettingsChange.RateLimit(45)), changes)
    }

    @Test
    fun rateLimitOutOfRangeCannotBeSaved() {
        show()
        composeRule.onNodeWithText("Sending rate").performClick()
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("0")
        composeRule.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun dailyCapCanBeRemoved() {
        show()
        composeRule.onNodeWithText("Daily cap, SIM 1").performClick()
        composeRule.onNode(hasText("No cap") and isToggleable()).performClick()
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(listOf<SettingsChange>(SettingsChange.DailyCap(0, null)), changes)
    }

    @Test
    fun routingChoiceIsReported() {
        show()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("SIM choice"))
        composeRule.onNodeWithText("SIM choice").performClick()
        composeRule.onNodeWithText("Least used today").performClick()
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(listOf<SettingsChange>(SettingsChange.Routing(SimRoutingMode.LeastUsed)), changes)
    }

    @Test
    fun switchesReportChanges() {
        show()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Delivery reports"))
        composeRule.onNodeWithText("Delivery reports").performClick()
        assertEquals(listOf<SettingsChange>(SettingsChange.DeliveryReports(false)), changes)
    }

    @Test
    fun aboutAndKeysNavigate() {
        show()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("API keys"))
        composeRule.onNodeWithText("API keys").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("About Homing"))
        composeRule.onNodeWithText("About Homing").performClick()
        assertEquals(listOf(SettingsDestination.ApiKeys, SettingsDestination.About), destinations)
    }

    @Test
    fun legalRowsOpenEachDocumentAndShowAcceptance() {
        show()
        listOf("Terms of Service", "Privacy Policy", "Acceptable Use Policy").forEach { title ->
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(title))
            composeRule.onNodeWithText(title).performClick()
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Terms accepted"))
        composeRule.onNodeWithText("Terms accepted").assertExists()
        assertEquals(
            listOf(SettingsDestination.TermsOfService, SettingsDestination.PrivacyPolicy, SettingsDestination.AcceptableUsePolicy),
            destinations
        )
    }

    @Test
    fun legalRowShowsWhenTermsWereNotAccepted() {
        show(SettingsSamples.typical.copy(legal = LegalSettingsUi()))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Not accepted yet"))
        composeRule.onNodeWithText("Not accepted yet").assertExists()
    }
}
