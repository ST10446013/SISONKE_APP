package com.example.sisonke_app.incidents

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.models.Incident
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class IncidentActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_incident)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val spinner =
            findViewById<Spinner>(R.id.spinnerCategory)

        val categories = arrayOf(
            "Theft",
            "Burglary",
            "Robbery",
            "Suspicious Activity",
            "Assault",
            "Other"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spinner.adapter = adapter

        findViewById<Button>(
            R.id.btnSubmitIncident
        ).setOnClickListener {

            submitIncident()
        }
    }

    private fun submitIncident() {

        val category =
            findViewById<Spinner>(R.id.spinnerCategory)
                .selectedItem.toString()

        val description =
            findViewById<EditText>(
                R.id.etDescription
            ).text.toString().trim()

        if (description.isEmpty()) {

            findViewById<EditText>(
                R.id.etDescription
            ).error = "Enter a description"

            return
        }

        val userId = auth.currentUser?.uid

        if (userId == null) {

            Toast.makeText(
                this,
                "Please log in first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val incidentId =
            "SIS-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .uppercase()

        val incident = Incident(
            id = incidentId,
            userId = userId,
            category = category,
            description = description,
            dateTime = System.currentTimeMillis()
        )

        firestore.collection("incidents")
            .document(incidentId)
            .set(incident)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Report submitted: $incidentId",
                    Toast.LENGTH_LONG
                ).show()

                findViewById<EditText>(
                    R.id.etDescription
                ).text.clear()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to submit report",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}