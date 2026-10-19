package com.example.data.model

data class CategoryOption(
    val id: String,
    val name: String,
    val iconKey: String = "tag",
    val colorHex: String = "#0284C7",
    val isDefault: Boolean = false
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            CategoryOption(id = "Task", name = "Task", iconKey = "done", colorHex = "#6366F1", isDefault = true),
            CategoryOption(id = "Idea", name = "Idea", iconKey = "idea", colorHex = "#F59E0B", isDefault = true),
            CategoryOption(id = "Note", name = "Note", iconKey = "note", colorHex = "#EC4899", isDefault = true),
            CategoryOption(id = "Purchase", name = "Purchase", iconKey = "shopping", colorHex = "#10B981", isDefault = true)
        )
    }
}
