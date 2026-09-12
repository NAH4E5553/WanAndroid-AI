package com.personal.wanandroid.feature.topics.state

import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.result.DataError

internal data class TopicsUiState(
    val topics: List<Topic> = emptyList(),
    val loading: Boolean = true,
    val error: DataError? = null,
    val selectedId: Long? = null,
    val selectedParentId: Long? = null,
    val pageStates: Map<Long, PagedUiState<Article>> = emptyMap()
) {
    val selectedTopic get() = topics.firstOrNull { it.id == selectedId }
    val parents get() = topics.filter { it.parentId == null }
    val tabs get() = topics.filter { selectedParentId != null && it.parentId == selectedParentId }
    val page get() = pageStates[selectedId] ?: PagedUiState()
}
