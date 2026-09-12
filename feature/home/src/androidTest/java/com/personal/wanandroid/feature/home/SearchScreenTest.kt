package com.personal.wanandroid.feature.home

import android.graphics.Bitmap
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.data.ArticleRepository
import com.personal.wanandroid.core.data.SearchSuggestionsRepository
import com.personal.wanandroid.core.designsystem.WanTheme
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.SearchHistory
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule val compose = createComposeRule()
    private val repository = FixtureSearchRepository()
    private val suggestions = FixtureSuggestions()
    private val selected = mutableStateOf<Article?>(null)
    private lateinit var viewModel: SearchViewModel

    private fun mount(dark: Boolean = false, fontScale: Float = 1f) {
        compose.runOnIdle {
            viewModel = SearchViewModel(repository, SavedStateHandle(), suggestions)
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                WanTheme(dark = dark) {
                    val holder = rememberSaveableStateHolder()
                    val article by selected
                    if (article == null) {
                        holder.SaveableStateProvider("search-entry") {
                            SearchRoute(onBack = {
                            }, onArticleClick = { selected.value = it }, viewModel = viewModel)
                        }
                    } else {
                        TextButton(onClick = { selected.value = null }) { Text("Return to search") }
                    }
                }
            }
        }
    }
    private fun search(query: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(query)
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.filesDir.resolve("$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun discoveryLayoutLight() {
        mount()
        compose.onNodeWithText("搜索历史").assertIsDisplayed()
        compose.onNodeWithText("推荐搜索").assertIsDisplayed()
        compose.onNodeWithText("代码混淆 安全").assertIsDisplayed()
        capture("search-discovery-light")
    }

    @Test fun discoveryLayoutDarkWithLargeFontRemainsUsable() {
        mount(dark = true, fontScale = 1.5f)
        compose.onNodeWithText("搜索历史").assertIsDisplayed()
        compose.onNodeWithText("推荐搜索").assertIsDisplayed()
        capture("search-discovery-dark-large")
        compose.onNodeWithText("上次搜索").performClick()
        compose.onNodeWithContentDescription("清除").performClick()
        compose.onNodeWithText("搜索历史").assertIsDisplayed()
    }

    @Test fun failedRecommendationsCanRetryWithoutLosingHistory() {
        suggestions.hotResult = DataResult.Failure(DataError.NETWORK)
        mount()
        compose.onNodeWithText("上次搜索").assertIsDisplayed()
        compose.runOnIdle { suggestions.hotResult = DataResult.Success(listOf("重试热词")) }
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("重试热词").assertIsDisplayed()
        compose.runOnIdle { assertTrue(repository.requests.isEmpty()) }
    }

    @Test fun recommendedKeywordStartsSearchAndAppearsInHistory() {
        mount()
        compose.onNodeWithText("推荐搜索").assertIsDisplayed()
        compose.onNodeWithText("性能优化").performClick()
        compose.onNodeWithText("Article 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("清除").performClick()
        compose.runOnIdle {
            assertEquals(listOf("性能优化" to 0), repository.requests.toList())
            assertEquals("性能优化", viewModel.uiState.value.suggestions.history.items.first())
        }
    }

    @Test fun clearHistoryRequiresConfirmationAndRetainsRecommendations() {
        mount()
        compose.onNodeWithContentDescription("清空搜索历史").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("上次搜索").assertIsDisplayed()
        compose.onNodeWithContentDescription("清空搜索历史").performClick()
        compose.onNodeWithText("清空", substring = false).performClick()
        compose.onNodeWithText("暂无搜索历史").assertIsDisplayed()
        compose.onNodeWithText("性能优化").assertIsDisplayed()
        compose.runOnIdle { assertTrue(repository.requests.isEmpty()) }
    }

    @Test fun historyKeywordStartsSearch() {
        mount()
        compose.onNodeWithText("上次搜索").performClick()
        compose.onNodeWithText("Article 1").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("上次搜索" to 0), repository.requests.toList()) }
    }

    @Test fun inputAndBlankSubmissionDoNotSearchUntilKeyboardSubmit() {
        mount()
        search("   ")
        compose.runOnIdle { assertTrue(repository.requests.isEmpty()) }
        compose.onNode(hasSetTextAction()).performTextReplacement("first")
        compose.runOnIdle { assertTrue(repository.requests.isEmpty()) }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil { repository.requests.size == 1 }
        compose.runOnIdle { assertEquals("first" to 0, repository.requests.single()) }
        compose.onNodeWithContentDescription("清除").performClick()
        compose.runOnIdle {
            assertEquals("", viewModel.uiState.value.input)
            assertEquals("first", viewModel.uiState.value.keyword)
        }
    }

    @Test fun typingAfterClearDoesNotRevealOldResultsUntilSearchButtonIsClicked() {
        mount()
        search("first")
        compose.onNodeWithText("Article 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("清除").performClick()
        for (text in listOf("f", "first", "second")) {
            compose.onNode(hasSetTextAction()).performTextReplacement(text)
            compose.onNodeWithText("Article 1").assertDoesNotExist()
            compose.onNodeWithText("推荐搜索").assertIsDisplayed()
            compose.runOnIdle { assertEquals(listOf("first" to 0), repository.requests.toList()) }
        }
        compose.onNodeWithText("搜索", substring = false).performClick()
        compose.onNodeWithText("Article 101").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("first" to 0, "second" to 0), repository.requests.toList())
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("s")
        compose.onNodeWithText("Article 101").assertDoesNotExist()
    }

    @Test fun emptyResultsShowRealEmptyState() {
        mount()
        search("empty")
        compose.onNodeWithText("没有找到相关文章，试试其他关键词。").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("empty" to 0), repository.requests.toList()) }
    }

    @Test fun initialFailureRetriesSubmittedQuery() {
        mount()
        search("failure")
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("Article 1").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("failure" to 0, "failure" to 0), repository.requests.toList())
        }
    }

    @Test fun readingReturnRestoresQueryResultsAndScrollWithoutNewRequest() {
        mount()
        search("first")
        compose.onNode(hasScrollAction()).performScrollToIndex(25)
        compose.onNodeWithText("Article 26").performClick()
        compose.runOnIdle {
            assertEquals(26L, selected.value?.id)
            assertEquals("https://reader.invalid/26", selected.value?.url)
        }
        compose.onNodeWithText("Return to search").performClick()
        compose.onNodeWithText("Article 26").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("first", viewModel.uiState.value.input)
            assertEquals("first", viewModel.uiState.value.keyword)
            assertEquals(1, repository.requests.size)
        }
    }

    @Test fun differentKeywordStartsAtTopWithItsOwnResults() {
        mount()
        search("first")
        compose.onNode(hasScrollAction()).performScrollToIndex(25)
        search("second")
        compose.onNodeWithText("Article 101").assertIsDisplayed()
        compose.onNodeWithText("Article 26").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("first" to 0, "second" to 0), repository.requests.toList())
        }
    }

    @Test fun scrollingLoadsNextPageOnceAndStopsAtEnd() {
        mount()
        search("pages")
        compose.onNode(hasScrollAction()).performScrollToIndex(19)
        compose.waitUntil { repository.requests.size == 2 }
        compose.onNode(hasScrollAction()).performScrollToIndex(29)
        compose.onNodeWithText("Article 30").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("pages" to 0, "pages" to 1), repository.requests.toList())
        }
    }
}
private class FixtureSearchRepository : ArticleRepository {
    val requests = CopyOnWriteArrayList<Pair<String, Int>>()
    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> {
        requests += keyword to page
        if (keyword == "failure" && requests.count { it.first == keyword } == 1) {
            return DataResult.Failure(DataError.NETWORK)
        }
        if (keyword == "empty") return DataResult.Success(PageResult(emptyList(), null))
        val ids = when {
            keyword == "second" -> 101L..150L
            keyword == "pages" && page == 0 -> 1L..20L
            keyword == "pages" -> 21L..30L
            else -> 1L..50L
        }
        return DataResult.Success(
            PageResult(
                ids.map { id ->
                    Article(
                        id, "Article <em>$id</em>", "https://reader.invalid/$id", "Author", "",
                        "Parent", "Child", "Today", false
                    )
                },
                if (keyword == "pages" && page == 0) 1 else null
            )
        )
    }
    override suspend fun articles(page: Int, categoryId: Long?): DataResult<PageResult<Article>> =
        error("Unexpected articles")
    override suspend fun questions(): DataResult<List<Article>> = error("Unexpected questions")
    override suspend fun questionPage(page: Int): DataResult<PageResult<Article>> =
        error("Unexpected questions")
    override suspend fun topics(): DataResult<List<Topic>> = error("Unexpected topics")
}

private class FixtureSuggestions : SearchSuggestionsRepository {
    override val history = MutableStateFlow(SearchHistory(listOf("上次搜索", "Kotlin"), ready = true))
    var hotResult: DataResult<List<String>> = DataResult.Success(
        listOf("面试", "动画", "自定义 View", "性能优化", "gradle", "Camera 相机", "代码混淆 安全", "逆向 加固")
    )
    override suspend fun hotKeys() = hotResult
    override suspend fun record(keyword: String): Boolean {
        history.value =
            SearchHistory((listOf(keyword) + history.value.items).distinct().take(20), ready = true)
        return true
    }
    override suspend fun clearHistory(): Boolean {
        history.value = SearchHistory(ready = true)
        return true
    }
}
