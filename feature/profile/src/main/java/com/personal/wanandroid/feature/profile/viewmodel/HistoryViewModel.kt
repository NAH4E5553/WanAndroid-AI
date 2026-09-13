package com.personal.wanandroid.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.common.paging.PagingController
import com.personal.wanandroid.core.data.repository.ReadingHistoryRepository
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.profile.state.HistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: ReadingHistoryRepository) :
    ViewModel() {
    private data class ActionState(val busy: Boolean = false, val error: DataError? = null)
    private val actions = MutableStateFlow(ActionState())
    private var observer: Job? = null
    private val pager =
        PagingController(viewModelScope, 0, ReadingHistory::url, Unit) { _, page ->
            repository.page(page)
        }
    val uiState = combine(pager.state, actions) { paging, action ->
        HistoryUiState(paging.page, action.busy, action.error)
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())
    init {
        pager.startInitialLoad()
        observe()
    }
    private fun observe() {
        observer = viewModelScope.launch {
            try {
                repository.changes.collect { pager.reset(Unit) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                actions.value = actions.value.copy(error = DataError.STORAGE)
            }
        }
    }
    fun delete(url: String) = mutate { repository.delete(url) }
    fun clear() = mutate { repository.clear() }
    private fun mutate(operation: suspend () -> DataResult<Unit>) {
        if (actions.value.busy) return
        actions.value = actions.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                when (val result = operation()) {
                    is DataResult.Success -> pager.reset(Unit)

                    is DataResult.Failure ->
                        actions.value =
                            actions.value.copy(error = result.reason)
                }
            } finally {
                actions.value = actions.value.copy(busy = false)
            }
        }
    }
    fun refresh() {
        actions.value = actions.value.copy(error = null)
        if (observer?.isActive != true) observe()
        pager.refresh()
    }
    fun retryInitial() = refresh()
    fun retryRefresh() = refresh()
    fun retryAppend() = pager.retryAppend()
    fun loadMore() = pager.loadMore()
    fun continueAfterPause() = pager.continueAfterPause()
}
