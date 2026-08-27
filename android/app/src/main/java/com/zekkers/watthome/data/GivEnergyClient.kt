package com.zekkers.watthome.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class GivEnergyClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    fun testToken(token: String) {
        get(GivEnergy.latestUrl(), token)
    }

    fun fetchLive(token: String, includeSeries: Boolean = true): LiveInverterSnapshot {
        val latest = GivEnergyParser.parseLatest(get(GivEnergy.latestUrl(), token))
        if (!includeSeries) return latest
        val series = runCatching { fetchTodaySeries(token) }.getOrElse {
            if (it is TokenRejectedException) throw it
            LiveInverterSnapshot()
        }
        return latest.copy(
            socSeries = series.socSeries,
            batteryWSeries = series.batteryWSeries
        )
    }

    private fun fetchTodaySeries(token: String): LiveInverterSnapshot {
        val date = LocalDate.now(StatusFormatter.london).toString()
        val points = mutableListOf<kotlinx.serialization.json.JsonObject>()
        var page = 1
        var lastPage = 1
        while (page <= lastPage && page <= 12) {
            val raw = get(GivEnergy.dataPointsUrl(date, page), token)
            points += GivEnergyParser.parseDataPoints(raw)
            lastPage = GivEnergyParser.lastPage(raw)
            page += 1
        }
        return GivEnergyParser.snapshotFromPoints(points)
    }

    private fun get(url: String, token: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", USER_AGENT)
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 401) throw TokenRejectedException()
            if (!response.isSuccessful) {
                throw IOException("Live battery HTTP ${response.code}")
            }
            return response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw IOException("Live battery was empty")
        }
    }

    companion object {
        private const val USER_AGENT =
            "WattHomeStatus/1.2.4 (family widget; +https://github.com/Zekkers/watt-home-status)"
    }
}
