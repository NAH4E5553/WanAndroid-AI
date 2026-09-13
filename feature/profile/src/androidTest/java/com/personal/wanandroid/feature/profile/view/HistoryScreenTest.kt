package com.personal.wanandroid.feature.profile.view

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
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
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.feature.profile.state.HistoryUiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()
    private val item = ReadingHistory("https://fixture.invalid", 1, "本机阅读文章", 123)
    private fun show(
        state: HistoryUiState = HistoryUiState(PagedUiState(items = listOf(item))),
        dark: Boolean = false,
        read: (ReadingHistory) -> Unit = {},
        delete: (String) -> Unit = {},
        clear: () -> Unit = {}
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (dark) 1.5f else 1f)
            ) {
                WanTheme(dark = dark) {
                    HistoryScreen(state, {}, read, delete, clear, {}, {}, {}, {}, {}, {})
                }
            }
        }
    }

    @Test fun readAndConfirmedSingleDeletionUseRecordedIdentity() {
        var read: ReadingHistory? = null
        var deleted: String? = null
        show(read = { read = it }, delete = { deleted = it })
        capture("history-light.png")
        compose.onNodeWithText("本机阅读文章").performClick()
        compose.onNodeWithText("删除记录").performClick()
        compose.onNodeWithText("仅删除本机阅读记录，不删除收藏和离线内容。").assertIsDisplayed()
        assertNull(deleted)
        compose.onNodeWithText("确认删除").performClick()
        compose.runOnIdle {
            assertEquals(item, read)
            assertEquals(item.url, deleted)
        }
    }

    @Test fun clearingNeedsConfirmationAndCanBeCancelled() {
        var clears = 0
        show(clear = { clears++ })
        compose.onNodeWithText("清空").performClick()
        compose.onNodeWithText("将删除本机全部阅读记录，不删除收藏和离线内容。").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(0, clears) }
        compose.onNodeWithText("清空").performClick()
        compose.onNodeWithText("确认删除").performClick()
        compose.runOnIdle { assertEquals(1, clears) }
    }

    @Test fun emptyHistoryDoesNotClaimOfflineSupport() {
        show(HistoryUiState())
        compose.onNodeWithText("暂无阅读历史").assertIsDisplayed()
        compose.onNodeWithText("清空").assertIsNotEnabled()
        compose.onNodeWithText("仅保存在本机；再次阅读需要网络。").assertIsDisplayed()
    }

    @Test fun darkLargeFontRetainsActions() {
        show(dark = true)
        capture("history-dark-large.png")
        compose.onNodeWithText("本机阅读文章").assertIsDisplayed()
        compose.onNodeWithText("删除记录").performClick()
        compose.onNodeWithText("确认删除").assertIsDisplayed()
    }
    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureScreenshots") != "true") return
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
