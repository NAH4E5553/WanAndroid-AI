package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleDto(
    val id: Long,
    val title: String,
    val link: String,
    val author: String? = null,
    val shareUser: String? = null,
    val superChapterName: String? = null,
    val chapterName: String? = null,
    val niceDate: String? = null,
    val collect: Boolean = false
)
