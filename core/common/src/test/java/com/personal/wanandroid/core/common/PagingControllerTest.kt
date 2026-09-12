package com.personal.wanandroid.core.common

import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagingControllerTest {
    @Test fun ownerCancellationRejectsEvenNonCooperativeCompletion() = runTest {
        val owner = Job(backgroundScope.coroutineContext[Job])
        val h = Harness(CoroutineScope(backgroundScope.coroutineContext + owner))
        h.controller.startInitialLoad()
        runCurrent()
        owner.cancel()
        h.complete(listOf(Item(99)), null)
        runCurrent()
        assertTrue(h.state.items.isEmpty())
        assertEquals(0L, h.state.datasetGeneration)
    }

    @Test fun explicitStartIsIdempotentAndPreservesInitialPage() = runTest {
        for (first in listOf(0, 1)) {
            val h = Harness(backgroundScope, first)
            assertTrue(h.requests.isEmpty())
            h.controller.startInitialLoad()
            h.controller.startInitialLoad()
            runCurrent()
            assertEquals(first, h.requests.single().page)
            h.complete(emptyList(), null)
            runCurrent()
            assertEquals(LoadState.Idle, h.state.initial)
            assertFalse(h.state.canLoadMore)
        }
    }

    @Test fun appendUpdatesExistingValuesInPlaceAndIgnoresDuplicateTriggers() = runTest {
        val h = Harness(backgroundScope)
        h.controller.startInitialLoad()
        runCurrent()
        h.complete(listOf(Item(1, "old"), Item(2)), 1)
        runCurrent()
        h.controller.loadMore()
        h.controller.loadMore()
        runCurrent()
        assertEquals(1, h.requests.size)
        assertEquals(1, h.state.nextPage)
        h.complete(listOf(Item(1, "new"), Item(3)), null)
        runCurrent()
        assertEquals(listOf(Item(1, "new"), Item(2), Item(3)), h.state.items)
        h.controller.loadMore()
        runCurrent()
        assertTrue(h.requests.isEmpty())
    }

    @Test fun failedAppendRequiresExplicitRetryOfSamePage() = runTest {
        val h = Harness(backgroundScope)
        h.controller.startInitialLoad()
        runCurrent()
        h.complete(listOf(Item(1)), 1)
        runCurrent()
        h.controller.loadMore()
        runCurrent()
        h.fail()
        runCurrent()
        h.controller.loadMore()
        runCurrent()
        assertTrue(h.requests.isEmpty())
        h.controller.retryAppend()
        runCurrent()
        assertEquals(1, h.requests.single().page)
        h.complete(listOf(Item(2)), null)
        runCurrent()
        assertNull(h.state.loadMoreError)
    }

    @Test fun refreshInvalidatesNonCooperativeAppendAndAllowsSameCursorInNewDataset() = runTest {
        val h = Harness(backgroundScope)
        h.controller.startInitialLoad()
        runCurrent()
        h.complete(listOf(Item(1)), 1)
        runCurrent()
        h.controller.loadMore()
        runCurrent()
        val old = h.requests.removeFirst()
        h.controller.refresh()
        runCurrent()
        assertEquals(LoadState.Idle, h.state.append)
        h.complete(listOf(Item(1)), 1)
        runCurrent()
        assertEquals(2L, h.state.datasetGeneration)
        old.continuation.resume(DataResult.Success(PageResult(listOf(Item(99)), null)))
        runCurrent()
        assertEquals(listOf(Item(1)), h.state.items)
        h.controller.loadMore()
        runCurrent()
        assertEquals(1, h.requests.single().page)
        h.complete(listOf(Item(2)), null)
        runCurrent()
    }

    @Test fun refreshFailurePreservesDataAndRetriesRefreshOnly() = runTest {
        val h = Harness(backgroundScope)
        h.controller.startInitialLoad()
        runCurrent()
        h.complete(listOf(Item(1)), 1)
        runCurrent()
        h.controller.refresh()
        runCurrent()
        h.fail()
        runCurrent()
        assertEquals(listOf(Item(1)), h.state.items)
        assertEquals(1, h.state.nextPage)
        assertFalse(h.state.canAutoLoadMore)
        h.controller.retryAppend()
        runCurrent()
        assertTrue(h.requests.isEmpty())
        h.controller.retryRefresh()
        runCurrent()
        assertEquals(0, h.requests.single().page)
        h.complete(emptyList(), null)
        runCurrent()
        assertNull(h.state.refreshError)
        assertTrue(h.state.items.isEmpty())
    }

    @Test fun emptyRefreshClearsInitialFailureAndCanDiscoverNextPage() = runTest {
        val h = Harness(backgroundScope)
        h.controller.startInitialLoad()
        runCurrent()
        h.fail()
        runCurrent()
        h.controller.refresh()
        runCurrent()
        assertNull(h.state.initialError)
        assertTrue(h.state.isInitialLoading)
        h.complete(emptyList(), 1)
        runCurrent()
        assertTrue(h.state.canAutoLoadMore)
        h.controller.loadMore()
        runCurrent()
        h.complete(listOf(Item(1)), null)
        runCurrent()
    }

    @Test fun noProgressPausesAndManualRetryPreservesManualSource() = runTest {
        val h = Harness(backgroundScope)
        h.controller.startInitialLoad()
        runCurrent()
        h.complete(listOf(Item(1)), 1)
        runCurrent()
        repeat(2) { index ->
            h.controller.loadMore()
            runCurrent()
            h.complete(listOf(Item(1)), index + 2)
            runCurrent()
        }
        assertTrue(h.state.autoLoadPaused)
        h.controller.loadMore()
        runCurrent()
        assertTrue(h.requests.isEmpty())
        h.controller.continueAfterPause()
        runCurrent()
        h.fail()
        runCurrent()
        h.controller.loadMore()
        runCurrent()
        assertTrue(h.requests.isEmpty())
        h.controller.retryAppend()
        runCurrent()
        assertEquals(3, h.requests.single().page)
        h.complete(emptyList(), 4)
        runCurrent()
        assertTrue(h.state.autoLoadPaused)
        h.controller.continueAfterPause()
        runCurrent()
        h.complete(listOf(Item(2)), 5)
        runCurrent()
        assertFalse(h.state.autoLoadPaused)
        assertEquals(0, h.state.consecutiveNoProgress)
    }

    @Test fun endTakesPriorityOverPauseAndInvalidCursorDoesNotCommitItems() = runTest {
        val h = Harness(backgroundScope)
        h.controller.startInitialLoad()
        runCurrent()
        h.complete(listOf(Item(1)), 1)
        runCurrent()
        h.controller.loadMore()
        runCurrent()
        h.complete(listOf(Item(99)), 1)
        runCurrent()
        assertEquals(listOf(Item(1)), h.state.items)
        assertEquals(DataError.INVALID_RESPONSE, h.state.loadMoreError)
        h.controller.retryAppend()
        runCurrent()
        h.complete(emptyList(), null)
        runCurrent()
        assertFalse(h.state.autoLoadPaused)
        assertNull(h.state.nextPage)
    }
}

private data class Item(val id: Int, val title: String = "fixture")
private data class Pending(
    val page: Int,
    val continuation: Continuation<DataResult<PageResult<Item>>>
)
private class Harness(scope: CoroutineScope, first: Int = 0) {
    val requests = ArrayDeque<Pending>()
    val controller = PagingController(scope, first, Item::id) { page ->
        suspendCoroutine { requests.addLast(Pending(page, it)) }
    }
    val state get() = controller.state.value
    fun complete(items: List<Item>, next: Int?) {
        requests.removeFirst().continuation.resume(DataResult.Success(PageResult(items, next)))
    }
    fun fail() {
        requests.removeFirst().continuation.resume(DataResult.Failure(DataError.NETWORK))
    }
}
