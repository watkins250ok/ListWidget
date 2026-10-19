package com.example.data.local

import android.content.Context
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object SampleDataProvider {

    suspend fun populateData(db: AppDatabase, context: Context, forceReset: Boolean = false) = withContext(Dispatchers.IO) {
        val listDao = db.listDao()
        val itemDao = db.listItemDao()

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Tomorrow 6:00 PM
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 0)
        val tomorrowDue = calendar.timeInMillis

        // Remove "Work Projects" list if present
        val existingLists = listDao.getAllListsSync()
        val workProjectsList = existingLists.find { it.name.equals("Work Projects", ignoreCase = true) }
        if (workProjectsList != null) {
            listDao.deleteList(workProjectsList)
        }

        // Rename "To-Do List" to "To-Do" and update description
        val todoList = existingLists.find { it.name.equals("To-Do List", ignoreCase = true) || it.name.equals("To-Do", ignoreCase = true) }
        if (todoList != null) {
            listDao.updateList(todoList.copy(name = "To-Do", description = "Tasks and priorities"))
        }

        val allItems = itemDao.getAllItemsSync()
        val shouldPopulate = forceReset || existingLists.isEmpty() || allItems.isEmpty() || workProjectsList != null || todoList?.name == "To-Do List"

        if (shouldPopulate) {
            itemDao.deleteAllItems()

            val list1Id = todoList?.id ?: listDao.insertList(
                ListEntity(
                    name = "To-Do",
                    iconName = "checklist",
                    description = "Tasks and priorities",
                    colorHex = "#7C3AED",
                    autoDeleteMode = "NEVER",
                    hideCheckedItems = false,
                    createdAt = now - 86400000L * 3,
                    updatedAt = now
                )
            )

            val ideasList = existingLists.find { it.name.equals("Ideas & Inspiration", ignoreCase = true) }
            val list2Id = ideasList?.id ?: listDao.insertList(
                ListEntity(
                    name = "Ideas & Inspiration",
                    iconName = "idea",
                    description = "Product concepts, widget designs, and research notes.",
                    colorHex = "#EC4899",
                    autoDeleteMode = "NEVER",
                    hideCheckedItems = false,
                    createdAt = now - 86400000L * 5,
                    updatedAt = now
                )
            )

            // --- 3 Example Items for "To-Do" List ---
            // All "Task" category. Default priorities and tags. At least one due date.
            itemDao.insertItem(
                ListItemEntity(
                    listId = list1Id,
                    title = "Review home renovation plans",
                    type = "TASK",
                    category = "Task",
                    section = "Home",
                    isChecked = false,
                    dueDate = tomorrowDue,
                    priority = "HIGH",
                    tags = "home",
                    description = "Check kitchen layout, lighting options, and material estimates.",
                    createdAt = now - 3600000L * 5,
                    updatedAt = now - 3600000L * 2
                )
            )

            itemDao.insertItem(
                ListItemEntity(
                    listId = list1Id,
                    title = "Buy groceries for the week",
                    type = "TASK",
                    category = "Task",
                    section = "General",
                    isChecked = false,
                    dueDate = null,
                    priority = "MEDIUM",
                    tags = "home",
                    description = "Fresh produce, milk, eggs, sourdough bread, and coffee beans.",
                    createdAt = now - 3600000L * 8,
                    updatedAt = now - 3600000L * 4
                )
            )

            itemDao.insertItem(
                ListItemEntity(
                    listId = list1Id,
                    title = "Organize workspace desk and cables",
                    type = "TASK",
                    category = "Task",
                    section = "General",
                    isChecked = false,
                    dueDate = null,
                    priority = "LOW",
                    tags = "work",
                    description = "Clean cable management under desk and archive completed paperwork.",
                    createdAt = now - 3600000L * 12,
                    updatedAt = now - 3600000L * 12
                )
            )

            // --- 3 Example Items for "Ideas & Inspiration" List ---
            // Various default categories (Idea, Note, Purchase), tags, priorities. Image on one item.
            itemDao.insertItem(
                ListItemEntity(
                    listId = list2Id,
                    title = "Minimalist habit tracking widget with heatmaps",
                    type = "NOTE",
                    category = "Idea",
                    section = "Concepts",
                    isChecked = false,
                    dueDate = null,
                    priority = "HIGH",
                    tags = "idea,work",
                    description = "A compact widget visualizing daily progress streaks using micro-dots, similar to GitHub contributions.",
                    imageUri = "sample_sticky_notes",
                    createdAt = now - 3600000L * 18,
                    updatedAt = now - 3600000L * 10
                )
            )

            itemDao.insertItem(
                ListItemEntity(
                    listId = list2Id,
                    title = "Ergonomic standing desk chair",
                    type = "TASK",
                    category = "Purchase",
                    section = "Shopping",
                    isChecked = false,
                    dueDate = null,
                    priority = "MEDIUM",
                    tags = "home",
                    description = "Compare models with adjustable lumbar support and breathable mesh.",
                    imageUri = null,
                    createdAt = now - 3600000L * 24,
                    updatedAt = now - 3600000L * 18
                )
            )

            itemDao.insertItem(
                ListItemEntity(
                    listId = list2Id,
                    title = "Color palette moodboard for upcoming themes",
                    type = "NOTE",
                    category = "Note",
                    section = "Design",
                    isChecked = false,
                    dueDate = null,
                    priority = "LOW",
                    tags = "idea",
                    description = "Theme tokens explored:\n  • Neon Violet (#7C3AED)\n  • Cyber Emerald (#059669)\n  • Sunset Amber (#D97706)",
                    imageUri = null,
                    createdAt = now - 86400000L * 2,
                    updatedAt = now - 86400000L * 2
                )
            )
        }
    }

    suspend fun populateIfEmpty(db: AppDatabase, context: Context) {
        populateData(db, context, forceReset = false)
    }
}
