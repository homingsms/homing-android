package com.homingsms.app.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.homingsms.app.core.designsystem.component.GatewayHealth
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun degradedHeroShowsReasonAndFixInvokesCallback() {
        val fixed = mutableListOf<HomeIssue>()
        val state = HomeSamples.online.copy(health = GatewayHealth.Degraded, issues = listOf(HomeIssue.SimNoSignal(1)))
        composeRule.setContent {
            HomingTestContent { HomeScreen(state, HomeActions(onFixIssue = { fixed += it })) }
        }
        composeRule.onNodeWithText("Degraded").assertIsDisplayed()
        composeRule.onNodeWithText("SIM 2 has no signal.").assertIsDisplayed()
        composeRule.onNodeWithText("Open network settings").performClick()
        assertEquals(listOf<HomeIssue>(HomeIssue.SimNoSignal(1)), fixed)
    }

    @Test
    fun outgoingLimitShowsExactAdbCommand() {
        composeRule.setContent { HomingTestContent { HomeScreen(HomeSamples.smsLimit, HomeActions()) } }
        composeRule.onNodeWithText("adb shell settings put global sms_outgoing_check_max_count 1000").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Copy Command").assertIsDisplayed()
    }

    @Test
    fun onlineHeroHasNoFixAction() {
        composeRule.setContent { HomingTestContent { HomeScreen(HomeSamples.online, HomeActions()) } }
        composeRule.onNodeWithText("Online").assertIsDisplayed()
        composeRule.onNodeWithText("Sending and receiving normally.").assertIsDisplayed()
    }

    @Test
    fun secondaryIssuesHaveTheirOwnFix() {
        val fixed = mutableListOf<HomeIssue>()
        composeRule.setContent {
            HomingTestContent { HomeScreen(HomeSamples.smsLimit, HomeActions(onFixIssue = { fixed += it })) }
        }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Open network settings"))
        composeRule.onNodeWithText("Open network settings").performClick()
        assertEquals(listOf<HomeIssue>(HomeIssue.SimNoSignal(1)), fixed)
    }

    @Test
    fun localServerSwitchReportsNewValue() {
        val toggles = mutableListOf<Boolean>()
        composeRule.setContent {
            HomingTestContent { HomeScreen(HomeSamples.online, HomeActions(onLocalServerToggle = { toggles += it })) }
        }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Local server"))
        composeRule.onNodeWithText("Local server").assertIsOn().performClick()
        assertEquals(listOf(false), toggles)
    }

    @Test
    fun cloudSwitchDisabledUntilPaired() {
        composeRule.setContent { HomingTestContent { HomeScreen(HomeSamples.offline, HomeActions()) } }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Homing Cloud"))
        composeRule.onNodeWithText("Homing Cloud").assertIsOff().assertIsNotEnabled()
    }
}
