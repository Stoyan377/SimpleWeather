package stoyanov.stoyan.simpleweather

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
                // 1. Geocode City Name to Latitude & Longitude
                val encodedCity = URLEncoder.encode(cityQuery.trim(), "UTF-8")
                val geoUrlString = "$GEO_URL?name=$encodedCity&count=1"
                val geoJson = httpGet(geoUrlString) ?: throw Exception("Geocoding network error")

                val results = geoJson.optJSONArray("results")
                if (results == null || results.length() == 0) {
                    mainHandler.post {
                        callback.onError("City '$cityQuery' not found. Please check spelling.")
                    }
                    return@execute
                }

                val locationObj = results.getJSONObject(0)
                val lat = locationObj.getDouble("latitude")
                val lon = locationObj.getDouble("longitude")
                val cityName = locationObj.optString("name", cityQuery)
                val country = locationObj.optString("country", "")

                // 2. Fetch Weather Data from Open-Meteo API
                val forecastUrlString = "$FORECAST_URL?latitude=$lat&longitude=$lon" +
                        "&current_weather=true" +
                        "&hourly=relativehumidity_2m,surface_pressure,apparent_temperature" +
                        "&daily=sunrise,sunset,temperature_2m_max,temperature_2m_min" +
                        "&timezone=auto"

                val weatherJson = httpGet(forecastUrlString) ?: throw Exception("Forecast network error")
                val weatherData = parseOpenMeteoJson(cityName, country, weatherJson)

                mainHandler.post {
                    callback.onSuccess(weatherData)
                }

            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                mainHandler.post {
                    callback.onError("Network error. Please check your internet connection.")
                }
            }
        }
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
            updatedOn = updatedOnStr
        )
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
            0 -> Pair("Clear sky", if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT)
            1 -> Pair("Mainly clear", if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT)
            2 -> Pair("Partly cloudy", WeatherCondition.CLOUDS)
            3 -> Pair("Overcast", WeatherCondition.CLOUDS)
            45, 48 -> Pair("Foggy", WeatherCondition.ATMOSPHERE)
            51, 53, 55 -> Pair("Drizzle", WeatherCondition.RAIN)
            56, 57 -> Pair("Freezing Drizzle", WeatherCondition.SNOW)
            61, 63, 65 -> Pair("Rain", WeatherCondition.RAIN)
            66, 67 -> Pair("Freezing Rain", WeatherCondition.SNOW)
            71, 73, 75, 77 -> Pair("Snow fall", WeatherCondition.SNOW)
            80, 81, 82 -> Pair("Rain showers", WeatherCondition.RAIN)
            85, 86 -> Pair("Snow showers", WeatherCondition.SNOW)
            95, 96, 99 -> Pair("Thunderstorm", WeatherCondition.THUNDERSTORM)
            else -> Pair("Clear", if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT)
        }
    }
}
