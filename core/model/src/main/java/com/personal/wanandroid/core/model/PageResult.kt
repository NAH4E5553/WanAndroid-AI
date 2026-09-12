package com.personal.wanandroid.core.model

/** nextPage is a request cursor, not the response curPage display number. */
data class PageResult<T>(val items: List<T>, val nextPage: Int?)
