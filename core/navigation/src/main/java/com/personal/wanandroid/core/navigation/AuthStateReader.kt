package com.personal.wanandroid.core.navigation

/** App adapts authentication; navigation never depends on repositories or transport. */
fun interface AuthStateReader {
    fun authenticatedAccountId(): Long?
    fun knownAccountId(): Long? = authenticatedAccountId()
}
