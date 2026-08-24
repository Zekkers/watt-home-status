package com.zekkers.watthome.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.time.OffsetDateTime

object HomeStatusParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun parse(raw: String): HomeStatus {
        val root = json.parseToJsonElement(raw)
        val obj = root as? JsonObject ?: return HomeStatus()
        return HomeStatus(
            updated = obj.string("updated"),
            socPercent = obj.int("soc_percent"),
            solarW = obj.int("solar_w"),
            target1600Percent = obj.int("target_1600_percent", "target_1600"),
            overnight = parseOvernight(obj),
            peakWindow = obj.string("peak_window"),
            nextPowerUp = parsePowerUp(obj["next_power_up"]),
            lastAction = obj.string("last_action"),
            weatherTomorrow = parseWeather(obj["weather_tomorrow"]),
            batteryW = obj.double("battery_w"),
            batteryWSeries = parseSeries(obj["battery_w_series"]),
            savings = parseSavings(obj["savings"])
        )
    }

    private fun parseOvernight(obj: JsonObject): Overnight? {
        val nested = obj["overnight"] as? JsonObject
        if (nested != null) {
            return Overnight(
                start = nested.string("start"),
                end = nested.string("end"),
                capPercent = nested.int("cap_percent"),
                label = nested.string("label")
            )
        }
        return when (val slot = obj["overnight_slot"]) {
            null, is JsonNull -> null
            is JsonPrimitive -> slot.contentOrNull?.takeIf { it.isNotBlank() }?.let {
                Overnight(label = it)
            }
            is JsonObject -> Overnight(
                start = slot.string("start"),
                end = slot.string("end"),
                capPercent = slot.int("cap_percent"),
                label = slot.string("label")
            )
            else -> null
        }
    }

    private fun parsePowerUp(element: JsonElement?): PowerUp? {
        return when (element) {
            null, is JsonNull -> null
            is JsonPrimitive -> {
                val text = element.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
                PowerUp(label = text)
            }
            is JsonObject -> {
                val powerUp = PowerUp(
                    from = element.string("from"),
                    to = element.string("to"),
                    date = element.string("date"),
                    optedIn = element.boolean("opted_in"),
                    label = element.string("label")
                )
                if (powerUp.from == null &&
                    powerUp.to == null &&
                    powerUp.date == null &&
                    powerUp.optedIn == null &&
                    powerUp.label == null
                ) {
                    null
                } else {
                    powerUp
                }
            }
            else -> null
        }
    }

    private fun parseWeather(element: JsonElement?): WeatherTomorrow? {
        val obj = element as? JsonObject ?: return null
        val weather = WeatherTomorrow(
            code = obj.string("code"),
            label = obj.string("label")
        )
        return if (weather.code == null && weather.label == null) null else weather
    }

    private fun parseSeries(element: JsonElement?): List<BatterySample> {
        val array = element as? JsonArray ?: return emptyList()
        val samples = array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val watts = obj.double("w") ?: return@mapNotNull null
            BatterySample(t = obj.string("t"), w = watts)
        }
        return samples.sortedBy { sample ->
            sample.t?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
                ?: OffsetDateTime.MIN
        }
    }

    private fun parseSavings(element: JsonElement?): Savings? {
        val obj = element as? JsonObject ?: return null
        val savings = Savings(
            lastGbp = obj.double("last_gbp"),
            label = obj.string("label")
        )
        return if (savings.lastGbp == null && savings.label == null) null else savings
    }
}

private fun JsonObject.string(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
}

private fun JsonObject.int(vararg keys: String): Int? {
    for (key in keys) {
        val primitive = this[key] as? JsonPrimitive ?: continue
        val value = primitive.intOrNull
            ?: primitive.doubleOrNull?.toInt()
            ?: primitive.contentOrNull?.toDoubleOrNull()?.toInt()
        if (value != null) return value
    }
    return null
}

private fun JsonObject.double(key: String): Double? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.doubleOrNull
        ?: primitive.intOrNull?.toDouble()
        ?: primitive.contentOrNull?.toDoubleOrNull()
}

private fun JsonObject.boolean(key: String): Boolean? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.booleanOrNull
        ?: when (primitive.contentOrNull?.lowercase()) {
            "true", "yes", "1" -> true
            "false", "no", "0" -> false
            else -> null
        }
}
