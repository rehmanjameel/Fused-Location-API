package com.example.fused_location_api

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var lastSavedTimestamp: Long = 0L


    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        startLocationUpdates()
        return START_STICKY
    }

    private fun setupLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10800000) // 5 seconds interval
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(36000) // Minimum interval between updates
            .setMaxUpdateDelayMillis(1000000)  // Maximum delay for batch updates
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    serviceScope.launch {
                        saveLocationToDatabase(location)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun saveLocationToDatabase(location: Location) {
        serviceScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val locationDao = AppDatabase.getDatabase(this@LocationService).locationDao()

                // Fetch the last saved location
                val lastLocation = locationDao.getLastLocation()

                // Save only if the location is not duplicate
                if (lastLocation == null || (currentTime - lastLocation.timestamp) > 10800000) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTime))
                    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(currentTime))

                    val newLocation = LocationEntity(
                        gpsId = 1001, // Example GPS ID
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = currentTime,
                        date = formattedDate,
                        time = formattedTime
                    )

                    // Insert the new location into the database
                    locationDao.insertLocation(newLocation)

                    Log.d("LocationService", "Location saved: $latitude, $longitude at $formattedTime")
                } else {
                    Log.d("LocationService", "Duplicate location ignored")
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Error saving location: ${e.message}")
            }
        }
    }

    private fun startForegroundNotification() {
        val notificationChannelId = "LocationServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Location Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking your location...")
            .setSmallIcon(R.drawable.baseline_location_pin_24)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceJob.cancel() // Cancel the CoroutineScope to prevent memory leaks
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


