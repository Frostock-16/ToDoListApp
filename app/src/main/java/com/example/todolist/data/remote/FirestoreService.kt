package com.example.todolist.data.remote

import android.util.Log
import com.example.todolist.data.modal.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FirestoreService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun fetchTasks(
        onSuccess: (List<Task>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .orderBy("taskCreatedAtTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val taskList = mutableListOf<Task>()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    task.taskId = document.id
                    taskList.add(task)
                }
                onSuccess(taskList)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreService", "Error fetching tasks", e)
                onFailure(e)
            }
    }

    fun deleteTask(
        taskId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun completeTask(
        task: Task,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = db.collection("users").document(userId)

        userDoc.collection("completed_tasks")
            .add(task)
            .addOnSuccessListener {
                userDoc.collection("tasks")
                    .document(task.taskId)
                    .delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun fetchTasksForDate(
        selectedDate: Calendar,
        onSuccess: (List<Task>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .orderBy("taskCreatedAtTime", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val taskList = mutableListOf<Task>()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    task.taskId = document.id
                    taskList.add(task)
                }

                // Filter tasks for selected date
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val selectedDateString = sdf.format(selectedDate.time)
                val filtered = taskList.filter { task ->
                    task.time.startsWith(selectedDateString)
                }

                onSuccess(filtered)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun fetchCompletedTasks(
        onSuccess: (List<Task>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("completed_tasks")
            .orderBy("taskCreatedAtTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val taskList = mutableListOf<Task>()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    task.taskId = document.id
                    taskList.add(task)
                }
                onSuccess(taskList)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteCompletedTask(
        taskId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("completed_tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateTask(
        taskId: String,
        field: String,
        value: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .document(taskId)
            .update(field, value)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addTask(
        task: Task,
        onSuccess: (docId: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid
            ?: return onFailure(IllegalStateException("User not logged in"))

        db.collection("users")
            .document(uid)
            .collection("tasks")
            .add(task)
            .addOnSuccessListener { ref -> onSuccess(ref.id) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun resetUserTasks(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        // Delete active tasks
        db.collection("users").document(userId).collection("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (document in snapshot) {
                    batch.delete(document.reference)
                }

                // Delete completed tasks after tasks
                db.collection("users").document(userId).collection("completed_tasks")
                    .get()
                    .addOnSuccessListener { snapshot2 ->
                        for (document in snapshot2) {
                            batch.delete(document.reference)
                        }

                        batch.commit()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure(e) }
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }


}
