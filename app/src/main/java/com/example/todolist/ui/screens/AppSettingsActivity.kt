package com.example.todolist

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class AppSettingsActivity : BaseActivity() {

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appsettings_activity)

        sharedPrefs = getSharedPreferences("ToDoList", MODE_PRIVATE)

        // Linear layout
        val llResetTasks = findViewById<LinearLayout>(R.id.ll_reset_tasks)
        val llChangeTypography = findViewById<LinearLayout>(R.id.ll_change_typography)
        val llChangeLanguage = findViewById<LinearLayout>(R.id.ll_change_language)

        llResetTasks.setOnClickListener {
            showAlertDialog(R.layout.delete_alertdialog)
        }

        llChangeTypography.setOnClickListener {
            Toast.makeText(this, "Typography settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        llChangeLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun showAlertDialog(@LayoutRes layoutResId: Int) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(layoutResId, null)
        val btnYes = view.findViewById<Button>(R.id.btn_yes)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        val alertTitle = view.findViewById<TextView>(R.id.tv_alerttitle)
        val alertDescription = view.findViewById<TextView>(R.id.tv_alertdescription)
        alertTitle.text = "Reset Tasks"
        alertDescription.text = "Are you sure you want to delete all your tasks? This cannot be undone"
        builder.setView(view)
        val dialog = builder.create()
        btnYes.setOnClickListener {
            resetTasks()
            dialog.dismiss()
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun resetTasks() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            val db = Firebase.firestore

            db.collection("users")
                .document(userId)
                .collection("tasks")
                .get()
                .addOnSuccessListener { snapshot ->
                    for (document in snapshot) {
                        db.collection("users")
                            .document(userId)
                            .collection("tasks")
                            .document(document.id)
                            .delete()
                    }

                    db.collection("users")
                        .document(userId)
                        .collection("completed_tasks")
                        .get()
                        .addOnSuccessListener { snapshot2 ->
                            for (document in snapshot2) {
                                db.collection("users")
                                    .document(userId)
                                    .collection("completed_tasks")
                                    .document(document.id)
                                    .delete()
                            }

                            sharedPrefs.edit().putString("TaskLeftCount", "0 TASK LEFT").putInt("TaskDoneCount", 0).apply()
                            Toast.makeText(this, "All tasks reset.", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }


    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Spanish", "Hindi", "German")
        val languageCodes = arrayOf("en", "es", "hi", "de")

        AlertDialog.Builder(this)
            .setTitle("Choose Language")
            .setItems(languages) { _, which ->
                val chosenLang = languageCodes[which]

                // Save choice in shared prefs
                getSharedPreferences("ToDoList", MODE_PRIVATE)
                    .edit()
                    .putString("AppLanguage", chosenLang)
                    .apply()

                LocaleHelper.setLocale(this, chosenLang)
                finish()
                startActivity(intent)
            }
            .show()
    }


}
