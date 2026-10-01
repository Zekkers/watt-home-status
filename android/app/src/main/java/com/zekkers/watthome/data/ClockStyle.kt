package com.zekkers.watthome.data

import android.content.Context

enum class ClockStyle {
    TwentyFourHour,
    TwelveHour;

    fun encode(): String = when (this) {
        TwentyFourHour -> "24"
        TwelveHour -> "12"
    }

    companion object {
        val DEFAULT = TwentyFourHour

        fun decode(raw: String?): ClockStyle = when (raw?.trim()?.lowercase()) {
            "12", "12h", "twelve", "twelve_hour" -> TwelveHour
            else -> TwentyFourHour
        }
    }
}

object ClockStylePrefs {
    private const val PREFS = "watt_home_clock_style"
    private const val KEY_STYLE = "style"

    fun read(context: Context): ClockStyle {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STYLE, null)
        return ClockStyle.decode(raw)
    }

    fun write(context: Context, style: ClockStyle) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STYLE, style.encode())
            .apply()
    }
}
