data class Task(
    var id: Long,
    var title: String,
    var description: String,
    var isCompleted: Boolean,
    var elapsedTime: Long = 0
)
