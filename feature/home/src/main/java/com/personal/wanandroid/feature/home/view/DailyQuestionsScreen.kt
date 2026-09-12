package com.personal.wanandroid.feature.home.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.designsystem.theme.WanSpacing
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.ui.R as CoreUiR
import com.personal.wanandroid.core.ui.component.card.displayMetadata
import com.personal.wanandroid.core.ui.component.network.NetworkListPage
import com.personal.wanandroid.feature.home.R
import com.personal.wanandroid.feature.home.state.DailyQuestionsUiState
import com.personal.wanandroid.feature.home.viewmodel.DailyQuestionsViewModel

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
        onRetryRefresh = viewModel::retryRefresh,
        onContinueAfterPause = viewModel::continueAfterPause,
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
    onRetryRefresh: () -> Unit,
    onContinueAfterPause: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        NetworkListPage(
            state = uiState, keyOf = Article::id,
            emptyMessage = stringResource(R.string.questions_empty),
            endMessage = stringResource(R.string.questions_end),
            onRefresh = onRefresh, onInitialRetry = onRetryInitialLoad,
            onRefreshRetry = onRetryRefresh, onAppendRetry = onRetryLoadMore,
            onContinueAfterPause = onContinueAfterPause, onLoadMore = onLoadMore
        ) { question -> DailyQuestionListItem(question, onClick = { onArticleClick(question) }) }
    }
}

@Composable
private fun DailyQuestionListItem(question: Article, onClick: () -> Unit) {
    val metadata = question.displayMetadata(
        unknown = stringResource(CoreUiR.string.metadata_unknown),
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
                            stringResource(CoreUiR.string.article_author, metadata.byline)
                        } else {
                            stringResource(CoreUiR.string.article_sharer, metadata.byline)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(CoreUiR.string.article_category, metadata.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(CoreUiR.string.article_time, metadata.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
