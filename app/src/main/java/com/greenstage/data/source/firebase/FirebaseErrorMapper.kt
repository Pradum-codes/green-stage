package com.greenstage.data.source.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

object FirebaseErrorMapper {

    fun map(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthUserCollisionException ->
                "An account with this email address already exists."
            is FirebaseAuthWeakPasswordException ->
                "Password must contain at least 6 characters."
            is FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password."
            is FirebaseAuthInvalidUserException ->
                "No user account found or account disabled."
            is FirebaseAuthException ->
                mapAuthErrorCode(throwable.errorCode) ?: (throwable.message ?: "Authentication failed.")
            is FirebaseNetworkException ->
                "Network failure. Please check your internet connection."
            is FirebaseTooManyRequestsException ->
                "Too many attempts. Please try again later."
            is FirebaseFirestoreException ->
                mapFirestoreErrorCode(throwable.code) ?: "Database error occurred."
            else ->
                throwable.message?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred."
        }
    }

    private fun mapAuthErrorCode(errorCode: String): String? {
        return when (errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email address already exists."
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_WRONG_PASSWORD" -> "Invalid email or password."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email address."
            "ERROR_USER_DISABLED" -> "This user account has been disabled."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many requests. Please try again later."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password accounts are not enabled."
            "ERROR_WEAK_PASSWORD" -> "Password must contain at least 6 characters."
            else -> null
        }
    }

    private fun mapFirestoreErrorCode(code: FirebaseFirestoreException.Code): String? {
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Access denied. Insufficient permissions."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Database service temporarily unavailable."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Requested resource not found."
            FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                "Resource already exists."
            else -> null
        }
    }
}
