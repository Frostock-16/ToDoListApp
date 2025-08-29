package com.example.todolist.data.remote

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest

class AuthService(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    val exception = task.exception as? FirebaseAuthException
                    val message = exception?.message ?: "Login failed"
                    Log.e("AuthService", "Login error: $message")
                    onResult(false, message)
                }
            }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    user?.updateProfile(profileUpdates)

                    onResult(true, null)
                } else {
                    val exception = task.exception as? FirebaseAuthException
                    val message = exception?.message ?: "Sign-up failed"
                    Log.e("AuthService", "Sign-up error: $message")
                    onResult(false, message)
                }
            }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() {
        auth.signOut()
    }

    fun updateDisplayName(
        newName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        if (user != null) {
            val updates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.message)
                    }
                }
        } else {
            onResult(false, "No logged-in user")
        }
    }

    fun reAuthenticateAndUpdateEmail(
        currentPassword: String,
        newEmail: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        val currentEmail = user?.email
        if (user != null && currentEmail != null) {
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            user.reauthenticate(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        user.verifyBeforeUpdateEmail(newEmail)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    user.sendEmailVerification()
                                    onResult(true, "Email updated. Verification sent.")
                                } else {
                                    onResult(false, updateTask.exception?.message)
                                }
                            }
                    } else {
                        onResult(false, "Re-auth failed: wrong password?")
                    }
                }
        } else {
            onResult(false, "User not logged in")
        }
    }

    fun reAuthenticateAndUpdatePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        val currentEmail = user?.email
        if (user != null && currentEmail != null) {
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            user.reauthenticate(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    onResult(true, "Password updated.")
                                } else {
                                    onResult(false, updateTask.exception?.message)
                                }
                            }
                    } else {
                        onResult(false, "Re-auth failed: wrong password?")
                    }
                }
        } else {
            onResult(false, "User not logged in")
        }
    }

    fun logout(onComplete: () -> Unit) {
        auth.signOut()
        onComplete()
    }

}
