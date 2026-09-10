package com.personal.wanandroid.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.designsystem.WanSpacing
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.DataError

@Composable
fun DailyQuestionsRoute(
    onBack: () -> Unit,
    onArticleClick: (url: String, title: String, articleId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyQuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DailyQuestionsScreen(
        uiState = uiState,
        onBack = onBack,
        onArticleClick = { article -> onArticleClick(article.url, article.title, article.id) },
        onRefresh = viewModel::refresh,
        onRetryInitialLoad = viewModel::retryInitialLoad,
        onLoadMore = viewModel::loadMore,
        onRetryLoadMore = viewModel::retryLoadMore,
        modifier = modifier
    )
}

@Composable
fun DailyQuestionsScreen(
    uiState: DailyQuestionsUiState,
    onBack: () -> Unit,
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

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = WanSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(72.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
            Text(
                text = stringResource(R.string.daily_question),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(72.dp))
        }
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = WanSpacing.section)
            ) {
                when {
                    uiState.isInitialLoading -> item(key = "initial-loading") {
                        DailyQuestionsLoading()
                    }

                    uiState.initialError != null -> item(key = "initial-error") {
                        DailyQuestionsError(
                            message = dailyQuestionErrorMessage(uiState.initialError),
                            onRetry = onRetryInitialLoad
                        )
                    }

                    uiState.questions.isEmpty() -> item(key = "empty") {
                        DailyQuestionsMessage(stringResource(R.string.questions_empty))
                    }

                    else -> {
                        uiState.refreshError?.let { error ->
                            item(key = "refresh-error") {
                                DailyQuestionsError(
                                    message = dailyQuestionErrorMessage(error),
                                    onRetry = onRefresh
                                )
                            }
                        }
                        items(items = uiState.questions, key = Article::id) { question ->
                            DailyQuestionListItem(
                                question = question,
                                onClick = { onArticleClick(question) }
                            )
                        }
                        item(key = "load-more") {
                            DailyQuestionsLoadMore(
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
}

@Composable
private fun DailyQuestionListItem(question: Article, onClick: () -> Unit) {
    val metadata = question.displayMetadata(
        unknown = stringResource(R.string.metadata_unknown),
        categorySeparator = " / "
    )
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = WanSpacing.page,
            vertical = WanSpacing.small
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier.fillMaxHeight().width(3.dp).background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                )
            )
            Column(
                modifier = Modifier.padding(WanSpacing.page),
                verticalArrangement = Arrangement.spacedBy(WanSpacing.medium)
            ) {
                Text(
                    text = AnnotatedString.fromHtml(question.title),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(WanSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(WanSpacing.small),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.widthIn(min = 48.dp).height(28.dp).border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ).padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.question_tag),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = if (metadata.usesAuthorLabel) {
                            stringResource(R.string.article_author, metadata.byline)
                        } else {
                            stringResource(R.string.article_sharer, metadata.byline)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.article_category, metadata.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.article_time, metadata.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyQuestionsLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(WanSpacing.section),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DailyQuestionsError(message: String, onRetry: () -> Unit) {
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
private fun DailyQuestionsMessage(message: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(WanSpacing.section)
        )
    }
}

@Composable
private fun DailyQuestionsLoadMore(
    isLoading: Boolean,
    error: DataError?,
    canLoadMore: Boolean,
    onRetry: () -> Unit
) {
    when {
        isLoading -> DailyQuestionsLoading()

        error != null -> DailyQuestionsError(dailyQuestionErrorMessage(error), onRetry)

        !canLoadMore -> Text(
            text = stringResource(R.string.questions_end),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)
        )
    }
}

@Composable
private fun dailyQuestionErrorMessage(error: DataError): String = stringResource(
    when (error) {
        DataError.NETWORK -> R.string.error_network
        DataError.SERVICE -> R.string.error_service
        DataError.SESSION_EXPIRED -> R.string.error_session_expired
        DataError.INVALID_RESPONSE -> R.string.error_invalid_response
    }
)
