package com.example.sisonke_app.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.Context
import com.google.firebase.firestore.Query
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.auth.LoginActivity
import com.example.sisonke_app.emergency.EmergencyActivity
import com.example.sisonke_app.incidents.IncidentActivity
import com.example.sisonke_app.incidents.IncidentReportsActivity
import com.example.sisonke_app.map.CrimeMapActivity
import com.example.sisonke_app.municipal.MunicipalActivity
import com.example.sisonke_app.nav.BottomNavHelper
import com.example.sisonke_app.settings.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val weatherClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        BottomNavHelper.setup(this, R.id.nav_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        loadWeather()
        registerForCommunityAlerts()
        listenForIncidentStatistics()
        syncMessagingToken()

        findViewById<android.view.View>(R.id.btnIncidentReports).setOnClickListener {
            startActivity(Intent(this, IncidentReportsActivity::class.java))
        }

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)

        val userId = auth.currentUser?.uid

        if (userId != null) {

            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->

                    val name = document.getString("name")

                    if (!name.isNullOrEmpty()) {
                        tvWelcome.text = getString(R.string.welcome_user, name)
                    }
                }
        }

        findViewById<android.view.View>(R.id.btnSettings).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(R.id.btnReportIncident).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    IncidentActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(R.id.btnCrimeMap).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CrimeMapActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(R.id.btnEmergency).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    EmergencyActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(R.id.btnMunicipal).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MunicipalActivity::class.java
                )
            )
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {

            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }
    }

    /**
     * Fetches live weather from Open-Meteo (no API key required).
     * Uses the device's last known location when permission is already granted;
     * otherwise it uses Durban as the South African fallback location.
     */
    private fun loadWeather() {
        val weatherView = findViewById<TextView>(R.id.tvWeather)
        weatherView.text = getString(R.string.weather_loading)
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) fetchWeather(location.latitude, location.longitude, "Your location")
                        else fetchWeather(-29.8587, 31.0218, "Durban")
                    }
                    .addOnFailureListener { fetchWeather(-29.8587, 31.0218, "Durban") }
            } catch (_: SecurityException) {
                fetchWeather(-29.8587, 31.0218, "Durban")
            }
        } else {
            fetchWeather(-29.8587, 31.0218, "Durban")
        }
    }

    private fun fetchWeather(latitude: Double, longitude: Double, place: String) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=precipitation_probability_max&forecast_days=1&timezone=auto"
        val request = Request.Builder().url(url).get().build()
        weatherClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    findViewById<TextView>(R.id.tvWeather).text = getString(R.string.weather_unavailable_connection)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnUiThread { findViewById<TextView>(R.id.tvWeather).text = getString(R.string.weather_unavailable) }
                        return
                    }
                    try {
                        val json = JSONObject(it.body?.string().orEmpty())
                        val current = json.getJSONObject("current")
                        val temp = current.getDouble("temperature_2m").toInt()
                        val feels = current.getDouble("apparent_temperature").toInt()
                        val wind = current.getDouble("wind_speed_10m").toInt()
                        val rain = json.optJSONObject("daily")
                            ?.optJSONArray("precipitation_probability_max")?.optInt(0, 0) ?: 0
                        val description = weatherDescription(current.optInt("weather_code", -1))
                        val result = "$place: ${temp}°C • $description • Feels ${feels}°C • Rain ${rain}% • Wind ${wind} km/h"
                        runOnUiThread { findViewById<TextView>(R.id.tvWeather).text = result }
                    } catch (_: Exception) {
                        runOnUiThread { findViewById<TextView>(R.id.tvWeather).text = getString(R.string.weather_data_unavailable) }
                    }
                }
            }
        })
    }

    private fun weatherDescription(code: Int): String = getString(when (code) {
        0 -> R.string.weather_clear
        1, 2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        51, 53, 55, 56, 57 -> R.string.weather_drizzle
        61, 63, 65, 66, 67 -> R.string.weather_rain
        71, 73, 75, 77 -> R.string.weather_snow
        80, 81, 82 -> R.string.weather_rain_showers
        85, 86 -> R.string.weather_snow_showers
        95, 96, 99 -> R.string.weather_thunderstorm
        else -> R.string.weather_conditions_unavailable
    })

    private fun registerForCommunityAlerts() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 620)
        }
        val notificationsEnabled = getSharedPreferences("sisonke_prefs", MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)
        val language = getSharedPreferences("sisonke_prefs", MODE_PRIVATE)
            .getString("preferred_language", "English")
        val topic = when (language) {
            "Afrikaans" -> "community-alerts-af"
            "isiZulu" -> "community-alerts-zu"
            else -> "community-alerts-en"
        }
        if (notificationsEnabled) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
        } else {
            listOf("community-alerts-en", "community-alerts-af", "community-alerts-zu")
                .forEach { FirebaseMessaging.getInstance().unsubscribeFromTopic(it) }
        }
    }


    private fun listenForIncidentStatistics() {
        firestore.collection("incidents")
            .addSnapshotListener(this) { snapshot, error ->
                val stats = findViewById<TextView>(R.id.tvIncidentStats)
                if (error != null || snapshot == null) {
                    stats.text = getString(R.string.incidents_load_error)
                    return@addSnapshotListener
                }
                stats.text = resources.getQuantityString(
                    R.plurals.incident_report_count, snapshot.size(), snapshot.size()
                )
            }
    }

    private fun syncMessagingToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
            firestore.collection("users").document(uid)
                .update("fcmToken", token)
                .addOnFailureListener {
                    firestore.collection("users").document(uid)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                }
        }
    }

}