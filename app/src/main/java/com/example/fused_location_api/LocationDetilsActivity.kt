package com.example.fused_location_api

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fused_location_api.databinding.ActivityLocationDetilsBinding

class LocationDetilsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocationDetilsBinding
    private lateinit var locationAdapter: LocationDetailsAdapter
    private val locationList = ArrayList<LocationEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationDetilsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.locDetailRV.layoutManager = LinearLayoutManager(this)

        binding.backButton.setOnClickListener{
            onBackPressed()
        }

        val locationList = AppDatabase.getDatabase(this).locationDao().getAllLocations()

        locationList.observe(this) { detailList ->
            locationAdapter = LocationDetailsAdapter(this, detailList)
            binding.locDetailRV.adapter = locationAdapter
        }

    }
}