package com.example.sisonke_app.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {

    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
    )

    fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS ?: EMAIL_PATTERN
        return pattern.matcher(email).matches()
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