package com.personal.wanandroid.feature.profile.view

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.CollectionItem
import com.personal.wanandroid.core.model.CollectionSnapshot
import com.personal.wanandroid.core.model.CollectionStatus
import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.feature.profile.state.CollectionsUiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CollectionsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val item = CollectionItem(
        CollectionTarget(42, 900),
        Article(
            42, "收藏测试文章", "https://example.invalid", "作者", "", "",
            "Android", "2026-09-13", true
        )
    )

    @Test fun itemReadsAndRemovesWithDistinctTargetIds() {
        var opened: CollectionItem? = null
        var removed: CollectionItem? = null
        compose.setContent {
            WanTheme {
                CollectionsScreen(
                    CollectionsUiState(
                        CollectionSnapshot(3, mapOf(item.target.key to CollectionStatus(true))),
                        PagedUiState(items = listOf(item))
                    ),
                    {
                    }, {}, {
                        opened =
                            it
                    }, { removed = it }, {}, {}, {}, {}, {}, {}
                )
            }
        }
        compose.onNodeWithText("收藏测试文章").performClick()
        compose.onNodeWithText("取消收藏").performClick()
        compose.onNodeWithText("已经到底了").assertIsDisplayed()
        capture("collections-light.png")
        compose.runOnIdle {
            assertEquals(42L, opened?.target?.articleId)
            assertEquals(900L, removed?.target?.recordId)
        }
    }

    @Test fun accountLossImmediatelyHidesItemsAndOffersLogin() {
        val state =
            mutableStateOf(
                CollectionsUiState(
                    CollectionSnapshot(3, mapOf(item.target.key to CollectionStatus(true, true))),
                    PagedUiState(items = listOf(item))
                )
            )
        var logins = 0
        compose.setContent {
            WanTheme {
                CollectionsScreen(state.value, {}, { logins++ }, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("处理中…").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(collections = CollectionSnapshot()) }
        compose.onNodeWithText("收藏测试文章").assertDoesNotExist()
        compose.onNodeWithText("登录").performClick()
        compose.runOnIdle { assertEquals(1, logins) }
    }

    @Test fun emptyStateIsActionableWithoutInventingItems() {
        compose.setContent {
            WanTheme {
                CollectionsScreen(CollectionsUiState(CollectionSnapshot(3)), {
                }, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("还没有收藏，阅读文章时可以添加收藏").assertIsDisplayed()
        compose.onNodeWithText("取消收藏").assertDoesNotExist()
    }

    @Test fun darkLargeTextKeepsReadingAndRemoveActionsVisible() {
        var removed = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                WanTheme(dark = true) {
                    CollectionsScreen(
                        state = CollectionsUiState(
                            CollectionSnapshot(3, mapOf(item.target.key to CollectionStatus(true))),
                            PagedUiState(items = listOf(item))
                        ),
                        onBack = {}, onLogin = {}, onArticle = {}, onRemove = { removed++ },
                        onRefresh = {}, onRetryInitial = {}, onRetryRefresh = {},
                        onRetryAppend = {}, onLoadMore = {}, onContinue = {}
                    )
                }
            }
        }
        compose.onNodeWithText("收藏测试文章").assertIsDisplayed()
        compose.onNodeWithText("取消收藏").performClick()
        compose.runOnIdle { assertEquals(1, removed) }
        capture("collections-dark-large.png")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
