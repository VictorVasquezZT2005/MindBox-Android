package xyz.zt.mindbox.data.model

data class Note(
    val id: String = "",
    val userId: String = "",
    val content: String = "",
    val type: String = "Personal",
    val timestamp: Long = 0L
)