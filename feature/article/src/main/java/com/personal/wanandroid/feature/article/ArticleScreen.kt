package com.personal.wanandroid.feature.article

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.navigation.ArticleRoute as ArticleKey
import com.personal.wanandroid.core.ui.AppScaffold
import com.personal.wanandroid.core.ui.ErrorContent
import com.personal.wanandroid.core.ui.MessageCard

@Composable
internal fun ArticleRoute(
    article: ArticleKey,
    onBack: () -> Unit,
    viewModel: ArticleViewModel = hiltViewModel<ArticleViewModel, ArticleViewModel.Factory>(
        creationCallback = { it.create(article) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val holder = remember { BrowserHolder() }
    val back = { navigateReaderBack(holder.view, onBack) }
    // Always intercept here; decide from the live browser when the gesture is dispatched.
    BackHandler { back() }
    ArticleScreen(
        state,
        back,
        {
            holder.captureHistory()
            viewModel.retry()
        },
        { viewModel.requestExternal() },
        viewModel::dismissExternal,
        {
            viewModel.consumeExternal()?.let { url ->
                try {
                    context.startActivity(readerExternalIntent(url))
                } catch (_: ActivityNotFoundException) {
                    viewModel.externalUnavailable()
                } catch (_: SecurityException) {
                    viewModel.externalUnavailable()
                }
            }
        },
        viewModel::clearNotice
    ) { modifier ->
        ReaderWebContent(
            state,
            holder,
            modifier,
            viewModel::attachBrowser,
            viewModel::detachBrowser,
            viewModel::onBrowserEvent
        )
    }
}

private class BrowserHolder {
    var view: ReaderWebView? = null
    var pendingHistory: Bundle? = null
    fun captureHistory() {
        pendingHistory = view?.takeUnless { it.released || it.rendererGone }?.let { browser ->
            Bundle().also { browser.saveState(it) }
        }
    }
}

@Composable
private fun ReaderWebContent(
    state: ReaderUiState,
    holder: BrowserHolder,
    modifier: Modifier,
    onAttach: () -> Long,
    onDetach: (Long) -> Unit,
    onEvent: (Long, ReaderEvent) -> Unit
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val background = MaterialTheme.colorScheme.background.toArgb()
    val fontScale = LocalConfiguration.current.fontScale
    val owner = LocalLifecycleOwner.current
    val latestEvent by rememberUpdatedState(onEvent)
    val latestDetach by rememberUpdatedState(onDetach)
    LaunchedEffect(state.failure) {
        if (state.failure ==
            ReaderFailure.TIMEOUT
        ) {
            holder.view?.takeUnless { it.released || it.rendererGone }?.stopLoading()
        }
    }
    DisposableEffect(owner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            holder.view?.takeUnless { it.released || it.rendererGone }?.let { browser ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> browser.onResume()
                    Lifecycle.Event.ON_PAUSE -> browser.onPause()
                    else -> Unit
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (ReaderUrlPolicy.inAppUrl(state.url) != null && state.failure != ReaderFailure.RENDERER) {
        key(state.reloadId, dark) {
            val binding = remember { BrowserBinding() }
            AndroidView(
                factory = { context ->
                    val theme = if (dark) {
                        android.R.style.Theme_Material_NoActionBar
                    } else {
                        android.R.style.Theme_Material_Light_NoActionBar
                    }
                    val themed = ContextThemeWrapper(context, theme)
                    ReaderWebView(themed).apply {
                        binding.id = onAttach()
                        holder.view = this
                        configureReader(dark, fontScale)
                        setBackgroundColor(background)
                        val emit: (ReaderEvent) -> Unit = { latestEvent(binding.id, it) }
                        webViewClient = ReaderWebViewClient(emit)
                        webChromeClient = ReaderChromeClient(emit)
                        setDownloadListener { _, _, _, _, _ -> emit(ReaderEvent.Blocked) }
                        val history = holder.pendingHistory
                        holder.pendingHistory = null
                        if (history == null || restoreState(history) == null) {
                            loadUrl(requireNotNull(ReaderUrlPolicy.inAppUrl(state.url)))
                        } else {
                            reload()
                        }
                        if (!owner.lifecycle.currentState.isAtLeast(
                                Lifecycle.State.RESUMED
                            )
                        ) {
                            onPause()
                        }
                    }
                },
                update = { browser ->
                    if (!browser.released &&
                        !browser.rendererGone
                    ) {
                        browser.settings.textZoom =
                            (fontScale * 100).toInt().coerceIn(50, 300)
                    }
                },
                onRelease = { browser ->
                    latestDetach(binding.id)
                    if (holder.view === browser) holder.view = null
                    browser.release()
                },
                modifier = modifier
            )
        }
    }
}
private class BrowserBinding {
    var id = 0L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismissExternal: () -> Unit,
    onConfirmExternal: () -> Unit,
    onNoticeShown: () -> Unit,
    webContent: @Composable (Modifier) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val notice = state.notice?.let {
        stringResource(
            if (it ==
                ReaderNotice.BLOCKED_LINK
            ) {
                R.string.reader_blocked_link
            } else {
                R.string.reader_external_unavailable
            }
        )
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            snackbar.showSnackbar(notice)
            onNoticeShown()
        }
    }
    AppScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.title.ifBlank {
                            stringResource(R.string.reader_title)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
                actions = {
                    if (ReaderUrlPolicy.inAppUrl(state.url) !=
                        null
                    ) {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.reader_refresh))
                        }
                    }
                    if (ReaderUrlPolicy.externalUrl(state.url) !=
                        null
                    ) {
                        TextButton(onClick = onOpenExternal) {
                            Text(stringResource(R.string.reader_external))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                LinearProgressIndicator(progress = {
                    state.progress / 100f
                }, modifier = Modifier.fillMaxWidth())
            }
            Box(Modifier.fillMaxSize()) {
                webContent(Modifier.fillMaxSize())
                state.failure?.let { failure ->
                    androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
                        if (failure == ReaderFailure.UNSUPPORTED_URL) {
                            MessageCard(stringResource(failure.messageId()))
                        } else {
                            ErrorContent(stringResource(failure.messageId()), onRetry)
                        }
                    }
                }
            }
        }
    }
    state.pendingExternal?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissExternal,
            title = { Text(stringResource(R.string.reader_external_title)) },
            text = { Text(stringResource(R.string.reader_external_message, target.take(300))) },
            confirmButton = {
                TextButton(onClick = onConfirmExternal) {
                    Text(stringResource(R.string.reader_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExternal) {
                    Text(stringResource(R.string.reader_cancel))
                }
            }
        )
    }
}
private fun ReaderFailure.messageId(): Int = when (this) {
    ReaderFailure.UNSUPPORTED_URL -> R.string.reader_unsupported_url
    ReaderFailure.NETWORK -> R.string.reader_network_error
    ReaderFailure.HTTP -> R.string.reader_http_error
    ReaderFailure.TLS -> R.string.reader_tls_error
    ReaderFailure.TIMEOUT -> R.string.reader_timeout
    ReaderFailure.RENDERER -> R.string.reader_renderer_error
    ReaderFailure.UNSAFE -> R.string.reader_unsafe
}

internal fun navigateReaderBack(browser: ReaderWebView?, onExit: () -> Unit) {
    if (browser != null && !browser.released && !browser.rendererGone && browser.canGoBack()) {
        browser.goBack()
    } else {
        onExit()
    }
}

internal fun readerExternalIntent(url: String): Intent {
    val target = Uri.parse(requireNotNull(ReaderUrlPolicy.externalUrl(url)))
    return Intent(Intent.ACTION_VIEW, target).apply {
        if (target.scheme in setOf("https", "http")) addCategory(Intent.CATEGORY_BROWSABLE)
    }
}
