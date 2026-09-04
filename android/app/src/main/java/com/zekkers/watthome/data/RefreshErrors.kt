package com.zekkers.watthome.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Maps refresh failures for the main app. Cancellation and timeouts are not
 * user-facing errors — they must never become "Couldn't refresh: the job was cancelled".
 */
object RefreshErrors {
    fun isStructuredCancellation(error: Throwable): Boolean =
        error is CancellationException && error !is TimeoutCancellationException

    fun userFacingMessage(error: Throwable): String? {
        if (isStructuredCancellation(error)) return null
        if (error is TimeoutCancellationException) return null
        if (error is TokenRejectedException) return error.message ?: "token rejected, re-enter"
        return null
    }

    fun looksLikeCancellation(error: Throwable): Boolean {
        if (isStructuredCancellation(error) || error is TimeoutCancellationException) return true
        val detail = error.message.orEmpty()
        return detail.contains("the job was cancelled", ignoreCase = true) ||
            detail.contains("StandaloneCoroutine was cancelled", ignoreCase = true)
    }
}
