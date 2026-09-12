package com.personal.wanandroid.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.personal.wanandroid.core.common.PagedUiState
import com.personal.wanandroid.core.designsystem.WanSpacing

/**
 * Adapted from CoolMall BaseNetWorkListView / RefreshContent: state and content slots,
 * with the existing Material refresh gesture and caller-owned scroll state.
 * Prefix content supports multi-request pages without merging their state machines.
 * Pager previews disable paging until their category is selected and the gesture settles.
 */
@Composable
fun <T : Any> NetworkListPage(
    state: PagedUiState<T>,
    keyOf: (T) -> Any,
    emptyMessage: String,
    endMessage: String,
    onRefresh: () -> Unit,
    onInitialRetry: () -> Unit,
    onRefreshRetry: () -> Unit,
    onAppendRetry: () -> Unit,
    onContinueAfterPause: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = state.isRefreshing,
    listState: LazyListState = rememberLazyListState(),
    prefix: LazyListScope.() -> Unit = {},
    endTextAlign: TextAlign = TextAlign.Start,
    pagingEnabled: Boolean = true,
    itemContent: @Composable (T) -> Unit
) {
    val shouldLoadMore by remember(listState, state) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 &&
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 3 &&
                state.canAutoLoadMore
        }
    }
    LaunchedEffect(pagingEnabled, shouldLoadMore, state.datasetGeneration, state.nextPage) {
        if (pagingEnabled && shouldLoadMore) onLoadMore()
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { if (pagingEnabled) onRefresh() },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WanSpacing.section)
        ) {
            prefix()
            when {
                state.isInitialLoading -> item(key = "initial-loading") { LoadingContent() }

                state.initialError != null -> item(key = "initial-error") {
                    ErrorContent(errorMessage(requireNotNull(state.initialError)), onInitialRetry)
                }

                else -> {
                    state.refreshError?.let { error ->
                        item(key = "refresh-error") {
                            ErrorContent(errorMessage(error), onRefreshRetry)
                        }
                    }
                    if (state.items.isEmpty()) item(key = "empty") { MessageCard(emptyMessage) }
                    items(state.items, key = keyOf) { itemContent(it) }
                    // Empty pages with a forward cursor still expose continuation controls.
                    item(key = "load-more") {
                        if (state.items.isNotEmpty() || state.nextPage != null) {
                            LoadMoreContent(
                                isLoading = state.isLoadingMore,
                                paused = state.autoLoadPaused,
                                onContinue = onContinueAfterPause,
                                error = state.loadMoreError,
                                canLoadMore = state.canLoadMore,
                                onRetry = onAppendRetry,
                                endMessage = endMessage,
                                endTextAlign = endTextAlign
                            )
                        }
                    }
                }
            }
        }
    }
}
