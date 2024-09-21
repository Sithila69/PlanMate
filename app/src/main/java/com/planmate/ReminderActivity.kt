package com.planmate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import android.provider.Settings

class ReminderActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var setReminderButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        titleEditText = findViewById(R.id.reminderTitleEditText)
        datePicker = findViewById(R.id.datePicker)
        timePicker = findViewById(R.id.timePicker)
        setReminderButton = findViewById(R.id.setReminderButton)

        setReminderButton.setOnClickListener {
            scheduleReminder()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleReminder() {
        val title = titleEditText.text.toString()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, datePicker.year)
            set(Calendar.MONTH, datePicker.month)
            set(Calendar.DAY_OF_MONTH, datePicker.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_TITLE", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // For Android 12 (API level 31) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    // Handle the SecurityException
                }
            } else {
                // Open settings to request exact alarm permission
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        } else {
            // For Android versions below 12, just set the alarm
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }

        // Return to MainActivity
        finish()
    }
}