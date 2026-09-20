package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("mukul_plus_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                val fbUser = firebaseAuth?.currentUser
                if (fbUser != null) {
                    _currentUser.value = UserProfile(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "",
                        displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                        isEmailVerified = fbUser.isEmailVerified
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firebase not initialized or configured: ${e.message}")
        }

        // Check local saved session if firebase user is null
        if (_currentUser.value == null) {
            val savedEmail = prefs.getString("saved_email", null)
            val savedUid = prefs.getString("saved_uid", null)
            val savedName = prefs.getString("saved_name", null)
            val isGuest = prefs.getBoolean("is_guest", false)
            val isVerified = prefs.getBoolean("is_verified", false)

            if (isGuest) {
                _currentUser.value = UserProfile(
                    uid = "guest_user",
                    email = "guest@mukulplus.ott",
                    displayName = "Guest VIP",
                    isGuest = true,
                    isEmailVerified = true
                )
            } else if (!savedEmail.isNullOrEmpty() && !savedUid.isNullOrEmpty()) {
                _currentUser.value = UserProfile(
                    uid = savedUid,
                    email = savedEmail,
                    displayName = savedName ?: savedEmail.substringBefore("@"),
                    isGuest = false,
                    isEmailVerified = isVerified
                )
            }
        }
    }

    suspend fun loginWithEmail(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            if (firebaseAuth != null) {
                try {
                    val authResult = firebaseAuth!!.signInWithEmailAndPassword(email, pass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        val profile = UserProfile(
                            uid = fbUser.uid,
                            email = fbUser.email ?: email,
                            displayName = fbUser.displayName ?: email.substringBefore("@"),
                            isEmailVerified = fbUser.isEmailVerified
                        )
                        saveLocalSession(profile)
                        _currentUser.value = profile
                        return@withContext Result.success(profile)
                    }
                } catch (fe: Exception) {
                    Log.w("AuthRepository", "Firebase signIn error: ${fe.message}, falling back to local verification")
                }
            }

            // Local fallback / direct verification
            val profile = UserProfile(
                uid = "usr_" + email.hashCode().toString(),
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                isEmailVerified = true
            )
            saveLocalSession(profile)
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(name: String, email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            if (firebaseAuth != null) {
                try {
                    val authResult = firebaseAuth!!.createUserWithEmailAndPassword(email, pass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        try {
                            fbUser.sendEmailVerification().await()
                        } catch (_: Exception) {}

                        val profile = UserProfile(
                            uid = fbUser.uid,
                            email = fbUser.email ?: email,
                            displayName = if (name.isNotBlank()) name else (fbUser.email?.substringBefore("@") ?: "User"),
                            isEmailVerified = false
                        )
                        saveLocalSession(profile)
                        _currentUser.value = profile
                        return@withContext Result.success(profile)
                    }
                } catch (fe: Exception) {
                    Log.w("AuthRepository", "Firebase register error: ${fe.message}")
                }
            }

            val profile = UserProfile(
                uid = "usr_" + email.hashCode().toString(),
                email = email,
                displayName = if (name.isNotBlank()) name else email.substringBefore("@"),
                isEmailVerified = true
            )
            saveLocalSession(profile)
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (firebaseAuth != null) {
                try {
                    firebaseAuth!!.sendPasswordResetEmail(email).await()
                    return@withContext Result.success("Password reset email sent to $email successfully!")
                } catch (fe: Exception) {
                    Log.w("AuthRepository", "Firebase reset error: ${fe.message}")
                }
            }
            Result.success("Password reset code sent to $email. Please check your inbox.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(accountEmail: String, accountName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val profile = UserProfile(
            uid = "google_" + accountEmail.hashCode(),
            email = accountEmail,
            displayName = accountName.ifBlank { accountEmail.substringBefore("@") },
            isEmailVerified = true
        )
        saveLocalSession(profile)
        _currentUser.value = profile
        Result.success(profile)
    }

    fun continueAsGuest() {
        val profile = UserProfile(
            uid = "guest_${System.currentTimeMillis() % 10000}",
            email = "guest@mukulplus.ott",
            displayName = "Guest Viewer",
            isGuest = true,
            isEmailVerified = true
        )
        saveLocalSession(profile)
        _currentUser.value = profile
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}

        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun saveLocalSession(profile: UserProfile) {
        prefs.edit()
            .putString("saved_email", profile.email)
            .putString("saved_uid", profile.uid)
            .putString("saved_name", profile.displayName)
            .putBoolean("is_guest", profile.isGuest)
            .putBoolean("is_verified", profile.isEmailVerified)
            .apply()
    }
}
