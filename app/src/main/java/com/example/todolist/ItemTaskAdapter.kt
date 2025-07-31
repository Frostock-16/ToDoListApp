package com.example.todolist


import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.modal.Task

class ItemTaskAdapter(private var itemList: List<Task>) :
    RecyclerView.Adapter<ItemTaskAdapter.ItemTaskViewHolder>() {

    interface onItemClickListener {
        fun onItemClick(task: Task)
    }

    var listener: onItemClickListener? = null
    private var fullList = ArrayList<Task>()

    init {
        fullList.addAll(itemList)
    }

    val categoryColorMap = mapOf(
        "Work" to R.color.orange,
        "Health" to R.color.red,
        "Music" to R.color.purple,
        "Movie" to R.color.sand,
        "University" to R.color.blue,
        "Design" to R.color.teal,
        "Social" to R.color.pink,
        "Sport" to R.color.cyan,
        "Home" to R.color.yellow,
        "Grocery" to R.color.green
    )


    class ItemTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFlagIcon: ImageView = itemView.findViewById(R.id.flag_icon)
        val tvTaskTitle: TextView = itemView.findViewById(R.id.task_title)
        val tvTaskTime: TextView = itemView.findViewById(R.id.task_time)
        val tvTaskCategory: TextView = itemView.findViewById(R.id.task_category)
        val tvFlagCount: TextView = itemView.findViewById(R.id.flag_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTaskViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_task_rv, parent, false)
        return ItemTaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        val item = itemList[position]
        holder.tvTaskTitle.text = item.title

        val colorResId = categoryColorMap[item.category] ?: R.color.md_theme_onSurfaceVariant
        val color = ContextCompat.getColor(holder.itemView.context, colorResId)

        ViewCompat.setBackgroundTintList(
            holder.tvTaskCategory,
            ColorStateList.valueOf(color)
        )

        holder.tvTaskTime.apply {
            if (item.time == "null") {
                holder.tvTaskTime.visibility = View.INVISIBLE
            } else {
                holder.tvTaskTime.visibility = View.VISIBLE
                holder.tvTaskTime.text = item.time
            }
        }

        holder.tvTaskCategory.apply {
            if (item.category == "null") {
                holder.tvTaskCategory.visibility = View.GONE
            } else {
                holder.tvTaskCategory.visibility = View.VISIBLE
                holder.tvTaskCategory.text = item.category
            }
        }

        holder.tvFlagCount.apply {
            if (item.priority == "null") {
                holder.tvFlagCount.visibility = View.GONE
                holder.ivFlagIcon.visibility = View.GONE
            } else {
                holder.ivFlagIcon.visibility = View.VISIBLE
                holder.tvFlagCount.visibility = View.VISIBLE
                holder.tvFlagCount.text = item.priority
            }
        }

        holder.itemView.setOnClickListener {
            listener?.onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Task>) {
        fullList.clear()
        fullList.addAll(newList)
        itemList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        itemList = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.title.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }


}