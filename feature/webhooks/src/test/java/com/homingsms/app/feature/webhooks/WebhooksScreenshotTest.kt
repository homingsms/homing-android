package com.homingsms.app.feature.webhooks

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
class WebhooksScreenshotTest(private val variant: ScreenshotVariant) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list() = composeRule.captureScreen("webhooks_list", variant) { WebhooksScreen(WebhooksSamples.list, WebhooksActions(onBack = {})) }

    @Test
    fun empty() = composeRule.captureScreen("webhooks_empty", variant) { WebhooksScreen(WebhooksSamples.empty, WebhooksActions()) }

    @Test
    fun editor() = composeRule.captureScreen("webhook_editor", variant) {
        WebhookEditorScreen(WebhooksSamples.newEditor, WebhookEditorActions())
    }

    @Test
    fun detail() = composeRule.captureScreen("webhook_detail", variant) {
        WebhookDetailScreen(WebhooksSamples.detail, WebhookDetailActions())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.entries.toList()
    }
}
