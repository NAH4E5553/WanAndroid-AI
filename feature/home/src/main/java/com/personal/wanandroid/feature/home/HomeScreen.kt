package com.personal.wanandroid.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.personal.wanandroid.core.designsystem.WanSpacing
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.DataError
import com.personal.wanandroid.core.ui.FeaturePlaceholder
import kotlinx.coroutines.delay

@Composable
fun HomeRoute(
    onSearch: () -> Unit,
    onQuestionsClick: () -> Unit,
    onArticleClick: (url: String, title: String, articleId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onSearch = onSearch,
        onQuestionsClick = onQuestionsClick,
        onArticleClick = { article ->
            onArticleClick(article.url, article.title, article.id)
        },
        onRefresh = viewModel::refresh,
        onRetryQuestions = viewModel::retryQuestions,
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
    onQuestionsClick: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onRefresh: () -> Unit,
    onRetryQuestions: () -> Unit,
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
        isRefreshing = uiState.isPullRefreshing,
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
            item(key = "daily-question") {
                DailyQuestionSection(
                    questions = uiState.questions,
                    isLoading = uiState.isQuestionLoading,
                    error = uiState.questionError,
                    onSeeAll = onQuestionsClick,
                    onQuestionClick = onArticleClick,
                    onRetry = onRetryQuestions
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
private fun DailyQuestionSection(
    questions: List<Article>,
    isLoading: Boolean,
    error: DataError?,
    onSeeAll: () -> Unit,
    onQuestionClick: (Article) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = WanSpacing.section),
        verticalArrangement = Arrangement.spacedBy(WanSpacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = WanSpacing.page),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.daily_question),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.see_more))
            }
        }

        when {
            isLoading && questions.isEmpty() -> QuestionLoadingCard()

            error != null && questions.isEmpty() -> QuestionErrorCard(error, onRetry)

            questions.isEmpty() -> MessageCard(stringResource(R.string.questions_empty))

            else -> {
                DailyQuestionCarousel(questions, onQuestionClick)
                if (error != null) {
                    QuestionRefreshError(error, onRetry)
                }
            }
        }
    }
}

@Composable
private fun DailyQuestionCarousel(questions: List<Article>, onQuestionClick: (Article) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val questionIds = remember(questions) { questions.map(Article::id) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, questions.lastIndex)

    LaunchedEffect(questionIds, lifecycleOwner) {
        currentIndex = safeIndex
        if (questions.size <= 1) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(QUESTION_INTERVAL_MILLIS)
                currentIndex = nextQuestionIndex(currentIndex, questions.size)
            }
        }
    }

    AnimatedContent(
        targetState = questions[safeIndex],
        transitionSpec = {
            (slideInVertically { height -> height } + fadeIn()) togetherWith
                (slideOutVertically { height -> -height } + fadeOut())
        },
        contentKey = Article::id,
        label = "daily-question",
        modifier = Modifier.fillMaxWidth().padding(horizontal = WanSpacing.page)
    ) { question ->
        DailyQuestionCard(
            question = question,
            currentIndex = questions.indexOfFirst { it.id == question.id }.coerceAtLeast(0),
            questionCount = questions.size,
            onClick = { onQuestionClick(question) }
        )
    }
}

@Composable
private fun DailyQuestionCard(
    question: Article,
    currentIndex: Int,
    questionCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 144.dp)) {
            Box(
                modifier = Modifier.fillMaxHeight().width(4.dp).background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                )
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(WanSpacing.page),
                verticalArrangement = Arrangement.spacedBy(WanSpacing.medium)
            ) {
                Text(
                    text = AnnotatedString.fromHtml(question.title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 26.sp
                    )
                )
                Spacer(Modifier.weight(1f))
                QuestionPosition(currentIndex, questionCount)
            }
        }
    }
}

@Composable
private fun QuestionPosition(currentIndex: Int, questionCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(questionCount) { index ->
            Box(
                modifier = Modifier.height(5.dp).width(if (index == currentIndex) 16.dp else 5.dp)
                    .background(
                        color = if (index == currentIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(5.dp)
                    )
            )
        }
    }
}

@Composable
private fun QuestionLoadingCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.padding(horizontal = WanSpacing.page).fillMaxWidth().height(144.dp)
    ) {
        Box(
            Modifier.fillMaxSize().padding(WanSpacing.section),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun QuestionErrorCard(error: DataError, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = WanSpacing.page)
    ) {
        Column(
            modifier = Modifier.padding(WanSpacing.page),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.small)
        ) {
            Text(errorMessage(error), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun QuestionRefreshError(error: DataError, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WanSpacing.page),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = errorMessage(error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

internal fun nextQuestionIndex(currentIndex: Int, questionCount: Int): Int =
    if (questionCount <= 1) 0 else (currentIndex + 1) % questionCount

private const val QUESTION_INTERVAL_MILLIS = 4_000L

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
            val metadata = article.displayMetadata(stringResource(R.string.metadata_unknown))
            Text(
                text = AnnotatedString.fromHtml(article.title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (metadata.usesAuthorLabel) {
                    stringResource(R.string.article_author, metadata.byline)
                } else {
                    stringResource(R.string.article_sharer, metadata.byline)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.article_category,
                    metadata.category
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.article_time, metadata.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal data class ArticleDisplayMetadata(
    val usesAuthorLabel: Boolean,
    val byline: String,
    val category: String,
    val publishedAt: String
)

internal fun Article.displayMetadata(
    unknown: String,
    categorySeparator: String = "/"
): ArticleDisplayMetadata {
    val usesAuthorLabel = author.isNotBlank()
    return ArticleDisplayMetadata(
        usesAuthorLabel = usesAuthorLabel,
        byline = if (usesAuthorLabel) author else shareUser.ifBlank { unknown },
        category = listOf(superChapterName, chapter)
            .filter(String::isNotBlank)
            .joinToString(categorySeparator)
            .ifBlank { unknown },
        publishedAt = publishedAt.ifBlank { unknown }
    )
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
