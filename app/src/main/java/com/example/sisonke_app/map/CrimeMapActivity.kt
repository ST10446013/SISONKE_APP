package com.example.sisonke_app.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sisonke_app.R
import com.example.sisonke_app.nav.BottomNavHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Shows the user's current location on a Google Map and plots reported
 * incidents (pulled from Firestore) as markers.
 *
 * Previously this screen only called
 * setContentView(android.R.layout.simple_list_item_1) - a placeholder
 * Android system layout - so it never showed a map. It now uses the real
 * activity_crime_map layout (a SupportMapFragment) and requests the
 * device's location at runtime.
 */
class CrimeMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100

        // Fallback camera position if location permission isn't granted yet.
        private val DEFAULT_LOCATION = LatLng(-29.8587, 31.0218) // Durban, South Africa
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crime_map)

        BottomNavHelper.setup(this, R.id.nav_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        firestore = FirebaseFirestore.getInstance()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 11f)
            )

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        loadIncidentMarkers()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (!hasLocationPermission()) return

        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val current = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15f))
            }
        }
    }

    private fun loadIncidentMarkers() {
        firestore.collection("incidents")
            .get()
            .addOnSuccessListener { snapshot ->

                for (doc in snapshot.documents) {

                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    val category = doc.getString("category") ?: "Incident"

                    if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title(category)
                        )
                    }
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                enableMyLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied - showing default area",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}