package com.example.todolist

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.todolist.modal.Task
import com.example.todolist.utils.scheduleNotification
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskDialogFragment(private val existingTitle: String = "",
                            private val existingDescription: String = "") : DialogFragment() {
    private var selectedTime:String? = null
    private var selectedCategory:String? = null
    private var selectedPriority:String? = null
    private var listener:onAddTaskListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.add_task_dialog_fragment, container, false)
    }

    interface onAddTaskListener{
        fun onAddTask(title:String, description: String="")
    }

    fun setOnAddTaskListener(listener: onAddTaskListener){
        this.listener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TextView
        val tvAddTaskTitle = view.findViewById<TextView>(R.id.tv_add_task_title)

        // EditText
        val etTaskTitle = view.findViewById<EditText>(R.id.et_task_title)
        val etTaskDescription = view.findViewById<EditText>(R.id.et_task_description)

        // Button
        val btnTimer = view.findViewById<MaterialButton>(R.id.btn_timer)
        val btnTag = view.findViewById<MaterialButton>(R.id.btn_tag)
        val btnFlag = view.findViewById<MaterialButton>(R.id.btn_flag)
        val btnSendTask = view.findViewById<MaterialButton>(R.id.btn_send_task)
        val btnEdit = view.findViewById<MaterialButton>(R.id.btn_edit)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)


        // Dialog Fields
        btnTimer.setOnClickListener {
            showTimePicker {
                btnTimer.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_primaryContainer
                )
                btnTimer.iconTint = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_onPrimaryContainer
                )
            }
        }

        btnFlag.setOnClickListener{
            val taskPriorityDialogFragment = TaskPriorityDialogFragment{
                btnFlag.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_primaryContainer
                )
                btnFlag.iconTint = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_onPrimaryContainer
                )
            }
            taskPriorityDialogFragment.setOnTaskPrioritySelected(object:TaskPriorityDialogFragment.onTaskPrioritySelectedListener{
                override fun onTaskPrioritySelected(taskPriority: String) {
                    selectedPriority = taskPriority
                }
            })

            taskPriorityDialogFragment.show(childFragmentManager, "TaskPriorityDialogFragment")
        }

        btnTag.setOnClickListener{
            val categoryDialogFragment = CategoryDialogFragment{
                btnTag.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_primaryContainer
                )
                btnTag.iconTint = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_onPrimaryContainer
                )
            }

            val bundle = Bundle()
            bundle.putString("source", "AddTaskDialogFragment")
            categoryDialogFragment.arguments = bundle

            categoryDialogFragment.setOnCategorySelectedListener(object: CategoryDialogFragment.OnCategorySelectedListener{
                override fun onCategorySelected(categoryName: String) {
                    selectedCategory = categoryName
                }
            })

            categoryDialogFragment.show(childFragmentManager, "CategoryDialogFragment")
        }

        // SaveTask (btnSendTask)
        btnSendTask.isEnabled = false
        etTaskTitle.addTextChangedListener {
            val titleText = it.toString().trim()
            btnSendTask.isEnabled = titleText.isNotEmpty()
        }
        btnSendTask.setOnClickListener{
            btnSendTask.isEnabled = false
            if(etTaskTitle.text.toString().trim().isEmpty()){
                etTaskTitle.error = "Required"
                btnSendTask.isEnabled = true
            }else{
                val task = Task(
                    title = etTaskTitle.text.toString(),
                    description = etTaskDescription.text.toString(),
                    time = selectedTime.toString(),
                    category = selectedCategory.toString(),
                    priority = selectedPriority.toString()
                )
                addTask(task)

            }
        }


        // Hiding the view in TaskScreen
        val hideViewsTaskScreen = arguments?.getBoolean("hide_viewsTaskScreen", false)?:false
        val hideViewsProfileScreen = arguments?.getBoolean("hideViews_ProfileScreen", false)?:false
        val hideViewsProfileScreenEmail = arguments?.getBoolean("hideViews_ProfileScreenEmail", false)?:false
        val hideViewsProfileScreenPassword = arguments?.getBoolean("hideViews_ProfileScreenPassword", false) ?: false
        if(hideViewsTaskScreen){
            val llAddTaskViews = view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
            tvAddTaskTitle.text = "Edit Task"
            btnTimer.visibility = View.GONE
            btnTag.visibility = View.GONE
            btnFlag.visibility = View.GONE
            btnSendTask.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
        }
        if(hideViewsProfileScreen)
        {
            val llAddTaskViews = view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
            tvAddTaskTitle.text = "Change Acccount Name"
            btnTimer.visibility = View.GONE
            btnTag.visibility = View.GONE
            btnFlag.visibility = View.GONE
            btnSendTask.visibility = View.GONE
            etTaskDescription.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            btnEdit.text = "Save"
            btnCancel.visibility = View.VISIBLE
            etTaskTitle.hint = "Eg.(xyz, Krish Malhotra...)"
        }
        if(hideViewsProfileScreenEmail)
        {
            val llAddTaskViews = view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
            tvAddTaskTitle.text = "Change Email"
            btnTimer.visibility = View.GONE
            btnTag.visibility = View.GONE
            btnFlag.visibility = View.GONE
            btnSendTask.visibility = View.GONE
            etTaskDescription.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            btnEdit.text = "Save"
            btnCancel.visibility = View.VISIBLE
            etTaskTitle.hint = "Email"
        }
        if (hideViewsProfileScreenPassword) {
            val llAddTaskViews = view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
            tvAddTaskTitle.text = "Change Password"
            btnTimer.visibility = View.GONE
            btnTag.visibility = View.GONE
            btnFlag.visibility = View.GONE
            btnSendTask.visibility = View.GONE
            etTaskDescription.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            btnEdit.text = "Save"
            btnCancel.visibility = View.VISIBLE
            etTaskTitle.hint = "New Password"
        }


        btnCancel.setOnClickListener{
            dismiss()
        }


        // Set title and description (description if exists)
        etTaskTitle.setText(existingTitle)
        etTaskDescription.setText(existingDescription)
        btnEdit.setOnClickListener{
            val title = etTaskTitle.text.toString()
            val description = etTaskDescription.text.toString()

            if (title.isBlank()) {
                Toast.makeText(requireContext(), "Title can't be empty", Toast.LENGTH_SHORT).show()
            } else {
                val finalDescription = if (description.isBlank()) existingDescription else description
                listener?.onAddTask(title, finalDescription)
                dismiss()
            }
        }
    }

    // Add task to firebase
    private fun addTask(task: Task) {
        val userId = Firebase.auth.currentUser?.uid
        val db = Firebase.firestore

        if (userId != null) {
            db.collection("users")
                .document(userId)
                .collection("tasks")
                .add(task)
                .addOnSuccessListener { documentReference ->
                    if (!task.time.isNullOrEmpty() && task.time != "null") {

                        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        try {
                            val timeInMillis = formatter.parse(task.time)?.time ?: 0L
                            if (timeInMillis > System.currentTimeMillis()) {
                                activity?.let {
                                    scheduleNotification(
                                        it,
                                        documentReference.id.hashCode(),
                                        timeInMillis,
                                        task.title,
                                        task.priority.toInt()
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Notification", "Failed to parse date for notification", e)
                        }
                    }
                    if (isAdded) {
                        parentFragmentManager.setFragmentResult("taskAddedRequest", Bundle())
                        dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error adding task", e)
                }
        } else {
            Log.d("Firestore", "User not logged in")
        }
    }



    // Select Date and Time
    private fun showTimePicker(timePicker: (String) -> Unit = {})
    {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                selectedTime = formatter.format(calendar.time)
                timePicker(selectedTime.toString())
                Log.d("Date Picker", "DatePickerDialog: $selectedTime")

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
            timePickerDialog.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setGravity(Gravity.BOTTOM)
    }
}
