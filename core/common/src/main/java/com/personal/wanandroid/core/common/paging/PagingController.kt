package com.personal.wanandroid.core.common.paging

import com.personal.wanandroid.core.common.base.state.LoadState
import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.common.base.state.PagingState
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Adapted from CoolMallKotlin BaseNetWorkListViewModel (cf5029b).
 * A single owner serializes calls on Main; requests use its scope and immutable page snapshots.
 * Context values must be immutable. A reset clears data and invalidates every older request.
 */
class PagingController<T : Any, C : Any>(
    private val scope: CoroutineScope,
    private val initialPage: Int,
    private val keyOf: (T) -> Any,
    initialContext: C,
    private val requestPage: suspend (C, Int) -> DataResult<PageResult<T>>
) {
    private enum class Operation { INITIAL, REFRESH, AUTO, CONTINUE }
    private data class Request(val id: Long, val page: Int, val operation: Operation)
    private val mutableState = MutableStateFlow(PagingState<T, C>(initialContext))
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    private var active: Request? = null
    private var failed: Request? = null
    private var suspendedRequest: Request? = null
    private var started = false
    private val automaticPages = mutableSetOf<Int>()

    /** Atomically publish the new context with an empty page before starting its first request. */
    fun reset(context: C) {
        active = null
        job?.cancel()
        failed = null
        automaticPages.clear()
        started = true
        val previous = state.value
        mutableState.value = PagingState(
            context = context,
            page = PagedUiState(
                initial = LoadState.Loading,
                datasetGeneration = previous.page.datasetGeneration + 1
            ),
            contextGeneration = previous.contextGeneration + 1
        )
        launch(initialPage, Operation.INITIAL)
    }

    /** Keep this list's data/cursor while its category is inactive; reject late completions. */
    fun pauseLoading() {
        val request = active ?: return
        suspendedRequest = request
        active = null
        job?.cancel()
    }

    /** Resume the interrupted operation at the same cursor, without resetting cached content. */
    fun resumeLoading() {
        val request = suspendedRequest ?: return
        launch(request.page, request.operation)
    }

    fun startInitialLoad() {
        if (started) return
        started = true
        replace()
    }

    fun refresh() {
        started = true
        replace()
    }

    fun retryInitial() = retry(Operation.INITIAL)
    fun retryRefresh() = retry(Operation.REFRESH)
    fun retryAppend() {
        val failure = failed ?: return
        if (failure.operation == Operation.AUTO || failure.operation == Operation.CONTINUE) {
            if (active == null) launch(failure.page, failure.operation)
        }
    }

    fun loadMore() {
        val snapshot = state.value.page
        val page = snapshot.nextPage ?: return
        if (active != null || !snapshot.canAutoLoadMore || !automaticPages.add(page)) return
        launch(page, Operation.AUTO)
    }

    fun continueAfterPause() {
        val snapshot = state.value.page
        val page = snapshot.nextPage ?: return
        if (active != null || !snapshot.autoLoadPaused || snapshot.loadMoreError != null) return
        launch(page, Operation.CONTINUE)
    }

    private fun retry(operation: Operation) {
        if (active != null || failed?.operation != operation) return
        replace()
    }

    private fun replace() {
        // Invalidate first, including non-cooperative continuations and cancellation cleanup.
        generation++
        active = null
        job?.cancel()
        launch(
            initialPage,
            if (state.value.page.items.isEmpty()) Operation.INITIAL else Operation.REFRESH
        )
    }

    private fun launch(page: Int, operation: Operation) {
        suspendedRequest = null
        val request = Request(++generation, page, operation)
        active = request
        failed = null
        val previous = state.value.page
        publish(
            when (operation) {
                Operation.INITIAL -> PagedUiState(
                    initial = LoadState.Loading,
                    datasetGeneration = previous.datasetGeneration
                )

                Operation.REFRESH -> previous.copy(
                    initial = LoadState.Idle,
                    refresh = LoadState.Loading,
                    append = LoadState.Idle,
                    consecutiveNoProgress = 0,
                    autoLoadPaused = false
                )

                Operation.AUTO, Operation.CONTINUE -> previous.copy(
                    initial = LoadState.Idle,
                    refresh = LoadState.Idle,
                    append = LoadState.Loading,
                    autoLoadPaused = false
                )
            }
        )
        val requestContext = state.value.context
        job = scope.launch request@{
            val result = try {
                requestPage(requestContext, page)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                DataResult.Failure(DataError.INVALID_RESPONSE)
            }
            if (active != request ||
                !scope.coroutineContext[Job].let { it == null || it.isActive }
            ) {
                return@request
            }
            when (result) {
                is DataResult.Failure -> finishFailure(request, result.reason)

                is DataResult.Success -> {
                    val next = result.value.nextPage
                    if (next != null && next <= page) {
                        finishFailure(request, DataError.INVALID_RESPONSE)
                    } else {
                        finishSuccess(request, result.value)
                    }
                }
            }
        }
    }

    private fun publish(page: PagedUiState<T>) {
        mutableState.value = state.value.copy(page = page)
    }

    private fun finishFailure(request: Request, error: DataError) {
        active = null
        failed = request
        val failure = LoadState.Failure(error)
        publish(
            when (request.operation) {
                Operation.INITIAL -> state.value.page.copy(initial = failure)
                Operation.REFRESH -> state.value.page.copy(refresh = failure)
                Operation.AUTO, Operation.CONTINUE -> state.value.page.copy(append = failure)
            }
        )
    }

    private fun finishSuccess(request: Request, page: PageResult<T>) {
        val current = state.value.page
        val replacing =
            request.operation == Operation.INITIAL || request.operation == Operation.REFRESH
        val merged = linkedMapOf<Any, T>()
        if (!replacing) current.items.forEach { merged[keyOf(it)] = it }
        val oldSize = merged.size
        page.items.forEach { merged[keyOf(it)] = it }
        val progress = merged.size > oldSize
        val count = if (replacing || progress ||
            page.nextPage == null
        ) {
            0
        } else {
            current.consecutiveNoProgress + 1
        }
        val paused = page.nextPage != null && !replacing && !progress &&
            (count >= 2 || request.operation == Operation.CONTINUE)
        if (replacing) automaticPages.clear()
        active = null
        failed = null
        publish(
            PagedUiState(
                items = merged.values.toList(),
                nextPage = page.nextPage,
                consecutiveNoProgress = count,
                autoLoadPaused = paused,
                datasetGeneration = current.datasetGeneration + if (replacing) 1 else 0
            )
        )
    }
}
