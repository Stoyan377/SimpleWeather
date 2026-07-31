package stoyanov.stoyan.simpleweather

enum class WeatherCondition {
    CLEAR_DAY,
    CLEAR_NIGHT,
    CLOUDS,
    RAIN,
    SNOW,
    THUNDERSTORM,
    ATMOSPHERE // Mist, Fog, Haze, etc.
}

data class HourlyItem(
    val time: String,
    val tempC: Double,
    val condition: WeatherCondition,
    val description: String
) {
    fun getTemp(isCelsius: Boolean): String {
        val temp = if (isCelsius) tempC else (tempC * 9 / 5) + 32
        return String.format("%.0f°", temp)
    }

    fun getEmoji(): String {
        return when (condition) {
            WeatherCondition.CLEAR_DAY -> "☀️"
            WeatherCondition.CLEAR_NIGHT -> "🌙"
            WeatherCondition.CLOUDS -> "☁️"
            WeatherCondition.RAIN -> "🌧️"
            WeatherCondition.SNOW -> "❄️"
            WeatherCondition.THUNDERSTORM -> "⛈️"
            WeatherCondition.ATMOSPHERE -> "🌫️"
        }
    }
}

data class DailyItem(
    val dayOfWeek: String,
    val tempMinC: Double,
    val tempMaxC: Double,
    val condition: WeatherCondition,
    val description: String
) {
    fun getMinMax(isCelsius: Boolean): String {
        val min = if (isCelsius) tempMinC else (tempMinC * 9 / 5) + 32
        val max = if (isCelsius) tempMaxC else (tempMaxC * 9 / 5) + 32
        return String.format("%.0f° / %.0f°", min, max)
    }

    fun getEmoji(): String {
        return when (condition) {
            WeatherCondition.CLEAR_DAY -> "☀️"
            WeatherCondition.CLEAR_NIGHT -> "🌙"
            WeatherCondition.CLOUDS -> "☁️"
            WeatherCondition.RAIN -> "🌧️"
            WeatherCondition.SNOW -> "❄️"
            WeatherCondition.THUNDERSTORM -> "⛈️"
            WeatherCondition.ATMOSPHERE -> "🌫️"
        }
    }
}

data class WeatherData(
    val city: String,
    val country: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val tempMinC: Double,
    val tempMaxC: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val description: String,
    val iconCode: String,
    val condition: WeatherCondition,
    val sunrise: String,
    val sunset: String,
    val updatedOn: String,
    val hourlyForecast: List<HourlyItem> = emptyList(),
    val dailyForecast: List<DailyItem> = emptyList()
) {
    fun getTemp(isCelsius: Boolean): String {
        val temp = if (isCelsius) tempC else (tempC * 9 / 5) + 32
        return String.format("%.1f°%s", temp, if (isCelsius) "C" else "F")
    }

    fun getFeelsLike(isCelsius: Boolean): String {
        val temp = if (isCelsius) feelsLikeC else (feelsLikeC * 9 / 5) + 32
        return String.format("%.1f°%s", temp, if (isCelsius) "C" else "F")
    }

    fun getTempMinMax(isCelsius: Boolean): String {
        val min = if (isCelsius) tempMinC else (tempMinC * 9 / 5) + 32
        val max = if (isCelsius) tempMaxC else (tempMaxC * 9 / 5) + 32
        return String.format("Мин: %.0f°  Макс: %.0f°", min, max)
    }

    fun getWeatherEmoji(): String {
        return when (condition) {
            WeatherCondition.CLEAR_DAY -> "☀️"
            WeatherCondition.CLEAR_NIGHT -> "🌙"
            WeatherCondition.CLOUDS -> "☁️"
            WeatherCondition.RAIN -> "🌧️"
            WeatherCondition.SNOW -> "❄️"
            WeatherCondition.THUNDERSTORM -> "⛈️"
            WeatherCondition.ATMOSPHERE -> "🌫️"
        }
    }
}
