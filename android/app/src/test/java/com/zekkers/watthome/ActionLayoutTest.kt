package com.zekkers.watthome

import com.zekkers.watthome.data.ActionKind
import com.zekkers.watthome.data.ActionLayout
import com.zekkers.watthome.data.FactKind
import com.zekkers.watthome.data.FactLayout
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.SessionLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class ActionLayoutTest {
    private val fridayEvening = ZonedDateTime.parse("2026-09-11T18:30:00+01:00[Europe/London]")

    private val liveMultiline = """
        ⚡ Typeform opted in Tue 15 Sep Power Up 14:00–16:00; CH2414G328 charge 14:00–16:00 @ 100%/6 kW (today’s 14–16 already past)
        🌙 overnight cleared (SOC 48% ≥ 30% PU-day cap); 01:30 will re-size if needed
        ⬇️ Power Down tonight 18:00–19:00 already opted in (session 90d85341…)
        ☀️ solar ~113 W house ~1306 W batt +1286 W grid ~-5 W
    """.trimIndent()

    @Test
    fun splitsEmojiLinesAndStripsPrefixes() {
        val lines = ActionLayout.lines(liveMultiline)
        assertEquals(4, lines.size)
        assertEquals(ActionKind.PowerUp, lines[0].kind)
        assertEquals("Power Up", lines[0].kind.title)
        assertTrue(lines[0].text.startsWith("Typeform opted in"))
        assertFalse(lines[0].text.contains("⚡"))
        assertEquals(ActionKind.Overnight, lines[1].kind)
        assertTrue(lines[1].text.startsWith("overnight cleared"))
        assertFalse(lines[1].text.contains("🌙"))
        assertEquals(ActionKind.PowerDown, lines[2].kind)
        assertTrue(lines[2].text.startsWith("Power Down tonight"))
        assertFalse(lines[2].text.startsWith("⬇"))
        assertEquals(ActionKind.LivePower, lines[3].kind)
        assertTrue(lines[3].text.startsWith("solar ~113 W"))
        assertFalse(lines[3].text.contains("☀"))
        assertTrue(lines[0].compact.startsWith("Typeform opted in"))
        assertTrue(lines[0].compact.length <= ActionLayout.COMPACT_MAX_CHARS)
        assertTrue(lines[1].compact.startsWith("overnight cleared"))
        assertTrue(lines[1].compact.length <= ActionLayout.COMPACT_MAX_CHARS)
        assertTrue(ActionLayout.displayText(lines[0]).contains("already past"))
        assertTrue(ActionLayout.displayText(lines[1]).contains("re-size if needed"))
        assertTrue(ActionLayout.displayText(lines[2]).contains("already opted in"))
        assertFalse(ActionLayout.displayText(lines[0]).contains("..."))
        assertEquals(
            "solar ~113 W\nhouse ~1306 W\nbatt +1286 W\ngrid ~-5 W",
            ActionLayout.displayText(lines[3])
        )
    }

    @Test
    fun skipsBlankLinesAndClassifiesWithoutEmoji() {
        val raw = """

            Typeform opted in Tue 15 Sep Power Up 14:00–16:00

            overnight cleared (SOC 48% ≥ 30% PU-day cap)
            Power Down tonight 18:00–19:00 already opted in
            solar ~113 W house ~1306 W batt +1286 W grid ~-5 W

        """.trimIndent()
        val lines = ActionLayout.lines(raw)
        assertEquals(4, lines.size)
        assertEquals(
            listOf(ActionKind.PowerUp, ActionKind.Overnight, ActionKind.PowerDown, ActionKind.LivePower),
            lines.map { it.kind }
        )
    }

    @Test
    fun legacySingleLineBlobIsOneRow() {
        val blob =
            "17:30 Power Up catch: no new Power Up / Happy Hour. Power Down tonight 19:00–20:00 already opted in at 16:00 (session 7d10b3d0…). SOC 46% < overnight cap 71%."
        val lines = ActionLayout.lines(blob)
        assertEquals(1, lines.size)
        assertEquals(ActionKind.PowerUp, lines.single().kind)
        assertEquals(blob, lines.single().text)
        assertFalse(lines.single().text.contains('\n'))
    }

    @Test
    fun unknownPrefixFallsBackToNeutralAction() {
        val lines = ActionLayout.lines("status.json seeded")
        assertEquals(1, lines.size)
        assertEquals(ActionKind.Other, lines.single().kind)
        assertEquals("Last action", lines.single().kind.title)
        assertEquals("status.json seeded", lines.single().text)
        assertTrue(ActionLayout.lines(null as String?).isEmpty())
        assertTrue(ActionLayout.lines("   \n  \n").isEmpty())
    }

    @Test
    fun doesNotUseWeatherEssayAndStaysOutOfSessionChips() {
        val status = HomeStatusParser.parse(
            """
            {
              "last_action": "⚡ Typeform opted in\n🌙 overnight cleared",
              "weather_tomorrow": {"code":"sunny","label":"Fri Solcast Garage+Home used overnight. Lean overnight 71%."},
              "booked_power_down": {"from":"19:00","to":"20:00","date":"2026-09-11","opted_in":true,"kind":"power_down"}
            }
            """.trimIndent()
        )
        val actions = ActionLayout.from(status)
        assertEquals(2, actions.size)
        assertFalse(actions.any { it.text.contains("Solcast") })
        assertFalse(actions.any { it.text.contains("Lean overnight 71%") })
        val facts = FactLayout.facts(status)
        assertEquals("Sunny", facts.first { it.kind == FactKind.Weather }.value)
        assertFalse(facts.any { it.value.contains("Solcast") })
        val sessions = SessionLayout.visible(status, fridayEvening)
        assertEquals(1, sessions.size)
        assertFalse(sessions.any { it.clock.oneLine.contains("Typeform") })
    }

    @Test
    fun widgetCompactKeepsTheFirstDecisionLines() {
        val status = HomeStatus(lastAction = liveMultiline)
        val compact = ActionLayout.compact(status, limit = 2)
        assertEquals(2, compact.size)
        assertEquals(ActionKind.PowerUp, compact[0].kind)
        assertEquals(ActionKind.Overnight, compact[1].kind)
        assertTrue(compact.all { it.compact.length <= ActionLayout.COMPACT_MAX_CHARS })
        assertEquals(1, ActionLayout.compact(status, limit = 1).size)
        val glance = ActionLayout.compact(status, limit = 1).single()
        assertTrue(glance.compact.isNotBlank())
        assertFalse(glance.text.isEmpty())
    }
}
