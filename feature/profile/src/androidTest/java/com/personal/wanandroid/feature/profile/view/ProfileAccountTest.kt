package com.personal.wanandroid.feature.profile.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.personal.wanandroid.core.data.model.ThemePalettePreference
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.core.model.auth.AccountUser
import com.personal.wanandroid.core.model.auth.AuthSession
import com.personal.wanandroid.core.model.auth.AuthStatus
import com.personal.wanandroid.feature.profile.state.AccountUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileAccountTest {
    @get:Rule val compose = createComposeRule()
    private fun show(
        state: AccountUiState,
        login: () -> Unit = {},
        logout: () -> Unit = {},
        retry: () -> Unit = {}
    ) {
        compose.setContent {
            WanTheme {
                ProfileScreen(
                    account = state,
                    onLogout = logout,
                    onRetrySession = retry,
                    themeSummary = "石板蓝",
                    currentPalette = ThemePalettePreference.SLATE_BLUE,
                    onLogin = login,
                    onThemeSettings = {},
                    onCollections = {},
                    onHistory = {}
                )
            }
        }
    }

    @Test fun guestCanOpenLogin() {
        var clicks = 0
        show(AccountUiState(AuthSession(status = AuthStatus.GUEST)), login = { clicks++ })
        compose.onNodeWithText("登录").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun loggedInAccountNeedsConfirmationBeforeLogout() {
        var clicks = 0
        show(
            AccountUiState(
                AuthSession(AuthStatus.AUTHENTICATED, AccountUser(7, "fixture", "测试用户"))
            ),
            logout = {
                clicks++
            }
        )
        compose.onNodeWithText("测试用户").assertIsDisplayed()
        compose.onNodeWithText("退出登录").performClick()
        compose.runOnIdle { assertEquals(0, clicks) }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("logout_confirm").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, clicks) }
        compose.onNodeWithText("退出登录").performClick()
        compose.onNodeWithTag("logout_confirm").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun unverifiedSessionOffersExplicitRetry() {
        var calls = 0
        show(
            AccountUiState(
                AuthSession(AuthStatus.UNVERIFIED, AccountUser(7, "fixture", "测试用户"))
            ),
            retry = {
                calls++
            }
        )
        compose.onNodeWithText("重新验证").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
