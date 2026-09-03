package com.zekkers.watthome

import com.zekkers.watthome.data.RefreshErrors
import com.zekkers.watthome.data.TokenRejectedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RefreshErrorsTest {
    @Test
    fun cancellationIsNotAUserFacingError() {
        val cancelled = CancellationException("the job was cancelled")
        assertTrue(RefreshErrors.isStructuredCancellation(cancelled))
        assertTrue(RefreshErrors.looksLikeCancellation(cancelled))
        assertNull(RefreshErrors.userFacingMessage(cancelled))
        assertFalse(
            RefreshErrors.userFacingMessage(cancelled)
                .orEmpty()
                .contains("Couldn't refresh", ignoreCase = true)
        )
    }

    @Test
    fun timeoutIsQuietAndNeverDumpsTheJobWasCancelled() {
        val timeout = runBlocking {
            try {
                withTimeout(1) { delay(50) }
                error("timeout should have fired")
            } catch (error: TimeoutCancellationException) {
                error
            }
        }
        assertFalse(RefreshErrors.isStructuredCancellation(timeout))
        assertTrue(RefreshErrors.looksLikeCancellation(timeout))
        assertNull(RefreshErrors.userFacingMessage(timeout))
        assertFalse(timeout.message.orEmpty().let { RefreshErrors.userFacingMessage(timeout).orEmpty() + it }
            .contains("Couldn't refresh: the job was cancelled"))
    }

    @Test
    fun ioFailureIsQuietLastKnownNotARawDump() {
        val error = IOException("Status feed HTTP 503")
        assertNull(RefreshErrors.userFacingMessage(error))
        assertFalse(RefreshErrors.looksLikeCancellation(error))
    }

    @Test
    fun tokenRejectedStaysVisible() {
        assertEquals(
            "token rejected, re-enter",
            RefreshErrors.userFacingMessage(TokenRejectedException())
        )
    }

    @Test
    fun wrappedCancellationTextIsNotShown() {
        val wrapped = RuntimeException("Couldn't refresh: the job was cancelled")
        assertTrue(RefreshErrors.looksLikeCancellation(wrapped))
        assertNull(RefreshErrors.userFacingMessage(wrapped))
    }
}
