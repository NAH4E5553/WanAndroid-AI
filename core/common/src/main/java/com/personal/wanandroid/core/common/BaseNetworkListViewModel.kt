package com.personal.wanandroid.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Thin adapter of the source list base: no duplicate page, job, or mutable UI state. */
abstract class BaseNetworkListViewModel<T : Any>(
    initialPage: Int,
    keyOf: (T) -> Any,
    requestPage: suspend (Int) -> DataResult<PageResult<T>>
) : ViewModel() {
    private val controller by lazy(LazyThreadSafetyMode.NONE) {
        PagingController(viewModelScope, initialPage, keyOf, Unit) { _, page -> requestPage(page) }
    }
    val uiState by lazy(LazyThreadSafetyMode.NONE) {
        controller.state.map { it.page }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            controller.state.value.page
        )
    }
    protected fun startInitialLoad() = controller.startInitialLoad()
    fun refresh() = controller.refresh()
    fun loadMore() = controller.loadMore()
    fun continueAfterPause() = controller.continueAfterPause()
    fun retryInitialLoad() = controller.retryInitial()
    fun retryRefresh() = controller.retryRefresh()
    fun retryLoadMore() = controller.retryAppend()
}
