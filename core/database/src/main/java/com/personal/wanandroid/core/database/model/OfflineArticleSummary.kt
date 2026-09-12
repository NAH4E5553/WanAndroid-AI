package com.personal.wanandroid.core.database.model

data class OfflineArticleSummary(
    val url: String,
    val title: String,
    val status: String,
    val cachedAt: Long?,
    val byteCount: Long
)
