package com.homingsms.app.feature.inbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InboxScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun kindChipTogglesAndReflectsSelection() {
        val toggled = mutableListOf<InboxKind>()
        val state = InboxSamples.list.copy(filters = InboxFilters(kinds = setOf(InboxKind.Mms)))
        composeRule.setContent { HomingTestContent { InboxScreen(state, InboxActions(onToggleKind = { toggled += it })) } }
        composeRule.onNode(hasText("MMS") and isSelectable()).assertIsSelected()
        composeRule.onNode(hasText("Data SMS") and isSelectable()).assertIsNotSelected().performClick()
        assertEquals(listOf(InboxKind.Data), toggled)
    }

    @Test
    fun emptyInboxExplainsWhatShowsUp() {
        composeRule.setContent { HomingTestContent { InboxScreen(InboxSamples.empty, InboxActions()) } }
        composeRule.onNodeWithText("Nothing received yet").assertIsDisplayed()
    }

    @Test
    fun attachmentActionsReportIds() {
        val opened = mutableListOf<String>()
        val shared = mutableListOf<String>()
        composeRule.setContent {
            HomingTestContent {
                InboxDetailScreen(
                    InboxSamples.mmsDetail,
                    InboxDetailActions(onOpenAttachment = { opened += it }, onShareAttachment = { shared += it })
                )
            }
        }
        composeRule.onNodeWithText("IMG_2041.jpg").performClick()
        composeRule.onNodeWithContentDescription("Share contact.vcf").performClick()
        assertEquals(listOf("att_1"), opened)
        assertEquals(listOf("att_2"), shared)
    }
}
