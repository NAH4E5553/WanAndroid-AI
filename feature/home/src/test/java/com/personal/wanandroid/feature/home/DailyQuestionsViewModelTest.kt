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
class DailyQuestionsViewModelTest {
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
    fun initialSuccessPublishesQuestionPage() = runTest(dispatcher) {
        val repository = ControllableQuestionRepository()
        val viewModel = DailyQuestionsViewModel(repository)
        runCurrent()

        val request = repository.takeRequest()
        assertEquals(1, request.page)
        request.complete(pageSuccess(listOf(question(1)), nextPage = 2))
        runCurrent()

        assertEquals(listOf(1L), viewModel.uiState.value.questions.map(Article::id))
        assertEquals(2, viewModel.uiState.value.nextPage)
        assertFalse(viewModel.uiState.value.isInitialLoading)
    }

    @Test
    fun loadMoreDeduplicatesAndDoesNotStartDuplicateRequest() = runTest(dispatcher) {
        val repository = ControllableQuestionRepository()
        val viewModel = DailyQuestionsViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(
            pageSuccess(listOf(question(1), question(2)), nextPage = 2)
        )
        runCurrent()

        viewModel.loadMore()
        viewModel.loadMore()
        runCurrent()
        assertEquals(1, repository.requestCount(2))
        repository.takeRequest().complete(
            pageSuccess(listOf(question(2), question(3)), nextPage = null)
        )
        runCurrent()

        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.questions.map(Article::id))
        assertNull(viewModel.uiState.value.nextPage)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun loadMoreFailureRetriesSameOneBasedPage() = runTest(dispatcher) {
        val repository = ControllableQuestionRepository()
        val viewModel = DailyQuestionsViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(pageSuccess(listOf(question(1)), nextPage = 2))
        runCurrent()

        viewModel.loadMore()
        runCurrent()
        repository.takeRequest().complete(DataResult.Failure(DataError.NETWORK))
        runCurrent()
        assertEquals(2, viewModel.uiState.value.nextPage)
        assertEquals(DataError.NETWORK, viewModel.uiState.value.loadMoreError)

        viewModel.retryLoadMore()
        runCurrent()
        val retry = repository.takeRequest()
        assertEquals(2, retry.page)
        retry.complete(pageSuccess(listOf(question(2)), nextPage = null))
        runCurrent()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.questions.map(Article::id))
        assertNull(viewModel.uiState.value.loadMoreError)
    }

    @Test
    fun refreshMakesOlderNonCooperativeResultStale() = runTest(dispatcher) {
        val repository = ControllableQuestionRepository()
        val viewModel = DailyQuestionsViewModel(repository)
        runCurrent()
        val oldRequest = repository.takeRequest()

        viewModel.refresh()
        runCurrent()
        val newRequest = repository.takeRequest()
        newRequest.complete(pageSuccess(listOf(question(2)), nextPage = null))
        runCurrent()
        oldRequest.complete(pageSuccess(listOf(question(1)), nextPage = 2))
        runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.value.questions.map(Article::id))
        assertNull(viewModel.uiState.value.nextPage)
    }

    @Test
    fun initialFailureCanRetry() = runTest(dispatcher) {
        val repository = ControllableQuestionRepository()
        val viewModel = DailyQuestionsViewModel(repository)
        runCurrent()
        repository.takeRequest().complete(DataResult.Failure(DataError.SERVICE))
        runCurrent()
        assertEquals(DataError.SERVICE, viewModel.uiState.value.initialError)
        assertTrue(viewModel.uiState.value.questions.isEmpty())

        viewModel.retryInitialLoad()
        runCurrent()
        repository.takeRequest().complete(pageSuccess(listOf(question(2)), nextPage = null))
        runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.value.questions.map(Article::id))
        assertNull(viewModel.uiState.value.initialError)
    }
}

private fun question(id: Long) = Article(
    id = id,
    title = "Question $id",
    url = "https://example.org/question/$id",
    author = "Author",
    shareUser = "",
    superChapterName = "Knowledge",
    chapter = "Android",
    publishedAt = "Today",
    collected = false
)

private fun pageSuccess(items: List<Article>, nextPage: Int?) =
    DataResult.Success(PageResult(items, nextPage))

private class ControllableQuestionRepository : ArticleRepository {
    private val pending = ArrayDeque<QuestionPageRequest>()
    private val pages = mutableListOf<Int>()

    override suspend fun questionPage(page: Int) =
        suspendCoroutine<DataResult<PageResult<Article>>> { continuation ->
            pages += page
            pending += QuestionPageRequest(page, continuation)
        }

    fun takeRequest(): QuestionPageRequest = pending.removeFirst()

    fun requestCount(page: Int): Int = pages.count { it == page }

    override suspend fun articles(page: Int, categoryId: Long?) = error("Not used")

    override suspend fun questions(): DataResult<List<Article>> = error("Not used")

    override suspend fun topics(): DataResult<List<Topic>> = error("Not used")

    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> =
        error("Not used")
}

private data class QuestionPageRequest(
    val page: Int,
    private val continuation: Continuation<DataResult<PageResult<Article>>>
) {
    fun complete(result: DataResult<PageResult<Article>>) {
        continuation.resume(result)
    }
}
