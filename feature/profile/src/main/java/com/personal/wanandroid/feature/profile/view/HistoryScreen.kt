package com.personal.wanandroid.feature.profile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.designsystem.theme.WanSpacing
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.core.ui.component.list.AppListItem
import com.personal.wanandroid.core.ui.component.network.NetworkListPage
import com.personal.wanandroid.core.ui.component.network.errorMessage
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
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var clearRequested by rememberSaveable { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(
                    stringResource(R.string.history),
                    Modifier.weight(1f).padding(WanSpacing.small),
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(
                    onClick = { clearRequested = true },
                    enabled = !state.busy && state.page.items.isNotEmpty()
                ) { Text(stringResource(R.string.history_clear)) }
            }
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
                endTextAlign = TextAlign.Center
            ) { item ->
                AppListItem(
                    title = item.title,
                    description = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(item.lastReadAt)),
                    onClick = { onArticle(item) },
                    trailingContent = {
                        TextButton(
                            onClick = { pendingDelete = item.url },
                            enabled = !state.busy
                        ) {
                            Text(stringResource(R.string.history_delete))
                        }
                    }
                )
            }
        }
    }
    if (clearRequested || pendingDelete != null) {
        AlertDialog(
            onDismissRequest = {
                clearRequested = false
                pendingDelete = null
            },
            title = {
                val title = if (clearRequested) R.string.history_clear else R.string.history_delete
                Text(stringResource(title))
            },
            text = { Text(stringResource(R.string.history_delete_hint)) },
            confirmButton = {
                TextButton(enabled = !state.busy, onClick = {
                    if (clearRequested) onClear() else pendingDelete?.let(onDelete)
                    clearRequested = false
                    pendingDelete = null
                }) { Text(stringResource(R.string.history_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    clearRequested = false
                    pendingDelete = null
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
