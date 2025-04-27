package com.example.mrszlaban

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class LogsActivity : AppCompatActivity() {

    private var clickCount = 0
    private var firstClickTime = 0L
    private val clickThreshold = 7
    private val timeWindowMillis = 8000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val textView = TextView(this)
        scrollView.addView(textView)
        setContentView(scrollView)

        val logContent = readLogs(this)
        textView.text = logContent

        textView.setOnClickListener {
            handleClicks(textView)
        }
    }

    private fun handleClicks(textView: TextView) {
        val currentTime = SystemClock.elapsedRealtime()

        if (firstClickTime == 0L || currentTime - firstClickTime > timeWindowMillis) {
            firstClickTime = currentTime
            clickCount = 1
        } else {
            clickCount++
        }

        if (clickCount >= clickThreshold) {
            // Wyczyszcz plik
            clearLogs(this)
            textView.text = "Logi zostały wyczyszczone."
            clickCount = 0
            firstClickTime = 0L
        }
    }

    private fun readLogs(context: Context): String {
        return try {
            val logFile = File(context.getExternalFilesDir(null), "error_log.txt")
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "Brak pliku logów."
            }
        } catch (e: Exception) {
            "Błąd podczas odczytu logów: ${e.message}"
        }
    }

    private fun clearLogs(context: Context) {
        try {
            val logFile = File(context.getExternalFilesDir(null), "error_log.txt")
            if (logFile.exists()) {
                logFile.writeText("")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
