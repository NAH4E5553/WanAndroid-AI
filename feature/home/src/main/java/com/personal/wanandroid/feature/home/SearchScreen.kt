package com.personal.wanandroid.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.ui.AppScaffold
import com.personal.wanandroid.core.ui.ErrorContent
import com.personal.wanandroid.core.ui.NetworkListPage
import com.personal.wanandroid.core.ui.errorMessage

@Composable
internal fun SearchRoute(
    onBack: () -> Unit,
    onArticleClick: (Article) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onInput = viewModel::editInput,
        onSubmit = viewModel::submit,
        onBack = onBack,
        onArticleClick = onArticleClick,
        onRefresh = viewModel::refresh,
        onInitialRetry = viewModel::retryInitialLoad,
        onRefreshRetry = viewModel::retryRefresh,
        onAppendRetry = viewModel::retryLoadMore,
        onContinueAfterPause = viewModel::continueAfterPause,
        onLoadMore = viewModel::loadMore,
        onSelectKeyword = viewModel::selectKeyword,
        onClearHistory = viewModel::clearHistory,
        onHotRetry = viewModel::retryHotKeys,
        onHistoryRetry = viewModel::retryHistory
    )
}

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onRefresh: () -> Unit,
    onInitialRetry: () -> Unit,
    onRefreshRetry: () -> Unit,
    onAppendRetry: () -> Unit,
    onContinueAfterPause: () -> Unit,
    onLoadMore: () -> Unit,
    onSelectKeyword: (String) -> Unit,
    onClearHistory: () -> Unit,
    onHotRetry: () -> Unit,
    onHistoryRetry: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        if (state.canSubmit) {
            keyboard?.hide()
            onSubmit()
        }
    }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    AppScaffold(topBar = { SearchHeader(state, onInput, submit, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (state.suggestions.historyWriteFailed) {
                Text(
                    stringResource(R.string.search_history_write_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            if (!state.showResults) {
                SearchSuggestions(
                    state.suggestions,
                    onSelect = {
                        keyboard?.hide()
                        onSelectKeyword(it)
                    },
                    onClear = { confirmClear = true },
                    onHotRetry = onHotRetry,
                    onHistoryRetry = onHistoryRetry
                )
            } else {
                Text(
                    stringResource(R.string.search_results, state.keyword),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                key(state.contextGeneration) {
                    val listState = rememberLazyListState()
                    NetworkListPage(
                        state = state.page, keyOf = Article::id,
                        emptyMessage = stringResource(R.string.search_empty),
                        endMessage = stringResource(R.string.articles_end),
                        onRefresh = onRefresh, onInitialRetry = onInitialRetry,
                        onRefreshRetry = onRefreshRetry, onAppendRetry = onAppendRetry,
                        onContinueAfterPause = onContinueAfterPause, onLoadMore = onLoadMore,
                        listState = listState
                    ) { article ->
                        ArticleCard(article, onClick = {
                            keyboard?.hide()
                            onArticleClick(article)
                        })
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.search_clear_history_title)) },
            text = { Text(stringResource(R.string.search_clear_history_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory()
                }) { Text(stringResource(R.string.search_clear_all)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmClear = false
                }) { Text(stringResource(R.string.search_cancel)) }
            }
        )
    }
}

@Composable
private fun SearchHeader(
    state: SearchUiState,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val fieldLabel = stringResource(R.string.search_keyword)
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_search_back), stringResource(R.string.back))
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.weight(1f)
            ) {
                Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_search),
                        null,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BasicTextField(
                        value = state.input, onValueChange = onInput, singleLine = true,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                            contentDescription =
                                fieldLabel
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        decorationBox = { field ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (state.input.isEmpty()) {
                                    Text(
                                        stringResource(R.string.search_placeholder),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                field()
                            }
                        }
                    )
                    if (state.input.isNotEmpty()) {
                        IconButton(onClick = { onInput("") }) {
                            Icon(
                                painterResource(R.drawable.ic_search_clear),
                                stringResource(R.string.search_clear),
                                Modifier.size(18.dp)
                            )
                        }
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = state.canSubmit,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.search_submit))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestions(
    state: SearchSuggestionsState,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onHotRetry: () -> Unit,
    onHistoryRetry: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.search_history),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (state.history.items.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        painterResource(R.drawable.ic_search_delete),
                        stringResource(R.string.search_clear_history)
                    )
                }
            }
        }
        when {
            !state.history.ready -> CircularProgressIndicator(Modifier.size(24.dp))

            state.history.readFailed -> ErrorContent(
                stringResource(R.string.search_history_read_failed),
                onHistoryRetry
            )

            state.history.items.isEmpty() -> Text(
                stringResource(R.string.search_history_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> KeywordChips(state.history.items, onSelect)
        }
        Text(
            stringResource(R.string.search_recommended),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )
        when {
            state.hotLoading -> CircularProgressIndicator(Modifier.size(24.dp))

            state.hotError != null -> ErrorContent(errorMessage(state.hotError), onHotRetry)

            state.hotKeys.isEmpty() -> Text(
                stringResource(R.string.search_recommended_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> KeywordChips(state.hotKeys, onSelect)
        }
    }
}

@Composable
private fun KeywordChips(keywords: List<String>, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keywords.forEach { keyword ->
            Surface(
                onClick = {
                    onSelect(keyword)
                },
                shape = RoundedCornerShape(
                    50
                ),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.heightIn(
                    min = 48.dp
                )
            ) {
                Text(
                    keyword,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
