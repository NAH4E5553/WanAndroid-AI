package com.personal.wanandroid.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.personal.wanandroid.core.common.LoadState
import com.personal.wanandroid.core.common.PagedUiState
import com.personal.wanandroid.core.designsystem.WanTheme
import com.personal.wanandroid.core.result.DataError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NetworkListPageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scrollingToEndTriggersLoadingWithoutChangingCursor() {
        val state = mutableStateOf(PagedUiState(items = (1..40).toList(), nextPage = 1))
        var loads = 0
        page(state, onLoad = { loads++ })
        compose.runOnIdle { assertEquals(0, loads) }
        compose.onNode(hasScrollAction()).performScrollToIndex(39)
        compose.waitUntil { loads == 1 }
    }

    @Test fun pausedAutomaticEntryIsSilentAndContinueCallsCorrectCallback() {
        val state =
            mutableStateOf(PagedUiState(items = listOf(1), nextPage = 3, autoLoadPaused = true))
        var loads = 0
        var continues = 0
        page(state, onLoad = { loads++ }, onContinue = { continues++ })
        compose.onNodeWithText("继续加载").performClick()
        compose.runOnIdle {
            assertEquals(0, loads)
            assertEquals(1, continues)
        }
    }

    @Test fun refreshFailureUsesRefreshRetryRatherThanAppendOrWholePageRefresh() {
        val state = mutableStateOf(
            PagedUiState(
                items = listOf(1),
                nextPage = 1,
                refresh = LoadState.Failure(DataError.NETWORK)
            )
        )
        var refreshRetry = 0
        page(state, onRefreshRetry = { refreshRetry++ })
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertEquals(1, refreshRetry) }
    }

    @Test fun newDatasetAtSameCursorReevaluatesAndArticleClickKeepsItsIdentity() {
        val state = mutableStateOf(PagedUiState(items = listOf(7), nextPage = 1))
        var loads = 0
        var clicked: Int? = null
        page(state, onLoad = { loads++ }, onItem = { clicked = it })
        compose.waitUntil { loads == 1 }
        compose.runOnIdle { state.value = state.value.copy(datasetGeneration = 1) }
        compose.waitUntil { loads == 2 }
        compose.onNodeWithText("Item 7").performClick()
        compose.runOnIdle { assertEquals(7, clicked) }
    }

    @Test fun nonInteractiveSettingHasNoClickAction() {
        compose.setContent { WanTheme { AppListItem(title = "Read only") } }
        compose.onNodeWithText("Read only").assertHasNoClickAction()
    }

    private fun page(
        state: MutableState<PagedUiState<Int>>,
        onLoad: () -> Unit = {},
        onContinue: () -> Unit = {},
        onRefreshRetry: () -> Unit = {},
        onItem: (Int) -> Unit = {}
    ) {
        compose.setContent {
            WanTheme {
                NetworkListPage(
                    state = state.value, keyOf = { it }, emptyMessage = "Empty", endMessage = "End",
                    onRefresh = { error("Unexpected whole-page refresh") },
                    onInitialRetry = { error("Unexpected initial retry") },
                    onRefreshRetry = onRefreshRetry,
                    onAppendRetry = { error("Unexpected append retry") },
                    onContinueAfterPause = onContinue, onLoadMore = onLoad
                ) { item ->
                    Text(
                        "Item $item",
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                            .clickable { onItem(item) }
                    )
                }
            }
        }
    }
}
