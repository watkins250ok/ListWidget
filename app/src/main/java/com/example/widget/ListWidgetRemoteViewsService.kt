package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.WidgetType
import com.example.data.repository.ListRepository
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getCategoryDrawableRes(categoryName: String): Int {
    return when (categoryName.trim().lowercase()) {
        "idea", "ideas", "lightbulb", "sparkles" -> R.drawable.ic_widget_lightbulb
        "note", "notes", "description" -> R.drawable.ic_widget_description
        "work" -> R.drawable.ic_widget_work
        "shopping", "shop" -> R.drawable.ic_widget_shopping
        "star", "favorite", "favorites" -> R.drawable.ic_widget_star
        "fitness", "gym", "workout" -> R.drawable.ic_widget_fitness
        "reading", "book", "books" -> R.drawable.ic_widget_book
        "code", "coding", "dev" -> R.drawable.ic_widget_code
        "person", "personal" -> R.drawable.ic_widget_person
        else -> R.drawable.ic_widget_tag
    }
}

fun getIconDrawableRes(iconName: String): Int {
    return when (iconName.trim().lowercase()) {
        "check", "checklist", "task", "task_alt", "done", "numbers" -> R.drawable.ic_widget_checkbox_checked
        "home", "house" -> R.drawable.ic_widget_home
        "analytics", "trending", "chart", "bar_chart", "improvement", "improvements", "stats" -> R.drawable.ic_widget_analytics
        "movie", "tv", "film", "watchlist", "cinema" -> R.drawable.ic_widget_movie
        "shopping", "shop", "grocery", "mall", "cart" -> R.drawable.ic_widget_shopping
        "work", "briefcase" -> R.drawable.ic_widget_work
        "idea", "lightbulb", "sparkles" -> R.drawable.ic_widget_lightbulb
        "star", "favorite", "flag", "bookmark" -> R.drawable.ic_widget_star
        "heart" -> R.drawable.ic_widget_heart
        "folder" -> R.drawable.ic_widget_folder
        "fitness", "gym", "sports", "health" -> R.drawable.ic_widget_fitness
        "book", "reading", "school" -> R.drawable.ic_widget_book
        "code", "terminal" -> R.drawable.ic_widget_code
        "person", "team", "group" -> R.drawable.ic_widget_person
        "note", "description", "receipt", "mail" -> R.drawable.ic_widget_description
        else -> R.drawable.ic_widget_tag
    }
}

class ListWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
        val listId = intent.getLongExtra(ListAppWidgetProvider.EXTRA_LIST_ID, 0L)
        return ListWidgetRemoteViewsFactory(applicationContext, appWidgetId, listId)
    }
}

private sealed class WidgetRowItem {
    data class SectionHeader(val sectionName: String, val count: Int) : WidgetRowItem()
    data class ListItem(val item: ListItemEntity) : WidgetRowItem()
}

class ListWidgetRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
    private val listId: Long
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<ListItemEntity> = emptyList()
    private var lists: List<ListEntity> = emptyList()
    private var listCountMap: Map<Long, Int> = emptyMap()
    private var widgetRows: List<WidgetRowItem> = emptyList()
    private var currentWidgetType: WidgetType = WidgetType.SPECIFIC_LIST
    private var showDescription: Boolean = true
    private var textScale: Float = 1.0f
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val dueDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    override fun onCreate() {
        fetchData()
    }

    override fun onDataSetChanged() {
        fetchData()
    }

    private fun fetchData() {
        runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val repo = ListRepository(context)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val providerInfo = try {
                    appWidgetManager?.getAppWidgetInfo(appWidgetId)
                } catch (_: Throwable) { null }

                val defaultType = when (providerInfo?.provider?.className) {
                    AllListsWidgetProvider::class.java.name -> WidgetType.ALL_LISTS
                    AllItemsWidgetProvider::class.java.name -> WidgetType.ALL_ITEMS
                    SpecificItemWidgetProvider::class.java.name -> WidgetType.SPECIFIC_ITEM
                    SpecificListWidgetProvider::class.java.name -> WidgetType.SPECIFIC_LIST
                    else -> null
                }

                val config = repo.getWidgetConfigForAppWidgetId(appWidgetId, defaultType)
                currentWidgetType = config.widgetType
                showDescription = config.showDescription
                textScale = when (config.textSizeScale.uppercase()) {
                    "SMALL" -> 0.85f
                    "LARGE" -> 1.15f
                    "XLARGE" -> 1.30f
                    else -> 1.0f
                }

                if (config.widgetType == WidgetType.ALL_LISTS) {
                    val allLists = db.listDao().getAllListsSync()
                    val allItems = db.listItemDao().getAllItemsSync()
                    lists = allLists
                    listCountMap = allItems.groupingBy { it.listId }.eachCount()
                    items = emptyList()
                    widgetRows = emptyList()
                } else if (config.widgetType == WidgetType.ALL_ITEMS) {
                    var allItems = db.listItemDao().getAllItemsSync()
                    if (config.categoryFilter != "ALL") {
                        allItems = allItems.filter { it.category.equals(config.categoryFilter, ignoreCase = true) }
                    }
                    if (config.tagFilter != "ALL") {
                        val searchTag = config.tagFilter.removePrefix("#").trim()
                        allItems = allItems.filter { it.tags.contains(searchTag, ignoreCase = true) }
                    }
                    if (config.hideChecked) {
                        allItems = allItems.filter { !it.isChecked }
                    }
                    items = allItems
                    lists = emptyList()
                    widgetRows = items.map { WidgetRowItem.ListItem(it) }
                } else {
                    // SPECIFIC_LIST
                    val targetListId = if (config.listId > 0) config.listId else listId
                    val allItems = if (targetListId > 0) {
                        db.listItemDao().getItemsForListSync(targetListId)
                    } else {
                        db.listItemDao().getAllItemsSync()
                    }
                    val filteredItems = if (config.hideChecked) {
                        allItems.filter { !it.isChecked }
                    } else {
                        allItems
                    }
                    items = filteredItems
                    lists = emptyList()

                    val hasSections = filteredItems.any { it.section.isNotBlank() }
                    if (hasSections) {
                        val grouped = filteredItems.groupBy {
                            if (it.section.isNotBlank()) it.section.trim() else "General"
                        }
                        val rows = mutableListOf<WidgetRowItem>()
                        grouped.forEach { (secName, secItems) ->
                            rows.add(WidgetRowItem.SectionHeader(secName, secItems.size))
                            secItems.forEach { item ->
                                rows.add(WidgetRowItem.ListItem(item))
                            }
                        }
                        widgetRows = rows
                    } else {
                        widgetRows = filteredItems.map { WidgetRowItem.ListItem(it) }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onDestroy() {
        items = emptyList()
        lists = emptyList()
        widgetRows = emptyList()
    }

    override fun getCount(): Int {
        return if (currentWidgetType == WidgetType.ALL_LISTS) lists.size else widgetRows.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (currentWidgetType == WidgetType.ALL_LISTS) {
            if (position !in lists.indices) return RemoteViews(context.packageName, R.layout.widget_all_lists_row)
            val list = lists[position]
            val views = RemoteViews(context.packageName, R.layout.widget_all_lists_row)

            views.setTextViewText(R.id.widget_list_row_name, list.name)
            views.setTextViewTextSize(R.id.widget_list_row_name, TypedValue.COMPLEX_UNIT_SP, 14f * textScale)

            // Exact List Vector Icon with exact list color
            val iconRes = getIconDrawableRes(list.iconName)
            views.setImageViewResource(R.id.widget_list_row_icon, iconRes)
            val listColor = try {
                Color.parseColor(list.colorHex)
            } catch (_: Exception) {
                Color.parseColor("#7C3AED")
            }
            views.setInt(R.id.widget_list_row_icon, "setColorFilter", listColor)

            // Description
            if (!list.description.isNullOrBlank()) {
                views.setViewVisibility(R.id.widget_list_row_desc, View.VISIBLE)
                views.setTextViewText(R.id.widget_list_row_desc, list.description)
                views.setTextViewTextSize(R.id.widget_list_row_desc, TypedValue.COMPLEX_UNIT_SP, 10.5f * textScale)
            } else {
                views.setViewVisibility(R.id.widget_list_row_desc, View.GONE)
            }

            // Hide Creation Date
            views.setViewVisibility(R.id.widget_list_row_date, View.GONE)

            // Number of items on its own line directly beneath title
            val count = listCountMap[list.id] ?: 0
            views.setTextViewText(R.id.widget_list_row_count, "$count item${if (count == 1) "" else "s"}")
            views.setTextViewTextSize(R.id.widget_list_row_count, TypedValue.COMPLEX_UNIT_SP, 11f * textScale)

            // Fill-in Intent to open List View in a floating pop-up over homescreen
            val fillInIntent = Intent().apply {
                putExtra(ListAppWidgetProvider.EXTRA_OPEN_LIST_DETAIL_ID, list.id)
            }
            views.setOnClickFillInIntent(R.id.widget_list_row_container, fillInIntent)
            views.setOnClickFillInIntent(R.id.widget_list_row_name, fillInIntent)
            return views
        }

        // Render Item Row or Section Header for ALL_ITEMS and SPECIFIC_LIST
        if (position !in widgetRows.indices) return RemoteViews(context.packageName, R.layout.widget_item_row)
        val row = widgetRows[position]

        if (row is WidgetRowItem.SectionHeader) {
            val views = RemoteViews(context.packageName, R.layout.widget_section_header_row)
            views.setTextViewText(R.id.widget_section_header_title, "📌 ${row.sectionName.uppercase()}")
            views.setTextViewText(R.id.widget_section_header_count, "(${row.count})")
            views.setTextViewTextSize(R.id.widget_section_header_title, TypedValue.COMPLEX_UNIT_SP, 11f * textScale)
            views.setTextViewTextSize(R.id.widget_section_header_count, TypedValue.COMPLEX_UNIT_SP, 10f * textScale)
            return views
        }

        val item = (row as WidgetRowItem.ListItem).item
        val views = RemoteViews(context.packageName, R.layout.widget_item_row)

        views.setTextViewText(R.id.widget_item_title, item.title)
        views.setTextViewTextSize(R.id.widget_item_title, TypedValue.COMPLEX_UNIT_SP, 13f * textScale)

        val isAllItemsWidget = currentWidgetType == WidgetType.ALL_ITEMS
        val isTask = item.type == "TASK" || item.effectiveCategory.equals("Task", ignoreCase = true)

        if (isTask) {
            views.setInt(R.id.widget_item_check_icon, "setBackgroundResource", 0)
            if (item.isChecked) {
                views.setImageViewResource(R.id.widget_item_check_icon, R.drawable.ic_widget_checkbox_checked)
                views.setTextColor(R.id.widget_item_title, Color.parseColor("#94A3B8"))
            } else {
                views.setImageViewResource(R.id.widget_item_check_icon, R.drawable.ic_widget_checkbox_unchecked)
                views.setTextColor(R.id.widget_item_title, Color.parseColor("#FFFFFF"))
            }
            val checkFillInIntent = Intent().apply {
                putExtra(ListAppWidgetProvider.EXTRA_TOGGLE_CHECK, true)
                putExtra(ListAppWidgetProvider.EXTRA_ITEM_ID, item.id)
                putExtra(ListAppWidgetProvider.EXTRA_IS_CHECKED, item.isChecked)
            }
            views.setOnClickFillInIntent(R.id.widget_item_check_icon, checkFillInIntent)
        } else {
            views.setInt(R.id.widget_item_check_icon, "setBackgroundResource", R.drawable.widget_icon_badge_bg)
            val categoryRes = getCategoryDrawableRes(item.effectiveCategory)
            views.setImageViewResource(R.id.widget_item_check_icon, categoryRes)
            views.setTextColor(R.id.widget_item_title, Color.parseColor("#FFFFFF"))
        }

        // Due Date badge
        if (item.dueDate != null && item.dueDate > 0) {
            views.setViewVisibility(R.id.widget_item_due_badge, View.VISIBLE)
            val dueLabel = formatDueDateLabel(item.dueDate)
            views.setTextViewText(R.id.widget_item_due_badge, "📅 $dueLabel")
            views.setTextViewTextSize(R.id.widget_item_due_badge, TypedValue.COMPLEX_UNIT_SP, 9.5f * textScale)
        } else {
            views.setViewVisibility(R.id.widget_item_due_badge, View.GONE)
        }

        // Tags (Hidden for ALL_ITEMS widget)
        if (!isAllItemsWidget && item.tags.isNotBlank()) {
            val firstTag = item.tags.split(",").firstOrNull()?.trim() ?: ""
            if (firstTag.isNotBlank()) {
                views.setViewVisibility(R.id.widget_item_tag_badge, View.VISIBLE)
                views.setTextViewText(R.id.widget_item_tag_badge, "#$firstTag")
                views.setTextViewTextSize(R.id.widget_item_tag_badge, TypedValue.COMPLEX_UNIT_SP, 9.5f * textScale)
            } else {
                views.setViewVisibility(R.id.widget_item_tag_badge, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_item_tag_badge, View.GONE)
        }

        // Description preview (Hidden for ALL_ITEMS widget)
        if (!isAllItemsWidget && showDescription && item.description.isNotBlank()) {
            views.setViewVisibility(R.id.widget_item_desc, View.VISIBLE)
            val cleanDesc = item.description.replace("#", "").replace("*", "").replace(">", "").trim()
            views.setTextViewText(R.id.widget_item_desc, cleanDesc)
            views.setTextViewTextSize(R.id.widget_item_desc, TypedValue.COMPLEX_UNIT_SP, 10.5f * textScale)
        } else {
            views.setViewVisibility(R.id.widget_item_desc, View.GONE)
        }

        // Fill-in Intent for Target B: Content Title Area (Opens Item Preview in dialog pop-up over home screen)
        val detailsFillInIntent = Intent().apply {
            putExtra(ListAppWidgetProvider.EXTRA_PREVIEW_ITEM_ID, item.id)
            putExtra(ListAppWidgetProvider.EXTRA_LIST_ID, item.listId)
        }
        views.setOnClickFillInIntent(R.id.widget_item_content_container, detailsFillInIntent)

        return views
    }

    private fun formatDueDateLabel(dueMillis: Long): String {
        val now = Calendar.getInstance()
        val due = Calendar.getInstance().apply { timeInMillis = dueMillis }

        return when {
            now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR) -> "DUE: TODAY"
            now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) + 1 == due.get(Calendar.DAY_OF_YEAR) -> "DUE: TOMORROW"
            else -> "DUE: " + dueDateFormat.format(Date(dueMillis)).uppercase()
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 3
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}

