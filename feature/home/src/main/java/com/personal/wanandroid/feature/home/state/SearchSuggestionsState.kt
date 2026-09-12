package com.personal.wanandroid.feature.home.state

import com.personal.wanandroid.core.model.SearchHistory
import com.personal.wanandroid.core.result.DataError

internal data class SearchSuggestionsState(
    val history: SearchHistory = SearchHistory(),
    val hotKeys: List<String> = emptyList(),
    val hotLoading: Boolean = true,
    val hotError: DataError? = null,
    val historyWriteFailed: Boolean = false
)
