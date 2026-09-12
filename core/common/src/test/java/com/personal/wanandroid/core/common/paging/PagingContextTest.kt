package com.personal.wanandroid.core.common.paging

import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagingContextTest {
    @Test fun resetClearsOldDataAndCursorAndRejectsLateAppend() = runTest {
        val requests = ArrayDeque<ContextRequest>()
        val pager = PagingController(backgroundScope, 0, { it: Int -> it }, "") { query, page ->
            suspendCoroutine { requests += ContextRequest(query, page, it) }
        }
        pager.reset("first")
        runCurrent()
        requests.removeFirst().complete(listOf(1), 1)
        runCurrent()
        pager.loadMore()
        runCurrent()
        val oldAppend = requests.removeFirst()
        pager.reset("second")
        assertEquals("second", pager.state.value.context)
        assertEquals(2L, pager.state.value.contextGeneration)
        assertTrue(pager.state.value.page.items.isEmpty())
        assertTrue(pager.state.value.page.isInitialLoading)
        assertNull(pager.state.value.page.nextPage)
        runCurrent()
        val next = requests.removeFirst()
        assertEquals("second", next.query)
        assertEquals(0, next.page)
        next.complete(listOf(2), 1)
        runCurrent()
        oldAppend.complete(listOf(99), null)
        runCurrent()
        assertEquals(listOf(2), pager.state.value.page.items)
        pager.loadMore()
        runCurrent()
        assertEquals("second", requests.first().query)
        assertEquals(1, requests.first().page)
        requests.removeFirst().complete(emptyList(), null)
        runCurrent()
    }

    @Test fun resetCancelsOldJobAndDoesNotCarryFailureOrAutomaticHistory() = runTest {
        var cancelled = false
        val requested = mutableListOf<Pair<String, Int>>()
        val pager = PagingController(backgroundScope, 0, { it: Int -> it }, "") { query, page ->
            requested += query to page
            when (query) {
                "slow" -> suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { cancelled = true }
                }

                "error" -> DataResult.Failure(DataError.NETWORK)

                else -> DataResult.Success(PageResult(listOf(page), if (page == 0) 1 else null))
            }
        }
        pager.reset("slow")
        runCurrent()
        pager.reset("error")
        runCurrent()
        assertTrue(cancelled)
        assertEquals(DataError.NETWORK, pager.state.value.page.initialError)
        pager.reset("ready")
        runCurrent()
        assertNull(pager.state.value.page.initialError)
        pager.loadMore()
        runCurrent()
        pager.reset("ready")
        runCurrent()
        pager.loadMore()
        runCurrent()
        assertEquals(2, requested.count { it == ("ready" to 1) })
        assertFalse(pager.state.value.page.canLoadMore)
    }

    @Test fun resetBeforeDispatchUsesOnlyCapturedLatestContext() = runTest {
        val requested = mutableListOf<String>()
        val pager = PagingController(backgroundScope, 0, { it: Int -> it }, "") { query, _ ->
            requested += query
            DataResult.Success(PageResult(emptyList<Int>(), null))
        }
        pager.reset("first")
        pager.reset("second")
        runCurrent()
        assertEquals(listOf("second"), requested)
        assertEquals("second", pager.state.value.context)
    }
}
private data class ContextRequest(
    val query: String,
    val page: Int,
    val continuation: Continuation<DataResult<PageResult<Int>>>
) {
    fun complete(items: List<Int>, next: Int?) =
        continuation.resume(DataResult.Success(PageResult(items, next)))
}
