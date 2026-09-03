package com.zekkers.watthome.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Tiny last-known reading used for first paint. No series, no remote payload,
 * no token material — cheap enough to hydrate before the first frame.
 */
object FirstPaintSnapshot {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun encode(status: HomeStatus): String = buildJsonObject {
        status.updated?.let { put("updated", JsonPrimitive(it)) }
        status.socPercent?.let { put("soc_percent", JsonPrimitive(it)) }
        status.solarW?.let { put("solar_w", JsonPrimitive(it)) }
        status.houseW?.let { put("house_w", JsonPrimitive(it)) }
        status.batteryW?.let { put("battery_w", JsonPrimitive(it)) }
    }.toString()

    fun decode(raw: String): HomeStatus {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: return HomeStatus()
        return HomeStatus(
            updated = primitive(root, "updated")?.contentOrNull?.takeIf { it.isNotBlank() },
            socPercent = primitive(root, "soc_percent")?.intOrNull
                ?: primitive(root, "soc_percent")?.doubleOrNull?.toInt(),
            solarW = primitive(root, "solar_w")?.intOrNull
                ?: primitive(root, "solar_w")?.doubleOrNull?.toInt(),
            houseW = primitive(root, "house_w")?.intOrNull
                ?: primitive(root, "house_w")?.doubleOrNull?.toInt(),
            batteryW = primitive(root, "battery_w")?.doubleOrNull
                ?: primitive(root, "battery_w")?.intOrNull?.toDouble()
        )
    }

    fun fromFull(status: HomeStatus): HomeStatus = HomeStatus(
        updated = status.updated,
        socPercent = status.socPercent,
        solarW = status.solarW,
        houseW = status.houseW,
        batteryW = status.batteryW
    )

    private fun primitive(obj: JsonObject, key: String): JsonPrimitive? =
        obj[key] as? JsonPrimitive
}
