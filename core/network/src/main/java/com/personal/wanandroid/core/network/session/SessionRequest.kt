package com.personal.wanandroid.core.network.session

import okhttp3.Cookie

/** Per-request identity also isolates login and detached logout from the shared session. */
class SessionRequest internal constructor(
    val generation: Long,
    internal val mode: Mode = Mode.NORMAL,
    internal val logoutCookies: List<Cookie> = emptyList()
) {
    internal enum class Mode { NORMAL, LOGIN, LOGOUT }
    override fun toString(): String = "SessionRequest(generation=$generation, mode=$mode)"
}
