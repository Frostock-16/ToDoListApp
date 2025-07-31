package com.example.todolist

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest


class SignupActivity : BaseActivity(), GoogleSignInCallback {
    private lateinit var auth: FirebaseAuth
    private lateinit var etFullname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var credentialManager: CredentialManager
    private lateinit var googleSignInHelper: GoogleSignInHelper

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        // Textview
        val tvGoToLogin = findViewById<TextView>(R.id.login_tv)
        tvGoToLogin.setOnClickListener {
            val intent = Intent(this@SignupActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // EditText
        etFullname = findViewById(R.id.fullname_et)
        etEmail = findViewById(R.id.email_et)
        etPassword = findViewById(R.id.password_et)

        // Button
        val btnSignUp = findViewById<Button>(R.id.signup_btn)
        val btnGoogleSignUp = findViewById<Button>(R.id.signupwithgoogle_btn)

        credentialManager = CredentialManager.create(this)
        googleSignInHelper = GoogleSignInHelper(this, this)

        auth = Firebase.auth

        btnSignUp.setOnClickListener {
            createAccount(etEmail.text.toString(), etPassword.text.toString())
        }

        btnGoogleSignUp.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                signIn()
            } else {
                Toast.makeText(this, "Google Sign-In requires Android 14+", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this, "current user exist", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No current user", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAccount(email: String, password: String) {
        if (!validateForm()) {
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = FirebaseAuth.getInstance().currentUser
                    val profileUpdates = userProfileChangeRequest {
                        displayName = etFullname.text.toString()
                    }
                    user?.updateProfile(profileUpdates)
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val signUpException = task.exception
                    if (signUpException is FirebaseAuthException) {
                        when (signUpException.errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> {
                                Log.d(TAG, "Email already in use")
                                etEmail.error = "Email already in use"
                            }

                            "ERROR_INVALID_EMAIL" -> {
                                etEmail.error = "Invalid email format."
                            }

                            else -> {
                                Log.e(TAG, "Sign-up error: ${signUpException.message}")
                                etEmail.error = signUpException.localizedMessage
                            }
                        }
                    }
                }
            }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        if (email.isEmpty()) {
            etEmail.error = "Required"
            valid = false
        } else {
            etEmail.error = null
        }

        if (password.isEmpty()) {
            etPassword.error = "Required"
            valid = false
        } else {
            etPassword.error = null
        }
        return valid
    }

    companion object {
        private const val TAG = "SignUp"
    }

    // GOOGLE SIGN UP //
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun signIn() {
        val signInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(serverClientId = this.getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        googleSignInHelper.launchCredentialManager(request)
    }

    override fun OnGoogleSignInSuccess() {
        val intent = Intent(this, HomeScreenActivity::class.java)
        startActivity(intent)
        finish()
    }

}
