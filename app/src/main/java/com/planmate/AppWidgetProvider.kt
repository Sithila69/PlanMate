package com.planmate

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray

class TaskWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds)
            }
        }
    }


    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val sharedPreferences = context.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        val taskJsonString = sharedPreferences.getString("tasks", null)
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Set up the intent to launch MainActivity when the widget is clicked
        val intent = Intent(context, MainActivity::class.java)
        // Create a PendingIntent with FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_task_list, pendingIntent) // Set the click listener on the TextView

        if (!taskJsonString.isNullOrEmpty()) {
            val taskJsonArray = JSONArray(taskJsonString)
            val taskList = StringBuilder()

            for (i in 0 until minOf(taskJsonArray.length(), 3)) {
                val taskObject = taskJsonArray.getJSONObject(i)
                taskList.append("• ${taskObject.getString("title")}\n")
            }

            views.setTextViewText(R.id.widget_task_list, taskList.toString())
        } else {
            views.setTextViewText(R.id.widget_task_list, "No tasks")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }


}