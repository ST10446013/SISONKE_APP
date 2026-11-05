package com.example.sisonke_app.municipal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.sisonke_app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
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
    private var selectedPhotoUri: Uri? = null
    private val httpClient = OkHttpClient()
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPhotoUri = uri
        Toast.makeText(this, if (uri != null) getString(R.string.photo_selected) else getString(R.string.no_photo_selected), Toast.LENGTH_SHORT).show()
    }

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

        val categories = resources.getStringArray(R.array.municipal_categories)


        findViewById<Spinner>(R.id.spinnerMunicipalCategory).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        findViewById<Button>(R.id.btnMunicipalLocation).setOnClickListener {
            useCurrentLocation()
        }

        findViewById<Button>(R.id.btnMunicipalPhoto).setOnClickListener {
            photoPicker.launch("image/*")
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
            etDescription.error = getString(R.string.err_describe_problem)
            return
        }

        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, getString(R.string.err_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        val reportId = "MUN-" + UUID.randomUUID().toString().substring(0, 8).uppercase()

        val report = hashMapOf<String, Any>(
            "id" to reportId,
            "userId" to userId,
            "category" to category,
            "description" to description,
            "latitude" to currentLat,
            "longitude" to currentLng,
            "hasPhoto" to (selectedPhotoUri != null),
            "dateTime" to System.currentTimeMillis()
        )

        val photoUri = selectedPhotoUri
        if (photoUri == null) {
            saveMunicipalReport(reportId, report, etDescription)
            return
        }

        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) {
            Toast.makeText(this, getString(R.string.err_storage_not_configured), Toast.LENGTH_LONG).show()
            return
        }

        auth.currentUser?.getIdToken(false)?.addOnSuccessListener { tokenResult ->
            val token = tokenResult.token
            if (token.isNullOrBlank()) {
                Toast.makeText(this, getString(R.string.err_account_verify), Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            Thread {
                try {
                    val mime = contentResolver.getType(photoUri) ?: "image/jpeg"
                    val ext = when (mime) {
                        "image/jpeg" -> "jpg"
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        else -> throw IOException("Choose a JPG, PNG or WEBP photo")
                    }
                    val bytes = contentResolver.openInputStream(photoUri)?.use { it.readBytes() }
                        ?: throw IOException("Could not read selected photo")
                    if (bytes.size > 5 * 1024 * 1024) throw IOException("Photo must be smaller than 5 MB")
                    val path = "$userId/municipal/$reportId.$ext"
                    val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                    val request = Request.Builder()
                        .url("$baseUrl/storage/v1/object/incident-images/$path")
                        .addHeader("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("x-upsert", "false")
                        .post(bytes.toRequestBody(mime.toMediaType()))
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Photo upload failed: ${response.code}")
                    }
                    report["imagePath"] = path
                    report["imageUrl"] = "$baseUrl/storage/v1/object/public/incident-images/$path"
                    runOnUiThread { saveMunicipalReport(reportId, report, etDescription) }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, e.message ?: getString(R.string.err_photo_upload), Toast.LENGTH_LONG).show() }
                }
            }.start()
        }?.addOnFailureListener {
            Toast.makeText(this, getString(R.string.err_account_verify), Toast.LENGTH_LONG).show()
        }
    }

    private fun saveMunicipalReport(reportId: String, report: HashMap<String, Any>, descriptionField: EditText) {
        firestore.collection("municipal_reports").document(reportId).set(report)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.municipal_report_submitted, reportId), Toast.LENGTH_LONG).show()
                descriptionField.text.clear()
                selectedPhotoUri = null
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.err_municipal_submit, it.message ?: ""), Toast.LENGTH_LONG).show()
            }
    }
}