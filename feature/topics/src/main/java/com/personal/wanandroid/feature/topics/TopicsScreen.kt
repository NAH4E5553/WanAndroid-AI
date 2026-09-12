package com.personal.wanandroid.feature.topics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.common.LoadState
import com.personal.wanandroid.core.common.PagedUiState
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.ui.ArticleCard
import com.personal.wanandroid.core.ui.ErrorContent
import com.personal.wanandroid.core.ui.LoadingContent
import com.personal.wanandroid.core.ui.MessageCard
import com.personal.wanandroid.core.ui.NetworkListPage
import com.personal.wanandroid.core.ui.errorMessage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun TopicsRoute(
    onArticleClick: (url: String, title: String, articleId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopicsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TopicsScreen(
        state, viewModel::selectParent, viewModel::selectChild, viewModel::retryTopics,
        onArticleClick = onArticleClick,
        onRefresh = viewModel::refresh,
        onInitialRetry = viewModel::retryInitial,
        onRefreshRetry = viewModel::retryRefresh,
        onAppendRetry = viewModel::retryAppend,
        onContinue = viewModel::continueAfterPause,
        onLoadMore = viewModel::loadMore,
        modifier = modifier
    )
}

@Composable
internal fun TopicsScreen(
    state: TopicsUiState,
    onSelect: (Long) -> Unit,
    onSelectChild: (Long, Long) -> Unit,
    onRetryTopics: () -> Unit,
    onArticleClick: (String, String, Long) -> Unit,
    onRefresh: (Long) -> Unit,
    onInitialRetry: (Long) -> Unit,
    onRefreshRetry: (Long) -> Unit,
    onAppendRetry: (Long) -> Unit,
    onContinue: (Long) -> Unit,
    onLoadMore: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.topics),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
            when {
                state.loading -> LoadingContent()

                state.error != null -> ErrorContent(errorMessage(state.error), onRetryTopics)

                state.parents.isEmpty() -> MessageCard(stringResource(R.string.topics_empty))

                else -> {
                    val holder = rememberSaveableStateHolder()
                    // Selection changes only after the pager settles, so a cancelled drag
                    // cannot resize the viewport underneath the user's finger.
                    val sidebarExpanded = state.selectedId == state.tabs.firstOrNull()?.id
                    val sidebarTransition = updateTransition(sidebarExpanded, label = "sidebar")
                    val sidebarFraction by sidebarTransition.animateFloat(
                        transitionSpec = { tween(320, easing = FastOutSlowInEasing) },
                        label = "sidebar-width"
                    ) { expanded -> if (expanded) 1f else 0f }
                    Row(Modifier.weight(1f).fillMaxWidth().testTag("topic-layout")) {
                        Box(
                            Modifier.width(100.dp * sidebarFraction).fillMaxHeight()
                                .clipToBounds()
                                .then(
                                    if (sidebarExpanded) {
                                        Modifier
                                    } else {
                                        Modifier.clearAndSetSemantics {}
                                    }
                                )
                        ) {
                            // Keep the menu composed at its normal width to preserve its scroll
                            // position and avoid text reflow as its visible slot closes.
                            TopicMenu(
                                state.parents,
                                state.selectedParentId,
                                onSelect,
                                Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
                                    .width(100.dp).fillMaxHeight()
                                    .graphicsLayer {
                                        translationX = -100.dp.toPx() * (1f - sidebarFraction)
                                        alpha = sidebarFraction
                                    }
                            )
                        }
                        val parentId = state.selectedParentId
                        if (parentId != null && state.tabs.isEmpty()) {
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                MessageCard(stringResource(R.string.subtopics_empty))
                            }
                        } else if (parentId != null) {
                            key(parentId) {
                                val tabs = state.tabs
                                val pager = rememberPagerState(
                                    initialPage = tabs.indexOfFirst { it.id == state.selectedId }
                                        .coerceAtLeast(0),
                                    pageCount = { tabs.size }
                                )
                                val scope = rememberCoroutineScope()
                                val selectChild by rememberUpdatedState(onSelectChild)
                                // Commit only settled pages; composing a neighbour makes no request.
                                LaunchedEffect(pager) {
                                    snapshotFlow { pager.settledPage }.distinctUntilChanged()
                                        .collect { selectChild(parentId, tabs[it].id) }
                                }
                                Column(
                                    Modifier.weight(1f).fillMaxHeight().testTag("topic-detail")
                                        .graphicsLayer {
                                            // A subtle fade during resize; settled content is opaque.
                                            alpha =
                                                1f - 0.6f * sidebarFraction * (1f - sidebarFraction)
                                        }
                                ) {
                                    SecondaryScrollableTabRow(
                                        selectedTabIndex = pager.currentPage,
                                        edgePadding = 0.dp,
                                        divider = {},
                                        modifier = Modifier.testTag("topic-tabs")
                                    ) {
                                        tabs.forEachIndexed { index, topic ->
                                            Tab(
                                                selected = pager.currentPage == index,
                                                selectedContentColor =
                                                    MaterialTheme.colorScheme.primary,
                                                unselectedContentColor =
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                onClick = {
                                                    scope.launch {
                                                        pager.animateScrollToPage(index)
                                                    }
                                                },
                                                modifier = Modifier.testTag(
                                                    "topic-tab-${topic.id}"
                                                ),
                                                text = {
                                                    Text(
                                                        topic.name
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    HorizontalPager(
                                        state = pager,
                                        key = { tabs[it].id },
                                        // Resize only between gestures; vertical scrolling remains available.
                                        userScrollEnabled = tabs.size > 1 &&
                                            !sidebarTransition.isRunning,
                                        modifier = Modifier.weight(1f).testTag("topic-pager")
                                    ) { index ->
                                        val id = tabs[index].id
                                        holder.SaveableStateProvider(id) {
                                            val active = state.selectedId == id
                                            NetworkListPage(
                                                state.pageStates[id]
                                                    ?: PagedUiState(initial = LoadState.Loading),
                                                keyOf = { it.id },
                                                emptyMessage = stringResource(
                                                    R.string.articles_empty
                                                ),
                                                endMessage = stringResource(R.string.articles_end),
                                                endTextAlign = TextAlign.Center,
                                                pagingEnabled = active &&
                                                    pager.settledPage == index &&
                                                    !pager.isScrollInProgress &&
                                                    !sidebarTransition.isRunning,
                                                onRefresh = { onRefresh(id) },
                                                onInitialRetry = { onInitialRetry(id) },
                                                onRefreshRetry = { onRefreshRetry(id) },
                                                onAppendRetry = { onAppendRetry(id) },
                                                onContinueAfterPause = { onContinue(id) },
                                                onLoadMore = { onLoadMore(id) },
                                                listState = rememberLazyListState(),
                                                modifier = Modifier.testTag(
                                                    if (active) {
                                                        "topic-articles"
                                                    } else {
                                                        "topic-preview-$id"
                                                    }
                                                )
                                            ) { article ->
                                                ArticleCard(article) {
                                                    onArticleClick(
                                                        article.url,
                                                        article.title,
                                                        article.id
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Adapted from CoolMallKotlin CategoryScreen (0e6a9aa): selection and rounded groups.
// The menu follows explicit clicks only; the article list never changes selectedId.
@Composable
private fun TopicMenu(
    topics: List<Topic>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier
) {
    val listState = rememberLazyListState()
    val index = topics.indexOfFirst { it.id == selectedId }
    val groupColor = MaterialTheme.colorScheme.surfaceContainerLow
    LaunchedEffect(index) {
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.animateScrollToItem(index)
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surface)) {
        Canvas(Modifier.fillMaxSize()) {
            drawTopicGroups(listState, index, topics.size, groupColor)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("topic-menu")) {
            itemsIndexed(topics, key = { _, topic -> topic.id }) { _, topic ->
                val selected = topic.id == selectedId
                val color by animateColorAsState(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "topic-color"
                )
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .testTag("topic-${topic.id}")
                        .selectable(
                            selected = selected,
                            enabled = !selected,
                            role = Role.Tab,
                            onClick = { onSelect(topic.id) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Spacer(
                            Modifier.align(Alignment.CenterStart).width(3.dp).height(24.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        topic.name,
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTopicGroups(
    listState: LazyListState,
    selectedIndex: Int,
    categoryCount: Int,
    color: Color
) {
    if (categoryCount == 0 || selectedIndex !in 0 until categoryCount) return

    val visibleItems = listState.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    val selectedItem = visibleItems.firstOrNull { it.index == selectedIndex }
    val viewportHeight = size.height

    if (selectedItem == null) {
        drawRightRoundedRect(
            color = color,
            top = 0f,
            bottom = viewportHeight,
            topEndRadius = 0f,
            bottomEndRadius = 0f
        )
        return
    }

    val radius = 20.dp.toPx()
    val selectedTop = selectedItem.offset.toFloat().coerceIn(0f, viewportHeight)
    val selectedBottom = (selectedItem.offset + selectedItem.size).toFloat()
        .coerceIn(0f, viewportHeight)
    val firstVisibleIndex = visibleItems.first().index

    if (selectedIndex > 0 && selectedTop > 0f) {
        drawRightRoundedRect(
            color = color,
            top = 0f,
            bottom = selectedTop,
            topEndRadius = if (firstVisibleIndex == 0) radius else 0f,
            bottomEndRadius = radius
        )
    }

    if (selectedBottom < viewportHeight) {
        drawRightRoundedRect(
            color = color,
            top = selectedBottom,
            bottom = viewportHeight,
            topEndRadius = radius,
            bottomEndRadius = 0f
        )
    }
}

private fun DrawScope.drawRightRoundedRect(
    color: Color,
    top: Float,
    bottom: Float,
    topEndRadius: Float,
    bottomEndRadius: Float
) {
    if (bottom <= top) return

    val right = size.width
    val height = bottom - top
    val topRadius = topEndRadius.coerceAtMost(height / 2f)
    val bottomRadius = bottomEndRadius.coerceAtMost(height / 2f)

    val path = Path().apply {
        moveTo(0f, top)
        lineTo(right - topRadius, top)
        if (topRadius > 0f) {
            quadraticTo(right, top, right, top + topRadius)
        } else {
            lineTo(right, top)
        }
        lineTo(right, bottom - bottomRadius)
        if (bottomRadius > 0f) {
            quadraticTo(right, bottom, right - bottomRadius, bottom)
        } else {
            lineTo(right, bottom)
        }
        lineTo(0f, bottom)
        close()
    }

    drawPath(path = path, color = color)
}
