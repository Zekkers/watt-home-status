package com.zekkers.watthome.data

data class HomeStatus(
    val updated: String? = null,
    val socPercent: Int? = null,
    val solarW: Int? = null,
    val houseW: Int? = null,
    val target1600Percent: Int? = null,
    val overnight: Overnight? = null,
    val peakWindow: PeakWindow? = null,
    val nextPowerUp: PowerUp? = null,
    val lastAction: String? = null,
    val weatherTomorrow: WeatherTomorrow? = null,
    val batteryW: Double? = null,
    val gridW: Double? = null,
    val batteryWSeries: List<BatterySample> = emptyList(),
    val socSeries: List<BatterySample> = emptyList(),
    val solarWSeries: List<BatterySample> = emptyList(),
    val houseWSeries: List<BatterySample> = emptyList(),
    val gridWSeries: List<BatterySample> = emptyList(),
    val lastSavings: LastSavings? = null
)

/**
 * The overnight charge slot. The feed writes `from` / `to` / `percent_limit` / `kw`;
 * older feeds used `start` / `end` / `cap_percent`, which still parse into the same fields.
 */
data class Overnight(
    val start: String? = null,
    val end: String? = null,
    val capPercent: Int? = null,
    val kw: Double? = null,
    val cleared: Boolean? = null
)

/**
 * The afternoon peak top-up window. [date] is the London day it belongs to: the
 * feed's own `date` when present, otherwise the day the feed was written.
 */
data class PeakWindow(
    val from: String? = null,
    val to: String? = null,
    val targetPercent: Int? = null,
    val date: String? = null,
    val label: String? = null
)

data class PowerUp(
    val from: String? = null,
    val to: String? = null,
    val date: String? = null,
    val optedIn: Boolean? = null,
    val label: String? = null
)

data class WeatherTomorrow(
    val code: String? = null,
    val label: String? = null
)

data class BatterySample(
    val t: String? = null,
    val w: Double? = null,
    val soc: Double? = null,
    val solarW: Double? = null,
    val houseW: Double? = null,
    val gridW: Double? = null
)

data class LastSavings(
    val gbp: Double? = null,
    val kwhExtra: Double? = null,
    val percentExtra: Double? = null,
    val kind: String? = null,
    val windowLabel: String? = null,
    val source: String? = null,
    val at: String? = null
)

data class StatusUiState(
    val status: HomeStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasToken: Boolean = false,
    val liveOk: Boolean = false,
    val tokenMessage: String? = null,
    val showingLastKnown: Boolean = false
)
