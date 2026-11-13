package com.homingsms.app.ui

import app.cash.turbine.test
import com.homingsms.app.core.designsystem.component.AppendState
import com.homingsms.app.feature.messages.LoadState
import com.homingsms.app.feature.messages.MessageNotice
import com.homingsms.app.feature.messages.MessageSource
import com.homingsms.app.feature.messages.MessageState as UiState
import com.homingsms.app.feature.messages.MessageType as UiType
import com.homingsms.app.ui.data.StoredMessage
import com.homingsms.app.ui.messages.MessageDetailViewModel
import com.homingsms.app.ui.messages.MessagesViewModel
import com.homingsms.app.ui.navigation.MessageDetailRoute
import com.homingsms.core.model.MessageError
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.SimStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val status = FakeAppStatus()
    private val store = FakeMessageStore()

    private fun seed(count: Int) {
        store.records.value = (1..count).map { index ->
            StoredMessage(message("msg_$index", createdAt = index.toLong(), state = MessageState.Delivered), local = true)
        }
    }

    @Test
    fun loadsFirstPageAndAppendsWithCursor() = runTest {
        seed(45)
        status.sims.value = listOf(SimStats(1, "Second", null, null, 0, null, true))
        val viewModel = MessagesViewModel(status, store)
        advanceUntilIdle()

        val first = viewModel.state.value
        assertEquals(LoadState.Ready, first.loadState)
        assertEquals(30, first.items.size)
        assertEquals("msg_45", first.items.first().id)
        assertEquals(UiState.Delivered, first.items.first().state)
        assertEquals(UiType.Sms, first.items.first().type)
        assertEquals(1, first.availableSims.single().slot)
        assertEquals(AppendState.Idle, first.appendState)

        viewModel.actions.onLoadMore()
        advanceUntilIdle()
        assertEquals(45, viewModel.state.value.items.size)
        assertEquals(AppendState.EndReached, viewModel.state.value.appendState)
    }

    @Test
    fun filtersAndSearchReachTheStore() = runTest {
        seed(2)
        val viewModel = MessagesViewModel(status, store)
        advanceUntilIdle()

        viewModel.actions.onToggleState(UiState.Failed)
        viewModel.actions.onToggleType(UiType.Mms)
        viewModel.actions.onToggleSim(1)
        advanceUntilIdle()
        val filter = store.filters.last()
        assertEquals(setOf(MessageState.Failed), filter.states)
        assertEquals(setOf(MessageType.Mms), filter.types)
        assertEquals(setOf(1), filter.simSlots)

        viewModel.actions.onQueryChange("+1555")
        advanceTimeBy(100)
        assertNull(store.filters.last().search)
        advanceUntilIdle()
        assertEquals("+1555", store.filters.last().search)

        viewModel.actions.onClearFilters()
        advanceUntilIdle()
        assertTrue(store.filters.last().states.isEmpty())
        assertFalse(viewModel.state.value.filters.isActive)
    }

    @Test
    fun errorsAreRetryableAndChangesRefresh() = runTest {
        store.failList = true
        val viewModel = MessagesViewModel(status, store)
        advanceUntilIdle()
        assertEquals(LoadState.Error, viewModel.state.value.loadState)

        store.failList = false
        seed(1)
        viewModel.actions.onRetryLoad()
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.items.size)

        seed(2)
        store.changes.tryEmit(Unit)
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.items.size)
    }

    @Test
    fun clickOpensDetail() = runTest {
        val viewModel = MessagesViewModel(status, store)
        viewModel.effects.test {
            viewModel.actions.onMessageClick("msg_1")
            assertEquals(UiEffect.Navigate(MessageDetailRoute("msg_1")), awaitItem())
        }
    }

    @Test
    fun detailMapsRecordAndAllowsRetryForFailedLocalMessages() = runTest {
        store.records.value = listOf(
            StoredMessage(message("msg_f", state = MessageState.Failed, error = MessageError("SEND_FAILED", "No service")), local = true)
        )
        val viewModel = MessageDetailViewModel("msg_f", store)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LoadState.Ready, state.loadState)
        assertEquals(UiState.Failed, state.message?.state)
        assertEquals(MessageSource.Local, state.message?.source)
        assertEquals("No service", state.message?.error?.message)
        assertEquals(listOf(UiState.Pending, UiState.Failed), state.message?.events?.map { it.state })
        assertTrue(state.canRetry)
        assertFalse(state.canCancel)

        viewModel.actions.onRetry()
        advanceUntilIdle()
        assertEquals(listOf("msg_f"), store.retried)
        assertEquals(MessageNotice.RetryQueued, viewModel.state.value.notice)
        viewModel.actions.onNoticeShown()
        assertNull(viewModel.state.value.notice)
    }

    @Test
    fun detailCancelsPendingAndBlocksCloudActions() = runTest {
        store.records.value = listOf(
            StoredMessage(message("msg_p", state = MessageState.Pending), local = true),
            StoredMessage(message("msg_c", state = MessageState.Failed), local = false)
        )
        val pending = MessageDetailViewModel("msg_p", store)
        val cloud = MessageDetailViewModel("msg_c", store)
        advanceUntilIdle()

        assertTrue(pending.state.value.canCancel)
        pending.actions.onCancel()
        advanceUntilIdle()
        assertEquals(listOf("msg_p"), store.cancelled)
        assertEquals(MessageNotice.Cancelled, pending.state.value.notice)

        assertEquals(MessageSource.Cloud, cloud.state.value.message?.source)
        assertFalse(cloud.state.value.canRetry)
        assertFalse(cloud.state.value.canCancel)
    }

    @Test
    fun detailShowsErrorWhenMissing() = runTest {
        val viewModel = MessageDetailViewModel("missing", store)
        advanceUntilIdle()
        assertEquals(LoadState.Error, viewModel.state.value.loadState)
    }
}
