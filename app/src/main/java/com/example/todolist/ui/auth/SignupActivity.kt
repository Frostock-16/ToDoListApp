package com.example.todolist.ui.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.todolist.BaseActivity
import com.example.todolist.data.remote.GoogleSignInCallback
import com.example.todolist.data.remote.GoogleSignInHelper
import com.example.todolist.R
import com.example.todolist.data.remote.AuthService
import com.example.todolist.ui.screens.HomeScreenActivity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.material.snackbar.Snackbar


class SignupActivity : BaseActivity(), GoogleSignInCallback {
    private lateinit var etFullname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var credentialManager: CredentialManager
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private val authService = AuthService()

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

        btnSignUp.setOnClickListener {
            createAccount(etEmail.text.toString(), etPassword.text.toString())
        }

        btnGoogleSignUp.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                signIn()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Google Sign-In requires Android 14+",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createAccount(email: String, password: String) {
        if (!validateForm()) {
            return
        }
        authService.signUp(etFullname.text.toString(), email, password) { success, message ->
            if (success) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                etEmail.error = message
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
