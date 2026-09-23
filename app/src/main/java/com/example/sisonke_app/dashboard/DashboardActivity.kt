package com.example.sisonke_app.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.auth.LoginActivity
import com.example.sisonke_app.emergency.EmergencyActivity
import com.example.sisonke_app.incidents.IncidentActivity
import com.example.sisonke_app.map.CrimeMapActivity
import com.example.sisonke_app.municipal.MunicipalActivity
import com.example.sisonke_app.nav.BottomNavHelper
import com.example.sisonke_app.settings.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        BottomNavHelper.setup(this, R.id.nav_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)

        val userId = auth.currentUser?.uid

        if (userId != null) {

            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->

                    val name = document.getString("name")

                    if (!name.isNullOrEmpty()) {
                        tvWelcome.text = "Welcome, $name"
                    }
                }
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }

        findViewById<Button>(R.id.btnReportIncident).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    IncidentActivity::class.java
                )
            )
        }

        findViewById<Button>(R.id.btnCrimeMap).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CrimeMapActivity::class.java
                )
            )
        }

        findViewById<Button>(R.id.btnEmergency).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    EmergencyActivity::class.java
                )
            )
        }

        findViewById<Button>(R.id.btnMunicipal).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MunicipalActivity::class.java
                )
            )
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {

            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }
    }
}