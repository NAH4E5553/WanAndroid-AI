package com.personal.wanandroid.feature.article.view

import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.core.model.CollectionStatus
import com.personal.wanandroid.feature.article.state.ReaderFailure
import com.personal.wanandroid.feature.article.state.ReaderUiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failureRetryAndBackReachTheirOwnCallbacks() {
        var retries = 0
        var backs = 0
        compose.setContent {
            WanTheme {
                ArticleScreen(
                    state = ReaderUiState(
                        "https://reader.invalid/",
                        "Fixture",
                        failure = ReaderFailure.NETWORK
                    ),
                    onBack = { backs++ },
                    onRetry = { retries++ },
                    onOpenExternal = {},
                    onDismissExternal = {},
                    onConfirmExternal = {},
                    onNoticeShown = {}
                ) { Text("Fixture content") }
            }
        }
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("返回").performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, backs)
        }
    }

    @Test fun externalDialogDoesNotLaunchUntilUserConfirms() {
        val state =
            mutableStateOf(
                ReaderUiState("https://reader.invalid/", "Fixture", pendingExternal = "tel:123")
            )
        var launches = 0
        compose.setContent {
            WanTheme {
                ArticleScreen(
                    state = state.value,
                    onBack = {},
                    onRetry = {},
                    onOpenExternal = {},
                    onDismissExternal = { state.value = state.value.copy(pendingExternal = null) },
                    onConfirmExternal = { launches++ },
                    onNoticeShown = {}
                ) { Text("Fixture content") }
            }
        }
        compose.runOnIdle { assertEquals(0, launches) }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("打开").assertDoesNotExist()
        compose.runOnIdle { assertNull(state.value.pendingExternal) }
        compose.runOnIdle { state.value = state.value.copy(pendingExternal = "tel:123") }
        compose.onNodeWithText("打开").performClick()
        compose.runOnIdle { assertEquals(1, launches) }
    }

    @Test fun invalidUrlHasNoRefreshOrRetry() {
        compose.setContent {
            WanTheme {
                ArticleScreen(
                    state = ReaderUiState(
                        "file:///blocked",
                        "Fixture",
                        failure = ReaderFailure.UNSUPPORTED_URL
                    ),
                    onBack = {},
                    onRetry = {},
                    onOpenExternal = {},
                    onDismissExternal = {},
                    onConfirmExternal = {},
                    onNoticeShown = {}
                ) { }
            }
        }
        compose.onNodeWithText("刷新").assertDoesNotExist()
        compose.onNodeWithText("重试").assertDoesNotExist()
        compose.onNodeWithText("外部打开").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("刷新").assertIsNotEnabled()
        compose.onNodeWithText("外部打开").assertIsNotEnabled()
        compose.onNodeWithText("收藏").assertIsNotEnabled()
    }

    @Test fun browsableCategoryAppliesOnlyToWebLinks() {
        listOf("https://reader.invalid/", "http://reader.invalid/").forEach { url ->
            assertTrue(readerExternalIntent(url).hasCategory(Intent.CATEGORY_BROWSABLE))
        }
        listOf("tel:123", "mailto:test@example.invalid", "geo:0,0").forEach { url ->
            val intent = readerExternalIntent(url)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(url, intent.dataString)
            assertFalse(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        }
    }

    @Test fun overflowMenuOrdersActionsAndClosesBeforeCallbacks() {
        var refreshes = 0
        var external = 0
        var collections = 0
        val collected = mutableStateOf(false)
        compose.setContent {
            WanTheme {
                ArticleScreen(
                    state = ReaderUiState("https://reader.invalid/", "菜单测试文章"),
                    onBack = {}, onRetry = { refreshes++ }, onOpenExternal = { external++ },
                    onDismissExternal = {}, onConfirmExternal = {}, onNoticeShown = {},
                    collectionStatus = CollectionStatus(collected.value), authenticated = true,
                    onCollection = {
                        collections++
                        collected.value = !collected.value
                    }
                ) { Text("Fixture content") }
            }
        }
        compose.onNodeWithText("刷新").assertDoesNotExist()
        compose.onNodeWithText("外部打开").assertDoesNotExist()
        compose.onNodeWithText("收藏").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多操作").performClick()
        val refreshTop = compose.onNodeWithText("刷新").fetchSemanticsNode().boundsInWindow.top
        val externalTop = compose.onNodeWithText("外部打开").fetchSemanticsNode().boundsInWindow.top
        val collectionTop = compose.onNodeWithText("收藏").fetchSemanticsNode().boundsInWindow.top
        assertTrue(refreshTop < externalTop && externalTop < collectionTop)
        captureMenu("reader-menu-light.png")
        compose.onNodeWithText("刷新").performClick()
        compose.onNodeWithText("外部打开").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, refreshes) }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("外部打开").performClick()
        compose.onNodeWithText("刷新").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, external) }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("收藏").performClick()
        compose.onNodeWithText("取消收藏").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("取消收藏").performClick()
        compose.runOnIdle {
            assertEquals(2, collections)
            assertFalse(collected.value)
        }
    }

    @Test fun backDismissesMenuWithoutLeavingReader() {
        var backs = 0
        compose.setContent {
            WanTheme {
                ArticleScreen(
                    state = ReaderUiState("https://reader.invalid/", "Fixture"),
                    onBack = { backs++ },
                    onRetry = {},
                    onOpenExternal = {},
                    onDismissExternal = {},
                    onConfirmExternal = {},
                    onNoticeShown = {}
                ) { Text("Fixture content") }
            }
        }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("刷新").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("刷新").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多操作").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, backs) }
    }

    @Test fun darkLargeTextMenuKeepsAllActionsVisible() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                WanTheme(dark = true) {
                    ArticleScreen(
                        state = ReaderUiState("https://reader.invalid/", "菜单测试文章"),
                        onBack = {}, onRetry = {}, onOpenExternal = {}, onDismissExternal = {},
                        onConfirmExternal = {}, onNoticeShown = {},
                        collectionStatus = CollectionStatus(true), authenticated = true
                    ) { Text("Fixture content") }
                }
            }
        }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("刷新").assertIsDisplayed()
        compose.onNodeWithText("外部打开").assertIsDisplayed()
        compose.onNodeWithText("取消收藏").assertIsDisplayed()
        captureMenu("reader-menu-dark-large.png")
    }

    private fun captureMenu(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.cacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
