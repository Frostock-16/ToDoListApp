package com.example.todolist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FilterBottomSheet(private val onApplyFilters: (priority: String?, categories: List<String>) -> Unit)
    : BottomSheetDialogFragment() {

    private var selectedPriority: String? = null
    private val selectedCategories = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.filter_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val chipGroupPriority = view.findViewById<ChipGroup>(R.id.chipGroupPriority)
        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chipGroupCategory)

        chipGroupPriority.setOnCheckedChangeListener { group, checkedId ->
            selectedPriority = if (checkedId != View.NO_ID)
                view.findViewById<Chip>(checkedId).text.toString()
            else null
        }

        chipGroupCategory.children.forEach { chipView ->
            val chip = chipView as Chip
            chip.setOnCheckedChangeListener { button, isChecked ->
                if (isChecked) selectedCategories.add(chip.text.toString())
                else selectedCategories.remove(chip.text.toString())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // apply filters on dismiss
        onApplyFilters(selectedPriority, selectedCategories)
    }
}
