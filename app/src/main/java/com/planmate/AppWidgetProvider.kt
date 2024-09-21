package com.planmate

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import org.json.JSONArray

class TaskWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val sharedPreferences = context.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        val taskJsonString = sharedPreferences.getString("tasks", null)
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

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