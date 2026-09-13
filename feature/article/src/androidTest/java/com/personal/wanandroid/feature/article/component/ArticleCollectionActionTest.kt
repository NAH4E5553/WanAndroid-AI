package com.personal.wanandroid.feature.article.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.core.model.CollectionStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArticleCollectionActionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun guestClickOnlyRequestsLoginAndLoginDoesNotReplayWrite() {
        var logins = 0
        var writes = 0
        val authenticated = mutableStateOf(false)
        compose.setContent {
            WanTheme {
                ArticleCollectionAction(CollectionStatus(false), authenticated.value, true) {
                    if (authenticated.value) writes++ else logins++
                }
            }
        }
        compose.onNodeWithText("收藏").performClick()
        compose.runOnIdle { authenticated.value = true }
        compose.runOnIdle {
            assertEquals(1, logins)
            assertEquals(0, writes)
        }
        compose.onNodeWithText("收藏").performClick()
        compose.runOnIdle { assertEquals(1, writes) }
    }

    @Test fun busyIsDisabledAndUncertainStateOffersVerification() {
        val state = mutableStateOf(CollectionStatus(busy = true))
        var clicks = 0
        compose.setContent {
            WanTheme { ArticleCollectionAction(state.value, true, true) { clicks++ } }
        }
        compose.onNodeWithText("处理中…").assertIsNotEnabled()
        compose.runOnIdle { state.value = CollectionStatus() }
        compose.onNodeWithText("确认收藏状态").performClick()
        compose.runOnIdle {
            assertEquals(1, clicks)
            state.value = CollectionStatus(true)
        }
        compose.onNodeWithText("取消收藏").performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
    }

    @Test fun removedExternalRecordCannotBeAddedWithRecordId() {
        compose.setContent {
            WanTheme { ArticleCollectionAction(CollectionStatus(false), true, false) {} }
        }
        compose.onNodeWithText("已取消收藏").assertIsNotEnabled()
    }
}
