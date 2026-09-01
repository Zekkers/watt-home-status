package com.zekkers.watthome.data

data class HomeStatus(
    val updated: String? = null,
    val socPercent: Int? = null,
    val solarW: Int? = null,
    val solarIntervalAvgW: Int? = null,
    val solarWSeries: List<BatterySample> = emptyList(),
    val lastWidgetPollAt: String? = null,
    val target1600Percent: Int? = null,
    val overnight: Overnight? = null,
    val peakWindow: String? = null,
    val nextPowerUp: PowerUp? = null,
    val lastAction: String? = null,
    val weatherTomorrow: WeatherTomorrow? = null,
    val batteryW: Double? = null,
    val batteryWSeries: List<BatterySample> = emptyList(),
    val socSeries: List<BatterySample> = emptyList(),
    val lastSavings: LastSavings? = null
)

data class Overnight(
    val start: String? = null,
    val end: String? = null,
    val capPercent: Int? = null
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
    val soc: Double? = null
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
    val tokenMessage: String? = null
)
