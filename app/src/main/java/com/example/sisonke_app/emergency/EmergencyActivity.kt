package com.example.sisonke_app.emergency

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

class EmergencyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object { private const val SOS_LOCATION_PERMISSION = 711 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)
        BottomNavHelper.setup(this, R.id.nav_emergency)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<Button>(R.id.btnSOS).setOnClickListener { sendSos() }
        findViewById<Button>(R.id.btnPolice).setOnClickListener { dial("10111") }
        findViewById<Button>(R.id.btnAmbulance).setOnClickListener { dial("10177") }
        findViewById<Button>(R.id.btnFire).setOnClickListener { dial("112") }
        findViewById<Button>(R.id.btnEmergencyProfile).setOnClickListener { editEmergencyProfile() }
    }

    private fun sendSos() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, getString(R.string.err_login_required), Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), SOS_LOCATION_PERMISSION)
            return
        }
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val alertId = "SOS-" + UUID.randomUUID().toString().take(8).uppercase()
                val payload = hashMapOf(
                    "id" to alertId,
                    "userId" to user.uid,
                    "email" to user.email,
                    "latitude" to (location?.latitude ?: 0.0),
                    "longitude" to (location?.longitude ?: 0.0),
                    "locationAvailable" to (location != null),
                    "createdAt" to System.currentTimeMillis(),
                    "status" to "ACTIVE"
                )
                firestore.collection("sos_alerts").document(alertId).set(payload)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.sos_recorded), Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, getString(R.string.err_send_sos, it.message ?: ""), Toast.LENGTH_LONG).show()
                    }
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.err_access_location, it.message ?: ""), Toast.LENGTH_LONG).show()
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, getString(R.string.err_sos_location_permission), Toast.LENGTH_LONG).show()
        }
    }

    private fun editEmergencyProfile() {
        val userId = auth.currentUser?.uid ?: return
        val fields = listOf(
            getString(R.string.emergency_contact_name) to "emergencyContactName",
            getString(R.string.emergency_contact_phone) to "emergencyContactPhone",
            getString(R.string.allergies) to "allergies",
            getString(R.string.medical_information) to "medicalInfo"
        )
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 8, 48, 0)
        }
        val inputs = fields.map { (hint, _) ->
            EditText(this).apply { this.hint = hint; setSingleLine(hint != getString(R.string.medical_information) && hint != getString(R.string.allergies)) }
                .also { container.addView(it) }
        }
        firestore.collection("users").document(userId).get().addOnSuccessListener { doc ->
            fields.forEachIndexed { index, pair -> inputs[index].setText(doc.getString(pair.second).orEmpty()) }
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.emergency_profile))
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val values = fields.mapIndexed { index, pair -> pair.second to inputs[index].text.toString().trim() }.toMap()
                firestore.collection("users").document(userId)
                    .set(values, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener { Toast.makeText(this, getString(R.string.emergency_profile_saved), Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, getString(R.string.err_save_profile, it.message ?: ""), Toast.LENGTH_LONG).show() }
            }.show()
    }

    private fun dial(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SOS_LOCATION_PERMISSION && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) sendSos()
    }
}
