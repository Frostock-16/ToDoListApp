package com.example.todolist

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.modal.Task
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
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
        val userId = Firebase.auth.currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (userId != null) {
            db.collection("users").document(userId).collection("tasks")
                .orderBy("taskCreatedAtTime", Query.Direction.ASCENDING).get()
                .addOnSuccessListener { result ->
                    taskList.clear()
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    for (document in result) {
                        Log.d("FirestoreData", "Document: ${document.data}")
                        val task = document.toObject(Task::class.java)
                        task.taskId = document.id
                        taskList.add(task)
                    }
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val selectedDateString = sdf.format(selectedDate.time)

                    val filtered = taskList.filter { task ->
                        task.time.startsWith(selectedDateString)
                    }
                    if (filtered.isEmpty()) {
                        emptyTaskImageView.visibility = View.VISIBLE
                        emptyTaskTextView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        Log.d("RecyclerViewCheck", "NotShowing ${filtered.size} tasks")
                    } else {
                        emptyTaskImageView.visibility = View.GONE
                        emptyTaskTextView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        Log.d("RecyclerViewCheck", "Showing ${filtered.size} tasks")
                    }
                    itemTaskAdapter.updateList(filtered)
                }

                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting tasks", e)
                }
        }
    }

    private fun fetchCompletedTasksFromFirestore() {
        val userId = Firebase.auth.currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            db.collection("users").document(userId).collection("completed_tasks")
                .orderBy("taskCreatedAtTime", Query.Direction.DESCENDING).get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        Log.d("FirestoreData", "Document: ${document.data}")
                        val task = document.toObject(Task::class.java)
                        task.taskId = document.id
                        Log.d(
                            "FirestoreTask", "taskCreatedAtTime: ${task.taskCreatedAtTime.toDate()}"
                        )
                        taskList.add(task)
                    }
                    Log.d("FirestoreData", "Task list size: ${taskList.size}")
                    itemTaskAdapter.updateList(taskList)
                }.addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting tasks", e)
                }
        }
        taskList.clear()
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
                ).setActionIconTint(Color.parseColor("#C3C3C3"))
                    .addActionIcon(R.drawable.ic_delete).addSwipeLeftLabel("Delete")
                    .setSwipeLeftLabelColor(Color.parseColor("#C3C3C3"))
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
        val userId = Firebase.auth.currentUser?.uid

        if (userId != null && task.taskId.isNotEmpty()) {
            Firebase.firestore.collection("users").document(userId).collection("completed_tasks")
                .document(task.taskId).delete().addOnSuccessListener {
                    taskList.removeAt(position)

                    val sharedPrefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
                    sharedPrefs.edit().putString("TaskLeftCount", "${taskList.size}").apply()

                    itemTaskAdapter.notifyItemRemoved(position)
                    Toast.makeText(
                        this@CalendarScreenActivity, "Task deleted", Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener {
                    Toast.makeText(
                        this@CalendarScreenActivity, "Failed to delete", Toast.LENGTH_SHORT
                    ).show()
                    itemTaskAdapter.notifyItemChanged(position)
                }
        } else {
            itemTaskAdapter.notifyItemChanged(position)
        }

    }
}