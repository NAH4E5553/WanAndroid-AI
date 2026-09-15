package com.personal.wanandroid.feature.profile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.designsystem.theme.WanSpacing
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.core.ui.component.list.AppListItem
import com.personal.wanandroid.core.ui.component.list.SwipeRevealActionItem
import com.personal.wanandroid.core.ui.component.list.collapseSwipeRevealOnVerticalScroll
import com.personal.wanandroid.core.ui.component.list.rememberSwipeRevealListState
import com.personal.wanandroid.core.ui.component.network.NetworkListPage
import com.personal.wanandroid.core.ui.component.network.errorMessage
import com.personal.wanandroid.core.ui.component.scaffold.AppTopBar
import com.personal.wanandroid.feature.profile.R
import com.personal.wanandroid.feature.profile.state.HistoryUiState
import com.personal.wanandroid.feature.profile.viewmodel.HistoryViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onArticle: (ReadingHistory) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state, onBack, onArticle, viewModel::delete, viewModel::clear,
        viewModel::refresh, viewModel::retryInitial, viewModel::retryRefresh,
        viewModel::retryAppend, viewModel::loadMore, viewModel::continueAfterPause
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onArticle: (ReadingHistory) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onRetryInitial: () -> Unit,
    onRetryRefresh: () -> Unit,
    onRetryAppend: () -> Unit,
    onLoadMore: () -> Unit,
    onContinue: () -> Unit
) {
    var clearRequested by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val swipeState = rememberSwipeRevealListState<String>()
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            AppTopBar(
                title = stringResource(R.string.history),
                backContentDescription = stringResource(R.string.back),
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = { clearRequested = true },
                        enabled = !state.busy && state.page.items.isNotEmpty()
                    ) { Text(stringResource(R.string.history_clear)) }
                }
            )
            Text(
                stringResource(R.string.history_local_hint),
                Modifier.padding(horizontal = WanSpacing.page),
                style = MaterialTheme.typography.bodySmall
            )
            state.error?.let {
                Text(
                    errorMessage(it),
                    Modifier.padding(WanSpacing.page),
                    color = MaterialTheme.colorScheme.error
                )
            }
            NetworkListPage(
                state = state.page, keyOf = ReadingHistory::url,
                emptyMessage = stringResource(R.string.history_empty),
                endMessage = stringResource(R.string.collections_end),
                onRefresh = onRefresh,
                onInitialRetry = onRetryInitial,
                onRefreshRetry = onRetryRefresh,
                onAppendRetry = onRetryAppend,
                onLoadMore = onLoadMore,
                onContinueAfterPause = onContinue,
                endTextAlign = TextAlign.Center,
                listState = listState,
                modifier = Modifier.collapseSwipeRevealOnVerticalScroll(swipeState)
            ) { item ->
                SwipeRevealActionItem(
                    itemKey = item.url,
                    enabled = !state.busy,
                    revealed = swipeState.isRevealed(item.url),
                    actionContentDescription = stringResource(R.string.history_delete),
                    contentPadding = PaddingValues(
                        horizontal = WanSpacing.page,
                        vertical = WanSpacing.small
                    ),
                    foregroundModifier = Modifier.testTag("history-item-${item.url}"),
                    actionModifier = Modifier.testTag("history-delete-background-${item.url}"),
                    onRevealed = { swipeState.reveal(item.url) },
                    onClosed = { swipeState.close(item.url) },
                    onAction = {
                        swipeState.close()
                        onDelete(item.url)
                    }
                ) {
                    AppListItem(
                        title = item.title,
                        description = DateFormat.getDateTimeInstance(
                            DateFormat.SHORT,
                            DateFormat.SHORT
                        ).format(Date(item.lastReadAt)),
                        onClick = { onArticle(item) }
                    )
                }
            }
        }
    }
    if (clearRequested) {
        AlertDialog(
            onDismissRequest = { clearRequested = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear_hint)) },
            confirmButton = {
                TextButton(enabled = !state.busy, onClick = {
                    onClear()
                    clearRequested = false
                }) { Text(stringResource(R.string.history_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { clearRequested = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
