package com.personal.wanandroid.core.network.session

/** Opaque encrypted persistence is supplied by core:data. Never log payloads. */
interface SessionStorage {
    fun read(): String?
    fun write(payload: String?)
}
