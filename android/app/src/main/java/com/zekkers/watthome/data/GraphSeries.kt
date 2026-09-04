package com.zekkers.watthome.data

import android.content.Context

data class GraphSeriesSelection(
    val solar: Boolean = true,
    val battery: Boolean = true,
    val house: Boolean = false,
    val grid: Boolean = false,
    val soc: Boolean = false
) {
    fun encode(): String = buildList {
        if (solar) add(ID_SOLAR)
        if (battery) add(ID_BATTERY)
        if (house) add(ID_HOUSE)
        if (grid) add(ID_GRID)
        if (soc) add(ID_SOC)
    }.joinToString(",")

    fun anyPower(): Boolean = solar || battery || house || grid

    fun any(): Boolean = anyPower() || soc

    companion object {
        const val ID_SOLAR = "solar"
        const val ID_BATTERY = "battery"
        const val ID_HOUSE = "house"
        const val ID_GRID = "grid"
        const val ID_SOC = "soc"

        val DEFAULT = GraphSeriesSelection()
        val WIDGET_COMPACT = GraphSeriesSelection(
            solar = true,
            battery = true,
            house = false,
            grid = false,
            soc = false
        )

        fun decode(raw: String?): GraphSeriesSelection {
            if (raw == null) return DEFAULT
            val parts = raw.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            return GraphSeriesSelection(
                solar = ID_SOLAR in parts,
                battery = ID_BATTERY in parts,
                house = ID_HOUSE in parts,
                grid = ID_GRID in parts,
                soc = ID_SOC in parts
            )
        }
    }
}

object GraphSeriesStyle {
    const val SOLAR = 0xFFF9A825
    const val BATTERY_CHARGE = 0xFF81C784
    const val BATTERY_DISCHARGE = 0xFFEF6C00
    const val HOUSE = 0xFF90CAF9
    const val GRID = 0xFFCE93D8
    const val SOC_PLOT = 0xFFE8F5E9
    const val SOC_UI = 0xFF2E7D32
    const val BATTERY_UI = 0xFF81C784
}

object GraphSeriesPrefs {
    private const val PREFS = "watt_home_graph_series"
    private const val KEY_ENABLED = "enabled"

    fun read(context: Context): GraphSeriesSelection {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENABLED, null)
        return GraphSeriesSelection.decode(raw)
    }

    fun write(context: Context, selection: GraphSeriesSelection) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENABLED, selection.encode())
            .apply()
    }
}
