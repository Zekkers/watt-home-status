package com.zekkers.watthome

import com.zekkers.watthome.data.FactKind
import com.zekkers.watthome.data.FactLayout
import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.PowerUpClockMode
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.SessionKind
import com.zekkers.watthome.data.SessionLayout
import com.zekkers.watthome.data.SocLayout
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.WidgetTextMeasure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class SessionLayoutTest {
    private val fridayMorning = ZonedDateTime.parse("2026-09-11T11:00:00+01:00[Europe/London]")
    private val fridayEvening = ZonedDateTime.parse("2026-09-11T18:30:00+01:00[Europe/London]")
    private val fridayDuring = ZonedDateTime.parse("2026-09-11T19:20:00+01:00[Europe/London]")
    private val fridayJustEnded = ZonedDateTime.parse("2026-09-11T20:00:00+01:00[Europe/London]")
    private val fridayFourAfter = ZonedDateTime.parse("2026-09-11T20:04:00+01:00[Europe/London]")
    private val fridayFiveAfter = ZonedDateTime.parse("2026-09-11T20:05:00+01:00[Europe/London]")
    private val thursday = ZonedDateTime.parse("2026-09-10T18:00:00+01:00[Europe/London]")
    private val saturday = ZonedDateTime.parse("2026-09-12T10:00:00+01:00[Europe/London]")

    private val livePowerDown = """
        {
          "last_action": "17:30 Power Up catch: no new Power Up / Happy Hour. Power Down tonight 19:00–20:00 already opted in at 16:00 (session 7d10b3d0…). SOC 46% < overnight cap 71%.",
          "weather_tomorrow": {"code":"sunny","label":"Fri Solcast Garage+Home used overnight (noon ~4.51 / 08–16 ~16.6 kWh). Lean overnight 71%. Morning: early PV lagging."},
          "next_power_up": null,
          "overnight": {"from":"02:00","to":"02:58","percent_limit":71,"kw":6,"cleared":false},
          "booked_power_up": {"date":"2026-09-05","from":"11:30","to":"13:30","opted_in":true,"label":"Power Up","kind":"power_up"},
          "booked_happy_hour": {"date":"2026-09-06","from":"12:00","to":"13:00","opted_in":true,"label":"Happy Hour","kind":"weekend_happy_hour"},
          "booked_power_down": {"date":"2026-09-11","from":"19:00","to":"20:00","opted_in":true,"label":"Power Down","kind":"power_down","session_id":"7d10b3d0-fa59-4f5c-96c4-d98389c10bc8"}
        }
    """.trimIndent()

    @Test
    fun parsesBookedPowerDownAndOvernightAliasesFromLiveShape() {
        val status = HomeStatusParser.parse(livePowerDown)
        assertEquals("19:00", status.bookedPowerDown?.from)
        assertEquals("20:00", status.bookedPowerDown?.to)
        assertEquals("2026-09-11", status.bookedPowerDown?.date)
        assertEquals(true, status.bookedPowerDown?.optedIn)
        assertEquals("Power Down", status.bookedPowerDown?.label)
        assertEquals("power_down", status.bookedPowerDown?.kind)
        assertEquals("7d10b3d0-fa59-4f5c-96c4-d98389c10bc8", status.bookedPowerDown?.sessionId)
        assertEquals("02:00", status.overnight?.start)
        assertEquals("02:58", status.overnight?.end)
        assertEquals(71, status.overnight?.capPercent)
        assertEquals("02:00–02:58 · cap 71%", StatusFormatter.overnight(status.overnight))
        assertEquals("11:30", status.bookedPowerUp?.from)
        assertEquals("Happy Hour", status.bookedHappyHour?.label)
    }

    @Test
    fun fridayLondonShowsOnlyTodaysPowerDownChip() {
        val status = HomeStatusParser.parse(livePowerDown)
        val sessions = SessionLayout.visible(status, fridayEvening)
        assertEquals(1, sessions.size)
        assertEquals(SessionKind.PowerDown, sessions.single().kind)
        assertEquals("Power Down", sessions.single().shortLabel)
        assertEquals("7pm", sessions.single().clock.from)
        assertEquals("8pm", sessions.single().clock.to)
        assertEquals("7pm - 8pm", sessions.single().clock.oneLine)
        assertEquals("7pm–8pm", sessions.single().clock.tightLine)
        assertTrue(sessions.single().optedIn)
        assertFalse(sessions.single().kind.isFreeElectric)
        assertFalse(SessionLayout.usesBolt(sessions.single().kind))
        assertTrue(StatusFormatter.isPowerUpCurrent(status.bookedPowerDown, fridayDuring))
        assertTrue(StatusFormatter.isPowerUpCurrent(status.bookedPowerDown, fridayJustEnded))
        assertTrue(StatusFormatter.isPowerUpCurrent(status.bookedPowerDown, fridayFourAfter))
    }

    @Test
    fun futureAndExpiredPowerDownStayHidden() {
        val status = HomeStatusParser.parse(livePowerDown)
        assertTrue(SessionLayout.visible(status, thursday).isEmpty())
        assertTrue(SessionLayout.visible(status, saturday).isEmpty())
        assertTrue(SessionLayout.visible(status, fridayFiveAfter).isEmpty())
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.oneByOne(status.bookedPowerDown, thursday))
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.twoByTwo(status.bookedPowerDown, fridayFiveAfter))
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.twoByOne(status.bookedPowerDown, now = thursday))
        assertEquals(
            PowerUpClockMode.Hidden,
            PowerUpLayout.wide(status.bookedPowerDown, availableDp = 200f, timeSp = 12f, density = 1f, now = saturday)
        )
        assertFalse(StatusFormatter.optedInPowerUp(status.bookedPowerDown, fridayFiveAfter))
    }

    @Test
    fun powerUpAndPowerDownTodayAreTwoSeparateRows() {
        val status = HomeStatusParser.parse(
            """
            {
              "last_action": "Both windows mashed into one paragraph 12:00–14:00 and Power Down 19:00–20:00.",
              "next_power_up": {"from":"12:00","to":"14:00","date":"2026-09-11","opted_in":true,"label":"Power Up","kind":"power_up"},
              "booked_power_down": {"from":"19:00","to":"20:00","date":"2026-09-11","opted_in":false,"label":"Power Down","kind":"power_down"}
            }
            """.trimIndent()
        )
        val sessions = SessionLayout.visible(status, fridayMorning)
        assertEquals(2, sessions.size)
        assertEquals(SessionKind.PowerUp, sessions[0].kind)
        assertEquals("12pm", sessions[0].clock.from)
        assertEquals("2pm", sessions[0].clock.to)
        assertTrue(sessions[0].optedIn)
        assertTrue(sessions[0].kind.isFreeElectric)
        assertTrue(SessionLayout.usesBolt(sessions[0].kind))
        assertEquals(SessionKind.PowerDown, sessions[1].kind)
        assertEquals("7pm", sessions[1].clock.from)
        assertEquals("8pm", sessions[1].clock.to)
        assertFalse(sessions[1].optedIn)
        assertFalse(sessions.any { it.shortLabel.contains("mashed") })
        assertFalse(sessions.any { it.clock.oneLine.contains("and") })
        val afterPowerUp = SessionLayout.visible(status, fridayEvening)
        assertEquals(1, afterPowerUp.size)
        assertEquals(SessionKind.PowerDown, afterPowerUp.single().kind)
    }

    @Test
    fun nextPowerUpAndBookedPowerUpDedupOnTheSameWindow() {
        val status = HomeStatusParser.parse(
            """
            {
              "next_power_up": {"from":"12:00","to":"14:00","date":"2026-09-11","opted_in":true},
              "booked_power_up": {"from":"12:00","to":"14:00","date":"2026-09-11","opted_in":true,"label":"Power Up","kind":"power_up"}
            }
            """.trimIndent()
        )
        val sessions = SessionLayout.visible(status, fridayMorning)
        assertEquals(1, sessions.size)
        assertEquals(SessionKind.PowerUp, sessions.single().kind)
        assertEquals("12pm - 2pm", sessions.single().clock.oneLine)
    }

    @Test
    fun happyHourUsesBoltAndOwnLabel() {
        val status = HomeStatusParser.parse(
            """{"booked_happy_hour":{"from":"12:00","to":"13:00","date":"2026-09-11","opted_in":true,"label":"Happy Hour","kind":"weekend_happy_hour"}}"""
        )
        val sessions = SessionLayout.visible(status, fridayMorning)
        assertEquals(1, sessions.size)
        assertEquals(SessionKind.HappyHour, sessions.single().kind)
        assertEquals("Happy Hour", sessions.single().shortLabel)
        assertEquals("12pm", sessions.single().clock.from)
        assertEquals("1pm", sessions.single().clock.to)
        assertTrue(SessionLayout.usesBolt(sessions.single().kind))
    }

    @Test
    fun lastActionAndWeatherProseAreNotSessions() {
        val status = HomeStatusParser.parse(livePowerDown)
        val sessions = SessionLayout.visible(status, fridayEvening)
        val blob = sessions.joinToString { "${it.shortLabel} ${it.clock.oneLine}" }
        assertFalse(blob.contains("last_action"))
        assertFalse(blob.contains("Solcast"))
        assertFalse(blob.contains("already opted in at 16:00"))
        assertFalse(blob.contains(status.lastAction.orEmpty().take(20)))
        assertEquals("sunny", status.weatherTomorrow?.code)
        assertTrue(status.weatherTomorrow?.label.orEmpty().length > 40)
    }

    @Test
    fun overnightAndWeatherFactsStayAfterPowerDownExpires() {
        val status = HomeStatusParser.parse(livePowerDown)
        assertTrue(SessionLayout.visible(status, fridayFiveAfter).isEmpty())
        val facts = FactLayout.facts(status)
        val header = FactLayout.headerFacts(status)
        assertEquals(listOf(FactKind.Overnight, FactKind.Weather), header.map { it.kind })
        assertEquals("02:00–02:58 · cap 71%", facts.first { it.kind == FactKind.Overnight }.value)
        assertEquals("02:00–02:58 · 71\u2060%", facts.first { it.kind == FactKind.Overnight }.compact)
        assertEquals("Sunny", facts.first { it.kind == FactKind.Weather }.value)
        assertFalse(facts.any { it.value.contains("Solcast") })
        assertFalse(facts.any { it.value.contains("already opted in") })
        assertFalse(header.any { it.value.contains("Power Up catch") })
        assertEquals("Sunny", StatusFormatter.weatherCodeLabel(status.weatherTomorrow))
        assertTrue(StatusFormatter.weatherLabel(status.weatherTomorrow).contains("Solcast"))
    }

    @Test
    fun factsDoNotNeedAPowerUpWindow() {
        val status = HomeStatusParser.parse(
            """
            {
              "overnight":{"from":"02:00","to":"02:58","percent_limit":71},
              "weather_tomorrow":{"code":"partly_cloudy","label":"a very long solcast paragraph"},
              "target_1600_percent":60,
              "peak_window":"16:00-19:00",
              "next_power_up":null
            }
            """.trimIndent()
        )
        val facts = FactLayout.facts(status)
        assertEquals(4, facts.size)
        assertEquals(FactKind.Overnight, facts[0].kind)
        assertEquals(FactKind.Weather, facts[1].kind)
        assertEquals("Partly cloudy", facts[1].value)
        assertEquals("60\u2060%", facts.first { it.kind == FactKind.Target1600 }.value)
        assertEquals("16:00-19:00", facts.first { it.kind == FactKind.Peak }.value)
        assertTrue(SessionLayout.visible(status, fridayFiveAfter).isEmpty())
        assertTrue(
            SocLayout.headerFactRowWidth(
                overnightLine = facts[0].compact,
                showWeather = true,
                density = 1f
            ) > SocLayout.HeaderBoltDp
        )
        assertTrue(
            WidgetTextMeasure.fits(facts[0].compact, 11f, SocLayout.CompactHeaderInnerDp, 1f)
        )
    }

    @Test
    fun skippedPowerDownStillShowsKindAndTimesWithoutPretendingOptedIn() {
        val status = HomeStatusParser.parse(
            """{"booked_power_down":{"from":"19:00","to":"20:00","date":"2026-09-11","opted_in":false,"kind":"power_down"}}"""
        )
        val session = SessionLayout.visible(status, fridayEvening).single()
        assertEquals(SessionKind.PowerDown, session.kind)
        assertFalse(session.optedIn)
        assertNotNull(PowerUpLayout.clock(status.bookedPowerDown, fridayEvening))
    }

    @Test
    fun oneByOneKeepsPowerDownClocksReadableAndDoesNotClipBadge() {
        val status = HomeStatusParser.parse(
            """{"booked_power_down":{"from":"19:00","to":"20:00","date":"2026-09-11","opted_in":true,"kind":"power_down"},"soc_percent":100}"""
        )
        val session = SessionLayout.visible(status, fridayEvening).single()
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.oneByOne(status.bookedPowerDown, fridayEvening))
        assertTrue(WidgetTextMeasure.fits(session.clock.from, 10f, SocLayout.OneByOneInnerDp, 1f))
        assertTrue(WidgetTextMeasure.fits(session.clock.to, 10f, SocLayout.OneByOneInnerDp, 1f))
        val chip = SocLayout.sessionChipWidth(session, density = 1f, timeSp = 10f)
        assertTrue(chip > SocLayout.HeaderBoltDp)
        assertTrue(chip <= SocLayout.OneByOneInnerDp)
        assertTrue(
            SocLayout.headerFits(
                percent = 100,
                innerWidthDp = SocLayout.CompactHeaderInnerDp,
                sessions = listOf(session),
                density = 1f
            )
        )
    }

    @Test
    fun twoTodaySessionsKeepSeparateReadableChipsOnCompactHeader() {
        val status = HomeStatusParser.parse(
            """
            {
              "soc_percent":100,
              "next_power_up":{"from":"12:00","to":"14:00","date":"2026-09-11","opted_in":true,"kind":"power_up"},
              "booked_power_down":{"from":"19:00","to":"20:00","date":"2026-09-11","opted_in":true,"kind":"power_down"}
            }
            """.trimIndent()
        )
        val sessions = SessionLayout.visible(status, fridayMorning)
        assertEquals(2, sessions.size)
        assertTrue(
            SocLayout.headerFits(
                percent = 100,
                innerWidthDp = SocLayout.CompactHeaderInnerDp,
                sessions = sessions,
                density = 1f,
                oneLine = true
            )
        )
        sessions.forEach { session ->
            assertTrue(WidgetTextMeasure.fits(session.clock.tightLine, 12f, 120f, 1f))
            assertFalse(session.clock.tightLine.contains("…"))
        }
    }
}
