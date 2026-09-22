package com.example.sisonke_app.models

data class Incident(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val dateTime: Long = 0L,
    val imageUrl: String = ""
)