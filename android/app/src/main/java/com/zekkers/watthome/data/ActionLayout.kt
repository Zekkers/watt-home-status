package com.zekkers.watthome.data

/**
 * Discrete Watt decisions from `last_action`. Split on newlines, strip
 * leading emoji, and never concatenate weather_tomorrow.label.
 */
enum class ActionKind {
    PowerUp,
    Overnight,
    PowerDown,
    LivePower,
    Other;

    val title: String
        get() = when (this) {
            PowerUp -> "Power Up"
            Overnight -> "Overnight"
            PowerDown -> "Power Down"
            LivePower -> "Live"
            Other -> "Last action"
        }
}

data class ActionLine(
    val kind: ActionKind,
    val text: String,
    val compact: String = text
)

object ActionLayout {
    const val WIDGET_LIMIT = 2
    const val GLANCE_LIMIT = 1
    const val COMPACT_MAX_CHARS = 42

    fun lines(raw: String?): List<ActionLine> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parseLine(it) }
    }

    fun from(status: HomeStatus?): List<ActionLine> = lines(status?.lastAction)

    fun compact(status: HomeStatus?, limit: Int = WIDGET_LIMIT): List<ActionLine> =
        from(status).take(limit.coerceAtLeast(0))

    fun displayText(action: ActionLine): String = when (action.kind) {
        ActionKind.LivePower -> densifyLiveWatts(action.text)
        else -> action.text
    }

    internal fun densifyLiveWatts(text: String): String {
        val pieces = LIVE_CLAUSE.split(text).map { it.trim() }.filter { it.isNotEmpty() }
        return if (pieces.size >= 2) pieces.joinToString("\n") else text
    }

    private fun parseLine(raw: String): ActionLine? {
        val (emojiKind, rest) = stripLeadingGlyph(raw)
        val text = rest.trim().ifEmpty { return null }
        val kind = emojiKind ?: classifyByText(text)
        return ActionLine(kind = kind, text = text, compact = compactText(text))
    }

    internal fun compactText(text: String, maxChars: Int = COMPACT_MAX_CHARS): String {
        val pick = text.substringBefore(';').trim().ifEmpty { text }
        if (pick.length <= maxChars) return pick
        return pick.take(maxChars - 1).trimEnd { it.isWhitespace() || it == '…' } + "…"
    }

    internal fun classifyByText(text: String): ActionKind {
        val lower = text.lowercase()
        val hits = listOfNotNull(
            indexOf(lower, "power down")?.let { it to ActionKind.PowerDown },
            indexOf(lower, "overnight")?.let { it to ActionKind.Overnight },
            firstIndex(lower, "typeform", "power up", "happy hour", "free electric")
                ?.let { it to ActionKind.PowerUp },
            livePowerIndex(lower)?.let { it to ActionKind.LivePower }
        )
        return hits.minByOrNull { it.first }?.second ?: ActionKind.Other
    }

    private fun stripLeadingGlyph(raw: String): Pair<ActionKind?, String> {
        var s = raw.trim()
        s = peelDecorators(s)
        for ((kind, mark) in KNOWN_PREFIXES) {
            if (s.startsWith(mark)) {
                return kind to peelDecorators(s.removePrefix(mark))
            }
        }
        val stripped = stripLeadingSymbols(s)
        return null to stripped.ifEmpty { s }
    }

    private fun peelDecorators(raw: String): String {
        var i = 0
        while (i < raw.length) {
            val cp = raw.codePointAt(i)
            if (cp != 0xFE0F && cp != 0xFE0E && cp != 0x200D && !Character.isWhitespace(cp)) break
            i += Character.charCount(cp)
        }
        return raw.substring(i)
    }

    private fun stripLeadingSymbols(raw: String): String {
        var i = 0
        while (i < raw.length) {
            val cp = raw.codePointAt(i)
            if (!isLeadingGlyph(cp)) break
            i += Character.charCount(cp)
        }
        return peelDecorators(raw.substring(i))
    }

    private fun isLeadingGlyph(cp: Int): Boolean {
        if (cp == 0xFE0F || cp == 0xFE0E || cp == 0x200D) return true
        if (Character.isWhitespace(cp)) return true
        return when (Character.getType(cp).toByte()) {
            Character.OTHER_SYMBOL,
            Character.MATH_SYMBOL,
            Character.MODIFIER_SYMBOL,
            Character.OTHER_PUNCTUATION -> true
            else -> false
        }
    }

    private fun livePowerIndex(lower: String): Int? {
        val solar = indexOf(lower, "solar") ?: return null
        val hasWatts = WATTS.containsMatchIn(lower)
        return if (hasWatts) solar else null
    }

    private fun firstIndex(lower: String, vararg needles: String): Int? =
        needles.mapNotNull { indexOf(lower, it) }.minOrNull()

    private fun indexOf(lower: String, needle: String): Int? {
        val at = lower.indexOf(needle)
        return if (at >= 0) at else null
    }

    private val WATTS = Regex("""\d\s*w\b""")
    private val LIVE_CLAUSE = Regex("""(?i)\s+(?=(?:house|batt(?:ery)?|grid)\b)""")

    private val KNOWN_PREFIXES = listOf(
        ActionKind.PowerUp to "⚡",
        ActionKind.Overnight to "🌙",
        ActionKind.PowerDown to "⬇",
        ActionKind.PowerDown to "↓",
        ActionKind.LivePower to "☀",
        ActionKind.LivePower to "🌞"
    )
}
