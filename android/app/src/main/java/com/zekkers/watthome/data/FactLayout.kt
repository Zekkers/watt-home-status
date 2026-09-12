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
    fun facts(status: HomeStatus?): List<StatusFact> {
        if (status == null) return emptyList()
        return listOfNotNull(
            overnightFact(status.overnight),
            weatherFact(status.weatherTomorrow),
            targetFact(status.target1600Percent),
            peakFact(status.peakWindow)
        )
    }

    /** Compact tiles: overnight + weather stay up when no session is in-window. */
    fun headerFacts(status: HomeStatus?): List<StatusFact> {
        if (status == null) return emptyList()
        return listOfNotNull(
            overnightFact(status.overnight),
            weatherFact(status.weatherTomorrow)
        )
    }

    fun overnightFact(overnight: Overnight?): StatusFact? {
        val value = StatusFormatter.overnightLineOrNull(overnight) ?: return null
        val compact = StatusFormatter.overnightChipLine(overnight) ?: value
        return StatusFact(FactKind.Overnight, "Overnight", value, compact)
    }

    fun weatherFact(weather: WeatherTomorrow?): StatusFact? {
        val code = StatusFormatter.weatherCodeLabel(weather) ?: return null
        return StatusFact(FactKind.Weather, "Weather", code, code)
    }

    fun targetFact(percent: Int?): StatusFact? {
        if (percent == null) return null
        val value = StatusFormatter.percent(percent)
        return StatusFact(FactKind.Target1600, "16:00 target", value, value)
    }

    fun peakFact(peakWindow: String?): StatusFact? {
        val value = peakWindow?.takeIf { it.isNotBlank() } ?: return null
        return StatusFact(FactKind.Peak, "Peak window", value, value)
    }
}
