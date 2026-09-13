package com.personal.wanandroid.feature.article.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.repository.CollectionRepository
import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class ArticleCollectionViewModel @Inject constructor(
    private val repository: CollectionRepository
) : ViewModel() {
    private val errors = MutableStateFlow<Pair<Long, DataError>?>(null)
    val collections = repository.state.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        repository.current()
    )
    val error = combine(repository.state, errors) { snapshot, failure ->
        failure?.second.takeIf {
            failure?.first ==
                snapshot.generation
        }
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun toggle(target: CollectionTarget, generation: Long, collected: Boolean) {
        if (repository.current().generation != generation) return
        errors.value = null
        viewModelScope.launch {
            // Unknown transport state is reconciled within this explicit action, not as a
            // separate user-facing step. A failed reconciliation must never trigger a POST.
            if (repository.current().status(target).collected == null) {
                val result = repository.reconcile(generation, target)
                if (result is DataResult.Failure) {
                    report(generation, result)
                    return@launch
                }
            }
            val snapshot = repository.current()
            if (snapshot.generation != generation || snapshot.status(target).busy ||
                snapshot.status(target).collected == null
            ) {
                return@launch
            }
            report(generation, repository.setCollected(generation, target, !collected))
        }
    }
    private fun report(generation: Long, result: DataResult<Unit>) {
        if (repository.current().generation ==
            generation
        ) {
            errors.value =
                (result as? DataResult.Failure)?.let { generation to it.reason }
        }
    }
}
