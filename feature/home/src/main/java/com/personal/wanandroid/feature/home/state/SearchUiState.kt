package com.personal.wanandroid.feature.home.state

import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.model.Article

internal data class SearchUiState(
    val input: String = "",
    val keyword: String = "",
    val page: PagedUiState<Article> = PagedUiState(),
    val contextGeneration: Long = 0,
    val suggestions: SearchSuggestionsState = SearchSuggestionsState(),
    val isEditing: Boolean = true
) {
    val hasSubmitted get() = keyword.isNotEmpty()
    val canSubmit get() = input.isNotBlank()
    val showResults get() = hasSubmitted && !isEditing && input.trim() == keyword
}
