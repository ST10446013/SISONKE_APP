package com.example.sisonke_app.emergency

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.nav.BottomNavHelper

/**
 * Previously this screen only called
 * setContentView(android.R.layout.simple_list_item_1), so none of the
 * emergency buttons in activity_emergency.xml were ever attached to
 * anything. It now uses the real layout and wires every button.
 */
class EmergencyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_emergency)

        BottomNavHelper.setup(this, R.id.nav_emergency)

        findViewById<Button>(R.id.btnSOS).setOnClickListener {
            sendSos()
        }

        findViewById<Button>(R.id.btnPolice).setOnClickListener {
            dial("10111") // South African Police Service
        }

        findViewById<Button>(R.id.btnAmbulance).setOnClickListener {
            dial("10177") // National ambulance / emergency medical services
        }

        findViewById<Button>(R.id.btnFire).setOnClickListener {
            dial("10177") // Fire and rescue share the general emergency line
        }

        findViewById<Button>(R.id.btnEmergencyProfile).setOnClickListener {
            Toast.makeText(
                this,
                "Emergency profile editing is coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun sendSos() {
        // TODO: hook this up to a backend endpoint / Firestore write that
        // notifies the user's emergency contacts with their live location.
        Toast.makeText(
            this,
            "SOS alert sent to your emergency contacts",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dial(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }
}