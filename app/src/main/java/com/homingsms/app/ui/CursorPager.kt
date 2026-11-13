package com.homingsms.app.ui

import com.homingsms.app.core.designsystem.component.AppendState
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InitialLoad { Loading, Error, Ready }

data class PagedSnapshot<T>(
    val items: List<T> = emptyList(),
    val initial: InitialLoad = InitialLoad.Loading,
    val append: AppendState = AppendState.Idle,
    val nextCursor: String? = null
)

class CursorPager<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val load: suspend (cursor: Cursor?, limit: Int) -> Page<T>
) {
    private val snapshot = MutableStateFlow(PagedSnapshot<T>())
    private var job: Job? = null

    val state: StateFlow<PagedSnapshot<T>> = snapshot.asStateFlow()

    fun refresh(keepLoaded: Boolean = false) {
        val current = snapshot.value
        val silent = keepLoaded && current.initial == InitialLoad.Ready
        val limit = if (silent) maxOf(pageSize, current.items.size) else pageSize
        if (!silent) snapshot.value = PagedSnapshot(initial = InitialLoad.Loading)
        job?.cancel()
        job = scope.launch {
            attempt { load(null, limit) }
                .onSuccess { page -> snapshot.value = PagedSnapshot(page.items, InitialLoad.Ready, appendFor(page), page.nextCursor) }
                .onFailure { if (!silent) snapshot.value = PagedSnapshot(initial = InitialLoad.Error) }
        }
    }

    fun loadMore() {
        val current = snapshot.value
        val cursor = current.nextCursor?.let(Cursor::decode) ?: return
        if (current.initial != InitialLoad.Ready || current.append == AppendState.Loading || job?.isActive == true) return
        snapshot.update { it.copy(append = AppendState.Loading) }
        job = scope.launch {
            attempt { load(cursor, pageSize) }
                .onSuccess { page ->
                    snapshot.update { it.copy(items = it.items + page.items, append = appendFor(page), nextCursor = page.nextCursor) }
                }
                .onFailure { snapshot.update { it.copy(append = AppendState.Error) } }
        }
    }

    private fun appendFor(page: Page<T>): AppendState = if (page.nextCursor == null) AppendState.EndReached else AppendState.Idle

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
    }
}

suspend fun <T> attempt(block: suspend () -> T): Result<T> {
    val result = runCatching { block() }
    val error = result.exceptionOrNull()
    if (error is CancellationException) throw error
    return result
}
