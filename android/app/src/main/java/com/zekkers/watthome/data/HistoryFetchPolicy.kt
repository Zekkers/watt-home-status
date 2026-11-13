package com.zekkers.watthome.data

/**
 * GivEnergy Cloud fair-use budget.
 *
 * Background work and home-screen widgets stay on a 15-minute live snapshot
 * (`/system-data/latest` only). Today's `/data-points` history is separate:
 * about every 2 hours from that background work, and when the app is opened or
 * the in-app refresh control is used. Those foreground history pulls are at
 * least 5 minutes apart. Skipping history still allows a live snapshot, unless
 * the short success coalesce applies.
 *
 * There is no sub-15-minute automatic poll.
 */
object HistoryFetchPolicy {
    const val KEY_TRIGGER = "refresh_trigger"

    /** WorkManager cannot schedule periodic work any faster than this. */
    const val BACKGROUND_PERIOD_MINUTES = 15L

    /** Automatic history from background and widget work. */
    const val BACKGROUND_HISTORY_MS = 2 * 60 * 60 * 1000L

    /** Minimum gap between app-open and explicit-refresh history fetches. */
    const val FOREGROUND_HISTORY_MIN_MS = 5 * 60 * 1000L

    enum class Trigger {
        /** Periodic work, process start, widget update. Not an app open. */
        BACKGROUND,

        /** MainActivity first paint or resume. */
        APP_OPEN,

        /** The in-app refresh control. */
        PULL_TO_REFRESH;

        fun wire(): String = when (this) {
            BACKGROUND -> "background"
            APP_OPEN -> "app_open"
            PULL_TO_REFRESH -> "pull_to_refresh"
        }

        companion object {
            fun fromWire(raw: String?): Trigger = when (raw) {
                "app_open" -> APP_OPEN
                "pull_to_refresh" -> PULL_TO_REFRESH
                else -> BACKGROUND
            }
        }
    }

    enum class Poll {
        /** A successful fetch landed inside the short coalesce window. No GivEnergy call. */
        COALESCED,

        /** One `/system-data/latest`. Append that tip to the cached series. */
        LATEST_ONLY,

        /** `/system-data/latest` plus today's `/data-points` pages. */
        LATEST_AND_HISTORY
    }

    fun shouldFetchHistory(
        trigger: Trigger,
        lastHistorySuccessAtMs: Long?,
        nowMs: Long,
        lastHistoryAttemptAtMs: Long? = null
    ): Boolean {
        val successAge = ageMillis(lastHistorySuccessAtMs, nowMs)
        val attemptAge = ageMillis(lastHistoryAttemptAtMs ?: lastHistorySuccessAtMs, nowMs)
        return when (trigger) {
            Trigger.BACKGROUND -> attemptAge == null || attemptAge >= BACKGROUND_HISTORY_MS
            Trigger.APP_OPEN, Trigger.PULL_TO_REFRESH ->
                successAge == null || successAge >= FOREGROUND_HISTORY_MIN_MS
        }
    }

    /**
     * What this trigger should do to GivEnergy right now.
     *
     * [lastAnySuccessAtMs] is the last successful refresh of any kind (latest or
     * history) and only drives the few-second coalesce. History timing uses the
     * history clocks, so a widget snapshot a second ago does not suppress an
     * app-open history fetch.
     */
    fun plan(
        trigger: Trigger,
        lastHistorySuccessAtMs: Long?,
        nowMs: Long,
        lastAnySuccessAtMs: Long? = null,
        lastHistoryAttemptAtMs: Long? = null,
        coalesceWindowMs: Long = RefreshPolicy.COALESCE_SUCCESS_MS
    ): Poll {
        val fetchHistory = shouldFetchHistory(
            trigger = trigger,
            lastHistorySuccessAtMs = lastHistorySuccessAtMs,
            nowMs = nowMs,
            lastHistoryAttemptAtMs = lastHistoryAttemptAtMs
        )
        if (fetchHistory) return Poll.LATEST_AND_HISTORY
        val sinceAny = ageMillis(lastAnySuccessAtMs, nowMs)
        if (sinceAny != null && sinceAny in 1 until coalesceWindowMs) return Poll.COALESCED
        return Poll.LATEST_ONLY
    }

    private fun ageMillis(lastMs: Long?, nowMs: Long): Long? {
        if (lastMs == null || lastMs <= 0L) return null
        return nowMs - lastMs
    }
}
