package com.planmate

import Task
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val context: Context,
    private val tasks: List<Task>,
    private val deleteTask: (Task) -> Unit,
    private val editTask: (Task) -> Unit,
    private val openTimerActivity: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val sharedPreferences = context.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.taskTitle)
        val description: TextView = itemView.findViewById(R.id.taskDescription)
        val dueDate: TextView = itemView.findViewById(R.id.dueDate)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val elapsedTime: TextView = itemView.findViewById(R.id.totalTimeTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openTimerActivity(tasks[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.title.text = task.title
        holder.description.text = task.description
        holder.dueDate.text = task.dueDate

        // Load the total elapsed time from SharedPreferences
        val totalElapsedTime = loadElapsedTimeFromPreferences(task.id)
        holder.elapsedTime.text = formatElapsedTime(totalElapsedTime)

        holder.editButton.setOnClickListener { editTask(task) }
        holder.deleteButton.setOnClickListener { deleteTask(task) }
    }

    override fun getItemCount() = tasks.size

    private fun loadElapsedTimeFromPreferences(taskId: Long): Long {
        return sharedPreferences.getLong("elapsedTime_$taskId", 0)
    }

    private fun formatElapsedTime(time: Long): String {
        val seconds = (time / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }
}