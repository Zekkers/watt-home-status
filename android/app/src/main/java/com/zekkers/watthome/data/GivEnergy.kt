package com.zekkers.watthome.data

import java.time.LocalDate

class TokenRejectedException : Exception("token rejected, re-enter")

data class LiveInverterSnapshot(
    val updated: String? = null,
    val socPercent: Int? = null,
    val solarW: Int? = null,
    val houseW: Int? = null,
    val batteryW: Double? = null,
    val gridW: Double? = null,
    val socSeries: List<BatterySample> = emptyList(),
    val batteryWSeries: List<BatterySample> = emptyList(),
    val solarWSeries: List<BatterySample> = emptyList(),
    val houseWSeries: List<BatterySample> = emptyList(),
    val gridWSeries: List<BatterySample> = emptyList(),
    /** True only when today's data-points pages were fetched without error. */
    val seriesFetched: Boolean = false
)

object GivEnergy {
    const val BASE_URL = "https://api.givenergy.cloud/v1"
    const val INVERTER_SERIAL = "CH2414G328"
    const val IGNORED_SERIAL = "GW2412G481"

    fun latestUrl(serial: String = INVERTER_SERIAL): String =
        "$BASE_URL/inverter/${requireHouseInverter(serial)}/system-data/latest"

    fun dataPointsUrl(date: String, page: Int, serial: String = INVERTER_SERIAL): String =
        "$BASE_URL/inverter/${requireHouseInverter(serial)}/data-points/$date?page=$page&pageSize=100"

    fun requireHouseInverter(serial: String): String {
        val trimmed = serial.trim()
        require(trimmed.equals(INVERTER_SERIAL, ignoreCase = true)) {
            "Only the house All-In-One inverter is used"
        }
        require(!trimmed.equals(IGNORED_SERIAL, ignoreCase = true)) {
            "Gateway serial is ignored"
        }
        return INVERTER_SERIAL
    }
}

object LiveStatus {
    /**
     * @param seriesFrom when set, its curves are kept (even if empty) instead of
     * the public feed's series. Latest-only polls pass today's cached curve so a
     * quiet poll does not replace it. Null uses the live series, then the public one.
     */
    fun merge(
        publicStatus: HomeStatus,
        live: LiveInverterSnapshot?,
        seriesFrom: HomeStatus? = null
    ): HomeStatus {
        if (live == null) return publicStatus
        val socSeries = seriesFrom?.socSeries ?: live.socSeries.ifEmpty { publicStatus.socSeries }
        val batteryWSeries = seriesFrom?.batteryWSeries ?: live.batteryWSeries.ifEmpty { publicStatus.batteryWSeries }
        val solarWSeries = seriesFrom?.solarWSeries ?: live.solarWSeries.ifEmpty { publicStatus.solarWSeries }
        val houseWSeries = seriesFrom?.houseWSeries ?: live.houseWSeries.ifEmpty { publicStatus.houseWSeries }
        val gridWSeries = seriesFrom?.gridWSeries ?: live.gridWSeries.ifEmpty { publicStatus.gridWSeries }
        return appendLiveTip(
            publicStatus.copy(
                updated = live.updated ?: publicStatus.updated,
                socPercent = live.socPercent ?: publicStatus.socPercent,
                solarW = live.solarW ?: publicStatus.solarW,
                houseW = live.houseW ?: publicStatus.houseW,
                batteryW = live.batteryW ?: publicStatus.batteryW,
                gridW = live.gridW ?: publicStatus.gridW,
                socSeries = socSeries,
                batteryWSeries = batteryWSeries,
                solarWSeries = solarWSeries,
                houseWSeries = houseWSeries,
                gridWSeries = gridWSeries
            ),
            live
        )
    }

    fun appendLiveTip(status: HomeStatus, live: LiveInverterSnapshot): HomeStatus {
        val stamp = live.updated ?: return status
        val socTip = live.socPercent?.toDouble()?.let { BatterySample(t = stamp, soc = it) }
        val wattTip = live.batteryW?.let { BatterySample(t = stamp, w = it) }
        val solarTip = live.solarW?.toDouble()?.let { BatterySample(t = stamp, w = it) }
        val houseTip = live.houseW?.toDouble()?.let { BatterySample(t = stamp, w = it) }
        val gridTip = live.gridW?.let { BatterySample(t = stamp, w = it) }
        return status.copy(
            socSeries = appendSample(status.socSeries, socTip) { it.soc != null },
            batteryWSeries = appendSample(status.batteryWSeries, wattTip) { it.w != null },
            solarWSeries = appendSample(status.solarWSeries, solarTip) { it.w != null },
            houseWSeries = appendSample(status.houseWSeries, houseTip) { it.w != null },
            gridWSeries = appendSample(status.gridWSeries, gridTip) { it.w != null }
        )
    }

    /** Drops samples from other London days so yesterday's curve does not share today's x-axis. */
    fun keepingToday(
        status: HomeStatus,
        today: LocalDate = LocalDate.now(StatusFormatter.london)
    ): HomeStatus {
        return status.copy(
            socSeries = status.socSeries.filter { sampleOn(it.t, today) },
            batteryWSeries = status.batteryWSeries.filter { sampleOn(it.t, today) },
            solarWSeries = status.solarWSeries.filter { sampleOn(it.t, today) },
            houseWSeries = status.houseWSeries.filter { sampleOn(it.t, today) },
            gridWSeries = status.gridWSeries.filter { sampleOn(it.t, today) }
        )
    }

    private fun sampleOn(raw: String?, today: LocalDate): Boolean {
        if (raw.isNullOrBlank()) return false
        val stamp = StatusFormatter.parseTimestamp(raw) ?: return false
        return stamp.atZoneSameInstant(StatusFormatter.london).toLocalDate() == today
    }

    private fun appendSample(
        existing: List<BatterySample>,
        newest: BatterySample?,
        keep: (BatterySample) -> Boolean
    ): List<BatterySample> {
        if (newest?.t == null) return existing
        val withoutDup = existing.filterNot { it.t == newest.t }
        return (withoutDup + newest).filter(keep)
    }
}
