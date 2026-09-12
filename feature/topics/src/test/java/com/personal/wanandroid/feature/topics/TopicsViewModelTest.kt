package com.personal.wanandroid.feature.topics

import androidx.lifecycle.SavedStateHandle
import com.personal.wanandroid.core.data.ArticleRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TopicsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = TopicRepositoryFixture()
    private fun model(saved: SavedStateHandle = SavedStateHandle()) =
        TopicsViewModel(repository, saved)

    @Before fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun after() {
        Dispatchers.resetMain()
    }

    @Test fun defaultsToFirstAndKeepsSameNamesSeparateById() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        assertEquals(listOf(9L, 10L, 11L, 12L), vm.uiState.value.topics.map { it.id })
        assertEquals(9L, vm.uiState.value.topics[1].parentId)
        assertEquals(10L, vm.uiState.value.selectedId)
        val first = repository.requests.removeFirst()
        assertEquals(10L to 0, first.id to first.page)
        first.success(listOf(article(1)), null)
        runCurrent()
        vm.selectTopic(10)
        vm.selectTopic(999)
        runCurrent()
        assertTrue(repository.requests.isEmpty())
        vm.selectTopic(12)
        runCurrent()
        assertEquals(12L, vm.uiState.value.selectedId)
        assertTrue(vm.uiState.value.page.items.isEmpty())
        repository.requests.removeFirst().success(emptyList(), null)
    }

    @Test fun switchingCancelsAppendAndResumesSameCursorWithoutAcceptingLateResult() =
        runTest(dispatcher) {
            val vm = model()
            runCurrent()
            repository.requests.removeFirst().success(listOf(article(1)), 1)
            runCurrent()
            vm.loadMore(10)
            runCurrent()
            val old = repository.requests.removeFirst()
            vm.selectTopic(11)
            runCurrent()
            assertTrue(old.job.isCancelled)
            repository.requests.removeFirst().success(listOf(article(2)), null)
            runCurrent()
            old.success(listOf(article(99)), null)
            runCurrent()
            assertEquals(listOf(2L), vm.uiState.value.page.items.map { it.id })
            vm.selectTopic(10)
            runCurrent()
            assertEquals(listOf(1L), vm.uiState.value.page.items.map { it.id })
            val resumed = repository.requests.removeFirst()
            assertEquals(10L to 1, resumed.id to resumed.page)
            resumed.success(listOf(article(3)), null)
            runCurrent()
            assertEquals(listOf(1L, 3L), vm.uiState.value.page.items.map { it.id })
        }

    @Test fun rapidSwitchBackRejectsLateInitialAndDisposedListCallbacks() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        val old = repository.requests.removeFirst()
        vm.selectTopic(11)
        runCurrent()
        val other = repository.requests.removeFirst()
        vm.selectTopic(10)
        vm.refresh(11)
        vm.loadMore(11)
        runCurrent()
        assertEquals(1, repository.requests.size)
        repository.requests.removeFirst().success(listOf(article(1)), null)
        old.success(listOf(article(99)), null)
        other.failure()
        runCurrent()
        assertEquals(10L, vm.uiState.value.selectedId)
        assertEquals(listOf(1L), vm.uiState.value.page.items.map { it.id })
        assertEquals(null, vm.uiState.value.page.initialError)
    }

    @Test fun failuresRemainWithTheirCategoryAndRetryUsesSameId() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        repository.requests.removeFirst().failure()
        runCurrent()
        vm.selectTopic(11)
        runCurrent()
        repository.requests.removeFirst().success(emptyList(), null)
        runCurrent()
        vm.selectTopic(10)
        runCurrent()
        assertTrue(repository.requests.isEmpty())
        assertEquals(DataError.NETWORK, vm.uiState.value.page.initialError)
        vm.retryInitial(10)
        runCurrent()
        val retry = repository.requests.removeFirst()
        assertEquals(10L to 0, retry.id to retry.page)
        retry.success(listOf(article(1)), null)
        runCurrent()
        vm.refresh(10)
        runCurrent()
        val refresh = repository.requests.removeFirst()
        vm.selectTopic(11)
        vm.selectTopic(10)
        runCurrent()
        assertTrue(refresh.job.isCancelled)
        refresh.failure()
        repository.requests.removeFirst().success(listOf(article(2)), null)
        runCurrent()
        assertEquals(listOf(2L), vm.uiState.value.page.items.map { it.id })
    }

    @Test fun appendFailureRetriesCursorAndEndNeverSelectsNextTopic() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        repository.requests.removeFirst().success(listOf(article(1)), 1)
        runCurrent()
        vm.loadMore(10)
        vm.loadMore(10)
        runCurrent()
        assertEquals(1, repository.requests.size)
        repository.requests.removeFirst().failure()
        runCurrent()
        vm.selectTopic(11)
        runCurrent()
        repository.requests.removeFirst().success(emptyList(), null)
        runCurrent()
        vm.selectTopic(10)
        runCurrent()
        assertEquals(DataError.NETWORK, vm.uiState.value.page.loadMoreError)
        vm.retryAppend(10)
        runCurrent()
        val retry = repository.requests.removeFirst()
        assertEquals(1, retry.page)
        retry.success(listOf(article(1), article(2)), null)
        runCurrent()
        vm.loadMore(10)
        runCurrent()
        assertEquals(10L, vm.uiState.value.selectedId)
        assertEquals(listOf(1L, 2L), vm.uiState.value.page.items.map { it.id })
        assertTrue(repository.requests.isEmpty())
        assertFalse(vm.uiState.value.page.canLoadMore)
    }

    @Test fun categoryFailureRetriesAndEmptyTreeDoesNotRequestArticles() = runTest(dispatcher) {
        repository.topics = DataResult.Failure(DataError.NETWORK)
        val vm = model()
        runCurrent()
        assertEquals(DataError.NETWORK, vm.uiState.value.error)
        assertTrue(repository.requests.isEmpty())
        repository.topics = DataResult.Success(emptyList())
        vm.retryTopics()
        runCurrent()
        assertFalse(vm.uiState.value.loading)
        assertEquals(null, vm.uiState.value.error)
        assertEquals(null, vm.uiState.value.selectedId)
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun hierarchyRestoresLastChildAndRejectsCallbacksFromAnotherParent() = runTest(
        dispatcher
    ) {
        repository.topics = DataResult.Success(
            listOf(
                Topic(10, "A"),
                Topic(11, "First", 10),
                Topic(12, "Child", 10),
                Topic(20, "B"),
                Topic(21, "Child", 20)
            )
        )
        val saved = SavedStateHandle()
        val vm = model(saved)
        runCurrent()
        repository.requests.removeFirst().success(listOf(article(1)), null)
        runCurrent()
        assertEquals(listOf(10L, 20L), vm.uiState.value.parents.map { it.id })
        assertEquals(listOf(11L, 12L), vm.uiState.value.tabs.map { it.id })
        vm.selectChild(10, 12)
        runCurrent()
        val child = repository.requests.removeFirst()
        vm.selectParent(20)
        runCurrent()
        assertTrue(child.job.isCancelled)
        repository.requests.removeFirst().success(listOf(article(2)), null)
        runCurrent()
        vm.selectChild(10, 12)
        vm.selectChild(20, 11)
        vm.selectParent(21)
        child.success(listOf(article(99)), null)
        runCurrent()
        assertEquals(21L, vm.uiState.value.selectedId)
        assertEquals(listOf(21L), vm.uiState.value.tabs.map { it.id })
        assertTrue(repository.requests.isEmpty())
        vm.selectParent(10)
        runCurrent()
        assertEquals(12L, vm.uiState.value.selectedId)
        repository.requests.removeFirst().success(listOf(article(3)), null)
        runCurrent()
        assertEquals(12L, saved.get<Long>("topics.child.10"))
        assertEquals(listOf(1L), vm.uiState.value.pageStates[11L]?.items?.map { it.id })
        assertEquals(listOf(2L), vm.uiState.value.pageStates[21L]?.items?.map { it.id })
    }

    @Test fun parentWithoutChildrenHasNoSyntheticTabOrArticleRequest() = runTest(dispatcher) {
        repository.topics = DataResult.Success(listOf(Topic(10, "A"), Topic(20, "B")))
        val vm = model(SavedStateHandle(mapOf("topics.child.20" to 999L)))
        runCurrent()
        assertEquals(10L, vm.uiState.value.selectedParentId)
        vm.selectParent(20)
        vm.selectTopic(20)
        runCurrent()
        assertEquals(20L, vm.uiState.value.selectedParentId)
        assertEquals(null, vm.uiState.value.selectedId)
        assertTrue(vm.uiState.value.tabs.isEmpty())
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun oldParentSelectionAndInvalidSavedChildRestoreFirstRealChild() = runTest(dispatcher) {
        val saved = SavedStateHandle(
            mapOf("topics.selectedId" to 9L, "topics.child.9" to 9L)
        )
        val vm = model(saved)
        runCurrent()
        assertEquals(9L, vm.uiState.value.selectedParentId)
        assertEquals(10L, vm.uiState.value.selectedId)
        assertEquals(listOf(10L, 11L, 12L), vm.uiState.value.tabs.map { it.id })
        val request = repository.requests.removeFirst()
        assertEquals(10L, request.id)
        request.success(emptyList(), null)
        runCurrent()
        assertEquals(10L, saved.get<Long>("topics.child.9"))
    }

    @Test fun reloadingTreeDropsRemovedControllersAndRejectsTheirLateResults() = runTest(
        dispatcher
    ) {
        val vm = model()
        runCurrent()
        repository.requests.removeFirst().success(listOf(article(1)), null)
        runCurrent()
        vm.selectTopic(11)
        runCurrent()
        val removed = repository.requests.removeFirst()
        repository.topics = DataResult.Success(
            listOf(Topic(9, "Parent"), Topic(10, "First", 9))
        )
        vm.retryTopics()
        runCurrent()
        assertTrue(removed.job.isCancelled)
        assertEquals(10L, vm.uiState.value.selectedId)
        assertEquals(setOf(10L), vm.uiState.value.pageStates.keys)
        assertEquals(listOf(1L), vm.uiState.value.page.items.map { it.id })
        removed.success(listOf(article(99)), null)
        runCurrent()
        assertEquals(setOf(10L), vm.uiState.value.pageStates.keys)
        assertTrue(repository.requests.isEmpty())
        repository.topics = DataResult.Success(
            listOf(Topic(9, "Parent"), Topic(10, "First", 9), Topic(11, "Restored", 9))
        )
        vm.retryTopics()
        runCurrent()
        vm.selectTopic(11)
        runCurrent()
        val reintroduced = repository.requests.removeFirst()
        assertEquals(11L to 0, reintroduced.id to reintroduced.page)
        assertTrue(vm.uiState.value.page.items.isEmpty())
        reintroduced.success(listOf(article(2)), null)
        runCurrent()
        assertEquals(listOf(2L), vm.uiState.value.page.items.map { it.id })
    }

    @Test fun restoresValidIdAndFallsBackWhenIdDisappears() = runTest(dispatcher) {
        for ((savedId, expected) in listOf(12L to 12L, 999L to 10L)) {
            val vm = model(SavedStateHandle(mapOf("topics.selectedId" to savedId)))
            runCurrent()
            assertEquals(expected, vm.uiState.value.selectedId)
            repository.requests.removeFirst().success(emptyList(), null)
        }
    }
}
private data class TopicRequest(
    val id: Long,
    val page: Int,
    val job: Job,
    val continuation: Continuation<DataResult<PageResult<Article>>>
) {
    fun success(items: List<Article>, next: Int?) =
        continuation.resume(DataResult.Success(PageResult(items, next)))
    fun failure() = continuation.resume(DataResult.Failure(DataError.NETWORK))
}
private class TopicRepositoryFixture : ArticleRepository {
    var topics: DataResult<List<Topic>> = DataResult.Success(
        listOf(
            Topic(9, "Parent"),
            Topic(10, "First", 9),
            Topic(11, "Same", 9),
            Topic(12, "Same", 9)
        )
    )
    val requests = ArrayDeque<TopicRequest>()
    override suspend fun topics() = topics
    override suspend fun articles(page: Int, categoryId: Long?): DataResult<PageResult<Article>> {
        val job = currentCoroutineContext()[Job]!!
        return suspendCoroutine {
            requests +=
                TopicRequest(requireNotNull(categoryId), page, job, it)
        }
    }
    override suspend fun questions(): DataResult<List<Article>> = error("unexpected")
    override suspend fun questionPage(page: Int): DataResult<PageResult<Article>> =
        error("unexpected")
    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> =
        error("unexpected")
}
private fun article(id: Long) = Article(
    id, "Article $id", "https://reader.invalid/$id", "Author", "", "Parent", "Child", "Today", false
)
