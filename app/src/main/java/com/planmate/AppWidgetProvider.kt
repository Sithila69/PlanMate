package com.planmate

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MyAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Example: Update widget text
        val taskTitle = getTaskTitleFromPreferences(context)
        views.setTextViewText(R.id.widgetTextView, taskTitle)

        // Handle button click
        val intent = Intent(context, TimerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getTaskTitleFromPreferences(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("task_title_key", "No Task") ?: "No Task"
    }
}
