package com.personal.wanandroid.core.common.base.state

/** Context and results move together; page is the same snapshot consumed by shared list UI. */
data class PagingState<T : Any, C : Any>(
    val context: C,
    val page: PagedUiState<T> = PagedUiState(),
    val contextGeneration: Long = 0
)
