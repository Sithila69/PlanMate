package com.planmate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Alarm triggered")

        // Retrieve the task title from the intent
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Reminder"

        // Launch AlarmActivity
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("taskTitle", taskTitle) // Pass the task title to AlarmActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(alarmIntent)
    }
}
