package com.personal.wanandroid.feature.article.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.navigation.ArticleRoute as ArticleKey
import com.personal.wanandroid.feature.article.policy.ReaderUrlPolicy
import com.personal.wanandroid.feature.article.state.ReaderEvent
import com.personal.wanandroid.feature.article.state.ReaderFailure
import com.personal.wanandroid.feature.article.state.ReaderNotice
import com.personal.wanandroid.feature.article.state.ReaderUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Browser objects stay in the composition; only a safe current URL enters saved state. */
@HiltViewModel(assistedFactory = ArticleViewModel.Factory::class)
internal class ArticleViewModel @AssistedInject constructor(
    private val savedState: SavedStateHandle,
    @Assisted private val article: ArticleKey
) : ViewModel() {
    private val initialUrl = savedState.get<String>("reader.url")?.let(ReaderUrlPolicy::inAppUrl)
        ?: ReaderUrlPolicy.inAppUrl(article.url) ?: article.url
    private val mutableState = MutableStateFlow(
        ReaderUiState(
            initialUrl,
            article.title,
            failure =
                if (ReaderUrlPolicy.inAppUrl(initialUrl) ==
                    null
                ) {
                    ReaderFailure.UNSUPPORTED_URL
                } else {
                    null
                }
        )
    )
    val uiState = mutableState.asStateFlow()
    private var sequence = 0L
    private var activeBrowser: Long? = null
    private var timeout: Job? = null

    fun attachBrowser(): Long {
        val id = ++sequence
        activeBrowser = id
        mutableState.value = uiState.value.copy(
            loading = true,
            progress = 0,
            failure = null,
            canGoBack = false,
            pendingExternal = null
        )
        armTimeout(id)
        return id
    }

    fun detachBrowser(id: Long) {
        if (activeBrowser != id) return
        activeBrowser = null
        timeout?.cancel()
        mutableState.value = uiState.value.copy(pendingExternal = null)
    }

    fun onBrowserEvent(id: Long, event: ReaderEvent) {
        if (activeBrowser != id) return
        val current = uiState.value
        when (event) {
            is ReaderEvent.Started -> {
                val url = ReaderUrlPolicy.inAppUrl(event.url) ?: return
                if (current.failure != null && ReaderUrlPolicy.samePage(current.url, url)) return
                mutableState.value = current.copy(
                    url = url,
                    loading = true,
                    progress = 0,
                    failure = null,
                    pendingExternal = null
                )
                savedState["reader.url"] = url
                armTimeout(id)
            }

            is ReaderEvent.Progress -> if (matches(event.url) && current.failure == null) {
                mutableState.value =
                    current.copy(progress = maxOf(current.progress, event.percent.coerceIn(0, 100)))
            }

            is ReaderEvent.Finished -> if (matches(event.url) && current.failure == null) {
                timeout?.cancel()
                mutableState.value = current.copy(
                    loading = false,
                    progress = 100,
                    title =
                        event.title?.trim()?.take(200)?.takeIf { it.isNotEmpty() } ?: current.title
                )
            }

            is ReaderEvent.History -> {
                val url = ReaderUrlPolicy.inAppUrl(event.url) ?: return
                mutableState.value = current.copy(url = url, canGoBack = event.canGoBack)
                savedState["reader.url"] = url
            }

            is ReaderEvent.Failed -> if (matches(event.url)) fail(event.reason)

            is ReaderEvent.External -> requestExternal(event.url)

            ReaderEvent.Blocked ->
                mutableState.value =
                    current.copy(notice = ReaderNotice.BLOCKED_LINK)

            ReaderEvent.Unsafe -> fail(ReaderFailure.UNSAFE)

            ReaderEvent.RendererGone -> fail(ReaderFailure.RENDERER)
        }
    }

    fun retry() {
        if (ReaderUrlPolicy.inAppUrl(uiState.value.url) == null) return
        activeBrowser = null
        timeout?.cancel()
        mutableState.value = uiState.value.copy(
            reloadId = uiState.value.reloadId + 1,
            failure = null,
            loading = true,
            progress = 0,
            canGoBack = false,
            pendingExternal = null
        )
    }
    fun requestExternal(url: String = uiState.value.url) {
        val target = ReaderUrlPolicy.externalUrl(url)
        mutableState.value =
            if (target == null) {
                uiState.value.copy(notice = ReaderNotice.BLOCKED_LINK)
            } else {
                uiState.value.copy(pendingExternal = target)
            }
    }
    fun dismissExternal() {
        mutableState.value = uiState.value.copy(pendingExternal = null)
    }
    fun consumeExternal(): String? {
        val target = uiState.value.pendingExternal?.let(ReaderUrlPolicy::externalUrl)
        dismissExternal()
        return target
    }
    fun externalUnavailable() {
        mutableState.value =
            uiState.value.copy(notice = ReaderNotice.EXTERNAL_UNAVAILABLE)
    }
    fun clearNotice() {
        mutableState.value = uiState.value.copy(notice = null)
    }
    private fun matches(url: String) = ReaderUrlPolicy.samePage(uiState.value.url, url)
    private fun fail(reason: ReaderFailure) {
        timeout?.cancel()
        mutableState.value =
            uiState.value.copy(
                failure = reason,
                loading = false,
                pendingExternal = null,
                canGoBack = if (reason == ReaderFailure.RENDERER) false else uiState.value.canGoBack
            )
    }
    private fun armTimeout(id: Long) {
        timeout?.cancel()
        timeout = viewModelScope.launch {
            delay(30_000)
            if (activeBrowser == id && uiState.value.loading) fail(ReaderFailure.TIMEOUT)
        }
    }

    @AssistedFactory interface Factory {
        fun create(article: ArticleKey): ArticleViewModel
    }
}
