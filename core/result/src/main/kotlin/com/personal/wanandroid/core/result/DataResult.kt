package com.personal.wanandroid.core.result

sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>
    data class Failure(val reason: DataError) : DataResult<Nothing>
}

enum class DataError { NETWORK, SERVICE, SESSION_EXPIRED, INVALID_RESPONSE }

/** Transform only successful values; cancellation and transform exceptions propagate. */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}
