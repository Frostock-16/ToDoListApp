package com.example.todolist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HorizontalCalendarAdapter(
    private val daysList: List<Calendar>,
    private var selectedPosition: Int = -1,
    private val onDateSelected: (Calendar) -> Unit
) : RecyclerView.Adapter<HorizontalCalendarAdapter.DayViewHolder>() {

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tv_day)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_rv, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val calendar = daysList[position]
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd", Locale.getDefault())

        holder.tvDay.text = dayFormat.format(calendar.time)
        holder.tvDate.text = dateFormat.format(calendar.time)

        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.bg_selected_calendar_day)
            holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.md_theme_onTertiaryContainer))
            holder.tvDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.md_theme_onTertiaryContainer))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_unselected_calendar_day)
            holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.md_theme_onSurfaceVariant))
            holder.tvDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.md_theme_onSurfaceVariant))
        }

        holder.itemView.setOnClickListener {
            if (selectedPosition == position) return@setOnClickListener
            val oldPos = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPos)
            notifyItemChanged(position)
            onDateSelected(calendar)
        }
    }

    override fun getItemCount(): Int = daysList.size

    fun setSelectedDate(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(position)
    }
}
