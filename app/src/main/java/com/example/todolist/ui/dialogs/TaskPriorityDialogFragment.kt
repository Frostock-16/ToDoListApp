package com.example.todolist.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.example.todolist.adapter.TaskPriorityAdapter
import com.google.android.material.snackbar.Snackbar

class TaskPriorityDialogFragment(private val onPrioritySelected: (String) -> Unit = {}) :
    DialogFragment() {
    private var listener: onTaskPrioritySelectedListener? = null

    interface onTaskPrioritySelectedListener {
        fun onTaskPrioritySelected(taskPriority: String)
    }

    fun setOnTaskPrioritySelected(listener: onTaskPrioritySelectedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.task_priority_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_priority_grid)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val adapter = TaskPriorityAdapter(requireContext()) { selectedPriority ->
            onPrioritySelected(selectedPriority.toString())
            listener?.onTaskPrioritySelected(selectedPriority.toString())
            Snackbar.make(requireView(), "Selected Priority $selectedPriority", Snackbar.LENGTH_SHORT).show()
            dismiss()
        }

        recyclerView.adapter = adapter
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
