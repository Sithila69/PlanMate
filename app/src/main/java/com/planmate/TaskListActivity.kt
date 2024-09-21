package com.example.planmate

import Task
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.planmate.R
import org.json.JSONArray
import org.json.JSONObject

class TaskListActivity : AppCompatActivity() {
    private lateinit var taskListView: ListView
    private val tasks = mutableListOf<Task>()
    private lateinit var taskAdapter: ArrayAdapter<Task>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // Load tasks from SharedPreferences
        tasks.addAll(loadTasksFromPreferences())

        // Initialize UI components
        taskListView = findViewById(R.id.taskListView)
        taskAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tasks)
        taskListView.adapter = taskAdapter

        val taskTitleInput = findViewById<EditText>(R.id.taskTitleInput)
        val taskDescriptionInput = findViewById<EditText>(R.id.taskDescriptionInput)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)

        addTaskButton.setOnClickListener {
            val title = taskTitleInput.text.toString()
            val description = taskDescriptionInput.text.toString()
            if (title.isNotEmpty()) {
                val newTask = Task(System.currentTimeMillis(), title, description, false)
                tasks.add(newTask)

                // Save the updated task list to SharedPreferences
                saveTasksToPreferences(tasks)

                // Update the ListView
                taskAdapter.notifyDataSetChanged()

                // Clear input fields
                taskTitleInput.text.clear()
                taskDescriptionInput.text.clear()
            }
        }
    }

    private fun saveTasksToPreferences(tasks: List<Task>) {
        val taskJsonArray = JSONArray()
        for (task in tasks) {
            val taskObject = JSONObject()
            taskObject.put("id", task.id)
            taskObject.put("title", task.title)
            taskObject.put("description", task.description)
            taskObject.put("isCompleted", task.isCompleted)
            taskJsonArray.put(taskObject)
        }
        editor.putString("tasks", taskJsonArray.toString())
        editor.apply()
    }

    private fun loadTasksFromPreferences(): MutableList<Task> {
        val tasks = mutableListOf<Task>()
        val taskJsonString = sharedPreferences.getString("tasks", null)

        if (!taskJsonString.isNullOrEmpty()) {
            val taskJsonArray = JSONArray(taskJsonString)
            for (i in 0 until taskJsonArray.length()) {
                val taskObject = taskJsonArray.getJSONObject(i)
                val task = Task(
                    id = taskObject.getLong("id"),
                    title = taskObject.getString("title"),
                    description = taskObject.getString("description"),
                    isCompleted = taskObject.getBoolean("isCompleted")
                )
                tasks.add(task)
            }
        }
        return tasks
    }
}
