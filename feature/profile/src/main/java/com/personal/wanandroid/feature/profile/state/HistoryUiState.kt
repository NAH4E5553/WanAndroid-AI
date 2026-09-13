package com.personal.wanandroid.feature.profile.state

import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.core.result.DataError

data class HistoryUiState(
    val page: PagedUiState<ReadingHistory> = PagedUiState(),
    val busy: Boolean = false,
    val error: DataError? = null
)
