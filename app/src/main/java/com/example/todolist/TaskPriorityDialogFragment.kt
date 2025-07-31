package com.example.todolist

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.adapter.TaskPriorityAdapter

class TaskPriorityDialogFragment(private val onPrioritySelected: (String) -> Unit = {}) :
    DialogFragment() {
    private var listener: onTaskPrioritySelectedListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.task_priority_dialog_fragment, container, false)
    }

    interface onTaskPrioritySelectedListener {
        fun onTaskPrioritySelected(taskPriority: String)
    }

    fun setOnTaskPrioritySelected(listener: onTaskPrioritySelectedListener) {
        this.listener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_priority_grid)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val adapter = TaskPriorityAdapter(requireContext()) { selectedPriority ->
            // Handle priority selected
            onPrioritySelected(selectedPriority.toString())
            listener?.onTaskPrioritySelected(selectedPriority.toString())
            Toast.makeText(requireContext(), "Selected Priority $selectedPriority", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        recyclerView.adapter = adapter


        // Previous implementation of priority selection (gridview)
//        selectedPriority = -1
//        buttons.forEachIndexed { index, button ->
//            button.setOnClickListener {
//                buttons.forEach { it.isSelected = false }
//                button.isSelected = true
//                selectedPriority = index + 1
//                Log.d("Task Priority", "TaskPriority: $selectedPriority")
//            }
//        }


    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setGravity(Gravity.BOTTOM)
    }
}