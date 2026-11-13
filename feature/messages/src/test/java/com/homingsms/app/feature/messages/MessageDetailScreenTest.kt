package com.homingsms.app.feature.messages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failedMessageShowsCarrierErrorAndRetries() {
        var retries = 0
        composeRule.setContent {
            HomingTestContent { MessageDetailScreen(MessagesSamples.failedDetail, MessageDetailActions(onRetry = { retries++ })) }
        }
        composeRule.onNodeWithText("Carrier error").assertIsDisplayed()
        composeRule.onNodeWithText("The carrier reported no service on SIM 2.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun cancelAsksForConfirmationBeforeCallback() {
        var cancels = 0
        composeRule.setContent {
            HomingTestContent { MessageDetailScreen(MessagesSamples.cancellable, MessageDetailActions(onCancel = { cancels++ })) }
        }
        composeRule.onNodeWithText("Cancel message").performClick()
        composeRule.onNode(isDialog()).assertIsDisplayed()
        composeRule.onNodeWithText("Cancel this message?").assertIsDisplayed()
        assertEquals(0, cancels)
        composeRule.onAllNodesWithTextInDialog("Cancel message").performClick()
        assertEquals(1, cancels)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun dismissingCancelDialogKeepsMessage() {
        var cancels = 0
        composeRule.setContent {
            HomingTestContent { MessageDetailScreen(MessagesSamples.cancellable, MessageDetailActions(onCancel = { cancels++ })) }
        }
        composeRule.onNodeWithText("Cancel message").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(0, cancels)
    }

    @Test
    fun sentWithReportShowsPendingDeliveryStep() {
        composeRule.setContent { HomingTestContent { MessageDetailScreen(MessagesSamples.sentDetail, MessageDetailActions()) } }
        composeRule.onNodeWithText("Waiting for delivery report").assertExists()
    }

    @Test
    fun noticeIsShownOnceAndReported() {
        var shown = 0
        val state = MessagesSamples.failedDetail.copy(notice = MessageNotice.RetryQueued)
        composeRule.setContent { HomingTestContent { MessageDetailScreen(state, MessageDetailActions(onNoticeShown = { shown++ })) } }
        composeRule.onNodeWithText("Message queued again").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextInDialog(text: String) = onNode(
        androidx.compose.ui.test.hasText(text) and androidx.compose.ui.test.hasAnyAncestor(isDialog())
    )
}
