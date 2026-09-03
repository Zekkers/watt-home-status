package com.zekkers.watthome.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Bounded refresh: short per-attempt timeout plus one retry. Structured
 * cancellation is rethrown immediately so a disposed job is not treated as a
 * failed fetch.
 */
object RefreshPolicy {
    const val MAX_ATTEMPTS = 2
    const val ATTEMPT_TIMEOUT_MS = 12_000L
    const val RETRY_DELAY_MS = 400L
    const val CONNECT_TIMEOUT_SECONDS = 8L
    const val READ_TIMEOUT_SECONDS = 10L
    const val CALL_TIMEOUT_SECONDS = 12L
    const val COALESCE_SUCCESS_MS = 3_000L

    fun connectTimeoutMs(): Long = TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT_SECONDS)

    suspend fun <T> runBounded(block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { index ->
            try {
                return withTimeout(ATTEMPT_TIMEOUT_MS) { block() }
            } catch (error: TimeoutCancellationException) {
                lastError = error
            } catch (error: CancellationException) {
                throw error
            } catch (error: TokenRejectedException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
            if (index < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        throw lastError ?: IllegalStateException("refresh failed")
    }
}
