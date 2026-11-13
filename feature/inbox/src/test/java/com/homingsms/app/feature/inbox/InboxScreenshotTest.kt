package com.homingsms.app.feature.inbox

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
class InboxScreenshotTest(private val variant: ScreenshotVariant) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list() = composeRule.captureScreen("inbox_list", variant) { InboxScreen(InboxSamples.list, InboxActions()) }

    @Test
    fun empty() = composeRule.captureScreen("inbox_empty", variant) { InboxScreen(InboxSamples.empty, InboxActions()) }

    @Test
    fun mmsDetail() = composeRule.captureScreen("inbox_detail_mms", variant) {
        InboxDetailScreen(InboxSamples.mmsDetail, InboxDetailActions())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.entries.toList()
    }
}
