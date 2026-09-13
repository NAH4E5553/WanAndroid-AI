package com.personal.wanandroid.feature.article.viewmodel

import com.personal.wanandroid.core.data.repository.CollectionRepository
import com.personal.wanandroid.core.model.CollectionItem
import com.personal.wanandroid.core.model.CollectionSnapshot
import com.personal.wanandroid.core.model.CollectionStatus
import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleCollectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val target = CollectionTarget(articleId = 42)

    @Before fun before() = Dispatchers.setMain(dispatcher)

    @After fun after() = Dispatchers.resetMain()

    private inner class Repository : CollectionRepository {
        override val state = MutableStateFlow(CollectionSnapshot(3))
        var reads = 0
        var writes = 0
        var failure: DataError? = DataError.NETWORK
        var gate: CompletableDeferred<Unit>? = null
        override fun current() = state.value
        override suspend fun articlePage(
            load: suspend () -> DataResult<PageResult<com.personal.wanandroid.core.model.Article>>
        ) = load()
        override suspend fun page(generation: Long, page: Int) =
            DataResult.Success(PageResult<CollectionItem>(emptyList(), null))
        override suspend fun reconcile(
            generation: Long,
            target: CollectionTarget
        ): DataResult<Unit> {
            reads++
            val result = failure
            gate?.await()
            return result?.let { DataResult.Failure(it) } ?: run {
                state.value =
                    CollectionSnapshot(generation, mapOf(target.key to CollectionStatus(false)))
                DataResult.Success(Unit)
            }
        }
        override suspend fun setCollected(
            generation: Long,
            target: CollectionTarget,
            collected: Boolean
        ): DataResult<Unit> {
            writes++
            return failure?.let { DataResult.Failure(it) } ?: run {
                state.value =
                    CollectionSnapshot(generation, mapOf(target.key to CollectionStatus(collected)))
                DataResult.Success(Unit)
            }
        }
    }

    @Test fun initialListValueIsUsedWithoutAnyRequests() = runTest(dispatcher) {
        val repo = Repository()
        repo.state.value = CollectionSnapshot(3, mapOf(target.key to CollectionStatus(false)))
        val vm = ArticleCollectionViewModel(repo)
        runCurrent()
        assertEquals(0, repo.reads)
        assertNull(vm.error.value)
        assertEquals(false, vm.collections.value.status(target).collected)
        assertEquals(0, repo.writes)
    }

    @Test fun collectedListValueIsRemovedWithoutQuerying() = runTest(dispatcher) {
        val repo = Repository().apply {
            failure = null
            state.value = CollectionSnapshot(3, mapOf(target.key to CollectionStatus(true)))
        }
        val vm = ArticleCollectionViewModel(repo)
        vm.toggle(target, 3, true)
        runCurrent()
        assertEquals(0, repo.reads)
        assertEquals(1, repo.writes)
        assertEquals(false, vm.collections.value.status(target).collected)
        assertNull(vm.error.value)
    }

    @Test fun unknownStateIsResolvedAndAppliedWithinOneExplicitAction() = runTest(dispatcher) {
        val repo = Repository()
        val vm = ArticleCollectionViewModel(repo)
        runCurrent()
        vm.toggle(target, 3, false)
        runCurrent()
        assertEquals(DataError.NETWORK, vm.error.value)
        assertEquals(1, repo.reads)
        assertEquals(0, repo.writes)
        repo.failure = null
        vm.toggle(target, 3, false)
        runCurrent()
        assertNull(vm.error.value)
        assertEquals(true, vm.collections.value.status(target).collected)
        assertEquals(1, repo.writes)
    }

    @Test fun explicitKnownStatusWriteStillReportsFailure() = runTest(dispatcher) {
        val repo = Repository()
        repo.state.value = CollectionSnapshot(3, mapOf(target.key to CollectionStatus(false)))
        val vm = ArticleCollectionViewModel(repo)
        vm.toggle(target, 3, false)
        runCurrent()
        assertEquals(0, repo.reads)
        assertEquals(1, repo.writes)
        assertEquals(DataError.NETWORK, vm.error.value)
    }

    @Test fun uncertainStateIsNotChangedWhenReconciliationFails() = runTest(
        dispatcher
    ) {
        val repo = Repository()
        val vm = ArticleCollectionViewModel(repo)
        repo.state.value = CollectionSnapshot(3, mapOf(target.key to CollectionStatus()))
        vm.toggle(target, 3, false)
        runCurrent()
        runCurrent()
        assertNull(vm.collections.value.status(target).collected)
        assertEquals(DataError.NETWORK, vm.error.value)
        assertEquals(1, repo.reads)
    }

    @Test fun openingDoesNotRequestAndAccountChangeHidesOldErrors() = runTest(dispatcher) {
        val repo = Repository()
        val vm = ArticleCollectionViewModel(repo)
        runCurrent()
        assertEquals(0, repo.reads)
        vm.toggle(target, 3, false)
        runCurrent()
        repo.state.value = CollectionSnapshot(4)
        runCurrent()
        assertNull(vm.error.value)
    }
}
