package com.zekkers.watthome.data

import java.time.OffsetDateTime
import kotlin.math.roundToInt

/**
 * Mean array-1 PV watts over the previous widget poll interval.
 *
 * The poll is adaptive (~90s while the pack is moving, 15 min when idle). This
 * is that last window, not a lifetime or fake 24h average. Samples come from
 * GivEnergy data-points when a full poll already fetched them, plus a small
 * on-disk buffer of live watts from each existing poll. First poll after
 * install uses data-points in the idle window when present, otherwise the
 * current watts (including 0 W at night) until two samples exist.
 */
object SolarInterval {
    const val RETAIN_MINUTES = 20L
    const val MAX_LOOKBACK_MINUTES = 20L

    fun finish(
        status: HomeStatus,
        previous: HomeStatus?,
        now: OffsetDateTime = OffsetDateTime.now(StatusFormatter.london)
    ): HomeStatus {
        val pollStamp = status.updated?.let { StatusFormatter.parseTimestamp(it) } ?: now
        val lastPoll = previous?.lastWidgetPollAt?.let { StatusFormatter.parseTimestamp(it) }
        val incoming = buildList {
            addAll(previous?.solarWSeries.orEmpty())
            addAll(status.solarWSeries)
            status.solarW?.let { watts ->
                add(BatterySample(t = pollStamp.toString(), w = watts.toDouble()))
            }
        }
        val samples = retainRecent(incoming, pollStamp)
        val average = averageInWindow(samples, lastPoll, pollStamp) ?: status.solarW
        return status.copy(
            solarWSeries = samples,
            solarIntervalAvgW = average,
            lastWidgetPollAt = pollStamp.toString()
        )
    }

    fun retainRecent(
        samples: List<BatterySample>,
        now: OffsetDateTime
    ): List<BatterySample> {
        val cutoff = now.minusMinutes(RETAIN_MINUTES)
        val byTime = linkedMapOf<String, BatterySample>()
        samples.forEach { sample ->
            val stamp = sample.t?.let { StatusFormatter.parseTimestamp(it) } ?: return@forEach
            if (sample.w == null || stamp.isBefore(cutoff)) return@forEach
            byTime[sample.t] = sample
        }
        return byTime.values.sortedBy { StatusFormatter.parseTimestamp(it.t ?: "") ?: OffsetDateTime.MIN }
    }

    fun averageInWindow(
        samples: List<BatterySample>,
        lastPollAt: OffsetDateTime?,
        now: OffsetDateTime
    ): Int? {
        val start = windowStart(lastPollAt, now)
        val watts = samples.mapNotNull { sample ->
            val stamp = sample.t?.let { StatusFormatter.parseTimestamp(it) } ?: return@mapNotNull null
            val value = sample.w ?: return@mapNotNull null
            if (!stamp.isBefore(start) && !stamp.isAfter(now)) value else null
        }
        if (watts.isEmpty()) return null
        return watts.average().roundToInt()
    }

    fun windowStart(lastPollAt: OffsetDateTime?, now: OffsetDateTime): OffsetDateTime {
        val idleStart = now.minusMinutes(RefreshCadence.IDLE_PERIOD_MINUTES)
        val maxStart = now.minusMinutes(MAX_LOOKBACK_MINUTES)
        if (lastPollAt == null || lastPollAt.isAfter(now)) return idleStart
        return if (lastPollAt.isBefore(maxStart)) maxStart else lastPollAt
    }
}
