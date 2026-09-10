package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey

/** Ignore a stale/double-click callback and never remove the root entry. */
fun MutableList<NavKey>.popIfCurrent(expected: NavKey): Boolean {
    if (size <= 1 || last() != expected) return false
    removeAt(lastIndex)
    return true
}
