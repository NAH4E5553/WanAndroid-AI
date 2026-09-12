package com.personal.wanandroid.feature.home.state

import com.personal.wanandroid.core.common.base.state.LoadState
import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.model.Article

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
