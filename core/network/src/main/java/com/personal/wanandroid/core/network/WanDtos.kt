package com.personal.wanandroid.core.network

import kotlinx.serialization.Serializable

@Serializable
data class WanResponse<T>(val errorCode: Int, val errorMsg: String = "", val data: T? = null)

@Serializable
data class WanPageDto<T>(val datas: List<T>, val curPage: Int, val over: Boolean, val total: Int)

@Serializable
data class ArticleDto(
    val id: Long,
    val title: String,
    val link: String,
    val author: String? = null,
    val shareUser: String? = null,
    val chapterName: String? = null,
    val niceDate: String? = null,
    val collect: Boolean = false
)

@Serializable
data class TopicDto(val id: Long, val name: String, val children: List<TopicDto> = emptyList())
