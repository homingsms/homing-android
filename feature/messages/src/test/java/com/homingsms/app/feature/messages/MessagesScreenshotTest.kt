package com.homingsms.app.feature.messages

import androidx.compose.ui.test.junit4.createComposeRule
import com.homingsms.app.core.testing.ScreenshotVariant
import com.homingsms.app.core.testing.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MessagesScreenshotTest(private val variant: ScreenshotVariant) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list() = composeRule.captureScreen("messages_list", variant) { MessagesScreen(MessagesSamples.list, MessagesActions()) }

    @Test
    fun noMatches() = composeRule.captureScreen("messages_no_matches", variant) {
        MessagesScreen(MessagesSamples.filteredEmpty, MessagesActions())
    }

    @Test
    fun detailFailed() = composeRule.captureScreen("message_detail_failed", variant) {
        MessageDetailScreen(MessagesSamples.failedDetail, MessageDetailActions())
    }

    @Test
    fun detailSent() = composeRule.captureScreen("message_detail_sent", variant) {
        MessageDetailScreen(MessagesSamples.sentDetail, MessageDetailActions())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.entries.toList()
    }
}
