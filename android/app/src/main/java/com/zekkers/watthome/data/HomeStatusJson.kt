package com.zekkers.watthome.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

object HomeStatusJson {
    fun encode(status: HomeStatus): String = buildJsonObject {
        status.updated?.let { put("updated", JsonPrimitive(it)) }
        status.socPercent?.let { put("soc_percent", JsonPrimitive(it)) }
        status.solarW?.let { put("solar_w", JsonPrimitive(it)) }
        status.target1600Percent?.let { put("target_1600_percent", JsonPrimitive(it)) }
        status.overnight?.let { overnight ->
            put(
                "overnight",
                buildJsonObject {
                    overnight.start?.let { put("start", JsonPrimitive(it)) }
                    overnight.end?.let { put("end", JsonPrimitive(it)) }
                    overnight.capPercent?.let { put("cap_percent", JsonPrimitive(it)) }
                }
            )
        }
        status.peakWindow?.let { put("peak_window", JsonPrimitive(it)) }
        status.nextPowerUp?.let { powerUp ->
            put(
                "next_power_up",
                buildJsonObject {
                    powerUp.from?.let { put("from", JsonPrimitive(it)) }
                    powerUp.to?.let { put("to", JsonPrimitive(it)) }
                    powerUp.date?.let { put("date", JsonPrimitive(it)) }
                    powerUp.optedIn?.let { put("opted_in", JsonPrimitive(it)) }
                    powerUp.label?.let { put("label", JsonPrimitive(it)) }
                }
            )
        }
        status.lastAction?.let { put("last_action", JsonPrimitive(it)) }
        status.weatherTomorrow?.let { weather ->
            put(
                "weather_tomorrow",
                buildJsonObject {
                    weather.code?.let { put("code", JsonPrimitive(it)) }
                    weather.label?.let { put("label", JsonPrimitive(it)) }
                }
            )
        }
        status.batteryW?.let { put("battery_w", JsonPrimitive(it)) }
        if (status.batteryWSeries.isNotEmpty()) {
            put("battery_w_series", seriesArray(status.batteryWSeries, includeW = true))
        }
        if (status.socSeries.isNotEmpty()) {
            put("soc_series", seriesArray(status.socSeries, includeSoc = true))
        }
        status.lastSavings?.let { savings ->
            put(
                "last_savings",
                buildJsonObject {
                    savings.gbp?.let { put("gbp", JsonPrimitive(it)) }
                    savings.kwhExtra?.let { put("kwh_extra", JsonPrimitive(it)) }
                    savings.percentExtra?.let { put("percent_extra", JsonPrimitive(it)) }
                    savings.kind?.let { put("kind", JsonPrimitive(it)) }
                    savings.windowLabel?.let { put("window_label", JsonPrimitive(it)) }
                    savings.source?.let { put("source", JsonPrimitive(it)) }
                    savings.at?.let { put("at", JsonPrimitive(it)) }
                }
            )
        }
    }.toString()

    private fun seriesArray(
        samples: List<BatterySample>,
        includeW: Boolean = false,
        includeSoc: Boolean = false
    ): JsonArray = buildJsonArray {
        samples.forEach { sample ->
            add(
                buildJsonObject {
                    sample.t?.let { put("t", JsonPrimitive(it)) }
                    if (includeW) sample.w?.let { put("w", JsonPrimitive(it)) }
                    if (includeSoc) sample.soc?.let { put("soc", JsonPrimitive(it)) }
                }
            )
        }
    }
}
