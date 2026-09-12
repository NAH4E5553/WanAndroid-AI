package com.personal.wanandroid.feature.article.state

internal sealed interface ReaderEvent {
    data class Started(val url: String) : ReaderEvent
    data class Progress(val url: String, val percent: Int) : ReaderEvent
    data class Finished(val url: String, val title: String?) : ReaderEvent
    data class History(val url: String, val canGoBack: Boolean) : ReaderEvent
    data class Failed(val url: String, val reason: ReaderFailure) : ReaderEvent
    data class External(val url: String) : ReaderEvent
    data object Blocked : ReaderEvent
    data object Unsafe : ReaderEvent
    data object RendererGone : ReaderEvent
}
