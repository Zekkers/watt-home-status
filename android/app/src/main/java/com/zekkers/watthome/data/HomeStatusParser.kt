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

    private val clockWindow = Regex("""(\d{1,2}:\d{2})\s*[–-]\s*(\d{1,2}:\d{2})""")

    fun parse(raw: String): HomeStatus {
        val root = json.parseToJsonElement(raw)
        val obj = root as? JsonObject ?: return HomeStatus()
        return HomeStatus(
            updated = obj.string("updated"),
            socPercent = obj.int("soc_percent"),
            solarW = obj.int("solar_w"),
            target1600Percent = obj.int("target_1600_percent"),
            overnight = parseOvernight(obj["overnight"]),
            peakWindow = obj.string("peak_window"),
            nextPowerUp = parsePowerUp(obj["next_power_up"]),
            lastAction = obj.string("last_action"),
            weatherTomorrow = parseWeather(obj["weather_tomorrow"]),
            batteryW = obj.double("battery_w"),
            batteryWSeries = parseSeries(obj["battery_w_series"]),
            lastSavings = parseLastSavings(obj["last_savings"])
        )
    }

    private fun parseOvernight(element: JsonElement?): Overnight? {
        val nested = element as? JsonObject ?: return null
        return Overnight(
            start = nested.string("start"),
            end = nested.string("end"),
            capPercent = nested.int("cap_percent")
        )
    }

    private fun parsePowerUp(element: JsonElement?): PowerUp? {
        return when (element) {
            null, is JsonNull -> null
            is JsonPrimitive -> {
                val text = element.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
                    ?: return null
                val clocks = clockWindow.find(text)
                PowerUp(
                    from = clocks?.groupValues?.get(1),
                    to = clocks?.groupValues?.get(2),
                    label = text
                )
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

    private fun parseLastSavings(element: JsonElement?): LastSavings? {
        val obj = element as? JsonObject ?: return null
        val savings = LastSavings(
            gbp = obj.double("gbp"),
            kwhExtra = obj.double("kwh_extra"),
            percentExtra = obj.double("percent_extra"),
            kind = obj.string("kind"),
            windowLabel = obj.string("window_label"),
            source = obj.string("source"),
            at = obj.string("at")
        )
        return if (
            savings.gbp == null &&
            savings.kwhExtra == null &&
            savings.percentExtra == null &&
            savings.kind == null &&
            savings.windowLabel == null &&
            savings.source == null &&
            savings.at == null
        ) {
            null
        } else {
            savings
        }
    }
}

private fun JsonObject.string(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
}

private fun JsonObject.int(key: String): Int? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.intOrNull
        ?: primitive.doubleOrNull?.toInt()
        ?: primitive.contentOrNull?.toDoubleOrNull()?.toInt()
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
