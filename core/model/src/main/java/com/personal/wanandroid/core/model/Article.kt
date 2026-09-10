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

data class Topic(val id: Long, val name: String, val parentId: Long? = null)

/** nextPage is a request cursor, not the response curPage display number. */
data class PageResult<T>(val items: List<T>, val nextPage: Int?)

sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>
    data class Failure(val reason: DataError) : DataResult<Nothing>
}

enum class DataError { NETWORK, SERVICE, SESSION_EXPIRED, INVALID_RESPONSE }
