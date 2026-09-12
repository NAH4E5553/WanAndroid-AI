package com.personal.wanandroid.core.model

/** Domain article ID is distinct from a collection record ID. */
data class Article(
    val id: Long,
    val title: String,
    val url: String,
    val author: String,
    val shareUser: String,
    val superChapterName: String,
    val chapter: String,
    val publishedAt: String,
    val collected: Boolean
)
