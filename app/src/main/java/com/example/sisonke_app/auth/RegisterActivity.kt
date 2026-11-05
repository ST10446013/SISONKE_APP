package com.example.sisonke_app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (name.isEmpty()) {
            etName.error = getString(R.string.err_enter_name)
            return
        }

        if (email.isEmpty()) {
            etEmail.error = getString(R.string.err_enter_email)
            return
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.err_enter_password)
            return
        }

        if (password.length < 6) {
            etPassword.error = getString(R.string.err_password_length)
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = getString(R.string.err_password_mismatch)
            return
        }

        btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val userId = auth.currentUser?.uid

                    if (userId != null) {

                        val user = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "language" to "English",
                            "notificationsEnabled" to true,
                            "createdAt" to System.currentTimeMillis()
                        )

                        firestore.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Account created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Registration is complete; return to login as requested.
                                auth.signOut()
                                val loginIntent = Intent(this, LoginActivity::class.java)
                                loginIntent.putExtra("registered_email", email)
                                loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(loginIntent)
                                finish()
                            }
                            .addOnFailureListener {

                                btnRegister.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Account created, but profile could not be saved",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }

                } else {

                    btnRegister.isEnabled = true

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}