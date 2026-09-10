package com.personal.wanandroid.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.ArticleRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.DataError
import com.personal.wanandroid.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val articles: List<Article> = emptyList(),
    val nextPage: Int? = 0,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val initialError: DataError? = null,
    val refreshError: DataError? = null,
    val loadMoreError: DataError? = null
) {
    val canLoadMore: Boolean
        get() = articles.isNotEmpty() && nextPage != null
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: ArticleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var articleJob: Job? = null
    private var requestGeneration = 0L

    init {
        requestFirstPage()
    }

    fun refresh() = requestFirstPage()

    fun retryInitialLoad() = requestFirstPage()

    fun loadMore() {
        val snapshot = _uiState.value
        val page = snapshot.nextPage ?: return
        if (
            snapshot.articles.isEmpty() ||
            snapshot.isInitialLoading ||
            snapshot.isRefreshing ||
            snapshot.isLoadingMore ||
            snapshot.initialError != null ||
            snapshot.loadMoreError != null
        ) {
            return
        }

        val generation = requestGeneration
        _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        articleJob = viewModelScope.launch {
            when (val result = repository.articles(page)) {
                is DataResult.Success -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update { current ->
                        current.copy(
                            articles = (current.articles + result.value.items).distinctBy(
                                Article::id
                            ),
                            nextPage = result.value.nextPage,
                            isLoadingMore = false,
                            loadMoreError = null
                        )
                    }
                }

                is DataResult.Failure -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update {
                        it.copy(isLoadingMore = false, loadMoreError = result.reason)
                    }
                }
            }
        }
    }

    fun retryLoadMore() {
        _uiState.update { it.copy(loadMoreError = null) }
        loadMore()
    }

    private fun requestFirstPage() {
        val generation = ++requestGeneration
        articleJob?.cancel()
        _uiState.update { current ->
            if (current.articles.isEmpty()) {
                current.copy(
                    isInitialLoading = true,
                    isRefreshing = false,
                    isLoadingMore = false,
                    initialError = null,
                    refreshError = null,
                    loadMoreError = null
                )
            } else {
                current.copy(
                    isInitialLoading = false,
                    isRefreshing = true,
                    isLoadingMore = false,
                    refreshError = null,
                    loadMoreError = null
                )
            }
        }
        articleJob = viewModelScope.launch {
            when (val result = repository.articles(FIRST_PAGE)) {
                is DataResult.Success -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update {
                        it.copy(
                            articles = result.value.items.distinctBy(Article::id),
                            nextPage = result.value.nextPage,
                            isInitialLoading = false,
                            isRefreshing = false,
                            initialError = null,
                            refreshError = null,
                            loadMoreError = null
                        )
                    }
                }

                is DataResult.Failure -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update { current ->
                        if (current.articles.isEmpty()) {
                            current.copy(
                                isInitialLoading = false,
                                isRefreshing = false,
                                initialError = result.reason
                            )
                        } else {
                            current.copy(isRefreshing = false, refreshError = result.reason)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val FIRST_PAGE = 0
    }
}
