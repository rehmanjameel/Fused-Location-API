package com.example.fused_location_api

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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
    private lateinit var mMap: GoogleMap
    private val locationPermissionCode = 1000
    private lateinit var lati: TextView
    private lateinit var longi: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Button to start/stop service
        findViewById<MaterialButton>(R.id.btnStartService).setOnClickListener {
        }
        startForegroundService()
        findViewById<MaterialButton>(R.id.btnStopService).setOnClickListener {
            stopForegroundService()
        }

        findViewById<MaterialButton>(R.id.detailsScreen).setOnClickListener{
            startActivity(Intent(this, LocationDetilsActivity::class.java))
        }

        lati = findViewById(R.id.latitudeValue)
        longi = findViewById(R.id.longitudeValue)

        // Check location permissions on app start
        if (!checkLocationPermissions()) {
            requestLocationPermissions()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (checkLocationPermissions()) {
            loadCurrentLocationOnMap()
        } else {
            requestLocationPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocationOnMap() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                lati.text = location.latitude.toString()
                longi.text = location.longitude.toString()

                mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun startForegroundService() {
        if (checkLocationPermissions()) {
            val intent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            Toast.makeText(this, "Location permissions are required to start the service.", Toast.LENGTH_SHORT).show()
            requestLocationPermissions()
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadCurrentLocationOnMap()
                startForegroundService()
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


