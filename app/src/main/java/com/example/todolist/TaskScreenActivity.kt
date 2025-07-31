package com.example.todolist

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.todolist.showDialogFragUtil.showDialogFragment
import com.example.todolist.utils.cancelNotification
import com.example.todolist.utils.scheduleNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class TaskScreenActivity : BaseActivity() {
    private lateinit var taskId: String
    private lateinit var tvTaskTitle: TextView
    private lateinit var tvTaskPriority: TextView
    private lateinit var title: String
    private lateinit var description: String
    private lateinit var time: String
    private lateinit var category: String
    private lateinit var priority: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task_screen_activity)
        setupToolbar("Edit Task", true)

        // Get task info from home screen
        taskId = intent.getStringExtra("taskId").toString()
        Log.d("Task Id", taskId)
        title = intent.getStringExtra("title").toString()
        description = intent.getStringExtra("description").toString()
        time = intent.getStringExtra("time").toString()
        category = intent.getStringExtra("category").toString()
        priority = intent.getStringExtra("priority").toString()

        // Buttons
        val ivEdit = findViewById<ImageView>(R.id.iv_edit)

        // TextView
        val tvTaskTime = findViewById<TextView>(R.id.label_task_time)
        val tvTaskCategory = findViewById<TextView>(R.id.label_task_category)
        tvTaskPriority = findViewById(R.id.label_task_priority)
        val tvDeleteTask = findViewById<TextView>(R.id.tv_delete_task)
        tvTaskTitle = findViewById(R.id.tv_task_title)
        val tvTaskDescription = findViewById<TextView>(R.id.tv_task_description)


        // Fragments
        val addTaskDialogFragment = AddTaskDialogFragment(title, description)
        val taskPriorityDialogFragment = TaskPriorityDialogFragment()

        // Set Task details
        tvTaskTitle.text = title
        tvTaskDescription.text = description
        tvTaskTime.text = time
        tvTaskCategory.text = category
        tvTaskPriority.text = priority

        if (tvTaskTime.text.toString() == "null") {
            tvTaskTime.text = "Choose Time"
            tvTaskTime.setTextColor(
                ContextCompat.getColor(
                    this@TaskScreenActivity,
                    R.color.md_theme_onSurfaceVariant
                )
            )
        }
        if (tvTaskCategory.text.toString() == "null") {
            tvTaskCategory.text = "Choose Category"
            tvTaskCategory.setTextColor(
                ContextCompat.getColor(
                    this@TaskScreenActivity,
                    R.color.md_theme_onSurfaceVariant
                )
            )
        }
        if (tvTaskPriority.text.toString() == "null") {
            tvTaskPriority.text = "Choose Priority"
            tvTaskPriority.setTextColor(
                ContextCompat.getColor(
                    this@TaskScreenActivity,
                    R.color.md_theme_onSurfaceVariant
                )
            )
        }
        if (tvTaskDescription.text.toString() == "") {
            tvTaskDescription.text = "Add description...."
            tvTaskDescription.setTextColor(
                ContextCompat.getColor(
                    this@TaskScreenActivity,
                    R.color.md_theme_onSurfaceVariant
                )
            )
        }


        ivEdit.setOnClickListener {
            showDialogFragment(
                addTaskDialogFragment,
                supportFragmentManager,
                "hide_viewsTaskScreen",
                "AddTaskDialogFragment"
            )
            addTaskDialogFragment.setOnAddTaskListener(object :
                AddTaskDialogFragment.onAddTaskListener {
                override fun onAddTask(title: String, description: String) {
                    tvTaskTitle.text = title
                    tvTaskDescription.text = description
                    updateTask("title", title)
                    updateTask("description", description)
                }

            })
        }

        tvTaskCategory.setOnClickListener {
            val categoryDialogFragment = CategoryDialogFragment()
            val bundle = Bundle()
            bundle.putString("source", "TaskScreenActivity")
            categoryDialogFragment.arguments = bundle
            showDialogFragment(
                categoryDialogFragment,
                supportFragmentManager,
                "hide_viewsTaskScreen",
                "CategoryDialogFragment"
            )

            categoryDialogFragment.setOnCategorySelectedListener(object :
                CategoryDialogFragment.OnCategorySelectedListener {
                override fun onCategorySelected(categoryName: String) {
                    tvTaskCategory.text = categoryName
                    tvTaskCategory.setTextColor(
                        ContextCompat.getColor(
                            this@TaskScreenActivity,
                            R.color.md_theme_onSurfaceVariant
                        )
                    )
                    updateTask("category", categoryName)
                }
            })
        }

        tvTaskTime.setOnClickListener {
            showTimePicker(tvTaskTime)
        }

        tvTaskPriority.setOnClickListener {
            showDialogFragment(
                taskPriorityDialogFragment,
                supportFragmentManager,
                "hide_viewsTaskScreen",
                "TaskPriorityDialogFragment"
            )

            taskPriorityDialogFragment.setOnTaskPrioritySelected(object :
                TaskPriorityDialogFragment.onTaskPrioritySelectedListener {
                override fun onTaskPrioritySelected(taskPriority: String) {
                    tvTaskPriority.text = taskPriority
                    tvTaskPriority.setTextColor(
                        ContextCompat.getColor(
                            this@TaskScreenActivity,
                            R.color.md_theme_onSurfaceVariant
                        )
                    )
                    updateTask("priority", taskPriority)
                }
            })
        }

        tvDeleteTask.setOnClickListener {
            showAlertDialog(R.layout.delete_alertdialog)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

    }

    private fun showTimePicker(tvTaskTime: TextView) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                    calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val selectedTime = formatter.format(calendar.time)
                    tvTaskTime.text = selectedTime
                    tvTaskTime.setTextColor(
                        ContextCompat.getColor(
                            this@TaskScreenActivity,
                            R.color.md_theme_onSurfaceVariant
                        )
                    )
                    updateTask("time", selectedTime)

                    // Reschedule notification
                    val priority =
                        getPriority(findViewById<TextView>(R.id.label_task_priority).text.toString())
                    scheduleNotification(
                        this,
                        taskId.hashCode(),
                        calendar.timeInMillis,
                        findViewById<TextView>(R.id.tv_task_title).text.toString(),
                        priority
                    )
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun getPriority(priorityText: String): Int {
        return priorityText.toIntOrNull() ?: 3
    }


    private fun showAlertDialog(@LayoutRes layoutResId: Int) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(layoutResId, null)
        val btnYes = view.findViewById<Button>(R.id.btn_yes)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        view.findViewById<TextView>(R.id.tv_alerttitle)
        view.findViewById<TextView>(R.id.tv_alertdescription)
        builder.setView(view)
        val dialog = builder.create()
        btnYes.setOnClickListener {
            deleteTask()
            dialog.dismiss()
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun deleteTask() {
        cancelNotification(this, taskId.hashCode())
        val userId = Firebase.auth.currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (userId != null && taskId.isNotEmpty()) {
            db.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeScreenActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to delete task: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("Firestore", "Delete error", e)
                }
        } else {
            Toast.makeText(this, "Error: Task ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTask(selectedField: String, value: String) {
        val userId = Firebase.auth.currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        if (userId != null && taskId.isNotEmpty()) {
            db.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .update(selectedField, value)
                .addOnSuccessListener {
                    Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to update task: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Error: Task ID not found", Toast.LENGTH_SHORT).show()
        }
    }

}
