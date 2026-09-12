package com.personal.wanandroid.feature.article.state

internal enum class ReaderFailure { UNSUPPORTED_URL, NETWORK, HTTP, TLS, TIMEOUT, RENDERER, UNSAFE }
internal enum class ReaderNotice { BLOCKED_LINK, EXTERNAL_UNAVAILABLE }
internal data class ReaderUiState(
    val url: String,
    val title: String,
    val loading: Boolean = false,
    val progress: Int = 0,
    val failure: ReaderFailure? = null,
    val canGoBack: Boolean = false,
    val reloadId: Long = 0,
    val pendingExternal: String? = null,
    val notice: ReaderNotice? = null
)
