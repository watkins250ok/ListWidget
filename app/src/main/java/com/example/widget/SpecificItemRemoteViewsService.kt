package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.SpannedString
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.CategoryOption
import com.example.data.model.ListItemEntity
import com.example.data.model.SubTask
import com.example.data.model.WidgetStyleConfig
import com.example.data.model.WidgetType
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
    private var categories: List<CategoryOption> = emptyList()

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
            widgetConfig = repo.getWidgetConfigForAppWidgetId(appWidgetId, WidgetType.SPECIFIC_ITEM)
            categories = repo.categories.value

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
                        rows.add(DisplayRow.Description(item.description))
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
                views.setTextViewText(R.id.widget_header_priority_badge, "${row.item.priority} Priority")

                views.setTextViewTextSize(R.id.widget_header_title, TypedValue.COMPLEX_UNIT_SP, 15f * textScale)

                if (isWhiteTheme) {
                    views.setTextColor(R.id.widget_header_title, Color.parseColor("#0F172A"))
                    views.setTextColor(R.id.widget_header_list_name, Color.parseColor("#64748B"))
                } else {
                    views.setTextColor(R.id.widget_header_title, Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_header_list_name, Color.parseColor("#CBD5E1"))
                }

                // Category Icon & Color
                val catObj = categories.find { it.name.equals(row.item.effectiveCategory, ignoreCase = true) }
                val catIconRes = getCategoryDrawableRes(row.item.effectiveCategory)
                views.setImageViewResource(R.id.widget_header_category_icon, catIconRes)

                val catColorHex = catObj?.colorHex ?: "#0284C7"
                try {
                    val catColor = Color.parseColor(catColorHex)
                    views.setInt(R.id.widget_header_category_icon, "setColorFilter", catColor)
                } catch (_: Throwable) {}

                // Priority Badge Color (High Contrast)
                val priColorHex = when (row.item.priority.uppercase()) {
                    "HIGH" -> "#EF4444"
                    "MEDIUM" -> "#F59E0B"
                    "LOW" -> "#10B981"
                    else -> "#64748B"
                }
                try {
                    val priColor = Color.parseColor(priColorHex)
                    views.setInt(R.id.widget_header_priority_badge, "setBackgroundColor", priColor)
                    views.setTextColor(R.id.widget_header_priority_badge, Color.WHITE)
                } catch (_: Throwable) {}

                views.setOnClickFillInIntent(R.id.widget_header_title, fillInIntent)
                views.setOnClickFillInIntent(R.id.widget_specific_header_container, fillInIntent)
                views
            }
            is DisplayRow.ImageBanner -> {
                val views = RemoteViews(context.packageName, R.layout.widget_specific_image_row)
                try {
                    val uri = Uri.parse(row.uriString)
                    val bitmap = loadBitmapFromUri(context, uri)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_specific_image_view, bitmap)
                    } else {
                        views.setImageViewUri(R.id.widget_specific_image_view, uri)
                    }
                } catch (_: Throwable) {}
                views
            }
            is DisplayRow.Description -> {
                val views = RemoteViews(context.packageName, R.layout.widget_specific_desc_row)
                val spannedText = convertMarkdownToHtml(row.text)
                views.setTextViewText(R.id.widget_specific_desc_text, spannedText)
                views.setTextViewTextSize(R.id.widget_specific_desc_text, TypedValue.COMPLEX_UNIT_SP, 11.5f * textScale)
                if (isWhiteTheme) {
                    views.setTextColor(R.id.widget_specific_desc_text, Color.parseColor("#334155"))
                    views.setTextColor(R.id.widget_specific_desc_label, Color.parseColor("#64748B"))
                } else {
                    views.setTextColor(R.id.widget_specific_desc_text, Color.parseColor("#F1F5F9"))
                    views.setTextColor(R.id.widget_specific_desc_label, Color.parseColor("#94A3B8"))
                }
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
                views.setOnClickFillInIntent(R.id.widget_specific_subtask_container, subtaskFillInIntent)
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
                views
            }
        }
    }

    private fun convertMarkdownToHtml(markdown: String): Spanned {
        if (markdown.isBlank()) return SpannedString("")

        val html = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&lt;u&gt;", "<u>")
            .replace("&lt;/u&gt;", "</u>")
            .replace("&lt;b&gt;", "<b>")
            .replace("&lt;/b&gt;", "</b>")
            .replace("&lt;i&gt;", "<i>")
            .replace("&lt;/i&gt;", "</i>")
            .replace(Regex("""\*\*(.*?)\*\*"""), "<b>$1</b>")
            .replace(Regex("""__(.*?)__"""), "<b>$1</b>")
            .replace(Regex("""\*(.*?)\*"""), "<i>$1</i>")
            .replace(Regex("""_(.*?)_"""), "<i>$1</i>")
            .replace(Regex("""~~(.*?)~~"""), "<s>$1</s>")
            .replace(Regex("""\[(.*?)\]\((https?://.*?)\)"""), "<a href=\"$2\">$1</a>")
            .replace(Regex("""(?<!href=")https?://[^\s<]+"""), "<a href=\"$0\">$0</a>")
            .replace("\n", "<br>")

        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Downscale for widget RemoteViews memory limit
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (_: Throwable) { null }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 5
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
