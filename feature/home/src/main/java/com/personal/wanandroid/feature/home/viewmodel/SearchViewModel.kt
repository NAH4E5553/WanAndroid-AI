package com.personal.wanandroid.feature.home.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.common.paging.PagingController
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.data.repository.SearchSuggestionsRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.home.state.SearchSuggestionsState
import com.personal.wanandroid.feature.home.state.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val MAX_SEARCH_LENGTH = 200
private data class SearchDraft(val text: String, val editing: Boolean)

@HiltViewModel
internal class SearchViewModel @Inject constructor(
    repository: ArticleRepository,
    private val savedState: SavedStateHandle,
    private val suggestionsRepository: SearchSuggestionsRepository
) : ViewModel() {
    private val restoredInput = savedState.get<String>(
        "search.input"
    ).orEmpty().take(MAX_SEARCH_LENGTH)
    private val input = MutableStateFlow(
        SearchDraft(
            restoredInput,
            savedState.get<Boolean>("search.editing")
                ?: (restoredInput.trim() != savedState.get<String>("search.keyword").orEmpty())
        )
    )
    private val paging = PagingController(viewModelScope, 0, Article::id, "") { keyword, page ->
        repository.search(page, keyword)
    }
    private val suggestions = MutableStateFlow(SearchSuggestionsState())
    private sealed interface HistoryOperation {
        data class Record(val keyword: String) : HistoryOperation
        data object Clear : HistoryOperation
    }
    private val historyOperations = Channel<HistoryOperation>(64)
    private var historyJob: Job? = null
    private var hotJob: Job? = null
    val uiState = combine(input, paging.state, suggestions) { draft, snapshot, discovery ->
        SearchUiState(
            draft.text,
            snapshot.context,
            snapshot.page,
            snapshot.contextGeneration,
            discovery,
            draft.editing
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState(input = input.value.text))

    init {
        retryHistory()
        retryHotKeys()
        viewModelScope.launch {
            for (operation in historyOperations) {
                val success = when (operation) {
                    is HistoryOperation.Record -> suggestionsRepository.record(operation.keyword)
                    HistoryOperation.Clear -> suggestionsRepository.clearHistory()
                }
                suggestions.update { it.copy(historyWriteFailed = !success) }
            }
        }
        val restored = savedState.get<String>(
            "search.keyword"
        ).orEmpty().take(MAX_SEARCH_LENGTH).trim()
        if (restored.isNotEmpty()) paging.reset(restored)
    }

    fun editInput(value: String) {
        val text = value.take(MAX_SEARCH_LENGTH)
        if (text == input.value.text) return
        input.value = SearchDraft(text, editing = true)
        savedState["search.input"] = text
        savedState["search.editing"] = true
    }

    fun submit() {
        val keyword = input.value.text.trim()
        if (keyword.isEmpty()) return
        input.value = input.value.copy(editing = false)
        savedState["search.editing"] = false
        val current = paging.state.value
        if (keyword == current.context) {
            if (current.page.isInitialLoading || current.page.isRefreshing ||
                current.page.isLoadingMore
            ) {
                return
            }
            paging.refresh()
        } else {
            savedState["search.keyword"] = keyword
            paging.reset(keyword)
        }
        enqueueHistory(HistoryOperation.Record(keyword))
    }

    fun selectKeyword(keyword: String) {
        editInput(keyword)
        submit()
    }
    fun clearHistory() = enqueueHistory(HistoryOperation.Clear)
    private fun enqueueHistory(operation: HistoryOperation) {
        if (historyOperations.trySend(operation).isFailure) {
            suggestions.update { it.copy(historyWriteFailed = true) }
        }
    }
    fun retryHistory() {
        historyJob?.cancel()
        suggestions.update { it.copy(history = it.history.copy(ready = false, readFailed = false)) }
        historyJob = viewModelScope.launch {
            suggestionsRepository.history.collect { history ->
                suggestions.update { it.copy(history = history) }
            }
        }
    }
    fun retryHotKeys() {
        if (hotJob?.isActive == true) return
        hotJob = viewModelScope.launch {
            suggestions.update { it.copy(hotLoading = true, hotError = null) }
            when (val result = suggestionsRepository.hotKeys()) {
                is DataResult.Success -> suggestions.update {
                    it.copy(hotLoading = false, hotKeys = result.value, hotError = null)
                }

                is DataResult.Failure -> suggestions.update {
                    it.copy(hotLoading = false, hotError = result.reason)
                }
            }
        }
    }
    override fun onCleared() {
        historyOperations.close()
        super.onCleared()
    }

    fun refresh() {
        if (paging.state.value.context.isNotEmpty()) paging.refresh()
    }
    fun retryInitialLoad() = paging.retryInitial()
    fun retryRefresh() = paging.retryRefresh()
    fun loadMore() = paging.loadMore()
    fun retryLoadMore() = paging.retryAppend()
    fun continueAfterPause() = paging.continueAfterPause()
}
