package com.example.mrszlaban

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import java.io.DataOutputStream

class MonitorService : Service(), LocationListener {
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var serviceStateListener: PhoneStateListener
    private lateinit var locationManager: LocationManager
    private lateinit var prefs: android.content.SharedPreferences
    private var lastCallTime = 0L
    private var noChargingStartTime = 1L
    private var speedLimitExceededAt = 0L
    private var isInRange = -2
    private var firstStartupDone = false

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences("MrSzlabanPrefs", Context.MODE_PRIVATE)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val notification = NotificationCompat.Builder(this)
            .setContentTitle("MrSzlaban działa...")
            .setContentText("Monitoring GPS i ładowania")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60_000L, 10f, this)
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        serviceStateListener = object : PhoneStateListener() {
            override fun onServiceStateChanged(serviceState: android.telephony.ServiceState?) {
                super.onServiceStateChanged(serviceState)

                if (serviceState?.state == android.telephony.ServiceState.STATE_IN_SERVICE) {
                    if (!firstStartupDone) {
                        firstStartupDone = true

                        val t1Seconds = (prefs.getString("t1", "0")?.toIntOrNull() ?: 0)
                        if (t1Seconds != 0) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                firstStartup()
                            }, t1Seconds * 1000L)
                        }
                    }
                }
            }
        }

        telephonyManager.listen(serviceStateListener, PhoneStateListener.LISTEN_SERVICE_STATE)

        Handler(Looper.getMainLooper()).postDelayed({
            checkChargingStatus()
        }, 10000)

        Logger.log(this, "==============================")
        Logger.log(this, "[MonitorService] Start service")
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        telephonyManager.listen(serviceStateListener, PhoneStateListener.LISTEN_NONE)
    }

    private fun checkChargingStatus() {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_PLUGGED_USB || status == BatteryManager.BATTERY_PLUGGED_AC

        if (!charging) {
            if (noChargingStartTime == 0L)
            {
                Logger.log(this, "[MonitorService] Device is not charging")
                noChargingStartTime = System.currentTimeMillis()
            }
            val t3Minutes = (prefs.getString("t3", "10")?.toIntOrNull() ?: 10)
            if ((System.currentTimeMillis() - noChargingStartTime) > t3Minutes * 60_000) {
                powerOffDevice()
            }
        } else if (noChargingStartTime > 0L) {
            Logger.log(this, "[MonitorService] Device is charging")
            noChargingStartTime = 0L
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkChargingStatus()
        }, 10_000)
    }

    private fun powerOffDevice() {
        try {
            Logger.log(this, "[MonitorService] Power off device")
            Logger.flush(this)
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("reboot -p\n")
            os.flush()
            os.close()
            process.waitFor()
        } catch (e: Exception) {
            Logger.logException(this, e)
        }
    }

    override fun onLocationChanged(location: Location) {
        val lat = prefs.getString("lat", "0")?.toDoubleOrNull() ?: 0.0
        val lon = prefs.getString("lon", "0")?.toDoubleOrNull() ?: 0.0
        val lastLat = prefs.getString("lastLat", "0")?.toDoubleOrNull() ?: 0.0
        val lastLon = prefs.getString("lastLon", "0")?.toDoubleOrNull() ?: 0.0
        val range = prefs.getString("range", "10")?.toFloatOrNull() ?: 10f
        val lastGpsPosTime = prefs.getLong("lastGpsPosTime", 0L)
        val gpsTimestamp = System.currentTimeMillis()
        val speedLimitInArea = 30f // m/s - if in the last 'speedLimitTimeRange' millis GPS based speed
                                   // was greater than this there must be some drift and value is invalid
        val e = prefs.edit()
        e.putLong("lastGpsPosTime", gpsTimestamp)
        e.putString("lastLat", location.latitude.toString())
        e.putString("lastLon", location.longitude.toString())
        e.apply()

        val lastLocation = Location("")
        lastLocation.latitude = lastLat
        lastLocation.longitude = lastLon

        val distance = location.distanceTo(lastLocation)
        val time = (gpsTimestamp - lastGpsPosTime) * 0.001f
        val speed = distance/time

        if (speed >= speedLimitInArea)
        {
            // phone moves too fast - some error or definitely not in area
            speedLimitExceededAt = System.currentTimeMillis()
        }

        val target = Location("")
        target.latitude = lat
        target.longitude = lon

        callIfInRange(location, target, distance, speed, range)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        return
    }

    private fun firstStartup() {
        val lat = prefs.getString("lat", "0")?.toDoubleOrNull() ?: 0.0
        val lon = prefs.getString("lon", "0")?.toDoubleOrNull() ?: 0.0
        val lastLat = prefs.getString("lastLat", "0")?.toDoubleOrNull() ?: 0.0
        val lastLon = prefs.getString("lastLon", "0")?.toDoubleOrNull() ?: 0.0
        val range = prefs.getString("range", "10")?.toFloatOrNull() ?: 10f
        val rangeA = prefs.getString("rangeA", "0.25")?.toFloatOrNull() ?: 0.25f

        val lastLocation = Location("")
        lastLocation.latitude = lastLat
        lastLocation.longitude = lastLon

        val target = Location("")
        target.latitude = lat
        target.longitude = lon

        Logger.log(this, "[MonitorService] First startup call")
        callIfInRange(lastLocation, target, 0f, 0f, range * range * rangeA)
    }

    private fun callIfInRange(currentLocation: Location, targetLocation: Location, distance: Float, speed: Float, range: Float) {
        val speedLimitTimeRange = 20L   // seconds, time range when speed must be lower than 'speedLimitInArea'

        if (lastCallTime != 0L)
        {
            val t2Minutes = (prefs.getString("t2", "5")?.toIntOrNull() ?: 5)
            val lastCallSecondsAgo = (System.currentTimeMillis() - lastCallTime) / 1000
            if (lastCallSecondsAgo < t2Minutes * 60) {
                Logger.log(this, "[MonitorService] Last call was $lastCallSecondsAgo seconds ago, within the allowed $t2Minutes minutes — preventing too many calls")
                return
            }
        }

        val targetDistance = currentLocation.distanceTo(targetLocation)
        val targetDistanceInRangeResult = targetDistance.compareTo(range)
        if (targetDistanceInRangeResult == isInRange) {
            return
        }

        isInRange = targetDistanceInRangeResult
        if (isInRange == 1) {
            Logger.log(this, "[MonitorService] New position is OUTSIDE the range $range [m] (current distance $targetDistance [m], change: $distance [m])")
            return
        }
        Logger.log(this, "[MonitorService] New position is INSIDE the range $range [m] (current distance $targetDistance [m], change: $distance [m])")

        val timePassedSpeedExceed = (System.currentTimeMillis() - speedLimitExceededAt) / 1000L
        if (timePassedSpeedExceed < speedLimitTimeRange)
        {
            Logger.log(this, "[MonitorService] Speed exceeded $timePassedSpeedExceed [s] ago, (${currentLocation.latitude}, ${currentLocation.longitude}) change: $distance [m], current speed: $speed [m/s], prevent call")
            return
        }

        lastCallTime = System.currentTimeMillis()
        makePhoneCall()
    }

    private fun makePhoneCall() {
        try {
            val phone = prefs.getString("phone", "") ?: return
            Logger.log(this, "[MonitorService] Call phone number $phone")
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = android.net.Uri.parse("tel:$phone")
            callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(callIntent)

            val now = System.currentTimeMillis()
            prefs.edit().putLong("lastCallAt", now).apply()
        } catch (e: Exception) {
            Logger.logException(this, e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}