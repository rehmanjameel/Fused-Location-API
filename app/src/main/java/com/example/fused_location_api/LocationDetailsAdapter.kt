package com.example.fused_location_api

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationDetailsAdapter(private val context: Context,
    private val locList: List<LocationEntity>):
    RecyclerView.Adapter<LocationDetailsAdapter.ViewHolder>() {
    // Holds the views for adding it to image and text
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val id: TextView = itemView.findViewById(R.id.id)
        val long: TextView = itemView.findViewById(R.id.longitude)
        val latitude: TextView = itemView.findViewById(R.id.latitude)
        val time: TextView = itemView.findViewById(R.id.time)
        val date: TextView = itemView.findViewById(R.id.date)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.location_details_layout, parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val details = locList[position]

        holder.id.text = details.id.toString()
        holder.long.text = details.longitude.toString()
        holder.latitude.text = details.latitude.toString()
        holder.date.text = details.date
        holder.time.text = details.time

    }

    override fun getItemCount(): Int {
        return locList.size
    }
}