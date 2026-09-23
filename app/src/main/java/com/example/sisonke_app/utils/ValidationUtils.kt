package com.example.sisonke_app.utils

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS
            .matcher(email)
            .matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun passwordsMatch(
        password: String,
        confirmPassword: String
    ): Boolean {
        return password == confirmPassword
    }

    fun isValidIncidentDescription(
        description: String
    ): Boolean {
        return description.trim().isNotEmpty()
    }
}