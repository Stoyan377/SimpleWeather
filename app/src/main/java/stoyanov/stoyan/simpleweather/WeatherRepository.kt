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

    private const val API_KEY = "48256fe1883d13b6ec7e564c68863ff9"
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface WeatherCallback {
        fun onSuccess(data: WeatherData)
        fun onError(errorMessage: String)
    }

    fun fetchWeatherByCity(cityQuery: String, callback: WeatherCallback) {
        executor.execute {
            try {
                val encodedCity = URLEncoder.encode(cityQuery.trim(), "UTF-8")
                val urlString = "$BASE_URL?q=$encodedCity&units=metric&appid=$API_KEY"
                val url = URL(urlString)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    reader.close()

                    val json = JSONObject(stringBuilder.toString())
                    val weatherData = parseWeatherJson(json)

                    mainHandler.post {
                        callback.onSuccess(weatherData)
                    }
                } else if (responseCode == 404) {
                    mainHandler.post {
                        callback.onError("City not found. Please check spelling.")
                    }
                } else {
                    mainHandler.post {
                        callback.onError("Failed to load weather data (HTTP $responseCode)")
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                mainHandler.post {
                    callback.onError("Network error. Please check your internet connection.")
                }
            }
        }
    }

    private fun parseWeatherJson(json: JSONObject): WeatherData {
        val weatherArray = json.getJSONArray("weather")
        val weatherObject = weatherArray.getJSONObject(0)
        val mainObject = json.getJSONObject("main")
        val sysObject = json.getJSONObject("sys")
        val windObject = json.optJSONObject("wind")

        val weatherId = weatherObject.getInt("id")
        val rawDescription = weatherObject.getString("description")
        val capitalizedDescription = rawDescription.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }

        val tempC = mainObject.getDouble("temp")
        val feelsLikeC = mainObject.optDouble("feels_like", tempC)
        val tempMinC = mainObject.optDouble("temp_min", tempC)
        val tempMaxC = mainObject.optDouble("temp_max", tempC)

        val humidity = mainObject.getInt("humidity")
        val pressure = mainObject.getInt("pressure")
        val windSpeed = windObject?.optDouble("speed", 0.0) ?: 0.0

        val cityName = json.getString("name")
        val country = sysObject.optString("country", "")

        val sunriseTime = sysObject.optLong("sunrise", 0L) * 1000
        val sunsetTime = sysObject.optLong("sunset", 0L) * 1000
        val currentTime = System.currentTimeMillis()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sunriseStr = if (sunriseTime > 0) timeFormat.format(Date(sunriseTime)) else "--:--"
        val sunsetStr = if (sunsetTime > 0) timeFormat.format(Date(sunsetTime)) else "--:--"

        val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val updatedOnStr = dateTimeFormat.format(Date(json.optLong("dt", 0L) * 1000))

        val isNight = currentTime < sunriseTime || currentTime >= sunsetTime
        val condition = determineCondition(weatherId, isNight)

        return WeatherData(
            city = cityName,
            country = country,
            tempC = tempC,
            feelsLikeC = feelsLikeC,
            tempMinC = tempMinC,
            tempMaxC = tempMaxC,
            humidity = humidity,
            pressure = pressure,
            windSpeed = windSpeed,
            description = capitalizedDescription,
            iconCode = weatherObject.optString("icon", "01d"),
            condition = condition,
            sunrise = sunriseStr,
            sunset = sunsetStr,
            updatedOn = updatedOnStr
        )
    }

    private fun determineCondition(weatherId: Int, isNight: Boolean): WeatherCondition {
        return when (weatherId) {
            in 200..232 -> WeatherCondition.THUNDERSTORM
            in 300..321, in 500..531 -> WeatherCondition.RAIN
            in 600..622 -> WeatherCondition.SNOW
            in 701..781 -> WeatherCondition.ATMOSPHERE
            800 -> if (isNight) WeatherCondition.CLEAR_NIGHT else WeatherCondition.CLEAR_DAY
            in 801..804 -> WeatherCondition.CLOUDS
            else -> if (isNight) WeatherCondition.CLEAR_NIGHT else WeatherCondition.CLEAR_DAY
        }
    }
}
