package com.homingsms.app.ui

import app.cash.turbine.test
import com.homingsms.app.feature.inbox.ForwardStatus
import com.homingsms.app.feature.inbox.InboxKind
import com.homingsms.app.feature.inbox.InboxLoadState
import com.homingsms.app.ui.data.CloudUpload
import com.homingsms.app.ui.inbox.InboxDetailViewModel
import com.homingsms.app.ui.inbox.InboxViewModel
import com.homingsms.app.ui.navigation.InboxDetailRoute
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxAttachmentInfo
import com.homingsms.core.model.MessageType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val status = FakeAppStatus()
    private val store = FakeInboxStore()
    private val sharing = FakeFileSharing()

    @Test
    fun mapsItemsAndMarksOptOutReplies() = runTest {
        store.records.value = listOf(inbox("inb_1", text = "stop", receivedAt = 2L), inbox("inb_2", text = "Hello", receivedAt = 1L))
        val viewModel = InboxViewModel(status, store)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(InboxLoadState.Ready, state.loadState)
        assertEquals(listOf("inb_1", "inb_2"), state.items.map { it.id })
        assertEquals("STOP", state.items.first().optOutKeyword)
        assertNull(state.items.last().optOutKeyword)
        assertEquals(InboxKind.Sms, state.items.first().kind)
    }

    @Test
    fun kindAndSimFiltersAndClicks() = runTest {
        val viewModel = InboxViewModel(status, store)
        advanceUntilIdle()
        viewModel.actions.onToggleKind(InboxKind.Mms)
        viewModel.actions.onToggleSim(1)
        advanceUntilIdle()
        assertEquals(setOf(MessageType.Mms), store.filters.last().types)
        assertEquals(setOf(1), store.filters.last().simSlots)

        viewModel.effects.test {
            viewModel.actions.onItemClick("inb_9")
            assertEquals(UiEffect.Navigate(InboxDetailRoute("inb_9")), awaitItem())
        }
    }

    @Test
    fun detailMapsAttachmentsAndForwardState() = runTest {
        val image = InboxAttachmentInfo("att_0", "image/png", "photo.png", 3)
        store.records.value = listOf(inbox("inb_m", type = MessageType.Mms, attachments = listOf(image)))
        store.attachments["att_0"] = InboxAttachmentContent("image/png", "photo.png", byteArrayOf(1, 2, 3))
        status.cloudPairing.value = CloudPairingState.Paired("dev", "acc", "https://x.test", 1L)
        status.settings.value = status.settings.value.copy(cloud = status.settings.value.cloud.copy(enabled = true))
        store.pending = CloudUpload(attempts = 0, lastError = null)

        val viewModel = InboxDetailViewModel("inb_m", status, store, sharing)
        advanceUntilIdle()

        val detail = viewModel.state.value.message
        assertEquals(InboxLoadState.Ready, viewModel.state.value.loadState)
        assertEquals(InboxKind.Mms, detail?.kind)
        assertNotNull(detail?.attachments?.single()?.thumbnail)
        assertEquals(ForwardStatus.Pending, detail?.forwardStatus)

        store.pending = CloudUpload(attempts = 2, lastError = "timeout")
        store.changes.tryEmit(Unit)
        advanceUntilIdle()
        assertEquals(ForwardStatus.Failed, viewModel.state.value.message?.forwardStatus)

        store.pending = null
        store.changes.tryEmit(Unit)
        advanceUntilIdle()
        assertEquals(ForwardStatus.Forwarded, viewModel.state.value.message?.forwardStatus)
    }

    @Test
    fun detailNotForwardedWithoutTargetsOrWhenTypeIsOff() = runTest {
        store.records.value = listOf(inbox("inb_s"))
        val viewModel = InboxDetailViewModel("inb_s", status, store, sharing)
        advanceUntilIdle()
        assertEquals(ForwardStatus.NotForwarded, viewModel.state.value.message?.forwardStatus)
    }

    @Test
    fun attachmentsOpenAndShareThroughFileProvider() = runTest {
        val image = InboxAttachmentInfo("att_0", "image/png", "photo.png", 3)
        store.records.value = listOf(inbox("inb_m", type = MessageType.Mms, attachments = listOf(image)))
        store.attachments["att_0"] = InboxAttachmentContent("image/png", "photo.png", byteArrayOf(1, 2, 3))
        val viewModel = InboxDetailViewModel("inb_m", status, store, sharing)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.actions.onOpenAttachment("att_0")
            assertEquals(UiEffect.System(SystemAction.ViewFile("content://shared/photo.png", "image/png")), awaitItem())
            viewModel.actions.onShareAttachment("att_0")
            assertEquals(UiEffect.System(SystemAction.ShareFile("content://shared/photo.png", "image/png")), awaitItem())
        }
    }

    @Test
    fun detailMissingShowsError() = runTest {
        val viewModel = InboxDetailViewModel("nope", status, store, sharing)
        advanceUntilIdle()
        assertEquals(InboxLoadState.Error, viewModel.state.value.loadState)
    }
}
