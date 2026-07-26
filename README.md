# 🌤️ SimpleWeather (Просто Време)

**SimpleWeather** е съвременно, стилно Android приложение за прогноза за времето, написано на **Kotlin** и **AndroidX**, с **Glassmorphism UI** дизайн, търсене на градове по целия свят, автоматично адаптивни фонове и пълна поддръжка на български език.

[![GitHub release](https://img.shields.io/github/v/release/Stoyan377/SimpleWeather?color=brightgreen)](https://github.com/Stoyan377/SimpleWeather/releases/tag/v1.0.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8.20-blue.svg)](https://kotlinlang.org/)
[![AndroidX](https://img.shields.io/badge/AndroidX-1.6.1-green.svg)](https://developer.android.com/jetpack/androidx)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ Основни Характеристики (Features)

- 🔍 **Глобално търсене на градове**: Вълнуваща възможност за търсене на прогноза за всеки град по света в реално време.
- 📍 **Бързи бутони за любими градове**: Бърз избор с едно докосване за **София**, **Пловдив**, **Varna** и **Бургас**.
- 🎨 **Glassmorphic & Адаптивен Дизайн**: 
  - Дизайн с полупрозрачни стъклени карти.
  - Автоматична промяна на градиентния фон според метеорологичната обстановка (Слънчево, Нощ, Облачно, Дъжд, Сняг, Гръмотевична буря).
- 🌡️ **Превключване на мерни единици (°C / °F)**: Бърза смяна между Целзий и Фаренхайт с едно натискане.
- 📊 **Подробна метеорологична информация**:
  - Температура и усещане (*Feels like*)
  - Минимална и максимална дневна температура
  - Влажност на въздуха (%)
  - Атмосферно налягане (hPa)
  - Скорост на вятъра (m/s)
  - Часове на изгрев и залез
- 🌐 **Open-Meteo API**: Използва 100% безплатен, отворен и бърз геокодинг и прогноза без необходимост от API ключ.
- 🇧🇬 **Пълна поддръжка на български език**: Всички бутони, описания, съобщения за грешки и индикатори са преведени на български.

---

## 📸 Скрийншот (Screenshot)

<p align="center">
  <img src="app/src/main/weather-web.png" alt="SimpleWeather Preview" width="320"/>
</p>

---

## 📲 Изтегляне (Download APK)

Можете да изтеглите готовия **APK** файл директно от секцията [Releases](https://github.com/Stoyan377/SimpleWeather/releases/tag/v1.0.0):

➡️ [**Изтегли SimpleWeather-v1.0.apk**](https://github.com/Stoyan377/SimpleWeather/releases/download/v1.0.0/SimpleWeather-v1.0.apk)

---

## 🛠️ Технологичен Стек (Tech Stack)

- **Език**: Kotlin
- **Архитектура**: Clean Repository pattern + Android Concurrency Executors
- **UI Framework**: AndroidX, Material Design 3, ConstraintLayout
- **Weather API**: Open-Meteo REST API & Geocoding API
- **Минимална версия на Android**: Android 5.0 (API 21+)
- **Целева версия на Android**: Android 13 (API 33)

---

## 🚀 Сглобяване от изходен код (Build Instructions)

1. Клонирайте хранилището:
   ```bash
   git clone https://github.com/Stoyan377/SimpleWeather.git
   cd SimpleWeather
   ```
2. Отворете проекта в **Android Studio**.
3. Направете **Sync Project with Gradle Files**.
4. Стартирайте на емулатор или физическо устройство.
