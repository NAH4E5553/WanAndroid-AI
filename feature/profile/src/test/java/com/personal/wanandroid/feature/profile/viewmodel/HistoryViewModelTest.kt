package com.personal.wanandroid.feature.profile.viewmodel

import com.personal.wanandroid.core.data.repository.ReadingHistoryRepository
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun before() = Dispatchers.setMain(dispatcher)

    @After fun after() = Dispatchers.resetMain()
    private class Repository : ReadingHistoryRepository {
        override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var rows = listOf(ReadingHistory("https://fixture.invalid", 1, "Fixture", 1))
        var calls = mutableListOf<Int>()
        var fail = false
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun page(page: Int): DataResult<PageResult<ReadingHistory>> {
            calls.add(page)
            val snapshot = rows
            gate?.await()
            return if (fail) {
                DataResult.Failure(DataError.STORAGE)
            } else {
                DataResult.Success(
                    PageResult(
                        snapshot,
                        if (page ==
                            0 &&
                            snapshot.isNotEmpty()
                        ) {
                            1
                        } else {
                            null
                        }
                    )
                )
            }
        }
        override suspend fun record(url: String, articleId: Long?, title: String) =
            DataResult.Success(Unit)
        override suspend fun delete(url: String): DataResult<Unit> {
            writes++
            gate?.await()
            if (fail) return DataResult.Failure(DataError.STORAGE)
            rows = rows.filterNot { it.url == url }
            return DataResult.Success(Unit)
        }
        override suspend fun clear() = delete("https://fixture.invalid")
    }

    @Test fun localDataLoadsWithoutLoginAndDeleteRefreshesFromZero() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistoryViewModel(repo)
        runCurrent()
        vm.loadMore()
        runCurrent()
        vm.delete(repo.rows.single().url)
        runCurrent()
        assertEquals(listOf(0, 1, 0), repo.calls)
        assertTrue(vm.uiState.value.page.items.isEmpty())
    }

    @Test fun failedWriteKeepsRowsAndDuplicateWritesDoNotQueue() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistoryViewModel(repo)
        runCurrent()
        repo.fail = true
        repo.gate = CompletableDeferred()
        vm.clear()
        vm.clear()
        runCurrent()
        assertEquals(1, repo.writes)
        repo.gate!!.complete(Unit)
        runCurrent()
        assertEquals(DataError.STORAGE, vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.page.items.size)
        assertFalse(vm.uiState.value.busy)
    }

    @Test fun invalidationCancelsOldPageAndReloadsCurrentDataset() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistoryViewModel(repo)
        runCurrent()
        repo.gate = CompletableDeferred()
        vm.loadMore()
        runCurrent()
        repo.rows = emptyList()
        repo.gate = null
        repo.changes.emit(Unit)
        runCurrent()
        assertTrue(vm.uiState.value.page.items.isEmpty())
        assertNull(vm.uiState.value.page.nextPage)
    }

    @Test fun failedReadRetriesSuccessfully() = runTest(dispatcher) {
        val repo = Repository()
        repo.fail = true
        val vm = HistoryViewModel(repo)
        runCurrent()
        assertEquals(DataError.STORAGE, vm.uiState.value.page.initialError)
        repo.fail = false
        vm.retryInitial()
        runCurrent()
        assertEquals(1, vm.uiState.value.page.items.size)
    }
}
