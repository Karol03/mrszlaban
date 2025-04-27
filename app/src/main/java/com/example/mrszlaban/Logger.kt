package com.example.mrszlaban

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object Logger {

    private const val FILE_NAME = "error_log.txt"
    private const val MAX_LINES = 500

    fun log(context: Context, message: String) {
        try {
            val logFile = File(context.getExternalFilesDir(null), FILE_NAME)

            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val newLogEntry = "[$timeStamp] $message"

            if (logFile.exists()) {
                val lines = logFile.readLines().toMutableList()
                lines.add(newLogEntry)

                val trimmedLines = if (lines.size > MAX_LINES) {
                    lines.takeLast(MAX_LINES - 100)
                } else {
                    lines
                }

                logFile.writeText(trimmedLines.joinToString("\n"))
            } else {
                logFile.writeText(newLogEntry)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logException(context: Context, exception: Exception) {
        try {
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            exception.printStackTrace(pw)
            val stackTrace = sw.toString()
            log(context, "Exception:\n$stackTrace")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun flush(context: Context) {
        try {
            val logFile = File(context.getExternalFilesDir(null), FILE_NAME)

            if (logFile.exists()) {
                logFile.outputStream().fd.sync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}