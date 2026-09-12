package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey

/** Compare instance identity, not equality of logical destination parameters. */
fun MutableList<NavKey>.popIfCurrent(expectedEntryId: String): Boolean {
    if (size <= 1 || (lastOrNull() as? AppRoute)?.entryId != expectedEntryId) return false
    removeAt(lastIndex)
    return true
}
