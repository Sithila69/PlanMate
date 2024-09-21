package com.planmate

import Task
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.util.Date
import java.util.Locale

class TimerActivity : AppCompatActivity() {

    private lateinit var taskTitleTextView: TextView
    private lateinit var startButton: Button
    private lateinit var totalTimeTextView: TextView
    private lateinit var timerTextView: TextView

    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isRunning = false
    private var taskId: Long = 0
    private var totalElapsedTime: Long = 0
    private val notificationUpdateInterval = 60000L

    private lateinit var setReminderButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor


    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                updateTimerText()
                updateNotificationIfNeeded()
                handler.postDelayed(this, 1000)
            }
        }
    }


    private val CHANNEL_ID = "task_timer_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        taskTitleTextView = findViewById(R.id.taskTitleTextView)
        startButton = findViewById(R.id.startButton)
        totalTimeTextView = findViewById(R.id.totalTimeTextView)
        timerTextView = findViewById(R.id.timerTextView)

        sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        taskId = intent.getLongExtra("taskId", 0)
        totalElapsedTime = loadElapsedTimeFromPreferences(taskId)

        updateTotalTimeText()
        createNotificationChannel()

        startButton.setOnClickListener {
            if (isRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }
        setReminderButton = findViewById(R.id.setReminderButton)
        setReminderButton.setOnClickListener {
            showReminderDialog()
        }
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

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitleTextView.text)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

        Toast.makeText(this, "Reminder set for ${formatDate(calendar.time)}", Toast.LENGTH_LONG).show()
    }
    private fun startTimer() {
        startTime = System.currentTimeMillis()
        isRunning = true
        startButton.text = "Stop Timer"
        sendTimerNotification()
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
        updateNotification() // Final update when stopping
        handler.removeCallbacks(runnable)

        val intent = Intent()
        intent.putExtra("taskId", taskId)
        intent.putExtra("elapsedTime", totalElapsedTime)
        setResult(Activity.RESULT_OK, intent)
        finish() // Optional: close activity when timer stops
    }

    private fun updateTimerText() {
        timerTextView.text = formatElapsedTime(elapsedTime)
        // Remove the updateNotification() call from here
    }
    private fun updateNotificationIfNeeded() {
        if (elapsedTime % notificationUpdateInterval < 1000) {
            updateNotification()
        }
    }

    private fun updateTotalTimeText() {
        totalTimeTextView.text = "Total time: ${formatElapsedTime(totalElapsedTime)}"
    }

    private fun sendTimerNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, TimerActivity::class.java).apply {
            putExtra("taskId", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Timer Started for Task: ${taskTitleTextView.text}")
            .setContentText("Elapsed Time: ${formatElapsedTime(elapsedTime)}")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Timer Running")
            .setContentText("Elapsed Time: ${formatElapsedTime(elapsedTime)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Task Timer Notifications"
            val channelDescription = "Notifications for task timer"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
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
