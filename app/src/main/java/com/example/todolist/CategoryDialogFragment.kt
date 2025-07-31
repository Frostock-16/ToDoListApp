package com.example.todolist

import CategoryAdapter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.modal.Category

class CategoryDialogFragment(
    private val onCategorySelected: (String) -> Unit = {}
) : DialogFragment() {

    private var listener: OnCategorySelectedListener? = null

    interface OnCategorySelectedListener {
        fun onCategorySelected(categoryName: String)
    }

    fun setOnCategorySelectedListener(listener: OnCategorySelectedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.category_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_categories)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val categories = listOf(
            Category(R.drawable.ic_grocery, "Grocery"),
            Category(R.drawable.ic_work, "Work"),
            Category(R.drawable.ic_sport, "Sport"),
            Category(R.drawable.ic_design, "Design"),
            Category(R.drawable.ic_university, "University"),
            Category(R.drawable.ic_socials, "Social"),
            Category(R.drawable.ic_music_w400, "Music"),
            Category(R.drawable.ic_health, "Health"),
            Category(R.drawable.ic_movie, "Movie"),
            Category(R.drawable.ic_home_w400, "Home")
        )

        val adapter = CategoryAdapter(categories) { selected ->
            listener?.onCategorySelected(selected.label)
            onCategorySelected(selected.label)
            Toast.makeText(requireContext(), "Selected ${selected.label}", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
        }
    }
}
