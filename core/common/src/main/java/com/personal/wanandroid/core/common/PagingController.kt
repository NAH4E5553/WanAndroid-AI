package com.personal.wanandroid.core.common

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
 * This controller supports a fixed request context only. No account/category switching contract.
 */
class PagingController<T : Any>(
    private val scope: CoroutineScope,
    private val initialPage: Int,
    private val keyOf: (T) -> Any,
    private val requestPage: suspend (Int) -> DataResult<PageResult<T>>
) {
    private enum class Operation { INITIAL, REFRESH, AUTO, CONTINUE }
    private data class Request(val id: Long, val page: Int, val operation: Operation)
    private val mutableState = MutableStateFlow(PagedUiState<T>())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    private var active: Request? = null
    private var failed: Request? = null
    private var started = false
    private val automaticPages = mutableSetOf<Int>()

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
        val snapshot = state.value
        val page = snapshot.nextPage ?: return
        if (active != null || !snapshot.canAutoLoadMore || !automaticPages.add(page)) return
        launch(page, Operation.AUTO)
    }

    fun continueAfterPause() {
        val snapshot = state.value
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
            if (state.value.items.isEmpty()) Operation.INITIAL else Operation.REFRESH
        )
    }

    private fun launch(page: Int, operation: Operation) {
        val request = Request(++generation, page, operation)
        active = request
        failed = null
        val previous = state.value
        mutableState.value = when (operation) {
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
        job = scope.launch request@{
            val result = try {
                requestPage(page)
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

    private fun finishFailure(request: Request, error: DataError) {
        active = null
        failed = request
        val failure = LoadState.Failure(error)
        mutableState.value = when (request.operation) {
            Operation.INITIAL -> state.value.copy(initial = failure)
            Operation.REFRESH -> state.value.copy(refresh = failure)
            Operation.AUTO, Operation.CONTINUE -> state.value.copy(append = failure)
        }
    }

    private fun finishSuccess(request: Request, page: PageResult<T>) {
        val current = state.value
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
        mutableState.value = PagedUiState(
            items = merged.values.toList(),
            nextPage = page.nextPage,
            consecutiveNoProgress = count,
            autoLoadPaused = paused,
            datasetGeneration = current.datasetGeneration + if (replacing) 1 else 0
        )
    }
}
