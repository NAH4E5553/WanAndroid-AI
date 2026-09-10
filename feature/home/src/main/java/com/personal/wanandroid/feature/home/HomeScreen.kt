package com.personal.wanandroid.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.designsystem.WanSpacing
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.DataError
import com.personal.wanandroid.core.ui.FeaturePlaceholder

@Composable
fun HomeRoute(
    onSearch: () -> Unit,
    onArticleClick: (url: String, title: String, articleId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onSearch = onSearch,
        onArticleClick = { article ->
            onArticleClick(article.url, article.title, article.id)
        },
        onRefresh = viewModel::refresh,
        onRetryInitialLoad = viewModel::retryInitialLoad,
        onLoadMore = viewModel::loadMore,
        onRetryLoadMore = viewModel::retryLoadMore,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearch: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onRefresh: () -> Unit,
    onRetryInitialLoad: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 && lastVisibleIndex >= layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(
        shouldLoadMore,
        uiState.nextPage,
        uiState.isInitialLoading,
        uiState.isRefreshing,
        uiState.isLoadingMore,
        uiState.loadMoreError
    ) {
        if (
            shouldLoadMore &&
            uiState.canLoadMore &&
            !uiState.isInitialLoading &&
            !uiState.isRefreshing &&
            !uiState.isLoadingMore &&
            uiState.loadMoreError == null
        ) {
            onLoadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WanSpacing.section)
        ) {
            item(key = "search") {
                OutlinedButton(
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)
                ) {
                    Text(stringResource(R.string.search_hint))
                }
            }
            item(key = "question-placeholder") {
                FeaturePlaceholder(
                    title = stringResource(R.string.daily_question),
                    description = stringResource(R.string.question_pending)
                )
            }
            item(key = "article-heading") {
                Text(
                    text = stringResource(R.string.latest_articles),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(
                        start = WanSpacing.page,
                        end = WanSpacing.page,
                        top = WanSpacing.small,
                        bottom = WanSpacing.medium
                    )
                )
            }

            when {
                uiState.isInitialLoading -> item(key = "initial-loading") {
                    LoadingContent()
                }

                uiState.initialError != null -> item(key = "initial-error") {
                    ErrorContent(
                        message = errorMessage(uiState.initialError),
                        onRetry = onRetryInitialLoad
                    )
                }

                uiState.articles.isEmpty() -> item(key = "empty") {
                    MessageCard(stringResource(R.string.articles_empty))
                }

                else -> {
                    uiState.refreshError?.let { error ->
                        item(key = "refresh-error") {
                            ErrorContent(message = errorMessage(error), onRetry = onRefresh)
                        }
                    }
                    items(items = uiState.articles, key = Article::id) { article ->
                        ArticleCard(article = article, onClick = { onArticleClick(article) })
                    }
                    item(key = "load-more") {
                        LoadMoreContent(
                            isLoading = uiState.isLoadingMore,
                            error = uiState.loadMoreError,
                            canLoadMore = uiState.canLoadMore,
                            onRetry = onRetryLoadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = WanSpacing.page,
            vertical = WanSpacing.small
        )
    ) {
        Column(
            modifier = Modifier.padding(WanSpacing.page),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.small)
        ) {
            Text(
                text = AnnotatedString.fromHtml(article.title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (article.author.isNotBlank()) {
                    stringResource(R.string.article_author, article.author)
                } else {
                    stringResource(R.string.article_sharer, article.shareUser)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.article_category,
                    article.superChapterName,
                    article.chapter
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.article_time, article.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(WanSpacing.section),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)) {
        Column(
            modifier = Modifier.padding(WanSpacing.page),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.medium)
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(WanSpacing.section)
        )
    }
}

@Composable
private fun LoadMoreContent(
    isLoading: Boolean,
    error: DataError?,
    canLoadMore: Boolean,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingContent()

        error != null -> ErrorContent(message = errorMessage(error), onRetry = onRetry)

        !canLoadMore -> Text(
            text = stringResource(R.string.articles_end),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)
        )
    }
}

@Composable
private fun errorMessage(error: DataError): String = stringResource(
    when (error) {
        DataError.NETWORK -> R.string.error_network
        DataError.SERVICE -> R.string.error_service
        DataError.SESSION_EXPIRED -> R.string.error_session_expired
        DataError.INVALID_RESPONSE -> R.string.error_invalid_response
    }
)

@Composable
fun SearchScreen(onBack: () -> Unit) {
    Column(Modifier.safeDrawingPadding()) {
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        FeaturePlaceholder(
            stringResource(R.string.search_hint),
            stringResource(R.string.search_pending)
        )
    }
}
