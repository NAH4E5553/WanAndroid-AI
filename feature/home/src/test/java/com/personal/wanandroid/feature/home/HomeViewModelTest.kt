package com.personal.wanandroid.feature.home

import com.personal.wanandroid.core.data.ArticleRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.DataError
import com.personal.wanandroid.core.model.DataResult
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.Topic
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialSuccessPublishesArticlesAndCursor() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()

        assertTrue(viewModel.uiState.value.isInitialLoading)
        repository.takeRequest().complete(success(listOf(article(1)), nextPage = 1))
        runCurrent()

        assertEquals(listOf(1L), viewModel.uiState.value.articles.map(Article::id))
        assertEquals(1, viewModel.uiState.value.nextPage)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertNull(viewModel.uiState.value.initialError)
    }

    @Test
    fun emptySuccessIsNotReportedAsFailure() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()

        repository.takeRequest().complete(success(emptyList(), nextPage = null))
        runCurrent()

        assertTrue(viewModel.uiState.value.articles.isEmpty())
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertNull(viewModel.uiState.value.initialError)
        assertFalse(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun initialFailureCanRetry() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()

        repository.takeRequest().complete(DataResult.Failure(DataError.NETWORK))
        runCurrent()
        assertEquals(DataError.NETWORK, viewModel.uiState.value.initialError)

        viewModel.retryInitialLoad()
        runCurrent()
        repository.takeRequest().complete(success(listOf(article(2)), nextPage = null))
        runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.value.articles.map(Article::id))
        assertNull(viewModel.uiState.value.initialError)
    }

    @Test
    fun initialFailureDoesNotPermitLoadMore() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()

        repository.takeRequest().complete(DataResult.Failure(DataError.NETWORK))
        runCurrent()
        viewModel.loadMore()
        runCurrent()

        assertEquals(1, repository.totalRequestCount)
        assertFalse(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun refreshFailurePreservesExistingArticles() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(success(listOf(article(1)), nextPage = 1))
        runCurrent()

        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.uiState.value.isRefreshing)
        repository.takeRequest().complete(DataResult.Failure(DataError.SERVICE))
        runCurrent()

        assertEquals(listOf(1L), viewModel.uiState.value.articles.map(Article::id))
        assertEquals(DataError.SERVICE, viewModel.uiState.value.refreshError)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun loadMoreDeduplicatesAndAdvancesOnlyAfterSuccess() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(success(listOf(article(1), article(2)), nextPage = 1))
        runCurrent()

        viewModel.loadMore()
        runCurrent()
        assertEquals(1, viewModel.uiState.value.nextPage)
        repository.takeRequest().complete(success(listOf(article(2), article(3)), nextPage = null))
        runCurrent()

        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.articles.map(Article::id))
        assertNull(viewModel.uiState.value.nextPage)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun loadMoreFailureKeepsCursorAndRetriesSamePage() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(success(listOf(article(1)), nextPage = 1))
        runCurrent()

        viewModel.loadMore()
        runCurrent()
        repository.takeRequest().complete(DataResult.Failure(DataError.NETWORK))
        runCurrent()
        assertEquals(1, viewModel.uiState.value.nextPage)
        assertEquals(DataError.NETWORK, viewModel.uiState.value.loadMoreError)

        viewModel.retryLoadMore()
        runCurrent()
        val retry = repository.takeRequest()
        assertEquals(1, retry.page)
        retry.complete(success(listOf(article(2)), nextPage = null))
        runCurrent()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.articles.map(Article::id))
        assertNull(viewModel.uiState.value.loadMoreError)
    }

    @Test
    fun repeatedLoadMoreDoesNotStartDuplicateRequests() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(success(listOf(article(1)), nextPage = 1))
        runCurrent()

        viewModel.loadMore()
        viewModel.loadMore()
        runCurrent()

        assertEquals(1, repository.requestCount(1))
        repository.takeRequest().complete(success(listOf(article(2)), nextPage = null))
        runCurrent()
    }

    @Test
    fun terminalPageDoesNotRequestMore() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(success(listOf(article(1)), nextPage = null))
        runCurrent()

        viewModel.loadMore()
        runCurrent()

        assertEquals(1, repository.totalRequestCount)
    }

    @Test
    fun refreshMakesOlderNonCooperativeResultStale() = runTest(dispatcher) {
        val repository = ControllableArticleRepository()
        val viewModel = HomeViewModel(repository)
        runCurrent()
        val oldRequest = repository.takeRequest()

        viewModel.refresh()
        runCurrent()
        val newRequest = repository.takeRequest()
        newRequest.complete(success(listOf(article(2)), nextPage = null))
        runCurrent()
        oldRequest.complete(success(listOf(article(1)), nextPage = 1))
        runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.value.articles.map(Article::id))
        assertNull(viewModel.uiState.value.nextPage)
    }
}

private fun article(id: Long) = Article(
    id = id,
    title = "Article $id",
    url = "https://example.org/$id",
    author = "Author",
    shareUser = "",
    superChapterName = "Knowledge",
    chapter = "Android",
    publishedAt = "Today",
    collected = false
)

private fun success(items: List<Article>, nextPage: Int?) =
    DataResult.Success(PageResult(items, nextPage))

private class ControllableArticleRepository : ArticleRepository {
    private val pending = ArrayDeque<Request>()
    private val pages = mutableListOf<Int>()

    val totalRequestCount: Int
        get() = pages.size

    override suspend fun articles(page: Int, categoryId: Long?) =
        suspendCoroutine<DataResult<PageResult<Article>>> { continuation ->
            pages += page
            pending += Request(page, continuation)
        }

    fun takeRequest(): Request = pending.removeFirst()

    fun requestCount(page: Int): Int = pages.count { it == page }

    override suspend fun questions(): DataResult<List<Article>> = error("Not used")

    override suspend fun topics(): DataResult<List<Topic>> = error("Not used")

    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> =
        error("Not used")
}

private data class Request(
    val page: Int,
    private val continuation: Continuation<DataResult<PageResult<Article>>>
) {
    fun complete(result: DataResult<PageResult<Article>>) {
        continuation.resume(result)
    }
}
