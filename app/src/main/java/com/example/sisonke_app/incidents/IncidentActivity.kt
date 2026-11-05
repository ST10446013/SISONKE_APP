
package com.example.sisonke_app.incidents

import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.widget.TextView
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.sisonke_app.BuildConfig
import com.example.sisonke_app.R
import com.example.sisonke_app.models.Incident
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

class IncidentActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var hasLocation: Boolean = false

    private val httpClient = OkHttpClient()

    private var selectedImageUri: Uri? = null
    private var isSubmitting = false
    private lateinit var submitButton: Button

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 410
    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {
                selectedImageUri = uri

                Toast.makeText(
                    this,
                    "Photo selected successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incident)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        submitButton = findViewById(R.id.btnSubmitIncident)

        val spinner = findViewById<Spinner>(R.id.spinnerCategory)

        val categories = resources.getStringArray(R.array.incident_categories)


        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spinner.adapter = adapter

        findViewById<Button>(R.id.btnPhoto).setOnClickListener {
            imagePicker.launch("image/*")
        }

        findViewById<Button>(R.id.btnLocation).setOnClickListener {
            captureLocation()
        }

        submitButton.setOnClickListener {
            submitIncident()
        }
    }

    private fun submitIncident() {

        if (isSubmitting) return

        val category =
            findViewById<Spinner>(R.id.spinnerCategory)
                .selectedItem.toString()

        val description =
            findViewById<EditText>(R.id.etDescription)
                .text.toString().trim()

        if (description.isEmpty()) {
            findViewById<EditText>(R.id.etDescription)
                .error = getString(R.string.err_enter_description)
            return
        }

        val user = auth.currentUser

        if (user == null) {
            showToast(getString(R.string.err_login_required))
            return
        }

        val incidentId = "SIS-" +
                UUID.randomUUID().toString()
                    .substring(0, 8).uppercase()

        val incident = Incident(
            id = incidentId,
            userId = user.uid,
            category = category,
            description = description,
            dateTime = System.currentTimeMillis(),
            latitude = currentLatitude,
            longitude = currentLongitude
        )

        isSubmitting = true
        submitButton.isEnabled = false
        submitButton.text = getString(R.string.submitting)

        val imageUri = selectedImageUri

        if (imageUri == null) {
            saveIncident(incident)
            return
        }

        if (BuildConfig.SUPABASE_URL.isBlank() ||
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()
        ) {
            resetSubmitState()
            showToast(getString(R.string.err_storage_not_configured))
            return
        }

        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->

                val firebaseToken = tokenResult.token

                if (firebaseToken.isNullOrBlank()) {
                    resetSubmitState()
                    showToast(getString(R.string.err_token_unavailable))
                    return@addOnSuccessListener
                }

                uploadPhoto(
                    imageUri = imageUri,
                    userId = user.uid,
                    incident = incident,
                    firebaseToken = firebaseToken
                )
            }
            .addOnFailureListener {
                resetSubmitState()
                showToast(getString(R.string.err_account_verify))
            }
    }

    private fun uploadPhoto(
        imageUri: Uri,
        userId: String,
        incident: Incident,
        firebaseToken: String
    ) {

        Thread {

            try {
                val mimeType =
                    contentResolver.getType(imageUri) ?: "image/jpeg"

                val extension = when (mimeType) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> throw IOException(
                        "Please select a JPG, PNG or WEBP image"
                    )
                }

                val imageBytes = contentResolver
                    .openInputStream(imageUri)
                    ?.use { it.readBytes() }
                    ?: throw IOException("Could not read selected photo")

                if (imageBytes.size > 5 * 1024 * 1024) {
                    throw IOException("Photo must be smaller than 5 MB")
                }

                val storagePath =
                    "$userId/incidents/${incident.id}.$extension"

                val baseUrl = BuildConfig.SUPABASE_URL
                    .trimEnd('/')

                val uploadUrl =
                    "$baseUrl/storage/v1/object/incident-images/$storagePath"

                val requestBody = imageBytes.toRequestBody(
                    mimeType.toMediaType()
                )

                val request = Request.Builder()
                    .url(uploadUrl)
                    .addHeader(
                        "apikey",
                        BuildConfig.SUPABASE_PUBLISHABLE_KEY
                    )
                    .addHeader(
                        "Authorization",
                        "Bearer $firebaseToken"
                    )
                    .addHeader("x-upsert", "false")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).enqueue(
                    object : Callback {

                        override fun onFailure(
                            call: Call,
                            e: IOException
                        ) {
                            runOnUiThread {
                                resetSubmitState()
                                showToast(
                                    "Photo upload failed: ${e.message}"
                                )
                            }
                        }

                        override fun onResponse(
                            call: Call,
                            response: Response
                        ) {
                            response.use {

                                if (it.isSuccessful) {

                                    val publicImageUrl = "$baseUrl/storage/v1/object/public/incident-images/$storagePath"
                                    val updatedIncident = incident.copy(
                                        imagePath = storagePath,
                                        imageUrl = publicImageUrl
                                    )

                                    saveIncident(updatedIncident)

                                } else {

                                    val errorMessage = it.body
                                        ?.string()
                                        ?.take(250)
                                        ?: getString(R.string.unknown_upload_error)

                                    runOnUiThread {
                                        resetSubmitState()
                                        showToast(
                                            getString(R.string.err_storage_upload, errorMessage)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )

            } catch (e: Exception) {

                runOnUiThread {
                    resetSubmitState()
                    showToast(
                        e.message ?: "Could not upload photo"
                    )
                }
            }

        }.start()
    }

    private fun saveIncident(incident: Incident) {
        firestore.collection("incidents")
            .document(incident.id)
            .set(incident)
            .addOnSuccessListener {
                resetSubmitState()
                findViewById<EditText>(R.id.etDescription).text.clear()
                selectedImageUri = null
                currentLatitude = 0.0
                currentLongitude = 0.0
                hasLocation = false
                findViewById<TextView>(R.id.tvLocation).text = getString(R.string.location_not_selected)
                Toast.makeText(this, getString(R.string.incident_submitted, incident.id), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                resetSubmitState()
                Toast.makeText(this, getString(R.string.err_incident_submit, exception.message ?: ""), Toast.LENGTH_LONG).show()
                android.util.Log.e("SISONKE_FIRESTORE", "Failed to save incident", exception)
            }
    }

    private fun resetSubmitState() {
        isSubmitting = false
        runOnUiThread {
            submitButton.isEnabled = true
            submitButton.text = getString(R.string.submit_incident)
        }
    }

    private fun captureLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    hasLocation = true
                    findViewById<TextView>(R.id.tvLocation).text =
                        "Location captured: %.5f, %.5f".format(currentLatitude, currentLongitude)
                } else {
                    showToast(getString(R.string.err_location_unavailable))
                }
            }.addOnFailureListener {
                showToast(getString(R.string.err_location_fetch, it.message ?: ""))
            }
        } catch (e: SecurityException) {
            showToast(getString(R.string.err_location_permission))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            captureLocation()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
