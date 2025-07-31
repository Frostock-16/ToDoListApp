package com.example.todolist

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {
    private lateinit var btnGotologin: Button
    private lateinit var btnCreateaccount: Button
    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomeScreenActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.welcomepage_activity)

        // Buttons
        btnGotologin = findViewById(R.id.gotologin_btn)
        btnCreateaccount = findViewById(R.id.createaccount_btn)

        btnGotologin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnCreateaccount.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
