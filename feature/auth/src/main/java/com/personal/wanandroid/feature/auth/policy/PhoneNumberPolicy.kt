package com.personal.wanandroid.feature.auth.policy

private val mainlandMobilePattern = Regex("1[3-9][0-9]{9}")

/** Local format check only; it does not verify assignment or ownership of a phone number. */
internal fun isMainlandMobileNumber(value: String): Boolean =
    mainlandMobilePattern.matches(value.trim())
