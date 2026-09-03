package com.zekkers.watthome

import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.SocLayout
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.WidgetTextMeasure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocLayoutTest {
    private val halfHour = HomeStatusParser.parse(
        """{"next_power_up":{"from":"12:30","to":"14:30","opted_in":true},"soc_percent":100}"""
    )
    private val skipped = HomeStatusParser.parse(
        """{"next_power_up":{"from":"12:30","to":"14:30","opted_in":false},"soc_percent":100}"""
    )

    @Test
    fun oneByOneKeepsPercentOnFullCharge() {
        assertFalse(
            WidgetTextMeasure.fits(
                StatusFormatter.percent(100),
                SocLayout.PreferredSp,
                SocLayout.OneByOneInnerDp,
                1f,
                bold = true
            )
        )
        val token = SocLayout.token(100, SocLayout.OneByOneInnerDp, density = 1f)
        assertEquals("100\u2060%", token.text)
        assertFalse(token.text.contains('…'))
        assertTrue(token.sizeSp < SocLayout.PreferredSp)
        assertTrue(token.sizeSp >= SocLayout.MinSp)
        assertTrue(
            WidgetTextMeasure.fits(token.text, token.sizeSp, SocLayout.OneByOneInnerDp, 1f, bold = true)
        )
    }

    @Test
    fun twoDigitSocStaysPreferredOnOneByOne() {
        val token = SocLayout.token(88, SocLayout.OneByOneInnerDp, density = 1f)
        assertEquals("88\u2060%", token.text)
        assertEquals(SocLayout.PreferredSp, token.sizeSp, 0.01f)
    }

    @Test
    fun dropsPercentOnlyWhenThreeDigitsCannotFit() {
        val token = SocLayout.token(100, availableDp = 36f, density = 1f)
        assertEquals("100", token.text)
        assertTrue(WidgetTextMeasure.fits(token.text, token.sizeSp, 36f, 1f, bold = true))
    }

    @Test
    fun compactHeaderKeepsFullChargeAndBolt() {
        val clock = PowerUpLayout.clock(halfHour.nextPowerUp)!!
        assertEquals("12:30pm", clock.from)
        assertEquals("2:30pm", clock.to)
        assertTrue(StatusFormatter.optedInPowerUp(halfHour.nextPowerUp))
        assertTrue(
            SocLayout.headerFits(
                percent = 100,
                innerWidthDp = SocLayout.CompactHeaderInnerDp,
                clock = clock,
                showBolt = true,
                density = 1f
            )
        )
        val reserved = SocLayout.headerTrailingDp(clock, showBolt = true, density = 1f)
        assertTrue(reserved > SocLayout.HeaderBoltDp)
        val soc = SocLayout.token(
            100,
            SocLayout.headerSocBudget(SocLayout.CompactHeaderInnerDp, clock, true, 1f),
            density = 1f
        )
        assertEquals("100\u2060%", soc.text)
        assertFalse(soc.text.contains('…'))
        val used = WidgetTextMeasure.widthDp(soc.text, soc.sizeSp, 1f, bold = true) + reserved
        assertTrue(used <= SocLayout.CompactHeaderInnerDp)
    }

    @Test
    fun compactHeaderStillFitsTwoDigitAtPreferredSize() {
        val clock = PowerUpLayout.clock(halfHour.nextPowerUp)!!
        val soc = SocLayout.token(
            88,
            SocLayout.headerSocBudget(SocLayout.CompactHeaderInnerDp, clock, true, 1f),
            density = 1f
        )
        assertEquals("88\u2060%", soc.text)
        assertEquals(SocLayout.PreferredSp, soc.sizeSp, 0.01f)
    }

    @Test
    fun boltOnlyWhenOptedIn() {
        assertTrue(StatusFormatter.optedInPowerUp(halfHour.nextPowerUp))
        assertFalse(StatusFormatter.optedInPowerUp(skipped.nextPowerUp))
        assertFalse(StatusFormatter.optedInPowerUp(null))
        assertTrue(StatusFormatter.hasPowerUp(skipped.nextPowerUp))
    }
}
