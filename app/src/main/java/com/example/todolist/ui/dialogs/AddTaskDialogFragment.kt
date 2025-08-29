package com.example.todolist.ui.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.todolist.R
import com.example.todolist.data.modal.Task
import com.example.todolist.data.remote.FirestoreService
import com.example.todolist.utils.scheduleNotification
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskDialogFragment(
    private val existingTitle: String = "",
    private val existingDescription: String = ""
) : DialogFragment() {

    private var selectedTime: String? = null
    private var selectedCategory: String? = null
    private var selectedPriority: String? = null
    private var listener: onAddTaskListener? = null

    private val firestoreService = FirestoreService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.add_task_dialog_fragment, container, false)

    interface onAddTaskListener {
        fun onAddTask(title: String, description: String = "")
    }

    fun setOnAddTaskListener(listener: onAddTaskListener) {
        this.listener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvAddTaskTitle = view.findViewById<TextView>(R.id.tv_add_task_title)
        val etTaskTitle = view.findViewById<EditText>(R.id.et_task_title)
        val etTaskDescription = view.findViewById<EditText>(R.id.et_task_description)
        val btnTimer = view.findViewById<MaterialButton>(R.id.btn_timer)
        val btnTag = view.findViewById<MaterialButton>(R.id.btn_tag)
        val btnFlag = view.findViewById<MaterialButton>(R.id.btn_flag)
        val btnSendTask = view.findViewById<MaterialButton>(R.id.btn_send_task)
        val btnEdit = view.findViewById<MaterialButton>(R.id.btn_edit)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

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

        btnFlag.setOnClickListener {
            val taskPriorityDialogFragment = TaskPriorityDialogFragment {
                btnFlag.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_primaryContainer
                )
                btnFlag.iconTint = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_onPrimaryContainer
                )
            }
            taskPriorityDialogFragment.setOnTaskPrioritySelected(object :
                TaskPriorityDialogFragment.onTaskPrioritySelectedListener {
                override fun onTaskPrioritySelected(taskPriority: String) {
                    selectedPriority = taskPriority
                }
            })
            taskPriorityDialogFragment.show(childFragmentManager, "TaskPriorityDialogFragment")
        }

        btnTag.setOnClickListener {
            val categoryDialogFragment = CategoryDialogFragment {
                btnTag.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_primaryContainer
                )
                btnTag.iconTint = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_onPrimaryContainer
                )
            }

            val bundle = Bundle().apply { putString("source", "AddTaskDialogFragment") }
            categoryDialogFragment.arguments = bundle

            categoryDialogFragment.setOnCategorySelectedListener(object :
                CategoryDialogFragment.OnCategorySelectedListener {
                override fun onCategorySelected(categoryName: String) {
                    selectedCategory = categoryName
                }
            })
            categoryDialogFragment.show(childFragmentManager, "CategoryDialogFragment")
        }

        // Save Task (send button)
        btnSendTask.isEnabled = false
        etTaskTitle.addTextChangedListener {
            btnSendTask.isEnabled = it.toString().trim().isNotEmpty()
        }
        btnSendTask.setOnClickListener {
            btnSendTask.isEnabled = false
            val titleText = etTaskTitle.text.toString().trim()
            if (titleText.isEmpty()) {
                etTaskTitle.error = "Required"
                btnSendTask.isEnabled = true
            } else {
                val task = Task(
                    title = titleText,
                    description = etTaskDescription.text.toString(),
                    time = selectedTime.orEmpty(),
                    category = selectedCategory.orEmpty(),
                    priority = selectedPriority.orEmpty()
                )
                addTask(task)
            }
        }

        // Hide/Show modes
        val hideViewsTaskScreen = arguments?.getBoolean("hide_viewsTaskScreen", false) ?: false
        val hideViewsProfileScreen = arguments?.getBoolean("hideViews_ProfileScreen", false) ?: false
        val hideViewsProfileScreenEmail = arguments?.getBoolean("hideViews_ProfileScreenEmail", false) ?: false
        val hideViewsProfileScreenPassword = arguments?.getBoolean("hideViews_ProfileScreenPassword", false) ?: false

        if (hideViewsTaskScreen) {
            view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
            tvAddTaskTitle.text = "Edit Task"
            btnTimer.visibility = View.GONE
            btnTag.visibility = View.GONE
            btnFlag.visibility = View.GONE
            btnSendTask.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
        }
        if (hideViewsProfileScreen) {
            view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
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
        if (hideViewsProfileScreenEmail) {
            view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
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
            view.findViewById<ConstraintLayout>(R.id.ll_add_task_views)
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

        btnCancel.setOnClickListener { dismiss() }

        // Pre-fill for edit
        etTaskTitle.setText(existingTitle)
        etTaskDescription.setText(existingDescription)

        btnEdit.setOnClickListener {
            val title = etTaskTitle.text.toString()
            val description = etTaskDescription.text.toString()

            if (title.isBlank()) {
                Snackbar.make(requireView(), "Title cannot be empty!", Snackbar.LENGTH_SHORT).show()
            } else {
                val finalDescription = description.ifBlank { existingDescription }
                listener?.onAddTask(title, finalDescription)
                dismiss()
            }
        }

    }

    // Firestore call via service + schedule notification
    private fun addTask(task: Task) {
        firestoreService.addTask(
            task = task,
            onSuccess = { documentId ->
                if (!task.time.isNullOrEmpty() && task.time != "null") {
                    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    try {
                        val timeInMillis = formatter.parse(task.time)?.time ?: 0L
                        if (timeInMillis > System.currentTimeMillis()) {
                            activity?.let {
                                val prio = task.priority.toIntOrNull() ?: 3
                                scheduleNotification(
                                    it,
                                    documentId.hashCode(),
                                    timeInMillis,
                                    task.title,
                                    prio
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
            },
            onFailure = { e ->
                Snackbar.make(requireView(), "unable to add task", Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    private fun showTimePicker(timePicked: (String) -> Unit = {}) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        selectedTime = formatter.format(calendar.time)
                        timePicked(selectedTime!!)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.setGravity(Gravity.BOTTOM)
    }
}
