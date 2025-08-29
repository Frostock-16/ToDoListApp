package com.example.todolist.ui.screens

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.BaseActivity
import com.example.todolist.R
import com.example.todolist.data.modal.Task
import com.example.todolist.data.remote.FirestoreService
import com.example.todolist.ui.screens.adapters.HorizontalCalendarAdapter
import com.example.todolist.ui.screens.adapters.ItemTaskAdapter
import com.example.todolist.ui.utils.BottomNavUtil
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarScreenActivity : BaseActivity() {
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemTaskAdapter: ItemTaskAdapter
    private lateinit var emptyTaskImageView: ImageView
    private lateinit var emptyTaskTextView: TextView
    private var currentSelectedDate: Calendar = Calendar.getInstance()
    private var isShowingCompleted = false
    private val taskList = mutableListOf<Task>()
    private val firestoreService = FirestoreService()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendarscreen_activity)

        setupToolbar("Calendar", true)

        // Shimmer layout
        shimmerLayout = findViewById(R.id.shimmer_layout)

        //Button
        val btnCompleted = findViewById<MaterialButton>(R.id.completed_btn)

        //TextView
        val selectedDateTextView = findViewById<TextView>(R.id.selectedDateText)
        emptyTaskImageView = findViewById(R.id.emptyTaskImageView)
        emptyTaskTextView = findViewById(R.id.emptyTaskTextView)

        // BottomNavView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        BottomNavUtil.setUpBottomNav(this@CalendarScreenActivity, bottomNav)
        bottomNav.selectedItemId = R.id.nav_calendar

        // Display current date textview
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val today = Calendar.getInstance()
        selectedDateTextView.text = displayFormat.format(today.time)

        btnCompleted.setOnClickListener {
            isShowingCompleted = !isShowingCompleted

            if (isShowingCompleted) {
                btnCompleted.text = "Hide Completed Tasks"
                emptyTaskImageView.visibility = View.GONE
                emptyTaskTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                itemTouchHelper.attachToRecyclerView(recyclerView)
                fetchCompletedTasksFromFirestore()
            } else {
                btnCompleted.text = "Show Completed Tasks"
                fetchTasksFromFirestore(currentSelectedDate)
            }
        }
        setUpRecyclerView()

        // Hide/Show bottom nav
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    bottomNav.animate().translationY(bottomNav.height.toFloat()).setDuration(300)
                        .start()
                } else if (dy < 0) {
                    bottomNav.animate().translationY(0f).setDuration(300).start()
                }
            }
        })

        setBackToHome()
    }
    override fun onResume() {
        super.onResume()
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        setUpCalendarView()
    }

    // Setup calendar
    private fun setUpCalendarView() {
        val calendarRecyclerView = findViewById<RecyclerView>(R.id.calendarRecyclerView)
        val daysList = getNextNDays(30)

        val calendarAdapter = HorizontalCalendarAdapter(daysList) { selectedDate ->
            currentSelectedDate = selectedDate

            if (!isShowingCompleted) {
                fetchTasksFromFirestore(selectedDate)
            }
        }

        calendarRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        calendarRecyclerView.adapter = calendarAdapter

        calendarAdapter.setSelectedDate(0)
        fetchTasksFromFirestore(daysList[0])
    }

    private fun getNextNDays(daysCount: Int): List<Calendar> {
        val calendarList = mutableListOf<Calendar>()
        val calendar = Calendar.getInstance()

        for (i in 0 until daysCount) {
            val day = calendar.clone() as Calendar
            calendarList.add(day)
            calendar.add(Calendar.DATE, 1)
        }
        return calendarList
    }


    private fun setUpRecyclerView() {
        recyclerView = findViewById(R.id.itemtask_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemTaskAdapter = ItemTaskAdapter(emptyList())
        recyclerView.adapter = itemTaskAdapter
        itemTaskAdapter.listener = object : ItemTaskAdapter.onItemClickListener {
            override fun onItemClick(task: Task) {
                val intent = Intent(this@CalendarScreenActivity, TaskScreenActivity::class.java)
                intent.putExtra("taskId", task.taskId).putExtra("title", task.title)
                    .putExtra("description", task.description).putExtra("time", task.time)
                    .putExtra("category", task.category).putExtra("priority", task.priority)

                startActivity(intent)
            }
        }
    }

    private fun fetchTasksFromFirestore(selectedDate: Calendar) {
        firestoreService.fetchTasksForDate(
            selectedDate,
            onSuccess = { filtered ->
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE

                taskList.clear()
                taskList.addAll(filtered)

                if (taskList.isEmpty()) {
                    emptyTaskImageView.visibility = View.VISIBLE
                    emptyTaskTextView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyTaskImageView.visibility = View.GONE
                    emptyTaskTextView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
                itemTaskAdapter.updateList(taskList)
            },
            onFailure = { e ->
                Log.e("Firestore", "Error getting tasks", e)
            }
        )
    }

    private fun fetchCompletedTasksFromFirestore() {
        firestoreService.fetchCompletedTasks(
            onSuccess = { tasks ->
                taskList.clear()
                taskList.addAll(tasks)
                itemTaskAdapter.updateList(taskList)
            },
            onFailure = { e ->
                Log.e("Firestore", "Error getting completed tasks", e)
            }
        )
    }



    // Swipe Delete Completed Task
    private val itemTouchHelper =
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                ).addBackgroundColor(
                    ContextCompat.getColor(
                        this@CalendarScreenActivity, R.color.md_theme_errorContainer
                    )
                ).setActionIconTint("#C3C3C3".toColorInt())
                    .addActionIcon(R.drawable.ic_delete).addSwipeLeftLabel("Delete")
                    .setSwipeLeftLabelColor("#C3C3C3".toColorInt())
                    .setSwipeLeftLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    .addCornerRadius(TypedValue.COMPLEX_UNIT_SP, 8).create().decorate()
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                deleteCompletedTask(position)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.2f
            }
        })

    private fun deleteCompletedTask(position: Int) {
        val task = taskList[position]

        firestoreService.deleteCompletedTask(
            taskId = task.taskId,
            onSuccess = {
                taskList.removeAt(position)
                val sharedPrefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
                sharedPrefs.edit { putString("TaskLeftCount", "${taskList.size}") }
                itemTaskAdapter.notifyItemRemoved(position)

                Snackbar.make(findViewById(android.R.id.content), "Task deleted", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.md_theme_onSurface))
                    .setTextColor(ContextCompat.getColor(this, R.color.md_theme_surfaceVariant))
                    .show()
            },
            onFailure = {
                Snackbar.make(findViewById(android.R.id.content), "Failed to delete", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.md_theme_errorContainer))
                    .setTextColor(ContextCompat.getColor(this, R.color.md_theme_onErrorContainer))
                    .show()

                itemTaskAdapter.notifyItemChanged(position)
            }
        )
    }


}