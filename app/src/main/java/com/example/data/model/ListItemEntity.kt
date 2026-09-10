package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val title: String,
    val type: String = "TASK", // "TASK" (checkbox) or "NOTE" (bullet point)
    val category: String = "Task", // "Task", "Idea", "Note", or custom category
    val section: String = "", // Section name within list e.g. "Work", "Ideas", "General"
    val isChecked: Boolean = false,
    val checkedAt: Long? = null,
    val description: String = "", // rich text with bullet list support
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null, // timestamp in millis or null
    val priority: String = "NONE", // "NONE", "LOW", "MEDIUM", "HIGH", or custom
    val tags: String = "", // comma-separated tags e.g. "Architecture,Design"
    val subtasksJson: String = "[]" // JSON array of SubTask
) {
    val effectiveCategory: String
        get() = when {
            category.isNotBlank() -> category
            type == "NOTE" -> "Note"
            else -> "Task"
        }
}

