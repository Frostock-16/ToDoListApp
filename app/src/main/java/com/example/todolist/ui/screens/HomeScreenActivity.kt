package com.example.todolist.ui.screens

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import com.example.todolist.ui.dialogs.AddTaskDialogFragment
import com.example.todolist.ui.dialogs.FilterBottomSheet
import com.example.todolist.ui.screens.adapters.ItemTaskAdapter
import com.example.todolist.ui.utils.BottomNavUtil
import com.example.todolist.utils.cancelNotification
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

class HomeScreenActivity : BaseActivity() {
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var itemTaskAdapter: ItemTaskAdapter
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var filterImgView: ShapeableImageView
    private var taskCompletedCount = 0
    private var taskList = mutableListOf<Task>()
    private val firestoreService = FirestoreService()

    private var selectedPriority: String? = null
    private val selectedCategories = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen_activity)
        setupToolbar("Home", false)

        filterImgView = findViewById(R.id.filter_icon)
        filterImgView.setOnClickListener {
            val bottomSheet = FilterBottomSheet { priority, categories ->
                Log.d("FILTERS", "Priority: $priority, Categories: $categories")
                applyFilters()
            }
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        // Task left and complete count
        val sharedPrefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
        taskCompletedCount = sharedPrefs.getInt("TaskDoneCount", 0)
        Log.d("TaskDoneCount", "Restored count: $taskCompletedCount")


        // Shimmer layout
        shimmerLayout = findViewById(R.id.shimmer_layout)

        // BottomNavView
        bottomNav = findViewById(R.id.bottom_nav)
        BottomNavUtil.setUpBottomNav(this@HomeScreenActivity, bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        // Button
        fabAdd = findViewById(R.id.fab_add)

        // Set add task dialog fragment
        fabAdd.setOnClickListener {
            AddTaskDialogFragment().show(supportFragmentManager, "AddTaskDialogFragment")
        }

        // Set up recycler view
        setUpRecyclerView()
        itemTouchHelper.attachToRecyclerView(recyclerView)
        supportFragmentManager.setFragmentResultListener("taskAddedRequest", this) { _, _ ->
            fetchTasksFromFirestore()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    bottomNav.animate().translationY(bottomNav.height.toFloat()).setDuration(300)
                        .start()
                    fabAdd.animate().translationY(bottomNav.height / 2f).setDuration(300).start()
                } else if (dy < 0) {
                    bottomNav.animate().translationY(0f).setDuration(300).start()
                    fabAdd.animate().translationY(0f).setDuration(300).start()
                }
            }
        })

        // Material-style SearchView
        val searchEditText = findViewById<TextInputEditText>(R.id.search_edit_text)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterImgView.visibility = if (searchEditText.hasFocus() || !s.isNullOrBlank()) View.GONE else View.VISIBLE
                itemTaskAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            val isSearchEmpty = searchEditText.text.isNullOrBlank()
            filterImgView.visibility = if (hasFocus || !isSearchEmpty) View.GONE else View.VISIBLE
        }


    }

    override fun onResume() {
        super.onResume()
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        fetchTasksFromFirestore()
    }

    private fun applyFilters() {
        val filteredList = taskList.filter { task ->
            val matchesPriority =
                selectedPriority?.equals(task.priority, ignoreCase = true) ?: true
            val matchesCategory =
                if (selectedCategories.isEmpty()) true else selectedCategories.any {
                    it.equals(
                        task.category,
                        ignoreCase = true
                    )
                }
            matchesPriority && matchesCategory
        }
        itemTaskAdapter.updateList(filteredList)
    }

    // Clear focus from search when clicked outside
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val view = currentFocus
        val searchEditText = findViewById<TextInputEditText>(R.id.search_edit_text)

        if (view != null && ev.action == MotionEvent.ACTION_DOWN) {
            val outRect = Rect()
            view.getGlobalVisibleRect(outRect)
            if (view == searchEditText && !outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                view.clearFocus()
                hideKeyboard(view)
                return true
            }
        }

        return super.dispatchTouchEvent(ev)
    }


    fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Display task
    private fun fetchTasksFromFirestore() {
        firestoreService.fetchTasks(
            onSuccess = { tasks ->
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                taskList.clear()
                taskList.addAll(tasks)

                val sharedPrefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
                sharedPrefs.edit { putString("TaskLeftCount", "${taskList.size}") }
                itemTaskAdapter.updateList(taskList)
            },
            onFailure = { e ->
                Log.e("Firestore", "Error getting tasks", e)
            }
        )
    }

    private fun setUpRecyclerView() {
        recyclerView = findViewById(R.id.itemtask_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemTaskAdapter = ItemTaskAdapter(emptyList())
        recyclerView.adapter = itemTaskAdapter
        itemTaskAdapter.listener = object : ItemTaskAdapter.onItemClickListener {
            override fun onItemClick(task: Task) {
                val intent = Intent(this@HomeScreenActivity, TaskScreenActivity::class.java)
                intent.putExtra("taskId", task.taskId)
                    .putExtra("title", task.title)
                    .putExtra("description", task.description)
                    .putExtra("time", task.time)
                    .putExtra("category", task.category)
                    .putExtra("priority", task.priority)

                startActivity(intent)
            }
        }
    }

    // Swipe delete and complete
    private val itemTouchHelper = ItemTouchHelper(object :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

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
                c,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
                .addBackgroundColor(
                    ContextCompat.getColor(
                        this@HomeScreenActivity,
                        R.color.md_theme_errorContainer
                    )
                )
                .setActionIconTint("#C3C3C3".toColorInt())
                .addActionIcon(R.drawable.ic_delete)
                .addSwipeLeftLabel("Delete")
                .setSwipeLeftLabelColor("#C3C3C3".toColorInt())
                .setSwipeLeftLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

                .addSwipeRightBackgroundColor(
                    ContextCompat.getColor(this@HomeScreenActivity, R.color.md_theme_secondary)
                )
                .addSwipeRightLabel("Complete")
                .addSwipeRightActionIcon(R.drawable.ic_check)
                .setSwipeRightLabelColor(Color.WHITE)
                .setSwipeRightLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

                .addCornerRadius(TypedValue.COMPLEX_UNIT_SP, 8)
                .create()
                .decorate()
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    deleteTask(position)
                }

                ItemTouchHelper.RIGHT -> {
                    completedTask(position)
                }
            }
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return 0.2f
        }
    })

    private fun deleteTask(position: Int) {
        val task = taskList[position]
        cancelNotification(this, task.taskId.hashCode())

        firestoreService.deleteTask(
            taskId = task.taskId,
            onSuccess = {
                taskList.removeAt(position)
                val sharedPrefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
                sharedPrefs.edit { putString("TaskLeftCount", "${taskList.size} TASK LEFT") }
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




    private fun completedTask(position: Int) {
        val task = taskList[position]
        cancelNotification(this, task.taskId.hashCode())

        firestoreService.completeTask(
            task,
            onSuccess = {
                taskList.removeAt(position)
                taskCompletedCount++
                itemTaskAdapter.notifyItemRemoved(position)

                val sharedPrefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
                sharedPrefs.edit { putInt("TaskDoneCount", taskCompletedCount) }
                sharedPrefs.edit { putString("TaskLeftCount", "${taskList.size} TASK LEFT") }

                Snackbar.make(findViewById(android.R.id.content), "Task Completed", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.md_theme_onSurface))
                    .setTextColor(ContextCompat.getColor(this, R.color.md_theme_surfaceVariant))
                    .show()
            },
            onFailure = { e ->
                Snackbar.make(findViewById(android.R.id.content), "Failed to mark as completed", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.md_theme_onSurface))
                    .setTextColor(ContextCompat.getColor(this, R.color.md_theme_surfaceVariant))
                    .show()
            }
        )
    }

}