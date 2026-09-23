package com.example.sisonke_app.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.auth.LoginActivity
import com.example.sisonke_app.nav.BottomNavHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Previously this screen only called
 * setContentView(android.R.layout.simple_list_item_1), so none of the
 * profile/preference controls in activity_settings.xml ever appeared. It
 * now loads the user's profile from Firestore, lets them edit and save it,
 * and wires the language spinner, notification switch and logout button.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val prefsName = "sisonke_prefs"
    private val keyNotifications = "notifications_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        BottomNavHelper.setup(this, R.id.nav_settings)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupLanguageSpinner()
        setupNotificationSwitch()
        loadProfile()

        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            saveProfile()
        }

        findViewById<Button>(R.id.btnEmergencyProfile).setOnClickListener {
            Toast.makeText(
                this,
                "Emergency profile editing is coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<Button>(R.id.btnLogoutSettings).setOnClickListener {
            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("English", "isiZulu", "Afrikaans", "isiXhosa", "Sesotho")

        findViewById<Spinner>(R.id.spinnerLanguage).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )
    }

    private fun setupNotificationSwitch() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)

        switchNotifications.isChecked = prefs.getBoolean(keyNotifications, true)

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(keyNotifications, isChecked).apply()
        }
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return

        findViewById<EditText>(R.id.etSettingsEmail).setText(user.email ?: "")

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name")

                if (!name.isNullOrEmpty()) {
                    findViewById<EditText>(R.id.etSettingsName).setText(name)
                }
            }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val etName = findViewById<EditText>(R.id.etSettingsName)
        val name = etName.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Enter your name"
            return
        }

        firestore.collection("users")
            .document(userId)
            .set(mapOf("name" to name), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_LONG).show()
            }
    }
}