package com.zekkers.watthome

import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.SocLayout
import com.zekkers.watthome.data.WidgetPlotLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class WidgetPlotLayoutTest {
    private val midday = ZonedDateTime.parse("2026-09-03T13:00:00+01:00[Europe/London]")
    private val halfHour = HomeStatusParser.parse(
        """{"next_power_up":{"from":"12:30","to":"14:30","opted_in":true},"soc_percent":100}"""
    )

    @Test
    fun overviewPlotSitsOnTheRightAtFullTileHeight() {
        val pane = WidgetPlotLayout.overviewSplit(360f, 146f)
        val innerW = 360f - WidgetPlotLayout.OverviewPadDp
        val innerH = 146f - WidgetPlotLayout.OverviewPadDp
        assertTrue(pane.besideHeader)
        assertEquals(innerH, pane.plotHeightDp, 0.01f)
        assertTrue(pane.plotWidthDp < innerW)
        assertTrue(pane.leftWidthDp + pane.plotWidthDp <= innerW + 0.01f)
        val squishedUnderText = WidgetPlotLayout.letterbox(innerW, 42f)
        assertTrue(pane.plotHeightDp > squishedUnderText.height * 2f)
        assertTrue(pane.plotWidthDp / pane.plotHeightDp < WidgetPlotLayout.PlotAspect)
    }

    @Test
    fun overviewDoesNotLetterboxTheRightPane() {
        val pane = WidgetPlotLayout.overviewSplit(360f, 146f)
        val filled = WidgetPlotLayout.plotBounds(pane.plotWidthDp, pane.plotHeightDp, fillSlot = true)
        val letterboxed = WidgetPlotLayout.plotBounds(pane.plotWidthDp, pane.plotHeightDp, fillSlot = false)
        assertEquals(0f, filled.left, 0.01f)
        assertEquals(pane.plotWidthDp, filled.width, 0.01f)
        assertEquals(pane.plotHeightDp, filled.height, 0.01f)
        assertTrue(letterboxed.width < filled.width || letterboxed.height < filled.height)
    }

    @Test
    fun zeroGutterOnlyWhenPowerTracesExist() {
        assertEquals(0f, WidgetPlotLayout.zeroGutterPx(hasPower = false, labelWidthPx = 18f), 0.01f)
        assertEquals(0f, WidgetPlotLayout.zeroGutterPx(hasPower = true, labelWidthPx = 0f), 0.01f)
        assertEquals(24f, WidgetPlotLayout.zeroGutterPx(hasPower = true, labelWidthPx = 18f), 0.01f)
        val plot = WidgetPlotLayout.plotBounds(200f, 80f, fillSlot = true)
        val withZero = WidgetPlotLayout.chartBounds(plot, 0f, 20f)
        val socOnly = WidgetPlotLayout.chartBounds(plot, 0f, 0f)
        assertEquals(20f, withZero.left, 0.01f)
        assertEquals(0f, socOnly.left, 0.01f)
        assertEquals(40f, withZero.midY, 0.01f)
        assertEquals("0W", WidgetPlotLayout.ZeroLabel)
    }

    @Test
    fun sessionUsesBelowHeaderOnTypicalTwoByOne() {
        val pane = WidgetPlotLayout.sessionPlot(160f, 72f)
        assertFalse(pane.besideHeader)
        assertEquals(160f - WidgetPlotLayout.SessionPadDp, pane.plotWidthDp, 0.01f)
        assertTrue(pane.plotHeightDp >= WidgetPlotLayout.MinPlotHeightDp)
    }

    @Test
    fun sessionFallsBackBesideOnVeryShortTile() {
        val pane = WidgetPlotLayout.sessionPlot(160f, 40f)
        assertTrue(pane.besideHeader)
        assertTrue(pane.plotWidthDp >= WidgetPlotLayout.MinPlotWidthDp)
        assertTrue(pane.leftWidthDp >= 72f)
        val clock = PowerUpLayout.clock(halfHour.nextPowerUp, midday)!!
        val soc = SocLayout.token(
            100,
            SocLayout.headerSocBudget(pane.leftWidthDp, clock, true, 1f),
            density = 1f
        )
        assertFalse(soc.text.contains('…'))
        assertTrue(soc.text.startsWith("100"))
    }

    @Test
    fun glanceBottomLeavesRoomForZeroLabelStrip() {
        val pane = WidgetPlotLayout.glanceBottom(160f, 160f, extraLines = 1)
        assertFalse(pane.besideHeader)
        assertTrue(pane.plotHeightDp >= WidgetPlotLayout.MinPlotHeightDp)
        assertEquals(160f - WidgetPlotLayout.GlancePadDp, pane.plotWidthDp, 0.01f)
    }

    @Test
    fun oneByOneBudgetIsUnchangedBySessionSplit() {
        val token = SocLayout.token(100, SocLayout.OneByOneInnerDp, density = 1f)
        assertEquals("100\u2060%", token.text)
        assertTrue(token.sizeSp >= SocLayout.MinSp)
    }
}
