package com.zekkers.watthome.widget

import com.zekkers.watthome.R
import com.zekkers.watthome.data.ActionKind

object ActionIcons {
    fun drawableRes(kind: ActionKind): Int = when (kind) {
        ActionKind.PowerUp -> R.drawable.ic_power_up_badge
        ActionKind.Overnight -> R.drawable.ic_overnight_badge
        ActionKind.PowerDown -> R.drawable.ic_power_down_badge
        ActionKind.LivePower -> R.drawable.ic_weather_clear
        ActionKind.Other -> R.drawable.ic_action_badge
    }
}
