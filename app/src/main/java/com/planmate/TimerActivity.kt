package com.planmate

import Task
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimerActivity : AppCompatActivity() {

    private lateinit var taskTitleTextView: TextView
    private lateinit var startButton: Button
    private lateinit var totalTimeTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var taskDescriptionTextView: TextView
    private lateinit var setReminderButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isRunning = false
    private var taskId: Long = 0
    private var totalElapsedTime: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                updateTimerText()
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        taskTitleTextView = findViewById(R.id.taskTitleTextView)
        taskDescriptionTextView = findViewById(R.id.taskDescriptionTextView)
        startButton = findViewById(R.id.startButton)
        totalTimeTextView = findViewById(R.id.totalTimeTextView)
        timerTextView = findViewById(R.id.timerTextView)
        setReminderButton = findViewById(R.id.setReminderButton)

        taskId = intent.getLongExtra("taskId", 0)
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Task Title"
        val taskDescription = intent.getStringExtra("taskDescription") ?: "Task Description"

        taskTitleTextView.text = taskTitle
        taskDescriptionTextView.text = taskDescription

        sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        totalElapsedTime = loadElapsedTimeFromPreferences(taskId)

        updateTotalTimeText()

        startButton.setOnClickListener {
            if (isRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }

        setReminderButton.setOnClickListener {
            showReminderDialog()
        }
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        isRunning = true
        startButton.text = "Stop Timer"
        handler.post(runnable)
    }

    private fun stopTimer() {
        isRunning = false
        startButton.text = "Start Timer"

        totalElapsedTime += elapsedTime
        saveElapsedTimeToPreferences(taskId, totalElapsedTime)
        updateTotalTimeText()

        elapsedTime = 0
        updateTimerText()
        handler.removeCallbacks(runnable)

        val intent = Intent()
        intent.putExtra("taskId", taskId)
        intent.putExtra("elapsedTime", totalElapsedTime)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun updateTimerText() {
        timerTextView.text = formatElapsedTime(elapsedTime)
    }

    private fun updateTotalTimeText() {
        totalTimeTextView.text = "Total time: ${formatElapsedTime(totalElapsedTime)}"
    }

    private fun showReminderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reminder, null)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Set Reminder")
            .setPositiveButton("Set") { _, _ ->
                val year = datePicker.year
                val month = datePicker.month
                val day = datePicker.dayOfMonth
                val hour = timePicker.hour
                val minute = timePicker.minute

                setReminder(year, month, day, hour, minute)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setReminder(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // Here you would typically set up an AlarmManager to schedule the reminder
        // For this example, we'll just show a toast message
        Toast.makeText(this, "Reminder set for ${formatDate(calendar.time)}", Toast.LENGTH_LONG).show()
    }

    private fun saveElapsedTimeToPreferences(taskId: Long, time: Long) {
        editor.putLong("elapsedTime_$taskId", time)
        editor.apply()
    }

    private fun loadElapsedTimeFromPreferences(taskId: Long): Long {
        return sharedPreferences.getLong("elapsedTime_$taskId", 0)
    }

    private fun formatElapsedTime(time: Long): String {
        val seconds = (time / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }

    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            stopTimer()
        }
    }
}