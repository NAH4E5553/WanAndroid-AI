package com.personal.wanandroid.feature.article

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.personal.wanandroid.core.designsystem.WanTheme
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
}
