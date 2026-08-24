package com.zekkers.watthome

import com.zekkers.watthome.data.GivEnergy
import com.zekkers.watthome.data.GivEnergyParser
import com.zekkers.watthome.data.HomeStatusJson
import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.LiveStatus
import com.zekkers.watthome.data.TokenRejectedException
import com.zekkers.watthome.data.TokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GivEnergyParserTest {
    private val latest = """
        {
          "data": {
            "time": "2026-08-24T15:34:00Z",
            "solar": {
              "power": 1000,
              "arrays": [
                {"array": 1, "voltage": 250, "current": 2, "power": 420},
                {"array": 2, "voltage": 250, "current": 2, "power": 580}
              ]
            },
            "battery": {"percent": 63, "power": 1250, "temperature": 32.1}
          }
        }
    """.trimIndent()

    private val points = """
        {
          "data": [
            {
              "time": "2026-08-24T12:00:00+01:00",
              "power": {"battery": {"percent": 50, "power": -800}}
            },
            {
              "time": "2026-08-24T12:05:00+01:00",
              "power": {"battery": {"percent": 51, "power": -700}}
            },
            {
              "time": "2026-08-24T12:16:00+01:00",
              "power": {"battery": {"percent": 55, "power": 400}}
            }
          ],
          "meta": {"current_page": 1, "last_page": 1}
        }
    """.trimIndent()

    @Test
    fun usesHouseInverterOnly() {
        assertEquals("CH2414G328", GivEnergy.INVERTER_SERIAL)
        assertTrue(GivEnergy.latestUrl().contains("CH2414G328"))
        assertTrue(GivEnergy.dataPointsUrl("2026-08-24", 1).contains("CH2414G328"))
        assertFalse(GivEnergy.latestUrl().contains("GW2412G481"))
        assertFalse(GivEnergy.dataPointsUrl("2026-08-24", 1).contains("GW2412G481"))
        try {
            GivEnergy.requireHouseInverter("GW2412G481")
            throw AssertionError("gateway serial must be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun ignoresGatewaySerial() {
        val ignored = GivEnergyParser.parseLatest(
            """{"serial_number":"GW2412G481","data":{"battery":{"percent":99,"power":1}}}"""
        )
        assertNull(ignored.socPercent)
        val kept = GivEnergyParser.parseDataPoints(
            """{"data":[
              {"serial_number":"GW2412G481","time":"2026-08-24T12:00:00Z","power":{"battery":{"percent":9,"power":1}}},
              {"time":"2026-08-24T12:15:00Z","power":{"battery":{"percent":63,"power":-200}}}
            ]}"""
        )
        assertEquals(1, kept.size)
    }

    @Test
    fun latestUsesArrayOneSolarAndNegatesBatteryPower() {
        val live = GivEnergyParser.parseLatest(latest)
        assertEquals(63, live.socPercent)
        assertEquals(420, live.solarW)
        assertEquals(-1250.0, live.batteryW)
        assertEquals(2289.0, GivEnergyParser.appBatteryW(-2289.0))
        assertEquals(-1250.0, GivEnergyParser.appBatteryW(1250.0))
        assertEquals("2026-08-24T15:34:00Z", live.updated)
    }

    @Test
    fun dataPointsDownsampleToFifteenMinutesAndNegateWatts() {
        val snapshot = GivEnergyParser.snapshotFromPoints(GivEnergyParser.parseDataPoints(points))
        assertEquals(2, snapshot.socSeries.size)
        assertEquals(51.0, snapshot.socSeries[0].soc)
        assertEquals(700.0, snapshot.batteryWSeries[0].w)
        assertEquals(55.0, snapshot.socSeries[1].soc)
        assertEquals(-400.0, snapshot.batteryWSeries[1].w)
        assertEquals(1, GivEnergyParser.lastPage(points))
    }

    @Test
    fun mergeKeepsPublicExtrasAndLetsLiveNumbersWin() {
        val publicStatus = HomeStatusParser.parse(
            """
            {
              "soc_percent": 10,
              "solar_w": 1,
              "battery_w": 5,
              "target_1600_percent": 55,
              "next_power_up": {"from":"12:00","to":"14:00","opted_in":true},
              "last_savings": {"gbp": 36.95, "window_label": "21 Jul–1 Aug 2026 (9 sessions)"},
              "weather_tomorrow": {"code":"partly_cloudy","label":"Partly cloudy"},
              "soc_series": [{"t":"2026-08-24T00:00:00+01:00","soc":10},{"t":"2026-08-24T01:00:00+01:00","soc":12}],
              "battery_w_series": [{"t":"2026-08-24T00:00:00+01:00","w":1},{"t":"2026-08-24T01:00:00+01:00","w":2}]
            }
            """.trimIndent()
        )
        val live = GivEnergyParser.parseLatest(latest).copy(
            socSeries = emptyList(),
            batteryWSeries = emptyList()
        )
        val merged = LiveStatus.merge(publicStatus, live)
        assertEquals(63, merged.socPercent)
        assertEquals(420, merged.solarW)
        assertEquals(-1250.0, merged.batteryW)
        assertTrue(merged.socSeries.isEmpty())
        assertTrue(merged.batteryWSeries.isEmpty())
        assertEquals("12:00", merged.nextPowerUp?.from)
        assertEquals(55, merged.target1600Percent)
        assertEquals(36.95, merged.lastSavings?.gbp)
        assertEquals("Partly cloudy", merged.weatherTomorrow?.label)
        val encoded = HomeStatusJson.encode(merged)
        assertFalse(encoded.contains("Bearer"))
        assertFalse(encoded.contains("token"))
        assertFalse(encoded.contains("GW2412G481"))
        val roundTrip = HomeStatusParser.parse(encoded)
        assertEquals(63, roundTrip.socPercent)
        assertEquals("12:00", roundTrip.nextPowerUp?.from)
    }

    @Test
    fun rejectedTokenMessageDoesNotIncludeSecret() {
        assertEquals("token rejected, re-enter", TokenRejectedException().message)
        assertEquals("abc", TokenStore.normalize("  Bearer abc  "))
        assertNull(TokenStore.normalize("   "))
    }
}
