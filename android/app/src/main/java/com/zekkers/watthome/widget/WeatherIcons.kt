package com.zekkers.watthome.widget

import com.zekkers.watthome.R
import com.zekkers.watthome.data.WeatherTomorrow

object WeatherIcons {
    fun drawableRes(weather: WeatherTomorrow?): Int? {
        if (weather == null) return null
        val code = weather.code?.lowercase()?.replace('-', '_')?.replace(' ', '_')
        return when (code) {
            "clear", "clear_sky", "sunny", "fair", "sun", "mainly_clear" -> R.drawable.ic_weather_clear
            "partly_cloudy", "partlycloudy", "few_clouds", "scattered_clouds" -> R.drawable.ic_weather_partly_cloudy
            "cloudy", "overcast", "broken_clouds" -> R.drawable.ic_weather_cloudy
            "rain", "drizzle", "showers", "rain_showers", "light_rain", "moderate_rain",
            "heavy_rain", "freezing_rain" -> R.drawable.ic_weather_rain
            "snow", "snow_showers", "sleet", "light_snow" -> R.drawable.ic_weather_snow
            "thunderstorm", "thunder", "storm" -> R.drawable.ic_weather_storm
            "fog", "mist", "haze" -> R.drawable.ic_weather_fog
            null, "" -> if (weather.label.isNullOrBlank()) null else R.drawable.ic_weather_cloudy
            else -> R.drawable.ic_weather_cloudy
        }
    }
}
