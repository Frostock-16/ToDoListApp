package com.example.todolist

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

object showDialogFragUtil {
    fun showDialogFragment(fragment: DialogFragment,manager: FragmentManager, key:String, tag:String)
    {
        val bundle = Bundle()
        bundle.putBoolean(key, true)
        fragment.arguments = bundle
        fragment.show(manager, tag)
    }
}