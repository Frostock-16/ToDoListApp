package com.example.todolist

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavUtil {
    fun setUpBottomNav(activity:AppCompatActivity, bottomNav:BottomNavigationView)
    {
        bottomNav.setOnItemSelectedListener { item->
            when(item.itemId){
                R.id.nav_home->{
                    if(activity!is HomeScreenActivity){
                        activity.startActivity(Intent(activity, HomeScreenActivity::class.java))
                        activity.finish()
                    }
                }
                R.id.nav_calendar->{
                    if(activity !is CalendarScreenActivity){
                        activity.startActivity(Intent(activity, CalendarScreenActivity::class.java))
                        activity.finish()
                    }
                }

                R.id.nav_focus->{
                    if(activity !is FocusScreenActivity){
                        activity.startActivity(Intent(activity, FocusScreenActivity::class.java))
                        activity.finish()
                    }
                }
                R.id.nav_profile->{
                    if(activity !is ProfileScreenActivity){
                        activity.startActivity(Intent(activity, ProfileScreenActivity::class.java))
                        activity.finish()
                    }
                }
            }
            true
        }
    }
}