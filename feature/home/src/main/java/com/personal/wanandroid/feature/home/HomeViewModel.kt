package com.personal.wanandroid.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.common.LoadState
import com.personal.wanandroid.core.common.PagedUiState
import com.personal.wanandroid.core.common.PagingController
import com.personal.wanandroid.core.data.ArticleRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionUiState(
    val items: List<Article> = emptyList(),
    val loading: Boolean = true,
    val error: DataError? = null
)

data class HomeUiState(
    val articleState: PagedUiState<Article> = PagedUiState(initial = LoadState.Loading),
    val questionState: QuestionUiState = QuestionUiState()
) {
    val articles get() = articleState.items
    val nextPage get() = articleState.nextPage
    val isInitialLoading get() = articleState.isInitialLoading
    val isRefreshing get() = articleState.isRefreshing
    val isLoadingMore get() = articleState.isLoadingMore
    val initialError get() = articleState.initialError
    val refreshError get() = articleState.refreshError
    val loadMoreError get() = articleState.loadMoreError
    val canLoadMore get() = articleState.canLoadMore
    val questions get() = questionState.items
    val isQuestionLoading get() = questionState.loading
    val questionError get() = questionState.error
    val isPullRefreshing get() = isRefreshing || (questions.isNotEmpty() && isQuestionLoading)
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: ArticleRepository) : ViewModel() {
    private val articles =
        PagingController(viewModelScope, 0, Article::id) { repository.articles(it) }
    private val questions = MutableStateFlow(QuestionUiState())
    val uiState = combine(articles.state, questions, ::HomeUiState).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState()
    )
    private var questionJob: Job? = null
    private var questionGeneration = 0L

    init {
        articles.startInitialLoad()
        requestQuestions()
    }
    fun refresh() {
        articles.refresh()
        requestQuestions()
    }
    fun retryInitialLoad() = articles.retryInitial()
    fun retryRefresh() = articles.retryRefresh()
    fun loadMore() = articles.loadMore()
    fun retryLoadMore() = articles.retryAppend()
    fun continueAfterPause() = articles.continueAfterPause()
    fun retryQuestions() = requestQuestions()

    private fun requestQuestions() {
        val generation = ++questionGeneration
        questionJob?.cancel()
        questions.update { it.copy(loading = true, error = null) }
        questionJob = viewModelScope.launch {
            when (val result = repository.questions()) {
                is DataResult.Success -> if (generation == questionGeneration) {
                    questions.value = QuestionUiState(result.value.distinctBy(Article::id), false)
                }

                is DataResult.Failure -> if (generation == questionGeneration) {
                    questions.update { it.copy(loading = false, error = result.reason) }
                }
            }
        }
    }
}
