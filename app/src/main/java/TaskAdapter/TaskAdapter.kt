package com.planmate

import Task
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: List<Task>,
    private val deleteTask: (Task) -> Unit,
    private val editTask: (Task) -> Unit,
    private val openTimerActivity: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.taskTitle)
        val description: TextView = itemView.findViewById(R.id.taskDescription)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val elapsedTime: TextView = itemView.findViewById(R.id.totalTimeTextView)
        init {
            // Set click listener to open TimerActivity when the task item is clicked
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

        // Set the elapsed time text
        holder.elapsedTime.text = formatElapsedTime(task.elapsedTime)

        // Set click listeners for edit and delete buttons
        holder.editButton.setOnClickListener { editTask(task) }
        holder.deleteButton.setOnClickListener { deleteTask(task) }
    }


    override fun getItemCount() = tasks.size

    private fun formatElapsedTime(time: Long): String {
        val seconds = (time / 1000).toInt()
        val minutes = seconds / 60
        val displaySeconds = seconds % 60
        return String.format("%02d:%02d", minutes, displaySeconds)
    }

}
