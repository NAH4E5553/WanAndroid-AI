package com.personal.wanandroid.feature.article.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.wanandroid.core.model.CollectionStatus
import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.navigation.ArticleRoute as ArticleKey
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.ui.R as CoreUiR
import com.personal.wanandroid.core.ui.component.network.ErrorContent
import com.personal.wanandroid.core.ui.component.network.MessageCard
import com.personal.wanandroid.core.ui.component.network.errorMessage
import com.personal.wanandroid.core.ui.component.scaffold.AppScaffold
import com.personal.wanandroid.feature.article.R
import com.personal.wanandroid.feature.article.component.ArticleCollectionAction
import com.personal.wanandroid.feature.article.component.ReaderChromeClient
import com.personal.wanandroid.feature.article.component.ReaderWebView
import com.personal.wanandroid.feature.article.component.ReaderWebViewClient
import com.personal.wanandroid.feature.article.component.configureReader
import com.personal.wanandroid.feature.article.policy.ReaderUrlPolicy
import com.personal.wanandroid.feature.article.state.ReaderEvent
import com.personal.wanandroid.feature.article.state.ReaderFailure
import com.personal.wanandroid.feature.article.state.ReaderNotice
import com.personal.wanandroid.feature.article.state.ReaderUiState
import com.personal.wanandroid.feature.article.viewmodel.ArticleCollectionViewModel
import com.personal.wanandroid.feature.article.viewmodel.ArticleViewModel

@Composable
internal fun ArticleRoute(
    article: ArticleKey,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    collectionViewModel: ArticleCollectionViewModel = hiltViewModel(),
    viewModel: ArticleViewModel = hiltViewModel<ArticleViewModel, ArticleViewModel.Factory>(
        creationCallback = { it.create(article) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val collections by collectionViewModel.collections.collectAsStateWithLifecycle()
    val collectionError by collectionViewModel.error.collectAsStateWithLifecycle()
    val target = remember(article) {
        if (article.articleId != null ||
            article.collectionRecordId != null
        ) {
            CollectionTarget(
                article.articleId,
                // A re-collection creates a new record; internal readers always use origin ID.
                article.collectionRecordId.takeIf { article.articleId == null }
            )
        } else {
            null
        }
    }
    val initialCollected = article.collected.takeIf {
        article.collectionSession != null && article.collectionSession == collections.sessionKey
    } ?: false
    val collectionStatus = target?.takeIf {
        ReaderUrlPolicy.samePage(state.url, article.url)
    }?.let(collections::status)
    val context = LocalContext.current
    val trace = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    LaunchedEffect(article, collectionStatus, collections.generation, trace) {
        if (trace) {
            val source = when {
                collections.generation == null -> "guest_rule"

                collectionStatus?.collected != null -> "account_cache"

                article.collectionSession != null &&
                    article.collectionSession == collections.sessionKey -> "route_collect"

                else -> "default"
            }
            Log.d(
                "CollectionTrace",
                "reader.state id=${article.articleId} routeCollect=${article.collected} " +
                    "cacheCollect=${collectionStatus?.collected} " +
                    "generation=${collections.generation} busy=${collectionStatus?.busy} " +
                    "available=${collectionStatus != null} source=$source"
            )
        }
    }
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
        viewModel::clearNotice,
        collectionStatus = collectionStatus,
        initialCollected = initialCollected,
        authenticated = collections.generation != null,
        canAddCollection = target?.articleId != null,
        collectionError = collectionError,
        onCollection = { displayedCollected ->
            val generation = collections.generation
            if (trace) {
                Log.d(
                    "CollectionTrace",
                    "reader.click id=${article.articleId} displayedCollect=$displayedCollected " +
                        "generation=$generation action=${if (generation == null) "login" else "toggle"}"
                )
            }
            if (generation == null) {
                onLogin()
            } else if (target != null) {
                collectionViewModel.toggle(target, generation, displayedCollected)
            }
        }
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
    collectionStatus: CollectionStatus? = null,
    initialCollected: Boolean = false,
    authenticated: Boolean = false,
    canAddCollection: Boolean = true,
    collectionError: DataError? = null,
    onCollection: (Boolean) -> Unit = {},
    webContent: @Composable (Modifier) -> Unit
) {
    // Use the list value until an account-scoped operation supplies a newer known value.
    val displayedCollection = collectionStatus?.let {
        it.copy(collected = it.collected ?: initialCollected)
    }
    val snackbar = remember { SnackbarHostState() }
    var menuExpanded by remember(state.url) { mutableStateOf(false) }
    val collectionMessage = collectionError?.let {
        stringResource(R.string.collection_operation_failed, errorMessage(it))
    }
    LaunchedEffect(collectionMessage) {
        if (collectionMessage != null) snackbar.showSnackbar(collectionMessage)
    }
    val notice = state.notice?.let {
        stringResource(
            when (it) {
                ReaderNotice.BLOCKED_LINK -> R.string.reader_blocked_link
                ReaderNotice.EXTERNAL_UNAVAILABLE -> R.string.reader_external_unavailable
                ReaderNotice.HISTORY_SAVE_FAILED -> R.string.reader_history_save_failed
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
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.reader_more_actions)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reader_refresh)) },
                                enabled = ReaderUrlPolicy.inAppUrl(state.url) != null,
                                onClick = {
                                    menuExpanded = false
                                    onRetry()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reader_external)) },
                                enabled = ReaderUrlPolicy.externalUrl(state.url) != null,
                                onClick = {
                                    menuExpanded = false
                                    onOpenExternal()
                                }
                            )
                            ArticleCollectionAction(
                                status = displayedCollection ?: CollectionStatus(false),
                                authenticated = authenticated,
                                canAdd = canAddCollection,
                                available = collectionStatus != null,
                                onClick = {
                                    menuExpanded = false
                                    onCollection(displayedCollection?.collected ?: initialCollected)
                                }
                            )
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
