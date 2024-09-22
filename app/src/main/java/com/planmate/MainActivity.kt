package com.planmate

import Task
import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    // UI components and data structures
    private lateinit var taskRecyclerView: RecyclerView
    private val tasks = mutableListOf<Task>() // List to store tasks
    private lateinit var taskAdapter: TaskAdapter // Adapter to manage tasks in RecyclerView
    private lateinit var sharedPreferences: SharedPreferences // SharedPreferences for storing tasks
    private lateinit var editor: SharedPreferences.Editor // Editor for saving data to SharedPreferences
    private var editingTask: Task? = null // Track which task is being edited

    // Timer request code for identifying timer-related results
    private companion object {
        const val TIMER_REQUEST_CODE = 1
    }

    // Register the Activity Result Launcher for TimerActivity results
    private val timerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val taskId = data?.getLongExtra("taskId", -1) ?: -1
            val elapsedTime = data?.getLongExtra("elapsedTime", 0) ?: 0
            if (taskId != -1L) {
                // Find the task by ID and update the elapsed time
                val task = tasks.find { it.id == taskId }
                task?.let {
                    it.elapsedTime += elapsedTime
                    saveTasksToPreferences(tasks) // Save updated tasks to preferences
                    taskAdapter.notifyDataSetChanged() // Notify adapter of changes
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences for storing tasks
        sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // Load saved tasks from SharedPreferences into the tasks list
        tasks.addAll(loadTasksFromPreferences())

        // Initialize RecyclerView and its adapter
        taskRecyclerView = findViewById(R.id.taskListView)
        taskRecyclerView.layoutManager = LinearLayoutManager(this) // Set linear layout for list
        taskAdapter = TaskAdapter(tasks, { task -> deleteTask(task) }, { task -> editTask(task) }) { task -> openTimerActivity(task) }
        taskRecyclerView.adapter = taskAdapter

        // UI references for task title, description, and due date input fields
        val taskTitleInput = findViewById<EditText>(R.id.taskTitleInput)
        val taskDescriptionInput = findViewById<EditText>(R.id.taskDescriptionInput)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)
        val dueDateInput = findViewById<TextInputEditText>(R.id.dueDateInput)

        // Show DatePickerDialog when the due date input is clicked
        dueDateInput.setOnClickListener {
            showDatePickerDialog(dueDateInput)
        }

        // Set click listener for adding or updating tasks
        addTaskButton.setOnClickListener {
            val title = taskTitleInput.text.toString()
            val description = taskDescriptionInput.text.toString()
            val dueDate = dueDateInput.text.toString()

            if (title.isNotEmpty() && dueDate.isNotEmpty()) {
                if (editingTask != null) {
                    // If editing a task, update the task's title, description, and due date
                    editingTask?.title = title
                    editingTask?.description = description
                    editingTask?.dueDate = dueDate
                    editingTask = null // Clear editing state
                    addTaskButton.text = getString(R.string.add_task) // Reset button text
                } else {
                    // Add a new task if no task is being edited
                    val newTask = Task(Random.nextLong(), title, description, false, dueDate = dueDate)
                    tasks.add(newTask) // Add new task to the list
                }

                // Save tasks to SharedPreferences after adding or updating
                saveTasksToPreferences(tasks)
                updateWidget() // Notify the widget to update

                // Notify adapter to update the RecyclerView with new data
                taskAdapter.notifyDataSetChanged()

                // Clear input fields after adding/updating a task
                taskTitleInput.text.clear()
                taskDescriptionInput.text.clear()
                dueDateInput.text?.clear() // Clear the due date input
            } else {
                // Optionally, show a message if title or due date is empty
                // e.g., Toast.makeText(this, "Title and due date are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to show a DatePickerDialog for selecting a due date
    private fun showDatePickerDialog(dateInput: TextInputEditText) {
        // Get the current date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Open DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Update the TextInputEditText with the selected date
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                dateInput.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.datePicker.minDate = calendar.timeInMillis // Ensure date cannot be in the past
        datePickerDialog.show()
    }

    // Function to delete a task
    private fun deleteTask(task: Task) {
        tasks.remove(task) // Remove task from list
        saveTasksToPreferences(tasks) // Save updated tasks to SharedPreferences
        updateWidget() // Notify the widget to update
        taskAdapter.notifyDataSetChanged() // Notify adapter to refresh the list
    }

    // Function to edit a task, pre-populating input fields with task data
    private fun editTask(task: Task) {
        val taskTitleInput = findViewById<EditText>(R.id.taskTitleInput)
        val taskDescriptionInput = findViewById<EditText>(R.id.taskDescriptionInput)
        val dueDateInput = findViewById<TextInputEditText>(R.id.dueDateInput)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)

        // Set input fields to the current task's title, description, and due date
        taskTitleInput.setText(task.title)
        taskDescriptionInput.setText(task.description)
        dueDateInput.setText(task.dueDate)
        editingTask = task // Set the current task as the task being edited
        addTaskButton.text = getString(R.string.update_task) // Change button text to indicate editing
    }

    // Function to open TimerActivity for tracking task time
    private fun openTimerActivity(task: Task) {
        val intent = Intent(this, TimerActivity::class.java).apply {
            putExtra("taskId", task.id) // Pass the task ID to TimerActivity
            putExtra("taskTitle", task.title) // Pass the task title
            putExtra("taskDescription", task.description) // Pass the task description
        }
        timerActivityResultLauncher.launch(intent) // Launch TimerActivity
    }

    // Save the list of tasks to SharedPreferences
    private fun saveTasksToPreferences(tasks: List<Task>) {
        val taskJsonArray = JSONArray() // JSON array to store task objects
        for (task in tasks) {
            val taskObject = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("isCompleted", task.isCompleted)
                put("dueDate", task.dueDate) // Save the due date
            }
            taskJsonArray.put(taskObject) // Add each task to the JSON array
        }
        editor.putString("tasks", taskJsonArray.toString()) // Store tasks as a JSON string
        editor.apply() // Apply the changes to SharedPreferences
    }

    // Load the list of tasks from SharedPreferences
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
                    isCompleted = taskObject.getBoolean("isCompleted"),
                    dueDate = if (taskObject.has("dueDate")) taskObject.getString("dueDate") else "" // Provide a default value
                )
                tasks.add(task)
            }
        }
        return tasks
    }

    // Function to update the widget
    private fun updateWidget() {
        val intent = Intent(this, TaskWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(this)
            .getAppWidgetIds(ComponentName(this, TaskWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

    // Override to handle activity results, specifically for the timer activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TIMER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val taskId = data?.getLongExtra("taskId", -1) ?: -1
            val elapsedTime = data?.getLongExtra("elapsedTime", 0) ?: 0
            if (taskId != -1L) {
                // Find the task by ID and update the elapsed time
                val task = tasks.find { it.id == taskId }
                task?.elapsedTime = elapsedTime // Update the elapsed time for the task
                saveTasksToPreferences(tasks) // Save updated tasks to preferences
                taskAdapter.notifyDataSetChanged() // Notify adapter of changes
            }
        }
    }
}
