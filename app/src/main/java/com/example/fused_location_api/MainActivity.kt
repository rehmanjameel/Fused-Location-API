package com.example.fused_location_api

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var latitudeT: TextView
    private lateinit var longitudeT: TextView
    private lateinit var detailScreen: MaterialButton
    private val locationPermissionCode = 1000
    private lateinit var mMap: GoogleMap

    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate called")

        latitudeT = findViewById(R.id.latitudeValue)
        longitudeT = findViewById(R.id.longitudeValue)
        detailScreen = findViewById(R.id.detailsScreen)

        title = "Location App"

        detailScreen.setOnClickListener{
            startActivity(Intent(this, LocationDetilsActivity::class.java))
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    onLocationChanged(location)
                }
            }
        }

        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        if (checkLocationPermissions()) {
            initializeMap()
        } else {
            requestLocationPermissions()
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onStart() {
        super.onStart()
        if (checkLocationPermissions()) {
            startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermissions()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationChanged(location)
            } else {
                Toast.makeText(this, "Unable to fetch last location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineLocation == PackageManager.PERMISSION_GRANTED ||
                coarseLocation == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            locationPermissionCode
        )
    }

    @SuppressLint("SetTextI18n")
    private fun onLocationChanged(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val currentTime = System.currentTimeMillis()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTime))
        val formattedTime = timeFormat.format(Date(currentTime))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lastLocation = AppDatabase.getDatabase(this@MainActivity).locationDao().getLastLocation()
                if (lastLocation == null || (currentTime - lastLocation.timestamp) > 180000) {
                    val newLocation = LocationEntity(
                        gpsId = 1001,
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = currentTime,
                        date = formattedDate,
                        time = formattedTime
                    )
                    AppDatabase.getDatabase(this@MainActivity).locationDao().insertLocation(newLocation)

                    withContext(Dispatchers.Main) {
                        formatCoordinates(latitude, longitude)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        formatCoordinates(lastLocation.latitude, lastLocation.longitude)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error accessing database: ${e.message}")
            }
        }

        if (::mMap.isInitialized) {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap.clear()
            mMap.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .title("You are here")
            )
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }
    }

    private fun formatCoordinates(latitude: Double, longitude: Double) {
        val latDirection = if (latitude >= 0) "N" else "S"
        val lonDirection = if (longitude >= 0) "E" else "W"

        val formattedLat = "%.4f° $latDirection".format(Math.abs(latitude))
        val formattedLon = "%.4f° $lonDirection".format(Math.abs(longitude))
        latitudeT.text = " $formattedLat"
        longitudeT.text = " $formattedLon"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (checkLocationPermissions()) {
            mMap.isMyLocationEnabled = true
        } else {
            requestLocationPermissions()
        }

        val defaultLocation = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeMap()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

