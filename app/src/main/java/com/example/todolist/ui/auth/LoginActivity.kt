package com.example.todolist.ui.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.todolist.BaseActivity
import com.example.todolist.data.remote.GoogleSignInCallback
import com.example.todolist.data.remote.GoogleSignInHelper
import com.example.todolist.R
import com.example.todolist.ui.screens.HomeScreenActivity
import com.example.todolist.data.remote.AuthService

class LoginActivity : BaseActivity(), GoogleSignInCallback {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLoginWithGoogle: Button
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private val authService = AuthService()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

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

    private fun login(email: String, password: String) {
        if (!validateForm()) {
            return
        }
        authService.login(email, password) { success, message ->
            if (success) {
                startActivity(Intent(this, HomeScreenActivity::class.java))
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
        private const val TAG = "UserLogin"
    }

    override fun OnGoogleSignInSuccess() {
        val intent = Intent(this, HomeScreenActivity::class.java)
        startActivity(intent)
        finish()
    }

}