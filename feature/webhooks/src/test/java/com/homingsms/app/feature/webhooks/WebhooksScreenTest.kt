package com.homingsms.app.feature.webhooks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebhooksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eventPickerReportsToggledEvent() {
        val toggled = mutableListOf<WebhookEvent>()
        composeRule.setContent {
            HomingTestContent { WebhookEditorScreen(WebhooksSamples.newEditor, WebhookEditorActions(onToggleEvent = { toggled += it })) }
        }
        composeRule.onNodeWithText("Message delivered").assertIsOn()
        composeRule.onNodeWithText("Message received").performScrollTo().assertIsOff().performClick()
        assertEquals(listOf(WebhookEvent.InboxReceived), toggled)
    }

    @Test
    fun httpUrlShowsError() {
        composeRule.setContent { HomingTestContent { WebhookEditorScreen(WebhooksSamples.newEditor, WebhookEditorActions()) } }
        composeRule.onNodeWithText("Use https:// so events are encrypted in transit.").assertIsDisplayed()
    }

    @Test
    fun saveDisabledWithoutEvents() {
        val state = WebhooksSamples.newEditor.copy(events = emptySet())
        composeRule.setContent { HomingTestContent { WebhookEditorScreen(state, WebhookEditorActions()) } }
        composeRule.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun deleteRequiresConfirmation() {
        var deleted = 0
        composeRule.setContent {
            HomingTestContent { WebhookEditorScreen(WebhooksSamples.editEditor, WebhookEditorActions(onDelete = { deleted++ })) }
        }
        composeRule.onNodeWithText("Delete webhook").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete this webhook?").assertIsDisplayed()
        assertEquals(0, deleted)
        composeRule.onNode(hasText("Delete webhook") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(1, deleted)
    }

    @Test
    fun rotateSecretRequiresConfirmation() {
        var rotated = 0
        composeRule.setContent {
            HomingTestContent { WebhookDetailScreen(WebhooksSamples.detail, WebhookDetailActions(onRotateSecret = { rotated++ })) }
        }
        composeRule.onNodeWithText("Rotate secret").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(0, rotated)
        composeRule.onNodeWithText("Rotate secret").performClick()
        composeRule.onNode(hasText("Rotate") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(1, rotated)
    }

    @Test
    fun sendTestInvokesCallback() {
        var tests = 0
        composeRule.setContent {
            HomingTestContent { WebhookDetailScreen(WebhooksSamples.detail, WebhookDetailActions(onSendTest = { tests++ })) }
        }
        composeRule.onNodeWithText("Send test event").performClick()
        composeRule.onNodeWithText("Test delivered, HTTP 200").assertIsDisplayed()
        assertEquals(1, tests)
    }

    @Test
    fun deliverySheetShowsResponseAndRetries() {
        val retried = mutableListOf<String>()
        composeRule.setContent {
            HomingTestContent { WebhookDetailScreen(WebhooksSamples.detail, WebhookDetailActions(onRetryDelivery = { retried += it })) }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("inbox.received"))
        composeRule.onNodeWithText("inbox.received").performClick()
        composeRule.onNodeWithText("Bad gateway").assertIsDisplayed()
        composeRule.onNodeWithText("Retry now").performScrollTo().performClick()
        assertEquals(listOf("whd_2"), retried)
    }

    @Test
    fun listSwitchPausesEndpoint() {
        val changes = mutableListOf<Pair<String, Boolean>>()
        composeRule.setContent {
            HomingTestContent { WebhooksScreen(WebhooksSamples.list, WebhooksActions(onEnabledChange = { id, on -> changes += id to on })) }
        }
        composeRule.onNodeWithText("Paused").performClick()
        assertEquals(listOf("whk_crm" to true), changes)
    }
}
