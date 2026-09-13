package com.personal.wanandroid.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.common.paging.PagingController
import com.personal.wanandroid.core.data.repository.CollectionRepository
import com.personal.wanandroid.core.model.CollectionItem
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.profile.state.CollectionsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CollectionsViewModel @Inject constructor(private val repository: CollectionRepository) :
    ViewModel() {
    private val errors = MutableStateFlow<Pair<Long, DataError>?>(null)
    private val pager = PagingController(
        viewModelScope,
        0,
        { item: CollectionItem -> requireNotNull(item.target.recordId) },
        -1L
    ) { generation, page ->
        if (generation < 0) {
            DataResult.Success(PageResult(emptyList(), null))
        } else {
            repository.page(generation, page)
        }
    }
    val uiState = combine(repository.state, pager.state, errors) { snapshot, paging, error ->
        CollectionsUiState(
            snapshot,
            if (snapshot.generation != null && paging.context == snapshot.generation) {
                paging.page.copy(
                    items = paging.page.items.filter {
                        snapshot.status(it.target).collected != false
                    }
                )
            } else {
                PagedUiState()
            },
            error?.second.takeIf { error?.first == snapshot.generation }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CollectionsUiState())

    init {
        viewModelScope.launch {
            repository.state.map { it.generation to it.revision }
                .distinctUntilChanged().collect { (generation, _) ->
                    if (errors.value?.first != generation) errors.value = null
                    pager.reset(generation ?: -1L)
                }
        }
    }

    fun remove(item: CollectionItem, generation: Long) {
        if (repository.current().generation != generation) return
        errors.value = null
        viewModelScope.launch {
            val result = repository.setCollected(generation, item.target, false)
            if (result is DataResult.Failure &&
                repository.current().generation == generation
            ) {
                errors.value =
                    generation to result.reason
            }
        }
    }
    fun refresh() = pager.refresh()
    fun retryInitial() = pager.retryInitial()
    fun retryRefresh() = pager.retryRefresh()
    fun retryAppend() = pager.retryAppend()
    fun loadMore() = pager.loadMore()
    fun continueAfterPause() = pager.continueAfterPause()
}
