package com.personal.wanandroid.core.network.session

import java.io.IOException

/** A response belongs to a superseded session, not a malformed server response. */
class SessionChangedException : IOException("Session changed")
