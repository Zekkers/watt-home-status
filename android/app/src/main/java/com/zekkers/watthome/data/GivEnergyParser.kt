package com.zekkers.watthome.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

object GivEnergyParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun appBatteryW(givEnergyBatteryPower: Double): Double = -givEnergyBatteryPower

    fun parseLatest(raw: String): LiveInverterSnapshot {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: return LiveInverterSnapshot()
        val data = obj(root["data"]) ?: root
        if (isIgnoredSerial(root) || isIgnoredSerial(data)) return LiveInverterSnapshot()
        val battery = obj(data["battery"])
        val solar = obj(data["solar"])
        return LiveInverterSnapshot(
            updated = string(data, "time"),
            socPercent = int(battery, "percent"),
            solarW = solarArray1(solar),
            batteryW = double(battery, "power")?.let(::appBatteryW)
        )
    }

    fun parseDataPoints(raw: String): List<JsonObject> {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: return emptyList()
        val data = root["data"] as? JsonArray ?: return emptyList()
        return data.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            if (isIgnoredSerial(obj)) null else obj
        }
    }

    fun lastPage(raw: String): Int {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: return 1
        val meta = obj(root["meta"]) ?: return 1
        return int(meta, "last_page")?.coerceAtLeast(1) ?: 1
    }

    fun snapshotFromPoints(points: List<JsonObject>): LiveInverterSnapshot {
        val samples = points.mapNotNull(::sampleFromPoint)
            .sortedBy { timestamp(it.t) ?: OffsetDateTime.MIN }
        val downsampled = downsample(samples, minutes = 15)
        val solar = points.mapNotNull(::solarSampleFromPoint)
            .sortedBy { timestamp(it.t) ?: OffsetDateTime.MIN }
        return LiveInverterSnapshot(
            socSeries = downsampled.map { BatterySample(t = it.t, soc = it.soc) },
            batteryWSeries = downsampled.map { BatterySample(t = it.t, w = it.w) },
            solarWSeries = solar
        )
    }

    fun downsample(samples: List<BatterySample>, minutes: Int = 15): List<BatterySample> {
        if (samples.size <= 2) return samples
        val buckets = linkedMapOf<Long, BatterySample>()
        samples.forEach { sample ->
            val stamp = timestamp(sample.t) ?: return@forEach
            val london = stamp.atZoneSameInstant(StatusFormatter.london)
            val bucket = london
                .withMinute((london.minute / minutes) * minutes)
                .withSecond(0)
                .withNano(0)
                .toInstant()
                .truncatedTo(ChronoUnit.MINUTES)
                .toEpochMilli()
            buckets[bucket] = sample
        }
        return buckets.values.sortedBy { timestamp(it.t) ?: OffsetDateTime.MIN }
    }

    private fun sampleFromPoint(point: JsonObject): BatterySample? {
        if (isIgnoredSerial(point)) return null
        val power = obj(point["power"]) ?: point
        val battery = obj(power["battery"]) ?: return null
        val t = string(point, "time") ?: return null
        val soc = double(battery, "percent") ?: int(battery, "percent")?.toDouble()
        val w = double(battery, "power")?.let(::appBatteryW)
        if (soc == null && w == null) return null
        return BatterySample(t = t, w = w, soc = soc)
    }

    private fun solarSampleFromPoint(point: JsonObject): BatterySample? {
        if (isIgnoredSerial(point)) return null
        val t = string(point, "time") ?: return null
        val power = obj(point["power"]) ?: point
        val solar = obj(power["solar"]) ?: obj(point["solar"])
        val watts = solarArray1(solar) ?: return null
        return BatterySample(t = t, w = watts.toDouble())
    }

    private fun solarArray1(solar: JsonObject?): Int? {
        val arrays = solar?.get("arrays") as? JsonArray
        if (arrays != null) {
            val match = arrays.mapNotNull { it as? JsonObject }.firstOrNull { int(it, "array") == 1 }
            return int(match, "power")
        }
        return int(solar, "power")
    }

    private fun isIgnoredSerial(obj: JsonObject): Boolean {
        val serial = string(obj, "serial") ?: string(obj, "serial_number") ?: return false
        return serial.equals(GivEnergy.IGNORED_SERIAL, ignoreCase = true)
    }

    private fun obj(element: JsonElement?): JsonObject? = element as? JsonObject

    private fun string(obj: JsonObject?, key: String): String? {
        val primitive = obj?.get(key) as? JsonPrimitive ?: return null
        return primitive.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun int(obj: JsonObject?, key: String): Int? {
        val primitive = obj?.get(key) as? JsonPrimitive ?: return null
        return primitive.intOrNull
            ?: primitive.doubleOrNull?.toInt()
            ?: primitive.contentOrNull?.toDoubleOrNull()?.toInt()
    }

    private fun double(obj: JsonObject?, key: String): Double? {
        val primitive = obj?.get(key) as? JsonPrimitive ?: return null
        return primitive.doubleOrNull
            ?: primitive.intOrNull?.toDouble()
            ?: primitive.contentOrNull?.toDoubleOrNull()
    }

    private fun timestamp(raw: String?): OffsetDateTime? {
        if (raw.isNullOrBlank()) return null
        runCatching { return OffsetDateTime.parse(raw) }
        runCatching { return java.time.Instant.parse(raw).atOffset(ZoneOffset.UTC) }
        return null
    }
}
