package com.homingsms.app.feature.messages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.homingsms.app.core.testing.HomingTestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessagesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun stateFilterChipTogglesSelectedState() {
        val toggled = mutableListOf<MessageState>()
        composeRule.setContent {
            HomingTestContent { MessagesScreen(MessagesSamples.list, MessagesActions(onToggleState = { toggled += it })) }
        }
        composeRule.onNodeWithText("State").performClick()
        composeRule.onNode(hasText("Failed") and isToggleable()).assertIsOff().performClick()
        assertEquals(listOf(MessageState.Failed), toggled)
    }

    @Test
    fun selectedFilterShowsInChipAndMenu() {
        val state = MessagesSamples.list.copy(filters = MessageFilters(states = setOf(MessageState.Delivered)))
        composeRule.setContent { HomingTestContent { MessagesScreen(state, MessagesActions()) } }
        composeRule.onNode(hasText("Delivered") and isSelectable()).assertIsSelected()
        composeRule.onNodeWithText("Clear filters").assertIsDisplayed()
    }

    @Test
    fun typeAndSimFiltersReportSelections() {
        val types = mutableListOf<MessageType>()
        val sims = mutableListOf<Int>()
        composeRule.setContent {
            HomingTestContent {
                MessagesScreen(
                    MessagesSamples.list,
                    MessagesActions(onToggleType = { types += it }, onToggleSim = { sims += it })
                )
            }
        }
        composeRule.onNodeWithText("Type").performClick()
        composeRule.onNode(hasText("MMS") and isToggleable()).performClick()
        composeRule.onNodeWithText("SIM").performClick()
        composeRule.onNode(hasText("SIM 2 · EE") and isToggleable()).performClick()
        assertEquals(listOf(MessageType.Mms), types)
        assertEquals(listOf(1), sims)
    }

    @Test
    fun selectedMenuItemIsChecked() {
        val state = MessagesSamples.list.copy(filters = MessageFilters(states = setOf(MessageState.Expired)))
        composeRule.setContent { HomingTestContent { MessagesScreen(state, MessagesActions()) } }
        composeRule.onNode(hasText("Expired") and isSelectable()).performClick()
        composeRule.onNode(hasText("Expired") and isToggleable()).assertIsOn()
    }

    @Test
    fun clearOnNoMatchesResetsQueryAndFilters() {
        var query: String? = null
        var cleared = false
        composeRule.setContent {
            HomingTestContent {
                MessagesScreen(
                    MessagesSamples.filteredEmpty,
                    MessagesActions(onQueryChange = { query = it }, onClearFilters = { cleared = true })
                )
            }
        }
        composeRule.onNodeWithText("No matching messages").assertIsDisplayed()
        composeRule.onAllNodes(hasText("Clear filters"))[1].performClick()
        assertEquals("", query)
        assertTrue(cleared)
    }

    @Test
    fun searchReportsQuery() {
        val queries = mutableListOf<String>()
        composeRule.setContent {
            HomingTestContent { MessagesScreen(MessagesSamples.list, MessagesActions(onQueryChange = { queries += it })) }
        }
        composeRule.onNodeWithText("Search by number or text").performTextInput("+44")
        assertEquals("+44", queries.last())
    }

    @Test
    fun rowClickOpensMessage() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            HomingTestContent { MessagesScreen(MessagesSamples.list, MessagesActions(onMessageClick = { opened += it })) }
        }
        composeRule.onNodeWithText("+44 7700 900982").performClick()
        assertEquals(listOf("msg_2"), opened)
    }
}
