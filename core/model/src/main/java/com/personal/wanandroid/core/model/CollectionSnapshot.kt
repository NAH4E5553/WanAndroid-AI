package com.personal.wanandroid.core.model

/** Null collected means unknown: reconcile before deciding whether a write is needed. */
data class CollectionStatus(val collected: Boolean? = null, val busy: Boolean = false)

data class CollectionSnapshot(
    val generation: Long? = null,
    val statuses: Map<String, CollectionStatus> = emptyMap(),
    val revision: Long = 0,
    val sessionKey: String? = null
) {
    fun status(target: CollectionTarget) = statuses[target.key] ?: CollectionStatus()
}
