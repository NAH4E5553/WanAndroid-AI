package com.personal.wanandroid.feature.auth.view

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.ui.component.network.errorMessage
import com.personal.wanandroid.core.ui.component.scaffold.AppScaffold
import com.personal.wanandroid.feature.auth.R
import com.personal.wanandroid.feature.auth.state.LoginUiState
import com.personal.wanandroid.feature.auth.viewmodel.LoginViewModel

@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val complete by rememberUpdatedState(onLoggedIn)
    LaunchedEffect(state.completed, lifecycleState) {
        if (state.completed && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) complete()
    }
    val back = {
        viewModel.cancel()
        onBack()
    }
    BackHandler(onBack = back)
    LoginScreen(
        state,
        viewModel::usernameChanged,
        viewModel::passwordChanged,
        viewModel::submit,
        back,
        viewModel::passwordFocused
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onPasswordFocused: () -> Unit
) {
    val focus = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<Int?>(null) }
    val submit = {
        if (state.canSubmit) {
            focus.clearFocus()
            onSubmit()
        }
    }
    AppScaffold(topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack) {
                LoginIcon(R.drawable.ic_login_back, stringResource(R.string.back))
            }
        }
    }) { padding ->
        LoginForm(
            padding,
            state,
            onUsernameChanged,
            onPasswordChanged,
            submit,
            showPassword,
            { showPassword = !showPassword },
            { notice = it },
            onPasswordFocused
        )
    }
    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text(stringResource(R.string.notice_title)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = { notice = null }) {
                    Text(stringResource(R.string.notice_confirm))
                }
            }
        )
    }
}

@Composable
private fun LoginForm(
    padding: PaddingValues,
    state: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    submit: () -> Unit,
    showPassword: Boolean,
    togglePassword: () -> Unit,
    showNotice: (Int) -> Unit,
    onPasswordFocused: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            Text(
                stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.login_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(44.dp))
            LoginInput(
                value = state.username,
                onValueChange = onUsernameChanged,
                label = stringResource(R.string.username),
                placeholder = stringResource(R.string.username_placeholder),
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.testTag("login_username")
            )
            if (state.phoneError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.phone_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("login_phone_error").semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
                )
            }
            Spacer(Modifier.height(28.dp))
            LoginInput(
                value = state.password,
                onValueChange = onPasswordChanged,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_placeholder),
                enabled = !state.isSubmitting,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    IconButton(
                        onClick = togglePassword,
                        modifier = Modifier.testTag("login_visibility")
                    ) {
                        LoginIcon(
                            if (showPassword) {
                                R.drawable.ic_login_eye
                            } else {
                                R.drawable.ic_login_eye_closed
                            },
                            stringResource(
                                if (showPassword) R.string.hide_password else R.string.show_password
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("login_password").onFocusChanged {
                    if (it.isFocused) onPasswordFocused()
                }
            )
            Spacer(Modifier.height(18.dp))
            LoginConsent(showNotice)
            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (error == DataError.SERVICE) {
                        stringResource(R.string.login_rejected)
                    } else {
                        errorMessage(error)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("login_error").semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = submit,
                enabled = state.canSubmit,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("login_submit")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                    Text(
                        stringResource(
                            if (state.isSubmitting) R.string.logging_in else R.string.login
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showNotice(R.string.register_unavailable) }) {
                    Text(stringResource(R.string.register))
                }
                VerticalDivider(
                    Modifier.padding(horizontal = 14.dp).height(16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                TextButton(onClick = { showNotice(R.string.reset_password_unavailable) }) {
                    Text(stringResource(R.string.forgot_password))
                }
            }
        }
    }
}

@Composable
private fun LoginInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = colors.onSurface,
            fontSize = 17.sp
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Column {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
                HorizontalDivider(
                    thickness = if (focused) 2.dp else 1.dp,
                    color = if (focused) colors.primary else colors.outlineVariant
                )
            }
        }
    )
}

@Composable
private fun LoginConsent(showNotice: (Int) -> Unit) {
    val linkStyle = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.consent_prefix))
            withLink(
                LinkAnnotation.Clickable("terms", linkStyle) {
                    showNotice(R.string.terms_unavailable)
                }
            ) {
                append(stringResource(R.string.terms))
            }
            append(stringResource(R.string.consent_and))
            withLink(
                LinkAnnotation.Clickable("privacy", linkStyle) {
                    showNotice(R.string.privacy_unavailable)
                }
            ) {
                append(stringResource(R.string.privacy_policy))
            }
        },
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("login_consent")
    )
}

@Composable
private fun LoginIcon(@DrawableRes resource: Int, description: String) {
    Icon(painterResource(resource), description, modifier = Modifier.size(24.dp))
}
