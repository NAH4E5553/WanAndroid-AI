package com.personal.wanandroid.core.common.base.state

import com.personal.wanandroid.core.result.DataError

sealed interface LoadState {
    data object Idle : LoadState
    data object Loading : LoadState
    data class Failure(val error: DataError) : LoadState
}

/** One authoritative snapshot for a list within its active context. Loading is not a data result. */
data class PagedUiState<T>(
    val items: List<T> = emptyList(),
    val initial: LoadState = LoadState.Idle,
    val refresh: LoadState = LoadState.Idle,
    val append: LoadState = LoadState.Idle,
    val nextPage: Int? = null,
    val consecutiveNoProgress: Int = 0,
    val autoLoadPaused: Boolean = false,
    val datasetGeneration: Long = 0
) {
    val isInitialLoading get() = initial == LoadState.Loading
    val isRefreshing get() = refresh == LoadState.Loading
    val isLoadingMore get() = append == LoadState.Loading
    val initialError get() = (initial as? LoadState.Failure)?.error
    val refreshError get() = (refresh as? LoadState.Failure)?.error
    val loadMoreError get() = (append as? LoadState.Failure)?.error
    val canLoadMore get() = nextPage != null
    val canAutoLoadMore get() = canLoadMore && initial == LoadState.Idle &&
        refresh == LoadState.Idle && append == LoadState.Idle && !autoLoadPaused
}
