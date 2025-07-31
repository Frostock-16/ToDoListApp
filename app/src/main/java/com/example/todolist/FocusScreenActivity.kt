package com.example.todolist

import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class FocusScreenActivity : BaseActivity() {
    private var isFocusModeOn = false
    private var secondsElapsed = 0
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var bottomNav: BottomNavigationView
    private var focusStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.focus_mode_activity)
        setupToolbar("Focus Mode", true)

        // Setup bottom nav
        bottomNav = findViewById(R.id.bottom_nav)
        BottomNavUtil.setUpBottomNav(this@FocusScreenActivity, bottomNav)
        bottomNav.selectedItemId = R.id.nav_focus

        // Focus mode
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                secondsElapsed++
                updateTimerUI()
                handler.postDelayed(this, 1000)
            }
        }

        val focusButton: Button = findViewById(R.id.btnStartFocus)
        focusButton.setOnClickListener {
            if (isFocusModeOn) {
                stopFocusMode()
            } else {
                startFocusMode()
            }
        }


        // Hide/Show bottom nav
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        var lastScrollY = 0
        val scrollThreshold = 10

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val currentScrollY = scrollView.scrollY
            when {
                currentScrollY - lastScrollY > scrollThreshold -> {
                    // Scrolling down
                    bottomNav.animate().translationY(bottomNav.height.toFloat()).setDuration(300).start()
                }
                lastScrollY - currentScrollY > scrollThreshold -> {
                    // Scrolling up
                    bottomNav.animate().translationY(0f).setDuration(300).start()
                }
            }
            lastScrollY = currentScrollY
        }

        setBackToHome()
        requestUsageStatsPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        setupFocusChart()
        displayTopUsedApps()
    }

    // sets the app on dnd
    private fun startFocusMode() {
        isFocusModeOn = true
        focusStartTime = System.currentTimeMillis()
        secondsElapsed = 0
        handler.post(runnable)
        requestDndPermissionAndEnable()

        findViewById<Button>(R.id.btnStartFocus).text = "Stop Focusing"
    }

    private fun stopFocusMode() {
        isFocusModeOn = false
        val focusEndTime = System.currentTimeMillis()
        val durationMillis = focusEndTime - focusStartTime
        val durationMinutes = durationMillis / (1000f * 60)
        Log.d("FocusTime", "Duration = $durationMinutes minutes")
        saveFocusDataForToday(durationMinutes)
//        debugPrintSavedFocusData()
        setupFocusChart()
        handler.removeCallbacks(runnable)
        disableDndMode()

        findViewById<Button>(R.id.btnStartFocus).text = "Start Focusing"
    }

    private fun updateTimerUI() {
        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        findViewById<TextView>(R.id.timerText).text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun requestDndPermissionAndEnable() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    private fun disableDndMode() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    // graph/chart for time spent on focus mode
    private fun setupFocusChart() {
        val barChart = findViewById<BarChart>(R.id.focusBarChart)

        val prefs = getSharedPreferences("focus_stats", Context.MODE_PRIVATE)
        val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val focusData = days.map { day -> prefs.getFloat(day, 0f) }

        focusData.forEachIndexed { index, value ->
            Log.d("ChartData", "${days[index]} = $value")
        }

        val entries = focusData.mapIndexed { index, minutes ->
            BarEntry(index.toFloat(), minutes)
        }

        val dataSet = BarDataSet(entries, "Focus Time (min)").apply {
            color = ContextCompat.getColor(barChart.context, R.color.md_theme_primaryContainer)
            valueTextColor = ContextCompat.getColor(barChart.context, R.color.md_theme_onPrimaryContainer)
            valueTextSize = 12f
            setDrawValues(true)
            barShadowColor = Color.TRANSPARENT
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.4f
        }

        barChart.apply {
            data = barData
            setFitBars(true)
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            setDrawBorders(true)
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)

            // X Axis styling
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(days)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                granularity = 1f
                labelCount = days.size
                textColor = ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)
                textSize = 12f
            }

            // Y Axis styling
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)
                textSize = 12f
            }

            axisRight.isEnabled = false
            setNoDataText("No focus data yet")
            setNoDataTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant))

            invalidate()
        }
    }

    private fun saveFocusDataForToday(duration: Float) {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        val day = sdf.format(Date()).uppercase(Locale.getDefault())

        val prefs = getSharedPreferences("focus_stats", Context.MODE_PRIVATE)
        val currentTotal = prefs.getFloat(day, 0f)
        prefs.edit().putFloat(day, currentTotal + duration).commit()
    }

//    private fun debugPrintSavedFocusData() {
//        val prefs = getSharedPreferences("focus_stats", Context.MODE_PRIVATE)
//        val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
//        for (day in days) {
//            Log.d("DEBUG_STATS", "$day: ${prefs.getFloat(day, 0f)}")
//        }
//    }

    private fun requestUsageStatsPermissionIfNeeded() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun displayTopUsedApps() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000 // last 24 hrs

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ).filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(5)

        val container = findViewById<LinearLayout>(R.id.appUsageContainer)
        container.removeAllViews()

        for (stat in stats) {
            val packageName = stat.packageName
            val appName = getAppNameFromPackage(packageName)
            val appIcon = getAppIcon(packageName)
            val timeInMin = stat.totalTimeInForeground / (1000 * 60)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 16, 8, 16)
            }

            val iconView = android.widget.ImageView(this).apply {
                setImageDrawable(appIcon)
                layoutParams = LinearLayout.LayoutParams(80, 80)
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 0, 0)
            }

            val nameView = TextView(this).apply {
                text = appName
                textSize = 16f
                setTextColor(Color.WHITE)
            }

            val timeView = TextView(this).apply {
                text = "${timeInMin} min today"
                textSize = 12f
                setTextColor(Color.LTGRAY)
            }

            textLayout.addView(nameView)
            textLayout.addView(timeView)

            row.addView(iconView)
            row.addView(textLayout)

            container.addView(row)
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            AppCompatResources.getDrawable(
                this@FocusScreenActivity,
                R.drawable.ic_launcher_foreground
            )
        }
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val appInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
