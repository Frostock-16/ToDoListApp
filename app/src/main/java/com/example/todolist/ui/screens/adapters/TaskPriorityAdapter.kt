package com.example.todolist.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.google.android.material.card.MaterialCardView

class TaskPriorityAdapter(
    private val context: Context,
    private val priorityLevels: List<Int> = (1..3).toList(),
    private val onPrioritySelected: (Int) -> Unit
) : RecyclerView.Adapter<TaskPriorityAdapter.PriorityViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriorityViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_priority_rv, parent, false)
        return PriorityViewHolder(view)
    }

    override fun onBindViewHolder(holder: PriorityViewHolder, position: Int) {
        val priority = priorityLevels[position]
        holder.priorityText.text = priority.toString()
        holder.cardView.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                if (previousPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)

                onPrioritySelected(priorityLevels[adapterPosition])
            }
        }
    }

    override fun getItemCount(): Int = priorityLevels.size

    inner class PriorityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView as MaterialCardView
        val priorityText: TextView = itemView.findViewById(R.id.text_priority_number)
    }
}
