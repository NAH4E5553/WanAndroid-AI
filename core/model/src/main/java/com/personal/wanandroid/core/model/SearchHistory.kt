package com.personal.wanandroid.core.model

/** Device-local recent queries; unrelated to account credentials or article reading history. */
data class SearchHistory(
    val items: List<String> = emptyList(),
    val ready: Boolean = false,
    val readFailed: Boolean = false
)
