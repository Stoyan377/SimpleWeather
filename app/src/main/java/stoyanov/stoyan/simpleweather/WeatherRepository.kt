package stoyanov.stoyan.simpleweather

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object WeatherRepository {

    private const val GEO_URL = "https://geocoding-api.open-meteo.com/v1/search"
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface WeatherCallback {
        fun onSuccess(data: WeatherData)
        fun onError(errorMessage: String)
    }

    fun fetchWeatherByCity(cityQuery: String, callback: WeatherCallback) {
        executor.execute {
            try {
                // 1. Geocode City Name to Latitude & Longitude with Bulgarian language support
                val encodedCity = URLEncoder.encode(cityQuery.trim(), "UTF-8")
                val geoUrlString = "$GEO_URL?name=$encodedCity&count=1&language=bg"
                val geoJson = httpGet(geoUrlString) ?: throw Exception("Geocoding network error")

                val results = geoJson.optJSONArray("results")
                if (results == null || results.length() == 0) {
                    mainHandler.post {
                        callback.onError("Град '$cityQuery' не е намерен. Моля, проверете изписването.")
                    }
                    return@execute
                }

                val locationObj = results.getJSONObject(0)
                val lat = locationObj.getDouble("latitude")
                val lon = locationObj.getDouble("longitude")
                val rawName = locationObj.optString("name", cityQuery)
                val rawCountry = locationObj.optString("country", "")

                // Normalize Bulgarian City & Country Names
                val (cityName, countryName) = localizeCityAndCountry(cityQuery, rawName, rawCountry)

                // 2. Fetch Weather Data from Open-Meteo API
                val weatherData = fetchOpenMeteoForecast(lat, lon, cityName, countryName)

                mainHandler.post {
                    callback.onSuccess(weatherData)
                }

            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather by city", e)
                mainHandler.post {
                    callback.onError("Мрежова грешка. Моля, проверете интернет връзката си.")
                }
            }
        }
    }

    fun fetchWeatherByCoordinates(context: Context, lat: Double, lon: Double, callback: WeatherCallback) {
        executor.execute {
            try {
                var cityName = "Моето местоположение"
                var countryName = ""

                try {
                    val geocoder = Geocoder(context, Locale("bg"))
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        cityName = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Моето местоположение"
                        countryName = address.countryName ?: ""
                    }
                } catch (e: Exception) {
                    Log.w("WeatherRepository", "Android Geocoder failed, using fallback coordinates name", e)
                }

                val (finalCity, finalCountry) = localizeCityAndCountry(cityName, cityName, countryName)
                val weatherData = fetchOpenMeteoForecast(lat, lon, finalCity, finalCountry)

                mainHandler.post {
                    callback.onSuccess(weatherData)
                }

            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather by coordinates", e)
                mainHandler.post {
                    callback.onError("Грешка при зареждане на времето за текущата локация.")
                }
            }
        }
    }

    private fun localizeCityAndCountry(query: String, name: String, country: String): Pair<String, String> {
        val qLower = query.lowercase(Locale.ROOT)
        val nameLower = name.lowercase(Locale.ROOT)

        val bulgarianCity = when {
            qLower.contains("varna") || nameLower.contains("varna") || qLower.contains("варна") -> "Варна"
            qLower.contains("sofia") || nameLower.contains("sofia") || qLower.contains("софия") -> "София"
            qLower.contains("plovdiv") || nameLower.contains("plovdiv") || qLower.contains("пловдив") -> "Пловдив"
            qLower.contains("burgas") || nameLower.contains("burgas") || qLower.contains("бургас") -> "Бургас"
            qLower.contains("shabla") || nameLower.contains("shabla") || qLower.contains("шабла") -> "Шабла"
            else -> name
        }

        val bulgarianCountry = when (country.trim()) {
            "Bulgaria", "BG" -> "България"
            "United States", "US", "USA" -> "САЩ"
            "United Kingdom", "UK", "GB" -> "Обединено кралство"
            "Germany", "DE" -> "Германия"
            "France", "FR" -> "Франция"
            "Italy", "IT" -> "Италия"
            "Spain", "ES" -> "Испания"
            "Greece", "GR" -> "Гърция"
            "Turkey", "TR" -> "Турция"
            "Romania", "RO" -> "Румъния"
            else -> country
        }

        return Pair(bulgarianCity, bulgarianCountry)
    }

    private fun fetchOpenMeteoForecast(lat: Double, lon: Double, cityName: String, country: String): WeatherData {
        val forecastUrlString = "$FORECAST_URL?latitude=$lat&longitude=$lon" +
                "&current_weather=true" +
                "&hourly=temperature_2m,relativehumidity_2m,surface_pressure,apparent_temperature,weathercode" +
                "&daily=sunrise,sunset,temperature_2m_max,temperature_2m_min,weathercode" +
                "&timezone=auto"

        val weatherJson = httpGet(forecastUrlString) ?: throw Exception("Forecast network error")
        return parseOpenMeteoJson(cityName, country, weatherJson)
    }

    private fun httpGet(urlString: String): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                reader.close()
                JSONObject(stringBuilder.toString())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "HTTP Request failed for URL: $urlString", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseOpenMeteoJson(city: String, country: String, json: JSONObject): WeatherData {
        val currentWeather = json.getJSONObject("current_weather")
        val tempC = currentWeather.getDouble("temperature")
        val windKmh = currentWeather.getDouble("windspeed")
        val windSpeedMs = String.format(Locale.US, "%.1f", windKmh / 3.6).toDouble()
        val weatherCode = currentWeather.getInt("weathercode")
        val isDay = currentWeather.optInt("is_day", 1) == 1

        val hourly = json.optJSONObject("hourly")
        val feelsLikeArray = hourly?.optJSONArray("apparent_temperature")
        val feelsLikeC = if (feelsLikeArray != null && feelsLikeArray.length() > 0) feelsLikeArray.getDouble(0) else tempC

        val humidityArray = hourly?.optJSONArray("relativehumidity_2m")
        val humidity = if (humidityArray != null && humidityArray.length() > 0) humidityArray.getInt(0) else 50

        val pressureArray = hourly?.optJSONArray("surface_pressure")
        val pressure = if (pressureArray != null && pressureArray.length() > 0) pressureArray.getDouble(0).toInt() else 1013

        val daily = json.optJSONObject("daily")
        val tempMaxArray = daily?.optJSONArray("temperature_2m_max")
        val tempMinArray = daily?.optJSONArray("temperature_2m_min")
        val tempMaxC = if (tempMaxArray != null && tempMaxArray.length() > 0) tempMaxArray.getDouble(0) else tempC
        val tempMinC = if (tempMinArray != null && tempMinArray.length() > 0) tempMinArray.getDouble(0) else tempC

        val sunriseArray = daily?.optJSONArray("sunrise")
        val sunsetArray = daily?.optJSONArray("sunset")

        val sunriseStr = formatIsoTime(sunriseArray?.optString(0))
        val sunsetStr = formatIsoTime(sunsetArray?.optString(0))

        val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val updatedOnStr = dateTimeFormat.format(Date())

        val (description, condition) = mapWeatherCode(weatherCode, isDay)

        // Parse Hourly Forecast
        val hourlyList = mutableListOf<HourlyItem>()
        if (hourly != null) {
            val timeArr = hourly.optJSONArray("time")
            val tempArr = hourly.optJSONArray("temperature_2m")
            val codeArr = hourly.optJSONArray("weathercode")

            if (timeArr != null && tempArr != null && codeArr != null) {
                val count = Math.min(24, timeArr.length())
                for (i in 0 until count) {
                    val isoTime = timeArr.getString(i)
                    val formattedTime = formatIsoTime(isoTime)
                    val hTemp = tempArr.getDouble(i)
                    val hCode = codeArr.getInt(i)
                    val (hDesc, hCond) = mapWeatherCode(hCode, true)
                    hourlyList.add(HourlyItem(formattedTime, hTemp, hCond, hDesc))
                }
            }
        }

        // Parse Daily Forecast
        val dailyList = mutableListOf<DailyItem>()
        if (daily != null) {
            val timeArr = daily.optJSONArray("time")
            val maxArr = daily.optJSONArray("temperature_2m_max")
            val minArr = daily.optJSONArray("temperature_2m_min")
            val codeArr = daily.optJSONArray("weathercode")

            if (timeArr != null && maxArr != null && minArr != null && codeArr != null) {
                val count = timeArr.length()
                for (i in 0 until count) {
                    val rawDate = timeArr.getString(i)
                    val dayLabel = formatDailyLabel(rawDate, i)
                    val dMax = maxArr.getDouble(i)
                    val dMin = minArr.getDouble(i)
                    val dCode = codeArr.getInt(i)
                    val (dDesc, dCond) = mapWeatherCode(dCode, true)
                    dailyList.add(DailyItem(dayLabel, dMin, dMax, dCond, dDesc))
                }
            }
        }

        return WeatherData(
            city = city,
            country = country,
            tempC = tempC,
            feelsLikeC = feelsLikeC,
            tempMinC = tempMinC,
            tempMaxC = tempMaxC,
            humidity = humidity,
            pressure = pressure,
            windSpeed = windSpeedMs,
            description = description,
            iconCode = "",
            condition = condition,
            sunrise = sunriseStr,
            sunset = sunsetStr,
            updatedOn = updatedOnStr,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList
        )
    }

    private fun formatDailyLabel(rawDate: String, index: Int): String {
        if (index == 0) return "Днес"
        if (index == 1) return "Утре"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(rawDate)
            if (date != null) {
                val outSdf = SimpleDateFormat("EEE (dd.MM)", Locale("bg"))
                val formatted = outSdf.format(date)
                formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("bg")) else it.toString() }
            } else {
                rawDate
            }
        } catch (e: Exception) {
            rawDate
        }
    }

    private fun formatIsoTime(isoString: String?): String {
        if (isoString == null || isoString.isEmpty()) return "--:--"
        return try {
            val parts = isoString.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                "${timeParts[0]}:${timeParts[1]}"
            } else {
                isoString
            }
        } catch (e: Exception) {
            "--:--"
        }
    }

    private fun mapWeatherCode(code: Int, isDay: Boolean): Pair<String, WeatherCondition> {
        return when (code) {
            0 -> Pair("Ясно небе", if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT)
            1 -> Pair("Предимно ясно", if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT)
            2 -> Pair("Частична облачност", WeatherCondition.CLOUDS)
            3 -> Pair("Значителна облачност", WeatherCondition.CLOUDS)
            45, 48 -> Pair("Мъгла", WeatherCondition.ATMOSPHERE)
            51, 53, 55 -> Pair("Лек ръмеж", WeatherCondition.RAIN)
            56, 57 -> Pair("Леден ръмеж", WeatherCondition.SNOW)
            61, 63, 65 -> Pair("Дъжд", WeatherCondition.RAIN)
            66, 67 -> Pair("Замръзващ дъжд", WeatherCondition.SNOW)
            71, 73, 75, 77 -> Pair("Снеговалеж", WeatherCondition.SNOW)
            80, 81, 82 -> Pair("Краткотраен дъжд", WeatherCondition.RAIN)
            85, 86 -> Pair("Краткотраен сняг", WeatherCondition.SNOW)
            95, 96, 99 -> Pair("Гръмотевична буря", WeatherCondition.THUNDERSTORM)
            else -> Pair("Ясно", if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT)
        }
    }
}
