package com.planmate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

class ReminderActivity : AppCompatActivity() {
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var setReminderButton: Button

    private var taskId: Long = 0
    private lateinit var taskTitle: String
    private lateinit var taskDescription: String
    private lateinit var dueDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        // Retrieve task details from the intent
        taskId = intent.getLongExtra("taskId", 0)
        taskTitle = intent.getStringExtra("taskTitle") ?: "Task Title"
        taskDescription = intent.getStringExtra("taskDescription") ?: "Task Description"
        dueDate = intent.getStringExtra("dueDate") ?: "Due Date"

        datePicker = findViewById(R.id.datePicker)
        timePicker = findViewById(R.id.timePicker)
        setReminderButton = findViewById(R.id.setReminderButton)

        setReminderButton.setOnClickListener {
            scheduleReminder()
        }
    }

    private fun scheduleReminder() {
        // Use the task title directly for the reminder
        val title = taskTitle

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, datePicker.year)
            set(Calendar.MONTH, datePicker.month)
            set(Calendar.DAY_OF_MONTH, datePicker.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitle)
            putExtra("reminderTitle", title) // Send the task title as the reminder title
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                Toast.makeText(this, "Reminder set for $title", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to set reminder. Permission required.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Open settings to request exact alarm permission
            val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(settingsIntent)
            return
        }

        // Return to previous activity
        finish()
    }

    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }
}
