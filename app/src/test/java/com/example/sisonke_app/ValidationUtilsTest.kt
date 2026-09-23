package com.example.sisonke_app

import com.example.sisonke_app.utils.ValidationUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun validEmail_returnsTrue() {

        assertTrue(
            ValidationUtils.isValidEmail(
                "test@example.com"
            )
        )
    }

    @Test
    fun invalidEmail_returnsFalse() {

        assertFalse(
            ValidationUtils.isValidEmail(
                "not-an-email"
            )
        )
    }

    @Test
    fun validPassword_returnsTrue() {

        assertTrue(
            ValidationUtils.isValidPassword(
                "Password123"
            )
        )
    }

    @Test
    fun shortPassword_returnsFalse() {

        assertFalse(
            ValidationUtils.isValidPassword(
                "123"
            )
        )
    }

    @Test
    fun matchingPasswords_returnsTrue() {

        assertTrue(
            ValidationUtils.passwordsMatch(
                "Password123",
                "Password123"
            )
        )
    }

    @Test
    fun differentPasswords_returnsFalse() {

        assertFalse(
            ValidationUtils.passwordsMatch(
                "Password123",
                "Password456"
            )
        )
    }

    @Test
    fun validIncidentDescription_returnsTrue() {

        assertTrue(
            ValidationUtils.isValidIncidentDescription(
                "There was suspicious activity."
            )
        )
    }

    @Test
    fun emptyIncidentDescription_returnsFalse() {

        assertFalse(
            ValidationUtils.isValidIncidentDescription("")
        )
    }
}