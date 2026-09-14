package com.zekkers.watthome

import com.zekkers.watthome.data.ClockStyle
import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.PowerUpClockMode
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.WidgetTextMeasure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class PowerUpLayoutTest {
    private val midday = ZonedDateTime.parse("2026-08-27T13:00:00+01:00[Europe/London]")
    private val noon = HomeStatusParser.parse(
        """{"next_power_up":{"from":"12:00","to":"14:00","opted_in":true},"soc_percent":63}"""
    ).nextPowerUp
    private val peak = HomeStatusParser.parse(
        """{"next_power_up":{"from":"16:00","to":"19:00","opted_in":true}}"""
    ).nextPowerUp
    private val skipped = HomeStatusParser.parse(
        """{"next_power_up":{"from":"12:00","to":"14:00","opted_in":false}}"""
    ).nextPowerUp

    @Test
    fun defaultClockIsTwentyFourHour() {
        val clock = PowerUpLayout.clock(noon, midday)!!
        assertEquals("12:00", clock.from)
        assertEquals("14:00", clock.to)
        assertEquals("12:00 - 14:00", clock.oneLine)
        assertEquals("12:00", StatusFormatter.formatClock(noon?.from))
        assertEquals("14:00", StatusFormatter.formatClock(noon?.to))
        val twelve = PowerUpLayout.clock(noon, midday, ClockStyle.TwelveHour)!!
        assertEquals("12pm", twelve.from)
        assertEquals("2pm", twelve.to)
        assertEquals("12pm - 2pm", twelve.oneLine)
        assertEquals("12pm", StatusFormatter.twelveHourClock(noon?.from))
        assertEquals("2pm", StatusFormatter.twelveHourClock(noon?.to))
        assertFalse(clock.from.contains("-"))
        assertFalse(clock.to.contains("-"))
        assertFalse(clock.oneLine.contains("63"))
        assertFalse("${StatusFormatter.percent(63)}${clock.oneLine}".contains("63%pm"))
    }

    @Test
    fun oneByOneNeverUsesOneLine() {
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.oneByOne(noon, midday))
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.oneByOne(peak, midday))
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.oneByOne(skipped, midday))
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.oneByOne(null, midday))
        assertNotEquals(PowerUpClockMode.OneLine, PowerUpLayout.oneByOne(noon, midday))
        assertEquals(PowerUpLayout.oneByOne(noon, midday), PowerUpLayout.twoByTwo(noon, midday))
    }

    @Test
    fun twoByOneAlwaysStacksNoonAndTwo() {
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.twoByOne(noon, now = midday))
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.twoByOne(noon, availableTimeDp = 220f, now = midday))
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.twoByOne(null, now = midday))
        assertNotEquals(PowerUpClockMode.OneLine, PowerUpLayout.twoByOne(noon, now = midday))
        val clock = PowerUpLayout.clock(noon, midday)!!
        assertEquals("12:00", clock.from)
        assertEquals("14:00", clock.to)
    }

    @Test
    fun wideOverviewKeepsFullTwelvePmDashTwoPmWhenWidthAllows() {
        assertEquals(
            PowerUpClockMode.OneLine,
            PowerUpLayout.wide(noon, availableDp = 200f, timeSp = 12f, density = 1f, now = midday)
        )
        assertEquals(
            PowerUpClockMode.Stacked,
            PowerUpLayout.wide(noon, availableDp = 80f, timeSp = 12f, density = 1f, now = midday)
        )
        assertEquals(
            PowerUpClockMode.OneLine,
            PowerUpLayout.wide(noon, availableDp = 200f, timeSp = 12f, density = 1f, showBolt = true, now = midday)
        )
        assertEquals(
            PowerUpClockMode.Stacked,
            PowerUpLayout.wide(noon, availableDp = 110f, timeSp = 12f, density = 1f, showBolt = true, now = midday)
        )
        assertNull(PowerUpLayout.clock(null, midday))
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.wide(null, 200f, 12f, 1f, now = midday))
    }

    @Test
    fun peakWindowUsesSixteenAndNineteenTokens() {
        val clock = PowerUpLayout.clock(peak, midday)!!
        assertEquals("16:00", clock.from)
        assertEquals("19:00", clock.to)
        assertEquals("16:00 - 19:00", clock.oneLine)
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.oneByOne(peak, midday))
        val twelve = PowerUpLayout.clock(peak, midday, ClockStyle.TwelveHour)!!
        assertEquals("4pm", twelve.from)
        assertEquals("7pm", twelve.to)
    }

    @Test
    fun oneLineIsWiderThanStackedTokens() {
        val clock = PowerUpLayout.clock(noon, midday)!!
        val one = WidgetTextMeasure.estimateWidthDp(clock.oneLine, 13f)
        val stacked = maxOf(
            WidgetTextMeasure.estimateWidthDp(clock.from, 13f),
            WidgetTextMeasure.estimateWidthDp(clock.to, 13f)
        )
        assertTrue(one > stacked)
        assertTrue(WidgetTextMeasure.estimateWidthDp("63\u2060%", 26f, bold = true) > 30f)
        assertTrue(WidgetTextMeasure.fits("21\u2060%", 26f, 56f, 1f, bold = true))
        assertFalse(WidgetTextMeasure.fits("21\u2060%", 32f, 56f, 1f, bold = true))
        assertFalse(WidgetTextMeasure.fits("100\u2060%", 26f, 66f, 1f, bold = true))
        assertTrue(WidgetTextMeasure.fits("12:00", 10f, 40f, 1f))
        assertTrue(WidgetTextMeasure.fits("14:00", 10f, 40f, 1f))
        assertFalse(WidgetTextMeasure.fits("12:00 - 14:00", 13f, 40f, 1f))
        assertTrue(WidgetTextMeasure.fits("12pm", 10f, 40f, 1f))
        assertTrue(WidgetTextMeasure.fits("2pm", 10f, 40f, 1f))
        assertFalse(WidgetTextMeasure.fits("12pm - 2pm", 13f, 40f, 1f))
    }

    @Test
    fun savingsLineStaysUnclippedToken() {
        val status = HomeStatusParser.parse(
            """{"last_savings":{"gbp":36.95,"window_label":"21 Jul–1 Aug 2026 (9 sessions)"}}"""
        )
        assertEquals("£36.95 · 9 sess", StatusFormatter.savingsWidgetLine(status.lastSavings))
        assertTrue(WidgetTextMeasure.fits("£36.95 · 9 sess", 11f, 160f, 1f))
    }
}
