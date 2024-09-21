package com.planmate

import Task
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import org.json.JSONObject

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

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            stopTimer()
        }
    }
}
