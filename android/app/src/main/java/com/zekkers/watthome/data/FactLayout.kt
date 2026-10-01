package com.zekkers.watthome.data

/**
 * Always-on house facts as separate chips. Independent of whether a Power Up
 * / Power Down window is in progress. Never includes last_action or the
 * weather_tomorrow.label essay.
 */
enum class FactKind {
    Overnight,
    Weather,
    Target1600,
    Peak
}

data class StatusFact(
    val kind: FactKind,
    val title: String,
    val value: String,
    val compact: String = value
)

object FactLayout {
    fun facts(status: HomeStatus?, style: ClockStyle = ClockStyle.DEFAULT): List<StatusFact> {
        if (status == null) return emptyList()
        return listOfNotNull(
            overnightFact(status.overnight, style),
            weatherFact(status.weatherTomorrow),
            targetFact(status.target1600Percent, style),
            peakFact(status.peakWindow, style)
        )
    }

    /** Compact tiles: overnight + weather stay up when no session is in-window. */
    fun headerFacts(status: HomeStatus?, style: ClockStyle = ClockStyle.DEFAULT): List<StatusFact> {
        if (status == null) return emptyList()
        return listOfNotNull(
            overnightFact(status.overnight, style),
            weatherFact(status.weatherTomorrow)
        )
    }

    fun overnightFact(overnight: Overnight?, style: ClockStyle = ClockStyle.DEFAULT): StatusFact? {
        val value = StatusFormatter.overnightLineOrNull(overnight, style) ?: return null
        val compact = StatusFormatter.overnightChipLine(overnight, style) ?: value
        return StatusFact(FactKind.Overnight, "Overnight", value, compact)
    }

    fun weatherFact(weather: WeatherTomorrow?): StatusFact? {
        val code = StatusFormatter.weatherCodeLabel(weather) ?: return null
        return StatusFact(FactKind.Weather, "Weather", code, code)
    }

    fun targetFact(percent: Int?, style: ClockStyle = ClockStyle.DEFAULT): StatusFact? {
        if (percent == null) return null
        val value = StatusFormatter.percent(percent)
        val clock = StatusFormatter.formatClock("16:00", style) ?: "16:00"
        return StatusFact(FactKind.Target1600, "$clock target", value, value)
    }

    fun peakFact(peakWindow: String?, style: ClockStyle = ClockStyle.DEFAULT): StatusFact? {
        val value = StatusFormatter.formatClockWindow(peakWindow, style) ?: return null
        return StatusFact(FactKind.Peak, "Peak window", value, value)
    }
}
