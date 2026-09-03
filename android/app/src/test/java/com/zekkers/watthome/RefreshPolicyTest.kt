package com.zekkers.watthome

import com.zekkers.watthome.data.RefreshPolicy
import com.zekkers.watthome.data.TokenRejectedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RefreshPolicyTest {
    @Test
    fun retriesTransientFailureThenSucceeds() = runTest {
        var attempts = 0
        val result = RefreshPolicy.runBounded {
            attempts += 1
            if (attempts < 2) throw IOException("flaky")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(2, attempts)
    }

    @Test
    fun doesNotRetryStructuredCancellation() = runTest {
        var attempts = 0
        val result = runCatching {
            RefreshPolicy.runBounded<String> {
                attempts += 1
                throw CancellationException("the job was cancelled")
            }
        }
        assertEquals(1, attempts)
        val error = result.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals("the job was cancelled", error?.message)
    }

    @Test
    fun doesNotRetryRejectedToken() = runTest {
        var attempts = 0
        try {
            RefreshPolicy.runBounded<String> {
                attempts += 1
                throw TokenRejectedException()
            }
            fail("token rejection should propagate")
        } catch (error: TokenRejectedException) {
            assertEquals("token rejected, re-enter", error.message)
        }
        assertEquals(1, attempts)
    }

    @Test
    fun timesOutAnAttemptThenRetries() = runTest {
        var attempts = 0
        try {
            RefreshPolicy.runBounded<String> {
                attempts += 1
                delay(RefreshPolicy.ATTEMPT_TIMEOUT_MS + 1_000)
                "late"
            }
            fail("bounded refresh should time out")
        } catch (_: Exception) {
        }
        assertEquals(RefreshPolicy.MAX_ATTEMPTS, attempts)
        assertTrue(attempts <= RefreshPolicy.MAX_ATTEMPTS)
    }
}
