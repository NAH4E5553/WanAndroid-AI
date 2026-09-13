package com.personal.wanandroid.feature.profile.viewmodel

import com.personal.wanandroid.core.data.repository.CollectionRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.CollectionItem
import com.personal.wanandroid.core.model.CollectionSnapshot
import com.personal.wanandroid.core.model.CollectionStatus
import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun before() = Dispatchers.setMain(dispatcher)

    @After fun after() = Dispatchers.resetMain()
    private val item =
        CollectionItem(
            CollectionTarget(42, 900),
            Article(42, "Fixture", "https://example.invalid", "", "", "", "", "", true)
        )
    private inner class Repository : CollectionRepository {
        override val state = MutableStateFlow(CollectionSnapshot(3))
        var requests = mutableListOf<Pair<Long, Int>>()
        var items = listOf(item)
        var failure: DataError? = null
        var writes = 0
        override fun current() = state.value
        override suspend fun page(
            generation: Long,
            page: Int
        ): DataResult<PageResult<CollectionItem>> {
            requests.add(generation to page)
            return DataResult.Success(PageResult(items, if (page == 0) 1 else null))
        }
        override suspend fun reconcile(generation: Long, target: CollectionTarget) =
            DataResult.Success(Unit)
        override suspend fun setCollected(
            generation: Long,
            target: CollectionTarget,
            collected: Boolean
        ): DataResult<Unit> {
            writes++
            if (failure == null) items = emptyList()
            state.value = state.value.copy(revision = state.value.revision + 1)
            return failure?.let { DataResult.Failure(it) } ?: DataResult.Success(Unit)
        }
    }

    @Test fun mutationRefreshesFromZeroAndErrorsSurviveRefreshNotification() = runTest(dispatcher) {
        val repo = Repository()
        val vm = CollectionsViewModel(repo)
        runCurrent()
        vm.loadMore()
        runCurrent()
        repo.failure = DataError.NETWORK
        vm.remove(item, 3)
        runCurrent()
        assertEquals(listOf(3L to 0, 3L to 1, 3L to 0), repo.requests)
        assertEquals(DataError.NETWORK, vm.uiState.value.error)
    }

    @Test fun guestClearsListAndStaleClickCannotWriteForAnotherAccount() = runTest(dispatcher) {
        val repo = Repository()
        val vm = CollectionsViewModel(repo)
        runCurrent()
        assertEquals(listOf(item), vm.uiState.value.page.items)
        repo.state.value = CollectionSnapshot()
        runCurrent()
        assertTrue(vm.uiState.value.page.items.isEmpty())
        assertNull(vm.uiState.value.collections.generation)
        repo.state.value = CollectionSnapshot(5)
        vm.remove(item, 3)
        runCurrent()
        assertEquals(0, repo.writes)
        assertTrue(repo.requests.all { it.first in listOf(3L, 5L) })
    }

    @Test fun crossPageRemovalImmediatelyHidesItemThenRefreshesPagination() = runTest(dispatcher) {
        val repo = Repository()
        val vm = CollectionsViewModel(repo)
        runCurrent()
        repo.items = emptyList()
        repo.state.value =
            CollectionSnapshot(3, mapOf(item.target.key to CollectionStatus(false)), 1)
        runCurrent()
        assertTrue(vm.uiState.value.page.items.isEmpty())
        assertEquals(listOf(3L to 0, 3L to 0), repo.requests)
    }
}
