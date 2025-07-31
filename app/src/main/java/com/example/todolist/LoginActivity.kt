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
import androidx.credentials.GetCredentialRequest
import com.example.todolist.SignupActivity.Companion
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth

class LoginActivity : BaseActivity(), GoogleSignInCallback {
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLoginWithGoogle: Button
    private lateinit var googleSignInHelper: GoogleSignInHelper

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        auth = Firebase.auth
        googleSignInHelper = GoogleSignInHelper(this, this)

        // TextView
        val tvGoToSignUp = findViewById<TextView>(R.id.register_tv)
        tvGoToSignUp.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }

        // EditText
        etEmail = findViewById(R.id.email_et)
        etPassword = findViewById(R.id.password_et)

        // Button
        btnLoginWithGoogle = findViewById(R.id.loginwithgoogle_btn)
        val btnLogin = findViewById<Button>(R.id.login_btn)
        btnLogin.setOnClickListener {
            login(etEmail.text.toString(), etPassword.text.toString())
        }

        btnLoginWithGoogle.setOnClickListener {
            googleSignInHelper.showBottomSheetLogin()
        }
    }

    public override fun onStart() {
        super.onStart()
        val currentUsers = auth.currentUser
        if (currentUsers != null) {
            Toast.makeText(this, "Current user exist", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No current users", Toast.LENGTH_SHORT).show()
        }
    }

    private fun login(email: String, password: String) {
        if (!validateForm()) {
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, HomeScreenActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val loginException = task.exception
                    if (loginException is FirebaseAuthException) {
                        when (loginException.errorCode) {
                            "ERROR_INVALID_EMAIL" -> {
                                etEmail.error = "Invalid email format"
                            }

                            "ERROR_WRONG_PASSWORD" -> {
                                etPassword.error = "Incorrect password"
                            }

                            else -> {
                                Log.e(TAG, "Sign-up error: ${loginException.message}")
                                etEmail.error = loginException.message
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
        private const val TAG = "UserLogin"
    }

    override fun OnGoogleSignInSuccess() {
        val intent = Intent(this, HomeScreenActivity::class.java)
        startActivity(intent)
        finish()
    }

}