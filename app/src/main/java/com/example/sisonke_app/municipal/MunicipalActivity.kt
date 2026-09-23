package com.example.sisonke_app.municipal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sisonke_app.R
import com.example.sisonke_app.nav.BottomNavHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Previously this screen only called
 * setContentView(android.R.layout.simple_list_item_1), so the municipal
 * report form in activity_municipal.xml was never shown. It now uses the
 * real layout and wires the category spinner, location button, photo
 * button and submit button.
 */
class MunicipalActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0
    private var hasPhoto: Boolean = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_municipal)

        BottomNavHelper.setup(this, R.id.nav_municipal)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val categories = arrayOf(
            "Water and Sanitation",
            "Electricity Outage",
            "Pothole / Road Damage",
            "Illegal Dumping",
            "Streetlight Fault",
            "Other"
        )

        findViewById<Spinner>(R.id.spinnerMunicipalCategory).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        findViewById<Button>(R.id.btnMunicipalLocation).setOnClickListener {
            useCurrentLocation()
        }

        findViewById<Button>(R.id.btnMunicipalPhoto).setOnClickListener {
            // TODO: launch an image picker and upload the photo; not
            // required for the prototype, so we just record that a photo
            // was attached.
            hasPhoto = true
            Toast.makeText(this, "Photo attached", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSubmitMunicipal).setOnClickListener {
            submitReport()
        }
    }

    private fun useCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude

                Toast.makeText(
                    this,
                    "Location captured (%.4f, %.4f)".format(currentLat, currentLng),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Couldn't get your location - try again outdoors",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            useCurrentLocation()
        }
    }

    private fun submitReport() {
        val category = findViewById<Spinner>(R.id.spinnerMunicipalCategory)
            .selectedItem.toString()

        val etDescription = findViewById<EditText>(R.id.etMunicipalDescription)
        val description = etDescription.text.toString().trim()

        if (description.isEmpty()) {
            etDescription.error = "Describe the problem"
            return
        }

        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val reportId = "MUN-" + UUID.randomUUID().toString().substring(0, 8).uppercase()

        val report = hashMapOf(
            "id" to reportId,
            "userId" to userId,
            "category" to category,
            "description" to description,
            "latitude" to currentLat,
            "longitude" to currentLng,
            "hasPhoto" to hasPhoto,
            "dateTime" to System.currentTimeMillis()
        )

        firestore.collection("municipal_reports")
            .document(reportId)
            .set(report)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Report submitted: $reportId",
                    Toast.LENGTH_LONG
                ).show()

                etDescription.text.clear()
                hasPhoto = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to submit report", Toast.LENGTH_LONG).show()
            }
    }
}