package com.example.mrszlaban

import android.Manifest
import android.content.*
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var currentLocationText: TextView
    private lateinit var lastCallText: TextView

    private lateinit var latEdit: EditText
    private lateinit var lonEdit: EditText
    private lateinit var rangeEdit: EditText
    private lateinit var rangeAEdit: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var t1Edit: EditText
    private lateinit var t2Edit: EditText
    private lateinit var t3Edit: EditText
    private val PREFS_NAME = "MrSzlabanPrefs"
    private lateinit var prefs: SharedPreferences
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "lastLat" || key == "lastLon" || key == "lastCallAt") {
            updateLocationAndCallTimeFromPrefs()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        latEdit = findViewById(R.id.latEdit)
        lonEdit = findViewById(R.id.lonEdit)
        rangeEdit = findViewById(R.id.rangeEdit)
        rangeAEdit = findViewById(R.id.rangeAEdit)
        phoneEdit = findViewById(R.id.phoneEdit)
        t1Edit = findViewById(R.id.t1Edit)
        t2Edit = findViewById(R.id.t2Edit)
        t3Edit = findViewById(R.id.t3Edit)

        currentLocationText = findViewById(R.id.currentLocationText)
        lastCallText = findViewById(R.id.lastCallText)

        findViewById<Button>(R.id.showLogsButton).setOnClickListener {
            val intent = Intent(this, LogsActivity::class.java)
            startActivity(intent)
        }

        loadSettings()

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveSettings()
            startMonitorService()
        }

        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE
        ), 0)

        startMonitorService()

        Logger.log(this, "[MainActivity] Start service")
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun updateLocationAndCallTimeFromPrefs() {
        try {
            val lat = prefs.getString("lastLat", null)
            val lon = prefs.getString("lastLon", null)
            val lastCallAtMillis = prefs.getLong("lastCallAt", 0L)

            if (lat != null && lon != null) {
                currentLocationText.text = "Aktualna lokalizacja:\nLat: $lat\nLon: $lon"
            } else {
                currentLocationText.text = "Brak danych lokalizacji"
            }

            if (lastCallAtMillis > 0) {
                val sdf =
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val formattedTime = sdf.format(java.util.Date(lastCallAtMillis))
                lastCallText.text = "Ostatnie połączenie:\n$formattedTime"
            } else {
                lastCallText.text = "Brak połączeń"
            }
        } catch (e: Exception) {
            Logger.logException(this, e)
        }
    }

    private fun saveSettings() {
        try {
            val e = prefs.edit()
            e.putString("lat", latEdit.text.toString())
            e.putString("lon", lonEdit.text.toString())
            e.putString("range", rangeEdit.text.toString())
            e.putString("rangeA", rangeAEdit.text.toString())
            e.putString("phone", phoneEdit.text.toString())
            e.putString("t1", t1Edit.text.toString())
            e.putString("t2", t2Edit.text.toString())
            e.putString("t3", t3Edit.text.toString())
            e.apply()
        } catch (e: Exception) {
            Logger.logException(this, e)
        }
    }

    private fun loadSettings() {
        try {
            latEdit.setText(prefs.getString("lat", ""))
            lonEdit.setText(prefs.getString("lon", ""))
            rangeEdit.setText(prefs.getString("range", ""))
            rangeAEdit.setText(prefs.getString("rangeA", ""))
            phoneEdit.setText(prefs.getString("phone", ""))
            t1Edit.setText(prefs.getString("t1", ""))
            t2Edit.setText(prefs.getString("t2", ""))
            t3Edit.setText(prefs.getString("t3", ""))
            updateLocationAndCallTimeFromPrefs()
        } catch (e: Exception) {
            Logger.logException(this, e)
        }
    }

    private fun startMonitorService() {
        try {
            val serviceIntent = Intent(this, MonitorService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            Logger.logException(this, e)
        }
    }
}