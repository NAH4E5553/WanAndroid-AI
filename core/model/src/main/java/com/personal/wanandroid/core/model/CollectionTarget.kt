package com.personal.wanandroid.core.model

/** Internal articles use articleId; externally added records only have recordId. */
data class CollectionTarget(val articleId: Long? = null, val recordId: Long? = null) {
    init {
        require(articleId != null || recordId != null)
        require(articleId == null || articleId >= 0)
        require(recordId == null || recordId >= 0)
    }
    val key: String get() = articleId?.let { "article:$it" } ?: "record:$recordId"
}
