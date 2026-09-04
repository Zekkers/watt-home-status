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
            "consumption": {"power": 406},
            "grid": {"power": -210},
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
        assertEquals(406, live.houseW)
        assertEquals(-1250.0, live.batteryW!!, 0.01)
        assertEquals(-210.0, live.gridW!!, 0.01)
        assertEquals(2289.0, GivEnergyParser.appBatteryW(-2289.0), 0.01)
        assertEquals(-1250.0, GivEnergyParser.appBatteryW(1250.0), 0.01)
        assertEquals("2026-08-24T15:34:00Z", live.updated)
    }

    @Test
    fun dataPointsDownsampleToFifteenMinutesAndNegateWatts() {
        val snapshot = GivEnergyParser.snapshotFromPoints(GivEnergyParser.parseDataPoints(points))
        assertEquals(2, snapshot.socSeries.size)
        assertEquals(51.0, snapshot.socSeries[0].soc!!, 0.01)
        assertEquals(700.0, snapshot.batteryWSeries[0].w!!, 0.01)
        assertEquals(55.0, snapshot.socSeries[1].soc!!, 0.01)
        assertEquals(-400.0, snapshot.batteryWSeries[1].w!!, 0.01)
        assertTrue(snapshot.solarWSeries.isEmpty())
        assertTrue(snapshot.houseWSeries.isEmpty())
        assertTrue(snapshot.gridWSeries.isEmpty())
        assertEquals(1, GivEnergyParser.lastPage(points))
    }

    @Test
    fun dataPointsKeepArrayOneSolarHouseGridAndIgnoreGateway() {
        val raw = """
            {
              "data": [
                {
                  "time": "2026-08-26T12:00:00+01:00",
                  "serial": "CH2414G328",
                  "power": {
                    "solar": {
                      "power": 1000,
                      "arrays": [
                        {"array": 1, "power": 420},
                        {"array": 2, "power": 580}
                      ]
                    },
                    "battery": {"percent": 50, "power": -800},
                    "consumption": {"power": 406},
                    "grid": {"power": -14}
                  }
                },
                {
                  "time": "2026-08-26T12:05:00+01:00",
                  "power": {
                    "solar": {
                      "arrays": [
                        {"array": 1, "power": 390},
                        {"array": 2, "power": 9999}
                      ]
                    },
                    "battery": {"percent": 51, "power": -700},
                    "consumption": {"power": 410},
                    "grid": {"power": 20}
                  }
                },
                {
                  "serial": "GW2412G481",
                  "time": "2026-08-26T12:08:00+01:00",
                  "power": {
                    "solar": {"arrays": [{"array": 1, "power": 1}]},
                    "battery": {"percent": 99, "power": 1},
                    "consumption": {"power": 1},
                    "grid": {"power": 1}
                  }
                },
                {
                  "time": "2026-08-26T12:16:00+01:00",
                  "power": {
                    "solar": {
                      "arrays": [
                        {"array": 1, "power": 800},
                        {"array": 2, "power": 50}
                      ]
                    },
                    "battery": {"percent": 55, "power": 400},
                    "consumption": {"power": 350},
                    "grid": {"power": 200}
                  }
                }
              ],
              "meta": {"last_page": 1}
            }
        """.trimIndent()
        val parsed = GivEnergyParser.parseDataPoints(raw)
        assertEquals(3, parsed.size)
        val snapshot = GivEnergyParser.snapshotFromPoints(parsed)
        assertEquals(2, snapshot.solarWSeries.size)
        assertEquals(390.0, snapshot.solarWSeries[0].w!!, 0.01)
        assertEquals(800.0, snapshot.solarWSeries[1].w!!, 0.01)
        assertEquals(700.0, snapshot.batteryWSeries[0].w!!, 0.01)
        assertEquals(-400.0, snapshot.batteryWSeries[1].w!!, 0.01)
        assertEquals(51.0, snapshot.socSeries[0].soc!!, 0.01)
        assertEquals(55.0, snapshot.socSeries[1].soc!!, 0.01)
        assertEquals(410.0, snapshot.houseWSeries[0].w!!, 0.01)
        assertEquals(350.0, snapshot.houseWSeries[1].w!!, 0.01)
        assertEquals(20.0, snapshot.gridWSeries[0].w!!, 0.01)
        assertEquals(200.0, snapshot.gridWSeries[1].w!!, 0.01)
        snapshot.solarWSeries.forEach { sample ->
            assertTrue(sample.w != 580.0 && sample.w != 9999.0 && sample.w != 1.0)
        }
    }

    @Test
    fun mergeKeepsPublicExtrasAndLetsLiveNumbersWin() {
        val publicStatus = HomeStatusParser.parse(
            """
            {
              "soc_percent": 10,
              "solar_w": 1,
              "house_w": 200,
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
            batteryWSeries = emptyList(),
            solarWSeries = emptyList(),
            houseWSeries = emptyList(),
            gridWSeries = emptyList()
        )
        val merged = LiveStatus.merge(publicStatus, live)
        assertEquals(63, merged.socPercent)
        assertEquals(420, merged.solarW)
        assertEquals(406, merged.houseW)
        assertEquals(-1250.0, merged.batteryW!!, 0.01)
        assertEquals(-210.0, merged.gridW!!, 0.01)
        assertEquals(10.0, merged.socSeries.first().soc!!, 0.01)
        assertEquals(63.0, merged.socSeries.last().soc!!, 0.01)
        assertEquals("2026-08-24T15:34:00Z", merged.socSeries.last().t)
        assertEquals(-1250.0, merged.batteryWSeries.last().w!!, 0.01)
        assertEquals(420.0, merged.solarWSeries.last().w!!, 0.01)
        assertEquals(406.0, merged.houseWSeries.last().w!!, 0.01)
        assertEquals(-210.0, merged.gridWSeries.last().w!!, 0.01)
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
        assertEquals(420.0, roundTrip.solarWSeries.last().w!!, 0.01)
        assertEquals(406.0, roundTrip.houseWSeries.last().w!!, 0.01)
        assertEquals(-210.0, roundTrip.gridWSeries.last().w!!, 0.01)
        assertEquals("12:00", roundTrip.nextPowerUp?.from)
    }

    @Test
    fun rejectedTokenMessageDoesNotIncludeSecret() {
        assertEquals("token rejected, re-enter", TokenRejectedException().message)
        assertEquals("abc", TokenStore.normalize("  Bearer abc  "))
        assertNull(TokenStore.normalize("   "))
    }
}
