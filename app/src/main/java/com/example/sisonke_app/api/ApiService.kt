package com.example.sisonke_app.api

import com.example.sisonke_app.models.IncidentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("api/incidents")
    suspend fun getIncidents():
            Response<List<IncidentResponse>>

    @POST("api/incidents")
    suspend fun createIncident(
        @Body incident: IncidentResponse
    ): Response<IncidentResponse>
}