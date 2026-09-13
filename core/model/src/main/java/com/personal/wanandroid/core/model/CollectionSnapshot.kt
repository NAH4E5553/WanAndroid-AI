package com.personal.wanandroid.core.model

/** Null collected means unknown: the next action is a read-only reconciliation. */
data class CollectionStatus(val collected: Boolean? = null, val busy: Boolean = false)

data class CollectionSnapshot(
    val generation: Long? = null,
    val statuses: Map<String, CollectionStatus> = emptyMap(),
    val revision: Long = 0
) {
    fun status(target: CollectionTarget) = statuses[target.key] ?: CollectionStatus()
}
