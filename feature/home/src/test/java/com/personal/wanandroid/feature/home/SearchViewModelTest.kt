package com.personal.wanandroid.feature.home

import androidx.lifecycle.SavedStateHandle
import com.personal.wanandroid.core.data.ArticleRepository
import com.personal.wanandroid.core.data.SearchSuggestionsRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.SearchHistory
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = SearchRepository()
    private val suggestions = FakeSuggestions()
    private fun model(saved: SavedStateHandle = SavedStateHandle()) =
        SearchViewModel(repository, saved, suggestions)

    @Before fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun after() {
        Dispatchers.resetMain()
    }

    @Test fun tagSearchRecordsTrimmedKeywordEvenOnSearchFailure() = runTest(dispatcher) {
        val vm = model()
        vm.selectKeyword("  推荐词  ")
        vm.submit()
        runCurrent()
        repository.requests.removeFirst().failure()
        runCurrent()
        assertEquals(listOf("推荐词"), suggestions.records)
        assertEquals(listOf("推荐词"), vm.uiState.value.suggestions.history.items)
        assertEquals(DataError.NETWORK, vm.uiState.value.page.initialError)
    }

    @Test fun slowHistoryWriteFinishesBeforeQueuedClear() = runTest(dispatcher) {
        suggestions.gate = CompletableDeferred()
        val vm = model()
        vm.selectKeyword("first")
        runCurrent()
        vm.clearHistory()
        runCurrent()
        assertEquals(0, suggestions.clears)
        suggestions.gate!!.complete(Unit)
        runCurrent()
        assertEquals(1, suggestions.clears)
        assertTrue(vm.uiState.value.suggestions.history.items.isEmpty())
        repository.requests.removeFirst().success(emptyList(), null)
    }

    @Test fun failedClearPreservesHistoryAndReportsFailure() = runTest(dispatcher) {
        suggestions.history.value = SearchHistory(listOf("previous"), ready = true)
        suggestions.writeSuccess = false
        val vm = model()
        vm.clearHistory()
        runCurrent()
        assertEquals(listOf("previous"), vm.uiState.value.suggestions.history.items)
        assertTrue(vm.uiState.value.suggestions.historyWriteFailed)
        suggestions.writeSuccess = true
        vm.clearHistory()
        runCurrent()
        assertFalse(vm.uiState.value.suggestions.historyWriteFailed)
        assertTrue(vm.uiState.value.suggestions.history.items.isEmpty())
    }

    @Test fun hotKeyFailureDoesNotBlockManualSearchAndCanRetry() = runTest(dispatcher) {
        suggestions.hotResult = DataResult.Failure(DataError.NETWORK)
        val vm = model()
        runCurrent()
        assertEquals(DataError.NETWORK, vm.uiState.value.suggestions.hotError)
        vm.selectKeyword("manual")
        runCurrent()
        assertEquals("manual", repository.requests.first().query)
        suggestions.hotResult = DataResult.Success(listOf("推荐词"))
        vm.retryHotKeys()
        runCurrent()
        assertEquals(listOf("推荐词"), vm.uiState.value.suggestions.hotKeys)
        assertNull(vm.uiState.value.suggestions.hotError)
        repository.requests.removeFirst().success(emptyList(), null)
    }

    @Test fun editingAndRetypingPreviousKeywordStayHiddenUntilExplicitSubmit() = runTest(
        dispatcher
    ) {
        val vm = model()
        vm.selectKeyword("first")
        runCurrent()
        repository.requests.removeFirst().success(listOf(searchArticle(1)), null)
        runCurrent()
        assertTrue(vm.uiState.value.showResults)
        for (text in listOf("", "f", "first")) {
            vm.editInput(text)
            runCurrent()
            assertFalse(vm.uiState.value.showResults)
            assertTrue(repository.requests.isEmpty())
        }
        vm.submit()
        runCurrent()
        assertTrue(vm.uiState.value.showResults)
        assertEquals("first", repository.requests.first().query)
        repository.requests.removeFirst().success(emptyList(), null)
    }

    @Test fun lateResponseCannotRevealResultsWhileEditingAndEditingSurvivesRestore() =
        runTest(dispatcher) {
            val saved = SavedStateHandle()
            val vm = model(saved)
            vm.selectKeyword("first")
            runCurrent()
            val old = repository.requests.removeFirst()
            vm.editInput("second")
            old.success(listOf(searchArticle(1)), null)
            runCurrent()
            assertFalse(vm.uiState.value.showResults)
            vm.editInput("first")
            runCurrent()
            val restored = model(saved)
            runCurrent()
            repository.requests.removeFirst().success(listOf(searchArticle(1)), null)
            runCurrent()
            assertEquals("first", restored.uiState.value.input)
            assertFalse(restored.uiState.value.showResults)
            restored.submit()
            runCurrent()
            assertTrue(restored.uiState.value.showResults)
            repository.requests.removeFirst().success(emptyList(), null)
        }

    @Test fun editingAndBlankSubmissionDoNotRequest() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        vm.refresh()
        vm.submit()
        vm.editInput("   ")
        vm.submit()
        runCurrent()
        assertTrue(repository.requests.isEmpty())
        assertFalse(vm.uiState.value.hasSubmitted)
        assertFalse(vm.uiState.value.canSubmit)
        vm.editInput("draft")
        runCurrent()
        assertTrue(repository.requests.isEmpty())
        assertEquals("draft", vm.uiState.value.input)
    }

    @Test fun trimsSubmissionStartsAtZeroAndIgnoresDuplicateWhileLoading() = runTest(dispatcher) {
        val vm = model()
        vm.editInput("  Kotlin  ")
        vm.submit()
        vm.submit()
        runCurrent()
        val request = repository.requests.removeFirst()
        assertEquals("Kotlin", request.query)
        assertEquals(0, request.page)
        assertTrue(repository.requests.isEmpty())
        request.success(listOf(searchArticle(1)), 1)
        runCurrent()
        assertEquals("Kotlin", vm.uiState.value.keyword)
        assertEquals(1, vm.uiState.value.page.nextPage)
    }

    @Test fun appendUsesSubmittedQueryAndRetriesCursor() = runTest(dispatcher) {
        val vm = model()
        vm.editInput("first")
        vm.submit()
        runCurrent()
        repository.requests.removeFirst().success(listOf(searchArticle(1)), 1)
        runCurrent()
        vm.editInput("draft")
        vm.loadMore()
        vm.loadMore()
        runCurrent()
        val append = repository.requests.removeFirst()
        assertEquals("first", append.query)
        assertEquals(1, append.page)
        append.failure()
        runCurrent()
        assertEquals(1, vm.uiState.value.page.nextPage)
        vm.retryLoadMore()
        runCurrent()
        val retry = repository.requests.removeFirst()
        assertEquals("first", retry.query)
        assertEquals(1, retry.page)
        retry.success(listOf(searchArticle(1), searchArticle(2)), null)
        runCurrent()
        assertEquals(listOf(1L, 2L), vm.uiState.value.page.items.map { it.id })
        assertEquals("draft", vm.uiState.value.input)
        vm.loadMore()
        runCurrent()
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun switchingQueryKeepsContextAndRowsTogether() = runTest(dispatcher) {
        val vm = model()
        val snapshots = mutableListOf<SearchUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect { snapshots += it }
        }
        vm.editInput("first")
        vm.submit()
        runCurrent()
        repository.requests.removeFirst().success(listOf(searchArticle(1)), 1)
        runCurrent()
        vm.loadMore()
        runCurrent()
        val old = repository.requests.removeFirst()
        vm.editInput("second")
        vm.submit()
        runCurrent()
        assertTrue(vm.uiState.value.page.items.isEmpty())
        repository.requests.removeFirst().success(listOf(searchArticle(2)), null)
        runCurrent()
        old.success(listOf(searchArticle(99)), null)
        runCurrent()
        assertEquals(listOf(2L), vm.uiState.value.page.items.map { it.id })
        assertTrue(
            snapshots.filter { it.keyword == "second" }.all { state ->
                state.page.items.all { it.id == 2L }
            }
        )
    }

    @Test fun oldInitialFailureCannotReplaceNewQuerySuccess() = runTest(dispatcher) {
        val vm = model()
        vm.editInput("first")
        vm.submit()
        runCurrent()
        val old = repository.requests.removeFirst()
        vm.editInput("second")
        vm.submit()
        runCurrent()
        repository.requests.removeFirst().success(emptyList(), null)
        runCurrent()
        old.failure()
        runCurrent()
        assertEquals("second", vm.uiState.value.keyword)
        assertTrue(vm.uiState.value.page.items.isEmpty())
        assertNull(vm.uiState.value.page.initialError)
        assertFalse(vm.uiState.value.page.canLoadMore)
    }

    @Test fun initialFailureRetriesSameQueryAndExplicitRepeatRefreshes() = runTest(dispatcher) {
        val vm = model()
        vm.editInput("first")
        vm.submit()
        runCurrent()
        repository.requests.removeFirst().failure()
        runCurrent()
        vm.retryInitialLoad()
        runCurrent()
        val retry = repository.requests.removeFirst()
        assertEquals("first", retry.query)
        assertEquals(0, retry.page)
        retry.success(listOf(searchArticle(1)), null)
        runCurrent()
        assertTrue(vm.uiState.value.canSubmit)
        vm.refresh()
        runCurrent()
        assertTrue(vm.uiState.value.page.isRefreshing)
        assertTrue(vm.uiState.value.canSubmit)
        vm.submit()
        runCurrent()
        assertEquals(1, repository.requests.size)
        repository.requests.removeFirst().failure()
        runCurrent()
        assertEquals(listOf(1L), vm.uiState.value.page.items.map { it.id })
        assertEquals(DataError.NETWORK, vm.uiState.value.page.refreshError)
        assertTrue(vm.uiState.value.canSubmit)
        vm.retryRefresh()
        runCurrent()
        repository.requests.removeFirst().success(emptyList(), null)
        runCurrent()
        assertTrue(vm.uiState.value.page.items.isEmpty())
    }

    @Test fun restoredSubmittedQueryReloadsButDraftDoesNotBecomeARequest() = runTest(dispatcher) {
        val saved = SavedStateHandle(
            mapOf("search.input" to "editing", "search.keyword" to "submitted")
        )
        val vm = model(saved)
        runCurrent()
        val request = repository.requests.removeFirst()
        assertEquals("submitted", request.query)
        assertEquals(0, request.page)
        request.success(emptyList(), null)
        runCurrent()
        assertEquals("editing", vm.uiState.value.input)
        assertEquals("submitted", vm.uiState.value.keyword)
        assertEquals(setOf("search.input", "search.keyword"), saved.keys())
    }

    @Test fun revisitingEarlierKeywordStartsNewContextAtFirstPage() = runTest(dispatcher) {
        val vm = model()
        for (query in listOf("first", "second", "first")) {
            vm.editInput(query)
            vm.submit()
            runCurrent()
            val request = repository.requests.removeFirst()
            assertEquals(query, request.query)
            assertEquals(0, request.page)
            request.success(emptyList(), null)
            runCurrent()
        }
        assertEquals(3L, vm.uiState.value.contextGeneration)
    }
}
private data class SearchRequest(
    val query: String,
    val page: Int,
    val continuation: Continuation<DataResult<PageResult<Article>>>
) {
    fun success(items: List<Article>, next: Int?) =
        continuation.resume(DataResult.Success(PageResult(items, next)))
    fun failure() = continuation.resume(DataResult.Failure(DataError.NETWORK))
}
private class SearchRepository : ArticleRepository {
    val requests = ArrayDeque<SearchRequest>()
    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> =
        suspendCoroutine { requests += SearchRequest(keyword, page, it) }
    override suspend fun articles(page: Int, categoryId: Long?): DataResult<PageResult<Article>> =
        error("Unexpected articles")
    override suspend fun questions(): DataResult<List<Article>> = error("Unexpected questions")
    override suspend fun questionPage(page: Int): DataResult<PageResult<Article>> =
        error("Unexpected questions")
    override suspend fun topics(): DataResult<List<Topic>> = error("Unexpected topics")
}
private fun searchArticle(id: Long) = Article(
    id, "Article $id", "https://reader.invalid/$id", "Author", "", "Parent", "Child", "Today", false
)

private class FakeSuggestions : SearchSuggestionsRepository {
    override val history = MutableStateFlow(SearchHistory(ready = true))
    val records = mutableListOf<String>()
    var clears = 0
    var gate: CompletableDeferred<Unit>? = null
    var writeSuccess = true
    var hotResult: DataResult<List<String>> = DataResult.Success(listOf("推荐词"))
    override suspend fun hotKeys() = hotResult
    override suspend fun record(keyword: String): Boolean {
        gate?.await()
        records += keyword
        if (writeSuccess) {
            history.value = SearchHistory(
                (listOf(keyword) + history.value.items).distinct().take(20),
                ready = true
            )
        }
        return writeSuccess
    }
    override suspend fun clearHistory(): Boolean {
        clears++
        if (writeSuccess) history.value = SearchHistory(ready = true)
        return writeSuccess
    }
}
