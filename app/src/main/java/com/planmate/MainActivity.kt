package com.planmate

import Task
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var taskRecyclerView: RecyclerView
    private val tasks = mutableListOf<Task>()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var editingTask: Task? = null

    private companion object {
        const val TIMER_REQUEST_CODE = 1
    }

    // Register the Activity Result Launcher
    private val timerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val taskId = data?.getLongExtra("taskId", -1) ?: -1
            val elapsedTime = data?.getLongExtra("elapsedTime", 0) ?: 0
            if (taskId != -1L) {
                val task = tasks.find { it.id == taskId }
                task?.let {
                    it.elapsedTime += elapsedTime
                    saveTasksToPreferences(tasks)
                    taskAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // Load tasks from SharedPreferences
        tasks.addAll(loadTasksFromPreferences())

        // Initialize UI components
        taskRecyclerView = findViewById(R.id.taskListView)
        taskRecyclerView.layoutManager = LinearLayoutManager(this) // Set layout manager
        taskAdapter = TaskAdapter(tasks, { task -> deleteTask(task) }, { task -> editTask(task) }) { task -> openTimerActivity(task) }
        taskRecyclerView.adapter = taskAdapter

        val taskTitleInput = findViewById<EditText>(R.id.taskTitleInput)
        val taskDescriptionInput = findViewById<EditText>(R.id.taskDescriptionInput)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)

        addTaskButton.setOnClickListener {
            val title = taskTitleInput.text.toString()
            val description = taskDescriptionInput.text.toString()
            if (editingTask != null) {
                // Update existing task
                editingTask?.title = title
                editingTask?.description = description
                editingTask = null
                addTaskButton.text = "Add Task" // Reset button text
            } else {
                // Add new task
                if (title.isNotEmpty()) {
                    val newTask = Task(System.currentTimeMillis(), title, description, false)
                    tasks.add(newTask)
                }
            }

            // Save the updated task list to SharedPreferences
            saveTasksToPreferences(tasks)

            // Update the RecyclerView
            taskAdapter.notifyDataSetChanged()

            // Clear input fields
            taskTitleInput.text.clear()
            taskDescriptionInput.text.clear()
        }
    }

    private fun deleteTask(task: Task) {
        tasks.remove(task)
        saveTasksToPreferences(tasks)
        taskAdapter.notifyDataSetChanged()
    }

    private fun editTask(task: Task) {
        // Populate input fields with the task's data for editing
        val taskTitleInput = findViewById<EditText>(R.id.taskTitleInput)
        val taskDescriptionInput = findViewById<EditText>(R.id.taskDescriptionInput)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)

        taskTitleInput.setText(task.title)
        taskDescriptionInput.setText(task.description)
        editingTask = task // Set the task to be edited
        addTaskButton.text = "Update Task" // Change button text to indicate editing
    }

    private fun openTimerActivity(task: Task) {
        val intent = Intent(this, TimerActivity::class.java).apply {
            putExtra("taskId", task.id)
        }
        timerActivityResultLauncher.launch(intent) // Use the registered launcher to start the activity
    }

    private fun saveTasksToPreferences(tasks: List<Task>) {
        val taskJsonArray = JSONArray()
        for (task in tasks) {
            val taskObject = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("isCompleted", task.isCompleted)
            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TIMER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val taskId = data?.getLongExtra("taskId", -1) ?: -1
            val totalElapsedTime = data?.getLongExtra("elapsedTime", 0) ?: 0
            if (taskId != -1L) {
                val task = tasks.find { it.id == taskId }
                task?.let {
                    it.elapsedTime = totalElapsedTime
                    saveTasksToPreferences(tasks)
                    taskAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}
