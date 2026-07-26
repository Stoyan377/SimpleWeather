package stoyanov.stoyan.simpleweather

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

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
    private var isCelsius = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()

        // Load default city on app launch
        loadWeather("Sofia")
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
        tvFeelsLikeMinMax.text = "Feels like $feelsLikeStr  |  $minMaxStr"

        tvHumidity.text = "${data.humidity}%"
        tvPressure.text = "${data.pressure} hPa"
        tvWind.text = "${data.windSpeed} m/s"
        tvSunTimes.text = "${data.sunrise} / ${data.sunset}"
        tvUpdatedTime.text = "Updated: ${data.updatedOn}"

        btnUnitToggle.text = if (isCelsius) "°C  ➜  °F" else "°F  ➜  °C"

        updateBackground(data.condition)
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
