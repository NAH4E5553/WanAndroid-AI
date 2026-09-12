package com.personal.wanandroid.feature.topics.view

import android.graphics.Bitmap
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.topics.viewmodel.TopicsViewModel
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TopicsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val repository = ScreenRepository()
    private val visible = mutableStateOf(true)
    private val article = mutableStateOf<Long?>(null)
    private lateinit var vm: TopicsViewModel
    private fun mount(dark: Boolean = false, fontScale: Float = 1f) {
        compose.runOnIdle { vm = TopicsViewModel(repository, SavedStateHandle()) }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                WanTheme(dark = dark) {
                    val holder = rememberSaveableStateHolder()
                    val show by visible
                    val selectedArticle by article
                    if (show && selectedArticle == null) {
                        holder.SaveableStateProvider("topic-tab") {
                            TopicsRoute(
                                onArticleClick = { _, _, id -> article.value = id },
                                viewModel = vm,
                                modifier = Modifier.safeDrawingPadding()
                            )
                        }
                    } else {
                        TextButton(onClick = {
                            visible.value = true
                            article.value = null
                        }) {
                            Text("Return to topics")
                        }
                    }
                }
            }
        }
    }
    private fun scroll(index: Int) = compose.onNode(
        hasScrollAction() and hasAnyAncestor(hasTestTag("topic-articles"))
    ).performScrollToIndex(index)

    private fun detailLeft() = compose.onNodeWithTag("topic-detail")
        .fetchSemanticsNode().boundsInRoot.left

    private fun assertFullWidth() {
        val layout = compose.onNodeWithTag("topic-layout").fetchSemanticsNode().boundsInRoot
        val detail = compose.onNodeWithTag("topic-detail").fetchSemanticsNode().boundsInRoot
        assertEquals(layout.left, detail.left, 1f)
        assertEquals(layout.width, detail.width, 1f)
        compose.onNodeWithTag("topic-menu").assertDoesNotExist()
    }

    @Test fun clickingChildExpandsPageAndReturningToFirstChildRestoresSidebarWithoutReload() {
        mount()
        val initialLeft = detailLeft()
        scroll(12)
        compose.onNodeWithTag("topic-tab-20").performClick()
        assertFullWidth()
        scroll(10)
        capture("topics-full-width")
        compose.onNodeWithTag("topic-tab-11").performScrollTo().performClick()
        assertEquals(initialLeft, detailLeft(), 1f)
        compose.onNodeWithTag("topic-10").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("文章 11-13").assertIsDisplayed()
        compose.onNodeWithTag("topic-tab-20").performClick()
        assertFullWidth()
        compose.onNodeWithText("文章 20-11").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(11L to 0, 20L to 0), repository.requests) }
    }

    @Test fun expandedSidebarKeepsItsOwnScrollPositionAfterHiding() {
        mount(fontScale = 1.5f)
        compose.onNodeWithTag("topic-menu").performScrollToIndex(7)
        compose.onNodeWithTag("topic-100").performClick()
        val menuTop = compose.onNodeWithTag("topic-100").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("topic-tab-102").performClick()
        assertFullWidth()
        compose.onNodeWithTag("topic-tab-101").performScrollTo().performClick()
        compose.onNodeWithTag("topic-100").assertIsDisplayed().assertIsSelected()
        assertEquals(
            menuTop,
            compose.onNodeWithTag("topic-100").fetchSemanticsNode().boundsInRoot.top,
            1f
        )
    }

    @Test fun sidebarMovesContinuouslyAndCanReverseDuringAnimation() {
        mount()
        val initialLeft = detailLeft()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("topic-tab-20").performClick()
        compose.mainClock.advanceTimeUntil(timeoutMillis = 5000) {
            vm.uiState.value.selectedId == 20L
        }
        compose.mainClock.advanceTimeBy(120)
        val movingLeft = detailLeft()
        assertTrue(movingLeft > 0f && movingLeft < initialLeft)
        capture("topics-sidebar-moving")
        compose.onNodeWithTag("topic-tab-11").performClick()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(initialLeft, detailLeft(), 1f)
        compose.onNodeWithTag("topic-10").assertIsDisplayed()
        compose.onNodeWithText("文章 11-1").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(11L to 0, 20L to 0), repository.requests) }
    }

    @Test fun fullWidthListCanRefreshAndReachEndWithoutOpeningSidebar() {
        mount()
        compose.onNodeWithTag("topic-tab-20").performClick()
        assertFullWidth()
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeDown() }
        compose.waitUntil { repository.requests.size == 3 }
        scroll(39)
        compose.waitUntil { repository.requests.size == 4 }
        scroll(59)
        assertFullWidth()
        compose.onNodeWithText("文章 20-60").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(11L to 0, 20L to 0, 20L to 0, 20L to 1), repository.requests)
        }
    }

    @Test fun onlyApiChildrenAppearAndEmptyParentKeepsSidebarWithoutRequest() {
        mount()
        compose.onNodeWithText("全部").assertDoesNotExist()
        compose.onNodeWithTag("topic-tab-10").assertDoesNotExist()
        compose.onNodeWithTag("topic-tab-11").assertIsSelected()
        compose.onNodeWithTag("topic-50").performClick()
        compose.onNodeWithText("该专题暂无二级分类").assertIsDisplayed()
        compose.onNodeWithTag("topic-50").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("topic-tabs").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(11L to 0), repository.requests) }
        compose.onNodeWithTag("topic-10").performClick()
        compose.onNodeWithTag("topic-tab-11").assertIsSelected()
        compose.onNodeWithText("文章 11-1").assertIsDisplayed()
    }

    @Test fun leftClickSwitchesOnlyThatTopicAndSameSelectionDoesNotReload() {
        mount()
        compose.onNodeWithTag("topic-10").assertIsSelected()
        compose.onNodeWithText("文章 11-1").assertIsDisplayed()
        compose.onNodeWithTag("topic-tab-20").performClick()
        compose.onNodeWithTag("topic-tab-20").assertIsSelected()
        compose.onNodeWithText("文章 20-1").assertIsDisplayed()
        compose.runOnIdle { assertEquals(20L, vm.uiState.value.selectedId) }
        compose.onNodeWithTag("topic-tab-20").performClick()
        compose.runOnIdle { assertEquals(listOf(11L to 0, 20L to 0), repository.requests.toList()) }
    }

    @Test fun sidebarContainsOnlyParentsAndParentReturnRestoresChildAndPosition() {
        mount()
        compose.onNodeWithTag("topic-20").assertDoesNotExist()
        compose.onNodeWithTag("topic-tab-20").performClick()
        scroll(12)
        compose.onNodeWithTag("topic-tab-11").performScrollTo().performClick()
        compose.onNodeWithTag("topic-40").performClick()
        compose.onNodeWithTag("topic-40").assertIsSelected()
        compose.onNodeWithTag("topic-tab-41").assertIsSelected()
        compose.onNodeWithTag("topic-tab-20").assertDoesNotExist()
        compose.onNodeWithTag("topic-10").performClick()
        compose.onNodeWithTag("topic-tab-20").performClick()
        compose.onNodeWithTag("topic-tab-20").assertIsSelected()
        compose.onNodeWithText("文章 20-13").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(11L to 0, 20L to 0, 41L to 0), repository.requests)
        }
    }

    @Test fun horizontalSwipeSwitchesChildrenButNeverCrossesParentBoundary() {
        mount()
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("topic-tab-20").assertIsSelected()
        compose.onNodeWithText("文章 20-1").assertIsDisplayed()
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("topic-tab-30").assertIsSelected()
        compose.onNodeWithText("该专题暂无文章").assertIsDisplayed()
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeLeft() }
        assertFullWidth()
        compose.runOnIdle {
            assertEquals(30L, vm.uiState.value.selectedId)
            assertEquals(10L, vm.uiState.value.selectedParentId)
        }
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeRight() }
        compose.onNodeWithText("文章 20-1").assertIsDisplayed()
        assertFullWidth()
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeRight() }
        compose.onNodeWithTag("topic-10").assertIsDisplayed().assertIsSelected()
        assertTrue(detailLeft() > 0f)
        compose.runOnIdle {
            assertEquals(listOf(11L to 0, 20L to 0, 30L to 0), repository.requests)
        }
    }

    @Test fun verticalScrollAndRefreshDoNotSwitchTabsOrLoadNeighbours() {
        mount()
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeUp() }
        compose.onNodeWithTag("topic-tab-11").assertIsSelected()
        scroll(0)
        compose.onNodeWithTag("topic-pager").performTouchInput { swipeDown() }
        compose.waitUntil { repository.requests.size > 1 }
        compose.onNodeWithTag("topic-tab-11").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(11L to 0, 11L to 0), repository.requests) }
    }

    @Test fun cancelledHorizontalDragAndMostlyVerticalDragKeepCurrentChild() {
        mount()
        compose.onNodeWithTag("topic-pager").performTouchInput {
            swipe(center, center - Offset(width * 0.08f, 0f), durationMillis = 1000)
        }
        compose.onNodeWithTag("topic-tab-11").assertIsSelected()
        compose.onNodeWithTag("topic-pager").performTouchInput {
            swipe(
                Offset(width * 0.7f, height * 0.8f),
                Offset(width * 0.6f, height * 0.2f),
                durationMillis = 500
            )
        }
        compose.onNodeWithTag("topic-tab-11").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(11L to 0), repository.requests) }
        compose.onNodeWithTag("topic-10").assertIsDisplayed()
        assertTrue(detailLeft() > 0f)
    }

    @Test fun parentChangeDuringTabAnimationRejectsOldPagerSelection() {
        mount()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("topic-tab-20").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("topic-40").performClick()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("topic-40").assertIsSelected()
        compose.onNodeWithText("文章 41-1").assertIsDisplayed()
        compose.runOnIdle { assertEquals(41L, vm.uiState.value.selectedId) }
    }

    @Test fun scrollingTabStripDoesNotSelectUntilClick() {
        mount()
        compose.onNodeWithTag("topic-tabs").performTouchInput { swipeLeft() }
        compose.runOnIdle { assertEquals(listOf(11L to 0), repository.requests) }
        compose.onNodeWithTag("topic-tab-30").performScrollTo().performClick()
        compose.onNodeWithTag("topic-tab-30").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(11L to 0, 30L to 0), repository.requests) }
    }

    @Test fun endLoadsOnlyCurrentTopicAndNeverAdvancesCategory() {
        mount()
        scroll(39)
        compose.waitUntil { repository.requests.size == 2 }
        scroll(59)
        compose.onNodeWithTag("topic-10").assertIsSelected()
        compose.onNodeWithText("文章 11-60").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(11L to 0, 11L to 1), repository.requests.toList())
            assertEquals(11L, vm.uiState.value.selectedId)
            assertEquals(null, vm.uiState.value.page.nextPage)
        }
    }

    @Test fun switchingBackRestoresEachCategoryPositionWithoutReload() {
        mount()
        scroll(20)
        compose.onNodeWithTag("topic-tab-20").performClick()
        scroll(10)
        compose.onNodeWithTag("topic-tab-11").performClick()
        compose.onNodeWithText("文章 11-21").assertIsDisplayed()
        compose.onNodeWithTag("topic-tab-20").performClick()
        compose.onNodeWithText("文章 20-11").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, repository.requests.size) }
    }

    @Test fun readingAndTabReturnKeepSelectionScrollAndRows() {
        mount()
        compose.onNodeWithTag("topic-tab-20").performClick()
        scroll(20)
        compose.onNodeWithText("文章 20-21").performClick()
        compose.runOnIdle { assertEquals(20021L, article.value) }
        compose.onNodeWithText("Return to topics").performClick()
        assertFullWidth()
        compose.onNodeWithTag("topic-tab-20").assertIsSelected()
        compose.onNodeWithText("文章 20-21").assertIsDisplayed()
        compose.runOnIdle { visible.value = false }
        compose.onNodeWithText("Return to topics").performClick()
        compose.onNodeWithText("文章 20-21").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, repository.requests.size) }
    }

    @Test fun categoryFailureCanRetryWithoutRequestingArticlesEarly() {
        repository.treeResult = DataResult.Failure(DataError.NETWORK)
        mount()
        compose.runOnIdle {
            assertTrue(repository.requests.isEmpty())
            repository.treeResult = DataResult.Success(categories)
        }
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("文章 11-1").assertIsDisplayed()
    }

    @Test fun articleFailureAndEmptyStateStayInSelectedTopic() {
        repository.failFirst = true
        mount()
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("文章 11-1").assertIsDisplayed()
        compose.onNodeWithTag("topic-tab-30").performScrollTo().performClick()
        compose.onNodeWithText("该专题暂无文章").assertIsDisplayed()
        compose.onNodeWithTag("topic-tab-30").assertIsSelected()
    }

    @Test fun lightLayout() {
        mount()
        compose.onNodeWithText("文章 11-1").assertIsDisplayed()
        capture("topics-light")
    }

    @Test fun darkLargeFontLayoutRemainsSelectable() {
        mount(dark = true, fontScale = 1.5f)
        compose.onNodeWithTag("topic-tab-20").performClick()
        compose.onNodeWithText("文章 20-1").assertIsDisplayed()
        compose.onNodeWithTag("topic-tab-20").assertIsSelected()
        capture("topics-dark-large")
    }

    private fun capture(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.cacheDir.resolve("$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
private val categories = listOf(
    Topic(10, "开发环境"), Topic(11, "Android Studio相关", 10),
    Topic(20, "gradle", 10), Topic(30, "构建工具", 10),
    Topic(40, "基础知识"), Topic(41, "基础 API", 40), Topic(50, "四大组件"), Topic(60, "常用控件"),
    Topic(
        70,
        "用户交互"
    ),
    Topic(
        80,
        "网络访问"
    ),
    Topic(90, "图片加载"), Topic(100, "数据存储"), Topic(101, "存储 API", 100), Topic(102, "文件存储", 100)
)
private class ScreenRepository : ArticleRepository {
    var treeResult: DataResult<List<Topic>> = DataResult.Success(categories)
    var failFirst = false
    val requests = CopyOnWriteArrayList<Pair<Long, Int>>()
    override suspend fun topics() = treeResult
    override suspend fun articles(page: Int, categoryId: Long?): DataResult<PageResult<Article>> {
        val id = requireNotNull(categoryId)
        requests += id to page
        if (failFirst && requests.size == 1) return DataResult.Failure(DataError.NETWORK)
        if (id == 30L) return DataResult.Success(PageResult(emptyList(), null))
        val range = if (page == 0) 1L..40L else 41L..60L
        return DataResult.Success(
            PageResult(
                range.map { n ->
                    Article(
                        id * 1000 + n, "文章 $id-$n", "https://reader.invalid/$id/$n", "作者", "",
                        "开发环境", "分类 $id", "今天", false
                    )
                },
                if (page == 0) 1 else null
            )
        )
    }
    override suspend fun questions(): DataResult<List<Article>> = error("unexpected")
    override suspend fun questionPage(page: Int): DataResult<PageResult<Article>> =
        error("unexpected")
    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> =
        error("unexpected")
}
