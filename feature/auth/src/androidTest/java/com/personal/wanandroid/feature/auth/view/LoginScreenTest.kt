package com.personal.wanandroid.feature.auth.view

import android.graphics.Bitmap
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.navigation3.runtime.NavKey
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.data.repository.AuthRepository
import com.personal.wanandroid.core.data.repository.LogoutResult
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.core.model.auth.AuthSession
import com.personal.wanandroid.core.model.auth.AuthStatus
import com.personal.wanandroid.core.navigation.LoginRoute as LoginRouteKey
import com.personal.wanandroid.core.navigation.MainRoute
import com.personal.wanandroid.core.navigation.NavEntryIdGenerator
import com.personal.wanandroid.core.navigation.NavigationDispatcher
import com.personal.wanandroid.core.navigation.NavigationSource
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.auth.state.LoginUiState
import com.personal.wanandroid.feature.auth.viewmodel.LoginViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val compose = createComposeRule()
    private val owner = ViewModelStore()

    @After fun clear() {
        compose.runOnIdle { owner.clear() }
    }
    private class Fake : AuthRepository {
        override val session = MutableStateFlow(AuthSession(status = AuthStatus.GUEST))
        val result = CompletableDeferred<DataResult<Unit>>()
        var calls = 0
        var finished = false
        override suspend fun login(username: String, password: String): DataResult<Unit> {
            calls++
            try {
                return result.await()
            } finally {
                finished = true
            }
        }
        override suspend fun restore() = DataResult.Success(Unit)
        override suspend fun logout() = LogoutResult(null, DataResult.Success(Unit))
    }

    @Test fun inputDoesNotLoginUntilExplicitSubmitAndBusyDisablesButton() {
        val state = mutableStateOf(LoginUiState())
        var calls = 0
        compose.setContent {
            WanTheme {
                LoginScreen(
                    state.value,
                    { state.value = state.value.copy(username = it) },
                    { state.value = state.value.copy(password = it) },
                    {
                        calls++
                        state.value = state.value.copy(isSubmitting = true)
                    },
                    {},
                    {}
                )
            }
        }
        compose.waitForIdle()
        screenshot("login-default.png")
        compose.onNodeWithTag("login_submit").assertIsNotEnabled()
        compose.onNodeWithTag("login_username").performTextInput("13800000000")
        compose.onNodeWithTag("login_password").performTextInput("fixture-password")
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithTag("login_submit").performScrollTo().performClick().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun keyboardDoneUsesSameSubmitPath() {
        var calls = 0
        var keyboardVisible = false
        compose.setContent {
            WanTheme {
                val visible = WindowInsets.isImeVisible
                SideEffect { keyboardVisible = visible }
                LoginScreen(LoginUiState("13800000000", "fixture-password"), {
                }, {}, { calls++ }, {}, {})
            }
        }
        compose.onNodeWithTag("login_password").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { keyboardVisible }
        compose.onNodeWithTag("login_password").assertIsDisplayed()
        screenshot("login-keyboard-v2.png", includeSystemUi = true)
        compose.onNodeWithTag("login_password").performImeAction()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun loginSuccessReturnsToExactSourceEntryOnce() {
        val fake = Fake()
        val stack = mutableListOf<NavKey>(MainRoute("original"), LoginRouteKey("login"))
        val dispatcher = NavigationDispatcher(NavEntryIdGenerator { "unused" }, {})
        val token = dispatcher.attach(stack)
        var backs = 0
        lateinit var vm: LoginViewModel
        compose.runOnIdle {
            vm = LoginViewModel(fake)
            owner.put("login", vm)
        }
        compose.setContent {
            WanTheme {
                LoginRoute({}, {
                    backs++
                    dispatcher.back(NavigationSource(token, "login"))
                }, vm)
            }
        }
        compose.onNodeWithTag("login_username").performTextInput("13800000000")
        compose.onNodeWithTag("login_password").performTextInput("fixture-password")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil { fake.calls == 1 }
        compose.runOnIdle { fake.result.complete(DataResult.Success(Unit)) }
        compose.waitUntil { backs == 1 }
        compose.runOnIdle { assertEquals(listOf(MainRoute("original")), stack) }
    }

    @Test fun backCancelsRequestAndDoesNotReportLoginSuccess() {
        val fake = Fake()
        var backs = 0
        var successes = 0
        lateinit var vm: LoginViewModel
        compose.runOnIdle {
            vm = LoginViewModel(fake)
            owner.put("login", vm)
        }
        compose.setContent { WanTheme { LoginRoute({ backs++ }, { successes++ }, vm) } }
        compose.onNodeWithTag("login_username").performTextInput("13800000000")
        compose.onNodeWithTag("login_password").performTextInput("fixture-password")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil { fake.calls == 1 }
        compose.onNodeWithContentDescription("返回").performClick()
        compose.waitUntil { fake.finished }
        compose.runOnIdle {
            assertEquals(1, backs)
            assertEquals(0, successes)
            assertEquals("", vm.uiState.value.password)
        }
    }

    @Test fun errorFormRemainsUsableInDarkLargeFont() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 1.6f)
            ) {
                WanTheme(dark = true) {
                    LoginScreen(
                        LoginUiState(
                            "13800000000",
                            "fixture-password",
                            error = DataError.SERVICE
                        ),
                        {
                        },
                        {},
                        {},
                        {},
                        {}
                    )
                }
            }
        }
        compose.waitForIdle()
        screenshot("login-dark-large.png")
        compose.onNodeWithTag("login_error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(
            "login_submit"
        ).performScrollTo().assertIsDisplayed().assertIsEnabled()
    }

    @Test fun passwordVisibilityDoesNotSubmitAndReadyFormMatchesDesign() {
        var calls = 0
        compose.setContent {
            WanTheme {
                LoginScreen(LoginUiState("13800000000", "fixture-password"), {
                }, {}, { calls++ }, {}, {})
            }
        }
        compose.waitForIdle()
        screenshot("login-ready-v2.png")
        compose.onNodeWithContentDescription("显示密码").performClick()
        compose.onNodeWithContentDescription("隐藏密码").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("显示密码").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun placeholderActionsOnlyExplainAvailability() {
        var calls = 0
        compose.setContent {
            WanTheme { LoginScreen(LoginUiState(), {}, {}, { calls++ }, {}, {}) }
        }
        compose.onNodeWithText("注册").performScrollTo().performClick()
        compose.onNodeWithText("注册功能暂未开放，请使用已有的 WanAndroid 账号登录。").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.onNodeWithText("忘记密码").performScrollTo().performClick()
        compose.onNodeWithText("找回密码功能暂未开放。").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.onNodeWithTag("login_consent").performScrollTo().performFirstLinkClick {
            (it.item as? LinkAnnotation.Clickable)?.tag == "terms"
        }
        compose.onNodeWithText("用户协议内容暂未提供。").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.onNodeWithTag("login_consent").performFirstLinkClick {
            (it.item as? LinkAnnotation.Clickable)?.tag == "privacy"
        }
        compose.onNodeWithText("隐私政策内容暂未提供。").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.onNodeWithTag("login_submit").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun focusingPasswordValidatesPhoneAndCorrectingItAllowsLogin() {
        val fake = Fake()
        lateinit var vm: LoginViewModel
        compose.runOnIdle {
            vm = LoginViewModel(fake)
            owner.put("login", vm)
        }
        compose.setContent { WanTheme { LoginRoute({}, {}, vm) } }
        compose.onNodeWithText("请输入手机号").assertIsDisplayed()
        compose.onNodeWithText("用户名").assertDoesNotExist()
        compose.onNodeWithText("密码").assertDoesNotExist()
        compose.onNodeWithTag("login_phone_error").assertDoesNotExist()
        compose.onNodeWithTag("login_username").performTextInput("12345")
        compose.onNodeWithTag("login_phone_error").assertDoesNotExist()
        compose.onNodeWithTag("login_password").performClick()
        compose.onNodeWithText("手机号输入有误，请重新输入").assertIsDisplayed()
        compose.onNodeWithTag("login_password").performTextInput("fixture-password")
        compose.onNodeWithTag("login_password").performImeAction()
        compose.runOnIdle { assertEquals(0, fake.calls) }
        compose.onNodeWithTag("login_username").performTextReplacement("13800000000")
        compose.onNodeWithTag("login_phone_error").assertDoesNotExist()
        compose.onNodeWithTag("login_username").performImeAction()
        compose.onNodeWithTag("login_phone_error").assertDoesNotExist()
        compose.onNodeWithTag("login_password").performImeAction()
        compose.waitUntil { fake.calls == 1 }
    }
    private fun screenshot(name: String, includeSystemUi: Boolean = false) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = if (includeSystemUi) {
            checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        } else {
            compose.onRoot().captureToImage().asAndroidBitmap()
        }
        instrumentation.targetContext.openFileOutput(name, 0).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
