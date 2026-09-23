package com.example.sisonke_app.models

data class IncidentResponse(
    val id: String,
    val userId: String,
    val category: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val dateTime: String,
    val imageUrl: String
)