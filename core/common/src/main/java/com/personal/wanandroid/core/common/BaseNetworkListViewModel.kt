package com.personal.wanandroid.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataResult

/** Thin adapter of the source list base: no duplicate page, job, or mutable UI state. */
abstract class BaseNetworkListViewModel<T : Any>(
    initialPage: Int,
    keyOf: (T) -> Any,
    requestPage: suspend (Int) -> DataResult<PageResult<T>>
) : ViewModel() {
    private val controller by lazy(LazyThreadSafetyMode.NONE) {
        PagingController(viewModelScope, initialPage, keyOf, requestPage)
    }
    val uiState get() = controller.state
    protected fun startInitialLoad() = controller.startInitialLoad()
    fun refresh() = controller.refresh()
    fun loadMore() = controller.loadMore()
    fun continueAfterPause() = controller.continueAfterPause()
    fun retryInitialLoad() = controller.retryInitial()
    fun retryRefresh() = controller.retryRefresh()
    fun retryLoadMore() = controller.retryAppend()
}
