package com.personal.wanandroid.core.model

/** Local reading metadata only; presence does not imply offline content exists. */
data class ReadingHistory(
    val url: String,
    val articleId: Long?,
    val title: String,
    val lastReadAt: Long
)
