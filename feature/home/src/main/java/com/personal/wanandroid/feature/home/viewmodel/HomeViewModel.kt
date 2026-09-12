package com.personal.wanandroid.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.common.paging.PagingController
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.home.state.HomeUiState
import com.personal.wanandroid.feature.home.state.QuestionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: ArticleRepository) : ViewModel() {
    private val articles =
        PagingController(viewModelScope, 0, Article::id, Unit) { _, page ->
            repository.articles(page)
        }
    private val questions = MutableStateFlow(QuestionUiState())
    val uiState = combine(articles.state, questions) { articles, questions ->
        HomeUiState(articles.page, questions)
    }.stateIn(
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
