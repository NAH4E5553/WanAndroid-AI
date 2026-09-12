package com.personal.wanandroid.feature.article

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.personal.wanandroid.core.designsystem.WanTheme
import org.junit.Assert.assertEquals
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
                    ReaderUiState(
                        "https://reader.invalid/",
                        "Fixture",
                        failure = ReaderFailure.NETWORK
                    ),
                    {
                        backs++
                    },
                    { retries++ },
                    {},
                    {},
                    {},
                    {}
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
                ArticleScreen(state.value, {}, {}, {}, {
                    state.value =
                        state.value.copy(pendingExternal = null)
                }, { launches++ }, {}) { Text("Fixture content") }
            }
        }
        compose.runOnIdle { assertEquals(0, launches) }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("打开").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(pendingExternal = "tel:123") }
        compose.onNodeWithText("打开").performClick()
        compose.runOnIdle { assertEquals(1, launches) }
    }

    @Test fun invalidUrlHasNoRefreshOrRetry() {
        compose.setContent {
            WanTheme {
                ArticleScreen(
                    ReaderUiState(
                        "file:///blocked",
                        "Fixture",
                        failure = ReaderFailure.UNSUPPORTED_URL
                    ),
                    {
                    },
                    {},
                    {},
                    {},
                    {},
                    {}
                ) { }
            }
        }
        compose.onNodeWithText("刷新").assertDoesNotExist()
        compose.onNodeWithText("重试").assertDoesNotExist()
        compose.onNodeWithText("外部打开").assertDoesNotExist()
    }
}
