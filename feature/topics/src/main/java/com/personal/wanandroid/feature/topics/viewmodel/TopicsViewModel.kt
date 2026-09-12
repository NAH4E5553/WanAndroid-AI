package com.personal.wanandroid.feature.topics.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.common.paging.PagingController
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.topics.state.TopicsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(TopicsUiState())
    internal val uiState = mutableState.asStateFlow()

    // Each visited ID owns one existing controller, including its retry/paused-page metadata.
    private val pages = mutableMapOf<Long, PagingController<Article, Long>>()
    private var categoryJob: Job? = null
    private var pageObserver: Job? = null

    init {
        retryTopics()
    }

    fun retryTopics() {
        if (categoryJob?.isActive == true) return
        categoryJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            when (val result = repository.topics()) {
                is DataResult.Success -> {
                    val topics = result.value.distinctBy { it.id }
                    val parentIds = topics.filter { it.parentId == null }.map { it.id }.toSet()
                    val childIds = topics.filter { it.parentId in parentIds }.map { it.id }.toSet()
                    pageObserver?.cancel()
                    pages.values.forEach { it.pauseLoading() }
                    pages.keys.retainAll(childIds)
                    mutableState.value = TopicsUiState(topics = topics, loading = false)
                    val restored = topics.firstOrNull {
                        it.id == savedState.get<Long>("topics.selectedId")
                    }
                    val parentId = restored?.let { it.parentId ?: it.id }
                        ?: savedState.get<Long>("topics.selectedParentId")
                    val parent = topics.firstOrNull { it.id == parentId && it.parentId == null }
                        ?: topics.firstOrNull { it.parentId == null }
                    if (parent != null) {
                        val child = restored?.takeIf { it.parentId == parent.id }
                        if (child != null) selectTopic(child.id) else selectParent(parent.id)
                    }
                }

                is DataResult.Failure -> mutableState.update {
                    it.copy(loading = false, error = result.reason)
                }
            }
        }
    }

    fun selectParent(id: Long) {
        val current = mutableState.value
        if (id == current.selectedParentId || current.parents.none { it.id == id }) return
        val restored = savedState.get<Long>("topics.child.$id")
        val target = current.topics.firstOrNull {
            it.id == restored && it.parentId == id
        }
        activate(id, target?.id ?: current.topics.firstOrNull { it.parentId == id }?.id)
    }

    fun selectChild(parentId: Long, id: Long) {
        val current = mutableState.value
        if (current.selectedParentId != parentId || current.tabs.none { it.id == id }) return
        selectTopic(id)
    }

    internal fun selectTopic(id: Long) {
        val topic = mutableState.value.topics.firstOrNull { it.id == id } ?: return
        val parentId = topic.parentId ?: return
        if (mutableState.value.parents.none { it.id == parentId }) return
        activate(parentId, id)
    }

    private fun activate(parentId: Long, id: Long?) {
        val current = mutableState.value
        if (current.selectedParentId == parentId && current.selectedId == id) return
        pageObserver?.cancel()
        pages[current.selectedId]?.pauseLoading()
        val controller = id?.let { childId ->
            pages.getOrPut(childId) {
                PagingController(viewModelScope, 0, Article::id, childId) { category, page ->
                    repository.articles(page, category)
                }
            }
        }
        controller?.startInitialLoad()
        controller?.resumeLoading()
        savedState["topics.selectedId"] = id
        savedState["topics.selectedParentId"] = parentId
        savedState["topics.child.$parentId"] = id
        mutableState.value = current.copy(
            selectedId = id,
            selectedParentId = parentId,
            pageStates = pages.mapValues { it.value.state.value.page }
        )
        if (controller == null || id == null) return
        pageObserver = viewModelScope.launch {
            controller.state.collect { snapshot ->
                mutableState.update { state ->
                    if (state.selectedId == id) {
                        state.copy(pageStates = state.pageStates + (id to snapshot.page))
                    } else {
                        state
                    }
                }
            }
        }
    }

    // Callbacks carry the rendered ID; a disposed list cannot act on the next category.
    private fun selected(id: Long): PagingController<Article, Long>? =
        if (mutableState.value.selectedId == id) pages[id] else null
    fun refresh(id: Long) {
        selected(id)?.refresh()
    }
    fun retryInitial(id: Long) {
        selected(id)?.retryInitial()
    }
    fun retryRefresh(id: Long) {
        selected(id)?.retryRefresh()
    }
    fun loadMore(id: Long) {
        selected(id)?.loadMore()
    }
    fun retryAppend(id: Long) {
        selected(id)?.retryAppend()
    }
    fun continueAfterPause(id: Long) {
        selected(id)?.continueAfterPause()
    }
}
