package com.personal.wanandroid.core.data.mapper

import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.session.SessionChangedException
import com.personal.wanandroid.core.network.session.SessionStorageException
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.core.result.map
import java.io.IOException
import kotlinx.coroutines.CancellationException

internal suspend fun <T : Any, R> requestWithData(
    call: suspend () -> WanResponse<T>,
    map: (T) -> R
): DataResult<R> = try {
    val response = call()
    val body = response.data
    when {
        response.errorCode == -1001 -> DataResult.Failure(DataError.SESSION_EXPIRED)
        response.errorCode != 0 -> DataResult.Failure(DataError.SERVICE)
        body == null -> DataResult.Failure(DataError.INVALID_RESPONSE)
        else -> DataResult.Success(body).map(map)
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: SessionStorageException) {
    DataResult.Failure(DataError.STORAGE)
} catch (_: SessionChangedException) {
    DataResult.Failure(DataError.SESSION_CHANGED)
} catch (_: IOException) {
    DataResult.Failure(DataError.NETWORK)
} catch (_: Exception) {
    DataResult.Failure(DataError.INVALID_RESPONSE)
}

/** Successful write responses may legitimately have no data. */
internal suspend fun requestWithoutData(call: suspend () -> WanResponse<*>): DataResult<Unit> =
    requestWithData({
        val response = call()
        WanResponse(response.errorCode, response.errorMsg, Unit)
    }) { Unit }
