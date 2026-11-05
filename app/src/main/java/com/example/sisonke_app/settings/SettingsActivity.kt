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
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.auth.LoginActivity
import com.example.sisonke_app.nav.BottomNavHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

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
            editEmergencyProfile()
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
        val languages = arrayOf("English", "isiZulu", "Afrikaans")

        val spinner = findViewById<Spinner>(R.id.spinnerLanguage)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )
        val savedLanguage = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString("preferred_language", "English") ?: "English"
        val savedIndex = languages.indexOf(savedLanguage).coerceAtLeast(0)
        spinner.setSelection(savedIndex)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selected = languages[position]
                getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
                    .putString("preferred_language", selected).apply()
                auth.currentUser?.uid?.let { uid ->
                    firestore.collection("users").document(uid)
                        .set(mapOf("language" to selected), com.google.firebase.firestore.SetOptions.merge())
                }
                val topic = when (selected) {
                    "Afrikaans" -> "community-alerts-af"
                    "isiZulu" -> "community-alerts-zu"
                    else -> "community-alerts-en"
                }
                if (getSharedPreferences(prefsName, Context.MODE_PRIVATE).getBoolean(keyNotifications, true)) {
                    listOf("community-alerts-en", "community-alerts-af", "community-alerts-zu")
                        .filter { it != topic }
                        .forEach { FirebaseMessaging.getInstance().unsubscribeFromTopic(it) }
                    FirebaseMessaging.getInstance().subscribeToTopic(topic)
                }
                val languageTag = when (selected) {
                    "isiZulu" -> "zu"
                    "Afrikaans" -> "af"
                    else -> "en"
                }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
            }
        }
    }

    private fun setupNotificationSwitch() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)

        switchNotifications.isChecked = prefs.getBoolean(keyNotifications, true)

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(keyNotifications, isChecked).apply()
            val selectedLanguage = prefs.getString("preferred_language", "English")
            val topic = when (selectedLanguage) {
                "Afrikaans" -> "community-alerts-af"
                "isiZulu" -> "community-alerts-zu"
                else -> "community-alerts-en"
            }
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
            } else {
                listOf("community-alerts-en", "community-alerts-af", "community-alerts-zu")
                    .forEach { FirebaseMessaging.getInstance().unsubscribeFromTopic(it) }
            }
            auth.currentUser?.uid?.let { uid ->
                firestore.collection("users").document(uid)
                    .set(mapOf("notificationsEnabled" to isChecked), com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return

        findViewById<EditText>(R.id.etSettingsEmail).setText(user.email ?: "")
        findViewById<android.widget.TextView>(R.id.tvSettingsProfileName)
            .text = user.displayName ?: user.email ?: getString(R.string.community_member)

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name")

                if (!name.isNullOrEmpty()) {
                    findViewById<EditText>(R.id.etSettingsName).setText(name)
                    findViewById<android.widget.TextView>(R.id.tvSettingsProfileName).text = name
                }
            }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, getString(R.string.err_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        val etName = findViewById<EditText>(R.id.etSettingsName)
        val name = etName.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = getString(R.string.err_enter_name)
            return
        }

        firestore.collection("users")
            .document(userId)
            .set(mapOf("name" to name), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.profile_save_failed), Toast.LENGTH_LONG).show()
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

}