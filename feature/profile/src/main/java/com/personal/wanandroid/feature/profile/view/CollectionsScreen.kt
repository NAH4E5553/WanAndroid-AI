package com.personal.wanandroid.feature.profile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.designsystem.theme.WanSpacing
import com.personal.wanandroid.core.model.CollectionItem
import com.personal.wanandroid.core.ui.component.card.ArticleCard
import com.personal.wanandroid.core.ui.component.network.NetworkListPage
import com.personal.wanandroid.core.ui.component.network.errorMessage
import com.personal.wanandroid.feature.profile.R
import com.personal.wanandroid.feature.profile.state.CollectionsUiState
import com.personal.wanandroid.feature.profile.viewmodel.CollectionsViewModel

@Composable
fun CollectionsRoute(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onArticle: (CollectionItem) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionsScreen(
        state, onBack, onLogin, onArticle,
        { item -> state.collections.generation?.let { viewModel.remove(item, it) } },
        viewModel::refresh, viewModel::retryInitial, viewModel::retryRefresh,
        viewModel::retryAppend, viewModel::loadMore, viewModel::continueAfterPause
    )
}

@Composable
fun CollectionsScreen(
    state: CollectionsUiState,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onArticle: (CollectionItem) -> Unit,
    onRemove: (CollectionItem) -> Unit,
    onRefresh: () -> Unit,
    onRetryInitial: () -> Unit,
    onRetryRefresh: () -> Unit,
    onRetryAppend: () -> Unit,
    onLoadMore: () -> Unit,
    onContinue: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
        ) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(
                    stringResource(R.string.collections),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(WanSpacing.small)
                )
            }
            if (state.collections.generation == null) {
                Text(
                    stringResource(R.string.collections_login_required),
                    Modifier.padding(WanSpacing.page)
                )
                TextButton(onClick = onLogin) { Text(stringResource(R.string.login)) }
            } else {
                state.error?.let {
                    Text(
                        errorMessage(it),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(WanSpacing.page)
                    )
                }
                key(state.collections.generation) {
                    NetworkListPage(
                        state = state.page,
                        keyOf = { requireNotNull(it.target.recordId) },
                        emptyMessage = stringResource(R.string.collections_empty),
                        endMessage = stringResource(R.string.collections_end),
                        onRefresh = onRefresh,
                        onInitialRetry = onRetryInitial,
                        onRefreshRetry = onRetryRefresh,
                        onAppendRetry = onRetryAppend,
                        onContinueAfterPause = onContinue,
                        onLoadMore = onLoadMore,
                        endTextAlign = TextAlign.Center
                    ) { item ->
                        Column {
                            ArticleCard(item.article, { onArticle(item) })
                            val status = state.collections.status(item.target)
                            TextButton(
                                onClick = { onRemove(item) },
                                enabled = !status.busy,
                                modifier = Modifier.padding(horizontal = WanSpacing.page)
                            ) {
                                Text(
                                    stringResource(
                                        when {
                                            status.busy -> R.string.collection_busy
                                            status.collected == null -> R.string.collection_verify
                                            else -> R.string.collection_remove
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
