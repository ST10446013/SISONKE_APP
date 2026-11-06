package com.example.sisonke_app.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.dashboard.DashboardActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Toast.makeText(this, getString(R.string.google_token_missing), Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener(this) { signInTask ->
                    if (signInTask.isSuccessful) openDashboard()
                    else Toast.makeText(this, signInTask.exception?.localizedMessage ?: getString(R.string.google_sign_in_failed), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val apiException = e as? com.google.android.gms.common.api.ApiException
                val message = when (apiException?.statusCode) {
                    12501 -> getString(R.string.google_sign_in_cancelled)
                    10 -> "Google Sign-In Developer Error (Status 10): Please ensure your app's SHA-1 fingerprint is registered in the Firebase Console (sisonke-8e84a) and Google Sign-In is enabled in Firebase Authentication."
                    7 -> "Network error during Google Sign-In. Please check your internet connection."
                    else -> apiException?.localizedMessage ?: e.localizedMessage ?: getString(R.string.google_sign_in_failed)
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        setupLanguageSpinner()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etEmail.setText(intent.getStringExtra("registered_email") ?: "")
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvRegister.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }

        tvForgotPassword.setOnClickListener {
            sendPasswordReset()
        }

        findViewById<LinearLayout>(R.id.btnGoogle).setOnClickListener {
            startGoogleSignIn()
        }
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("English", "isiZulu", "Afrikaans")
        val spinner = findViewById<Spinner>(R.id.spinnerLanguage)
        spinner.adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_dropdown_item,
            languages
        )
        val prefs = getSharedPreferences("sisonke_prefs", MODE_PRIVATE)
        val savedLanguage = prefs.getString("preferred_language", "English") ?: "English"
        val savedIndex = languages.indexOf(savedLanguage).coerceAtLeast(0)
        spinner.setSelection(savedIndex, false)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = languages[position]
                if (savedLanguage == selected) return
                prefs.edit().putString("preferred_language", selected).apply()
                val languageTag = when (selected) {
                    "isiZulu" -> "zu"
                    "Afrikaans" -> "af"
                    else -> "en"
                }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
                recreate()
            }
        }
    }

    private fun loginUser() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = getString(R.string.err_enter_email)
            return
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.err_enter_password)
            return
        }

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                btnLogin.isEnabled = true

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Login successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    openDashboard()

                } else {

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    private fun startGoogleSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.example.sisonke_app.R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, options)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun openDashboard() {
        val user = auth.currentUser
        if (user != null) {
            val profile = hashMapOf(
                "email" to (user.email ?: ""),
                "name" to (user.displayName ?: ""),
                "lastLoginAt" to System.currentTimeMillis()
            )
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.uid)
                .set(profile, com.google.firebase.firestore.SetOptions.merge())
        }
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun sendPasswordReset() {

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = getString(R.string.err_enter_email_first)
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Password reset email sent",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Unable to send reset email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}