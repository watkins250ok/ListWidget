package com.example.data.local

import android.content.Context
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.SubTask
import java.util.Calendar

object SampleDataProvider {

    suspend fun populateIfEmpty(db: AppDatabase, context: Context) {
        val listDao = db.listDao()
        val itemDao = db.listItemDao()

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Today 6:00 PM
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 0)
        val todayDue = calendar.timeInMillis

        // Sep 2
        calendar.add(Calendar.DAY_OF_MONTH, 2)
        val sep2Due = calendar.timeInMillis

        // Tomorrow
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrowDue = calendar.timeInMillis

        val list1Id = listDao.insertList(
            ListEntity(
                name = "To-Do List",
                iconName = "checklist",
                description = "Daily priorities, development milestones, and errand tracking.",
                colorHex = "#7C3AED",
                autoDeleteMode = "NEVER",
                hideCheckedItems = false,
                createdAt = now - 86400000L * 3,
                updatedAt = now
            )
        )

        val list2Id = listDao.insertList(
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

        val list3Id = listDao.insertList(
            ListEntity(
                name = "Work Projects",
                iconName = "work",
                description = "Sprint goals, release checklist, and team sync agendas.",
                colorHex = "#0284C7",
                autoDeleteMode = "AFTER_24H",
                hideCheckedItems = false,
                createdAt = now - 86400000L * 7,
                updatedAt = now
            )
        )

        val subtasks1 = listOf(
            SubTask("1", "Check system sequence diagram", true),
            SubTask("2", "Validate database schema migration", true),
            SubTask("3", "Verify background service worker limits", false)
        )

        // Item 1: Review quarterly architecture design document
        itemDao.insertItem(
            ListItemEntity(
                listId = list1Id,
                title = "Review quarterly architecture design document",
                type = "TASK",
                category = "Task",
                section = "Milestones",
                isChecked = false,
                dueDate = sep2Due,
                priority = "MEDIUM",
                tags = "Architecture",
                description = "Ensure clean separation between data repositories, widget RemoteViews providers, and Compose UI view models.\n\nKey Focus Areas:\n  • Modular database entities\n  • Low memory footprint for widgets\n  • Robust offline-first caching",
                subtasksJson = SubTask.toJson(subtasks1),
                createdAt = now - 3600000L * 5,
                updatedAt = now - 3600000L * 2
            )
        )

        // Item 2: Finalize mobile widget layout interactions
        itemDao.insertItem(
            ListItemEntity(
                listId = list1Id,
                title = "Finalize mobile widget layout interactions",
                type = "TASK",
                category = "Task",
                section = "Milestones",
                isChecked = true,
                checkedAt = now - 1800000L,
                dueDate = todayDue,
                priority = "HIGH",
                tags = "Design",
                description = "Implement direct checkbox toggling in the Android AppWidget without opening the full application pop-up.",
                createdAt = now - 3600000L * 8,
                updatedAt = now - 1800000L
            )
        )

        // Item 3: Pick up dry cleaning & packages
        itemDao.insertItem(
            ListItemEntity(
                listId = list1Id,
                title = "Pick up dry cleaning & packages",
                type = "TASK",
                category = "Task",
                section = "Personal",
                isChecked = false,
                dueDate = null, // No due date -> hidden in widget
                priority = "LOW",
                tags = "Errands",
                description = "Pick up order #8492 before 6:30 PM. Remember receipt in glove compartment.",
                createdAt = now - 3600000L * 12,
                updatedAt = now - 3600000L * 12
            )
        )

        // Item 4: WiFi passcode for new office floor (NOTE with bullet point)
        itemDao.insertItem(
            ListItemEntity(
                listId = list1Id,
                title = "WiFi passcode for new office floor",
                type = "NOTE",
                category = "Note",
                section = "Personal",
                isChecked = false,
                dueDate = null,
                priority = "NONE",
                tags = "Reference",
                description = "SSID: **HQ_Guest_Fast5G**\nPassword: `NexusOrbit2026!`\n\nNotes for visitors:\n  • Valid for 30 days\n  • VPN access requires 2FA authentication",
                createdAt = now - 3600000L * 24,
                updatedAt = now - 3600000L * 24
            )
        )

        // Sample Idea Note with Rich Text & Sub-bullets (Matching Screenshot 1 & 3)
        val noteDescription = """### Concept Overview
A compact 2x2 widget that visualizes daily streaks using micro-dots, similar to GitHub contributions.

> "Simplicity is prerequisite for reliability." — Edsger W. Dijkstra

### Key Technical Specs:
  • Zero background battery drain
  • Instant reactive updates via Room Flow
  • Adaptive dark/light theme blending
  • Fluid spring animations on pop-up entry
"""

        itemDao.insertItem(
            ListItemEntity(
                listId = list2Id,
                title = "Minimalist habit tracking widget with heatmaps",
                type = "NOTE",
                category = "Idea",
                isChecked = false,
                dueDate = null,
                priority = "HIGH",
                tags = "Brainstorm,Product",
                description = noteDescription,
                imageUri = "sample_sticky_notes",
                createdAt = now - 3600000L * 18,
                updatedAt = now - 3600000L * 10
            )
        )

        itemDao.insertItem(
            ListItemEntity(
                listId = list2Id,
                title = "Color palette moodboard for upcoming themes",
                type = "NOTE",
                category = "Idea",
                isChecked = false,
                dueDate = null,
                priority = "MEDIUM",
                tags = "Inspiration,UI",
                description = "Palette tokens explored:\n  • Neon Violet (#7C3AED)\n  • Cyber Emerald (#059669)\n  • Sunset Amber (#D97706)\n  • Deep Midnight (#0F172A)",
                imageUri = "sample_colors",
                createdAt = now - 86400000L * 2,
                updatedAt = now - 86400000L * 2
            )
        )

        itemDao.insertItem(
            ListItemEntity(
                listId = list3Id,
                title = "Prepare sprint demo slides",
                type = "TASK",
                category = "Task",
                isChecked = false,
                dueDate = tomorrowDue,
                priority = "HIGH",
                tags = "Sprint,Demo",
                description = "Include live recording of the home screen widget quick-add interaction and bullet list note rendering.",
                createdAt = now - 3600000L * 4,
                updatedAt = now - 3600000L * 4
            )
        )
    }
}
