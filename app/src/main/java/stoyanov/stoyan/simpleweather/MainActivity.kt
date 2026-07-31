package stoyanov.stoyan.simpleweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val KEY_CURRENT_CITY = "KEY_CURRENT_CITY"
        private const val KEY_IS_CELSIUS = "KEY_IS_CELSIUS"
    }

    private lateinit var rootLayout: ScrollView
    private lateinit var etSearchCity: EditText
    private lateinit var btnSearch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private lateinit var tvCity: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvWeatherEmoji: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvFeelsLikeMinMax: TextView
    private lateinit var btnUnitToggle: Button
    private lateinit var btnHourlyForecast: Button
    private lateinit var btnDailyForecast: Button

    private lateinit var tvHumidity: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvSunTimes: TextView
    private lateinit var tvUpdatedTime: TextView

    private lateinit var chipSofia: Button
    private lateinit var chipPlovdiv: Button
    private lateinit var chipVarna: Button
    private lateinit var chipBurgas: Button

    private var currentWeatherData: WeatherData? = null
    private var currentCityQuery: String? = null
    private var isCelsius = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()

        // Restore state if rotated or recreated
        if (savedInstanceState != null) {
            currentCityQuery = savedInstanceState.getString(KEY_CURRENT_CITY)
            isCelsius = savedInstanceState.getBoolean(KEY_IS_CELSIUS, true)
        }

        if (!currentCityQuery.isNullOrEmpty()) {
            loadWeather(currentCityQuery!!)
        } else {
            // Initial launch: check GPS location or fallback to Sofia
            checkLocationAndLoadWeather()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_CITY, currentCityQuery)
        outState.putBoolean(KEY_IS_CELSIUS, isCelsius)
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.root_layout)
        etSearchCity = findViewById(R.id.et_search_city)
        btnSearch = findViewById(R.id.btn_search)
        progressBar = findViewById(R.id.progress_bar)
        tvError = findViewById(R.id.tv_error)

        tvCity = findViewById(R.id.tv_city)
        tvCountry = findViewById(R.id.tv_country)
        tvWeatherEmoji = findViewById(R.id.tv_weather_emoji)
        tvTemp = findViewById(R.id.tv_temp)
        tvDescription = findViewById(R.id.tv_description)
        tvFeelsLikeMinMax = findViewById(R.id.tv_feels_like_minmax)
        btnUnitToggle = findViewById(R.id.btn_unit_toggle)
        btnHourlyForecast = findViewById(R.id.btn_hourly_forecast)
        btnDailyForecast = findViewById(R.id.btn_daily_forecast)

        tvHumidity = findViewById(R.id.tv_humidity)
        tvPressure = findViewById(R.id.tv_pressure)
        tvWind = findViewById(R.id.tv_wind)
        tvSunTimes = findViewById(R.id.tv_sun_times)
        tvUpdatedTime = findViewById(R.id.tv_updated_time)

        chipSofia = findViewById(R.id.chip_sofia)
        chipPlovdiv = findViewById(R.id.chip_plovdiv)
        chipVarna = findViewById(R.id.chip_varna)
        chipBurgas = findViewById(R.id.chip_burgas)
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            performSearch()
        }

        etSearchCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        chipSofia.setOnClickListener { loadWeather("Sofia") }
        chipPlovdiv.setOnClickListener { loadWeather("Plovdiv") }
        chipVarna.setOnClickListener { loadWeather("Varna") }
        chipBurgas.setOnClickListener { loadWeather("Burgas") }

        btnUnitToggle.setOnClickListener {
            isCelsius = !isCelsius
            currentWeatherData?.let { renderWeatherUI(it) }
        }

        btnHourlyForecast.setOnClickListener {
            currentWeatherData?.let { showHourlyForecastDialog(it) }
        }

        btnDailyForecast.setOnClickListener {
            currentWeatherData?.let { showDailyForecastDialog(it) }
        }
    }

    private fun checkLocationAndLoadWeather() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loadDeviceLocationWeather()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun loadDeviceLocationWeather() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var location: Location? = null

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (location != null) {
                WeatherRepository.fetchWeatherByCoordinates(
                    this,
                    location.latitude,
                    location.longitude,
                    object : WeatherRepository.WeatherCallback {
                        override fun onSuccess(data: WeatherData) {
                            progressBar.visibility = View.GONE
                            currentWeatherData = data
                            currentCityQuery = data.city
                            renderWeatherUI(data)
                        }

                        override fun onError(errorMessage: String) {
                            loadWeather("Sofia")
                        }
                    }
                )
            } else {
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        locationManager.removeUpdates(this)
                        WeatherRepository.fetchWeatherByCoordinates(
                            this@MainActivity,
                            loc.latitude,
                            loc.longitude,
                            object : WeatherRepository.WeatherCallback {
                                override fun onSuccess(data: WeatherData) {
                                    progressBar.visibility = View.GONE
                                    currentWeatherData = data
                                    currentCityQuery = data.city
                                    renderWeatherUI(data)
                                }

                                override fun onError(errorMessage: String) {
                                    loadWeather("Sofia")
                                }
                            }
                        )
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper())
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper())
                } else {
                    loadWeather("Sofia")
                }
            }
        } catch (e: SecurityException) {
            loadWeather("Sofia")
        } catch (e: Exception) {
            loadWeather("Sofia")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadDeviceLocationWeather()
            } else {
                loadWeather("Sofia")
            }
        }
    }

    private fun performSearch() {
        val query = etSearchCity.text.toString().trim()
        if (query.isNotEmpty()) {
            hideKeyboard()
            loadWeather(query)
        }
    }

    private fun loadWeather(city: String) {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        WeatherRepository.fetchWeatherByCity(city, object : WeatherRepository.WeatherCallback {
            override fun onSuccess(data: WeatherData) {
                progressBar.visibility = View.GONE
                currentWeatherData = data
                currentCityQuery = city
                renderWeatherUI(data)
            }

            override fun onError(errorMessage: String) {
                progressBar.visibility = View.GONE
                tvError.text = errorMessage
                tvError.visibility = View.VISIBLE
            }
        })
    }

    private fun renderWeatherUI(data: WeatherData) {
        tvCity.text = data.city
        tvCountry.text = if (data.country.isNotEmpty()) data.country else ""
        tvWeatherEmoji.text = data.getWeatherEmoji()
        tvTemp.text = data.getTemp(isCelsius)
        tvDescription.text = data.description

        val feelsLikeStr = data.getFeelsLike(isCelsius)
        val minMaxStr = data.getTempMinMax(isCelsius)
        tvFeelsLikeMinMax.text = "Усеща се като $feelsLikeStr  |  $minMaxStr"

        tvHumidity.text = "${data.humidity}%"
        tvPressure.text = "${data.pressure} hPa"
        tvWind.text = "${data.windSpeed} m/s"
        tvSunTimes.text = "${data.sunrise} / ${data.sunset}"
        tvUpdatedTime.text = "Обновено: ${data.updatedOn}"

        btnUnitToggle.text = if (isCelsius) "°C  ➜  °F" else "°F  ➜  °C"

        updateBackground(data.condition)
    }

    private fun showHourlyForecastDialog(data: WeatherData) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundResource(R.drawable.glass_card_background)
        }

        val titleTv = TextView(this).apply {
            text = "🕒 Почасова прогноза — ${data.city}"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 0, 0, 32)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        dialogView.addView(titleTv)

        val scrollView = ScrollView(this)
        val itemsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (data.hourlyForecast.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Няма налична почасова прогноза."
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            itemsContainer.addView(emptyTv)
        } else {
            for (item in data.hourlyForecast) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16, 20, 16, 20)
                    weightSum = 4f
                }

                val timeTv = TextView(this).apply {
                    text = item.time
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val emojiTv = TextView(this).apply {
                    text = item.getEmoji()
                    textSize = 22f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f)
                }

                val tempTv = TextView(this).apply {
                    text = item.getTemp(isCelsius)
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f)
                }

                val descTv = TextView(this).apply {
                    text = item.description
                    setTextColor(Color.LTGRAY)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
                }

                rowLayout.addView(timeTv)
                rowLayout.addView(emojiTv)
                rowLayout.addView(tempTv)
                rowLayout.addView(descTv)

                itemsContainer.addView(rowLayout)

                // Divider line
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.parseColor("#30FFFFFF"))
                }
                itemsContainer.addView(divider)
            }
        }

        scrollView.addView(itemsContainer)
        dialogView.addView(scrollView)

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showDailyForecastDialog(data: WeatherData) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundResource(R.drawable.glass_card_background)
        }

        val titleTv = TextView(this).apply {
            text = "📅 7-дневна прогноза — ${data.city}"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 0, 0, 32)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        dialogView.addView(titleTv)

        val scrollView = ScrollView(this)
        val itemsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (data.dailyForecast.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Няма налична 7-дневна прогноза."
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            itemsContainer.addView(emptyTv)
        } else {
            for (item in data.dailyForecast) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16, 24, 16, 24)
                    weightSum = 4f
                }

                val dayTv = TextView(this).apply {
                    text = item.dayOfWeek
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
                }

                val emojiTv = TextView(this).apply {
                    text = item.getEmoji()
                    textSize = 22f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.6f)
                }

                val descTv = TextView(this).apply {
                    text = item.description
                    setTextColor(Color.LTGRAY)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
                }

                val tempTv = TextView(this).apply {
                    text = item.getMinMax(isCelsius)
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                rowLayout.addView(dayTv)
                rowLayout.addView(emojiTv)
                rowLayout.addView(descTv)
                rowLayout.addView(tempTv)

                itemsContainer.addView(rowLayout)

                // Divider line
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.parseColor("#30FFFFFF"))
                }
                itemsContainer.addView(divider)
            }
        }

        scrollView.addView(itemsContainer)
        dialogView.addView(scrollView)

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun updateBackground(condition: WeatherCondition) {
        val bgDrawableRes = when (condition) {
            WeatherCondition.CLEAR_DAY -> R.drawable.bg_sunny
            WeatherCondition.CLEAR_NIGHT -> R.drawable.bg_night
            WeatherCondition.CLOUDS -> R.drawable.bg_clouds
            WeatherCondition.RAIN -> R.drawable.bg_rain
            WeatherCondition.SNOW -> R.drawable.bg_snow
            WeatherCondition.THUNDERSTORM -> R.drawable.bg_thunder
            WeatherCondition.ATMOSPHERE -> R.drawable.bg_clouds
        }
        rootLayout.setBackgroundResource(bgDrawableRes)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let {
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}
