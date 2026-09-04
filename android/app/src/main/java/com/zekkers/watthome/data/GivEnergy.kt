package com.zekkers.watthome.data

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
    val gridWSeries: List<BatterySample> = emptyList()
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
    fun merge(publicStatus: HomeStatus, live: LiveInverterSnapshot?): HomeStatus {
        if (live == null) return publicStatus
        val socSeries = live.socSeries.ifEmpty { publicStatus.socSeries }
        val batteryWSeries = live.batteryWSeries.ifEmpty { publicStatus.batteryWSeries }
        val solarWSeries = live.solarWSeries.ifEmpty { publicStatus.solarWSeries }
        val houseWSeries = live.houseWSeries.ifEmpty { publicStatus.houseWSeries }
        val gridWSeries = live.gridWSeries.ifEmpty { publicStatus.gridWSeries }
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
