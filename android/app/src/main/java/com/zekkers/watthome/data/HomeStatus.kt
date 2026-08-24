package com.zekkers.watthome.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeStatus(
    val updated: String? = null,
    @SerialName("soc_percent") val socPercent: Int? = null,
    @SerialName("solar_w") val solarW: Int? = null,
    @SerialName("target_1600_percent") val target1600Percent: Int? = null,
    val overnight: Overnight? = null,
    @SerialName("peak_window") val peakWindow: String? = null,
    @SerialName("next_power_up") val nextPowerUp: String? = null,
    @SerialName("last_action") val lastAction: String? = null
)

@Serializable
data class Overnight(
    val start: String? = null,
    val end: String? = null,
    @SerialName("cap_percent") val capPercent: Int? = null
)

data class StatusUiState(
    val status: HomeStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
