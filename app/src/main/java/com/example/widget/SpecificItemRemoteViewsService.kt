package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ListItemEntity
import com.example.data.model.SubTask
import com.example.data.model.WidgetStyleConfig
import com.example.data.repository.ListRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpecificItemRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return SpecificItemRemoteViewsFactory(applicationContext, intent)
    }
}

class SpecificItemRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var currentItem: ListItemEntity? = null
    private var parentListName: String = "All Items"
    private var widgetConfig: WidgetStyleConfig = WidgetStyleConfig()
    private var subtasks: List<SubTask> = emptyList()

    private sealed class DisplayRow {
        data class Header(val item: ListItemEntity, val listName: String) : DisplayRow()
        data class ImageBanner(val uriString: String) : DisplayRow()
        data class Description(val text: String) : DisplayRow()
        data class SubTaskItem(val subtask: SubTask, val index: Int) : DisplayRow()
        data class Metadata(val dueDate: Long?, val createdAt: Long, val tags: String) : DisplayRow()
    }

    private val rows = mutableListOf<DisplayRow>()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val dueFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    override fun onCreate() {}

    override fun onDataSetChanged() {
        try {
            val db = AppDatabase.getDatabase(context)
            val repo = ListRepository(context)
            widgetConfig = repo.getWidgetConfigForAppWidgetId(appWidgetId, com.example.data.model.WidgetType.SPECIFIC_ITEM)

            val allItems = db.listItemDao().getAllItemsSync()
            val item = allItems.find { it.id == widgetConfig.itemId } ?: allItems.firstOrNull()
            currentItem = item

            rows.clear()
            if (item != null) {
                val lists = db.listDao().getAllListsSync()
                parentListName = lists.find { it.id == item.listId }?.name ?: "All Items"
                subtasks = SubTask.fromJson(item.subtasksJson)

                // 1. Header Row
                rows.add(DisplayRow.Header(item, parentListName))

                // 2. Image Row if present
                val imageUri = widgetConfig.customImageUri ?: item.imageUri
                if (!imageUri.isNullOrBlank()) {
                    rows.add(DisplayRow.ImageBanner(imageUri))
                }

                // 3. Description Row if present
                if (item.description.isNotBlank()) {
                    val cleanDesc = item.description.replace("#", "").replace("*", "").trim()
                    if (cleanDesc.isNotBlank()) {
                        rows.add(DisplayRow.Description(cleanDesc))
                    }
                }

                // 4. Subtasks Rows
                subtasks.forEachIndexed { index, st ->
                    rows.add(DisplayRow.SubTaskItem(st, index))
                }

                // 5. Metadata Row
                val hasDueDate = item.dueDate != null && item.dueDate > 0
                val hasTags = item.tags.isNotBlank()
                if (hasDueDate || hasTags) {
                    rows.add(DisplayRow.Metadata(item.dueDate, item.createdAt, item.tags))
                }
            }
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        rows.clear()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in rows.indices) return null
        val isWhiteTheme = widgetConfig.themeStyle.equals("WHITE", ignoreCase = true)
        val item = currentItem ?: return null

        val fillInIntent = Intent().apply {
            putExtra(ListAppWidgetProvider.EXTRA_OPEN_ITEM_ID, item.id)
            putExtra(ListAppWidgetProvider.EXTRA_LIST_ID, item.listId)
            putExtra(ListAppWidgetProvider.EXTRA_IS_CHECKED, item.isChecked)
        }

        val textScale = when (widgetConfig.textSizeScale.uppercase()) {
            "SMALL" -> 0.85f
            "LARGE" -> 1.15f
            "XLARGE" -> 1.30f
            else -> 1.0f
        }

        return when (val row = rows[position]) {
            is DisplayRow.Header -> {
                val views = RemoteViews(context.packageName, R.layout.widget_specific_header_row)
                views.setTextViewText(R.id.widget_header_title, row.item.title)
                views.setTextViewText(R.id.widget_header_list_name, row.listName)
                views.setTextViewText(R.id.widget_header_category_badge, row.item.category)
                views.setTextViewText(R.id.widget_header_priority_badge, "${row.item.priority} Priority")

                views.setTextViewTextSize(R.id.widget_header_title, TypedValue.COMPLEX_UNIT_SP, 15f * textScale)

                if (isWhiteTheme) {
                    views.setTextColor(R.id.widget_header_title, Color.parseColor("#0F172A"))
                    views.setTextColor(R.id.widget_header_list_name, Color.parseColor("#64748B"))
                    views.setTextColor(R.id.widget_header_category_badge, Color.parseColor("#4338CA"))
                } else {
                    views.setTextColor(R.id.widget_header_title, Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_header_list_name, Color.parseColor("#CBD5E1"))
                    views.setTextColor(R.id.widget_header_category_badge, Color.parseColor("#FFFFFF"))
                }

                if (row.item.type == "TASK") {
                    views.setImageViewResource(
                        R.id.widget_header_checkbox,
                        if (row.item.isChecked) R.drawable.ic_widget_checkbox_checked else R.drawable.ic_widget_checkbox_unchecked
                    )
                    val checkFillInIntent = Intent().apply {
                        putExtra(ListAppWidgetProvider.EXTRA_TOGGLE_CHECK, true)
                        putExtra(ListAppWidgetProvider.EXTRA_ITEM_ID, row.item.id)
                        putExtra(ListAppWidgetProvider.EXTRA_IS_CHECKED, row.item.isChecked)
                    }
                    views.setOnClickFillInIntent(R.id.widget_header_checkbox, checkFillInIntent)
                } else {
                    val categoryRes = getCategoryDrawableRes(row.item.effectiveCategory)
                    views.setImageViewResource(R.id.widget_header_checkbox, categoryRes)
                }

                views.setOnClickFillInIntent(R.id.widget_header_title, fillInIntent)
                views
            }
            is DisplayRow.ImageBanner -> {
                val views = RemoteViews(context.packageName, R.layout.widget_specific_image_row)
                try {
                    views.setImageViewUri(R.id.widget_specific_image_view, Uri.parse(row.uriString))
                } catch (_: Throwable) {}
                views.setOnClickFillInIntent(R.id.widget_specific_image_container, fillInIntent)
                views
            }
            is DisplayRow.Description -> {
                val views = RemoteViews(context.packageName, R.layout.widget_specific_desc_row)
                views.setTextViewText(R.id.widget_specific_desc_text, row.text)
                views.setTextViewTextSize(R.id.widget_specific_desc_text, TypedValue.COMPLEX_UNIT_SP, 11.5f * textScale)
                if (isWhiteTheme) {
                    views.setTextColor(R.id.widget_specific_desc_text, Color.parseColor("#334155"))
                    views.setTextColor(R.id.widget_specific_desc_label, Color.parseColor("#64748B"))
                } else {
                    views.setTextColor(R.id.widget_specific_desc_text, Color.parseColor("#F1F5F9"))
                    views.setTextColor(R.id.widget_specific_desc_label, Color.parseColor("#94A3B8"))
                }
                views.setOnClickFillInIntent(R.id.widget_specific_desc_container, fillInIntent)
                views
            }
            is DisplayRow.SubTaskItem -> {
                val views = RemoteViews(context.packageName, R.layout.widget_specific_subtask_row)
                views.setTextViewText(R.id.widget_specific_subtask_title, row.subtask.title)
                views.setTextViewTextSize(R.id.widget_specific_subtask_title, TypedValue.COMPLEX_UNIT_SP, 11.5f * textScale)
                views.setImageViewResource(
                    R.id.widget_specific_subtask_check,
                    if (row.subtask.isCompleted) R.drawable.ic_widget_checkbox_checked else R.drawable.ic_widget_checkbox_unchecked
                )
                if (isWhiteTheme) {
                    views.setTextColor(R.id.widget_specific_subtask_title, Color.parseColor("#334155"))
                } else {
                    views.setTextColor(R.id.widget_specific_subtask_title, Color.parseColor("#E2E8F0"))
                }

                val subtaskFillInIntent = Intent().apply {
                    putExtra(ListAppWidgetProvider.EXTRA_TOGGLE_SUBTASK, true)
                    putExtra(ListAppWidgetProvider.EXTRA_ITEM_ID, item.id)
                    putExtra(ListAppWidgetProvider.EXTRA_SUBTASK_INDEX, row.index)
                }
                views.setOnClickFillInIntent(R.id.widget_specific_subtask_check, subtaskFillInIntent)

                views.setOnClickFillInIntent(R.id.widget_specific_subtask_container, fillInIntent)
                views
            }
            is DisplayRow.Metadata -> {
                val views = RemoteViews(context.packageName, R.layout.widget_specific_meta_row)
                if (row.dueDate != null && row.dueDate > 0) {
                    views.setTextViewText(R.id.widget_specific_meta_due, "📅 Due: ${dueFormat.format(Date(row.dueDate))}")
                    views.setViewVisibility(R.id.widget_specific_meta_due, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_specific_meta_due, View.GONE)
                }

                views.setTextViewText(R.id.widget_specific_meta_created, "Created ${dateFormat.format(Date(row.createdAt))}")

                if (row.tags.isNotBlank()) {
                    val tagStr = row.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ") { "#$it" }
                    views.setTextViewText(R.id.widget_specific_meta_tags, tagStr)
                    views.setViewVisibility(R.id.widget_specific_meta_tags, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_specific_meta_tags, View.GONE)
                }

                if (isWhiteTheme) {
                    views.setTextColor(R.id.widget_specific_meta_created, Color.parseColor("#64748B"))
                } else {
                    views.setTextColor(R.id.widget_specific_meta_created, Color.parseColor("#94A3B8"))
                }

                views.setOnClickFillInIntent(R.id.widget_specific_meta_container, fillInIntent)
                views
            }
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 5
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
