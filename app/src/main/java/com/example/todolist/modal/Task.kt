package com.example.todolist.modal

import com.google.firebase.Timestamp


data class Task(
    var taskId:String = "",
    val title:String = "",
    val description:String = "",
    val time:String = "",
    val category:String = "",
    val priority:String = "",
    val taskCreatedAtTime:Timestamp = Timestamp.now()
)
