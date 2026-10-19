package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.WidgetType
import com.example.data.repository.ListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class ListAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_ITEM_CLICK -> {
                val isToggleCheck = intent.getBooleanExtra(EXTRA_TOGGLE_CHECK, false)
                val isToggleSubtask = intent.getBooleanExtra(EXTRA_TOGGLE_SUBTASK, false)

                if (isToggleCheck) {
                    val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                    val isChecked = intent.getBooleanExtra(EXTRA_IS_CHECKED, false)
                    if (itemId > 0) {
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val repo = ListRepository(context)
                                repo.toggleTaskChecked(itemId, !isChecked)
                                sendUpdateBroadcast(context)
                            } catch (_: Throwable) {
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                } else if (isToggleSubtask) {
                    val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                    val subtaskIndex = intent.getIntExtra(EXTRA_SUBTASK_INDEX, -1)
                    if ((itemId > 0) && (subtaskIndex >= 0)) {
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val repo = ListRepository(context)
                                repo.toggleSubTaskChecked(itemId, subtaskIndex)
                            } catch (_: Throwable) {
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                } else {
                    val openItemId = intent.getLongExtra(EXTRA_OPEN_ITEM_ID, -1L)
                    val listId = intent.getLongExtra(EXTRA_LIST_ID, -1L)

                    val targetIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (openItemId > 0) {
                            putExtra(EXTRA_OPEN_ITEM_ID, openItemId)
                        }
                        if (listId > 0) {
                            putExtra(EXTRA_LIST_ID, listId)
                        }
                    }
                    context.startActivity(targetIntent)
                }
            }
            ACTION_TOGGLE_TASK -> {
                val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                val isChecked = intent.getBooleanExtra(EXTRA_IS_CHECKED, false)
                if (itemId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val repo = ListRepository(context)
                            repo.toggleTaskChecked(itemId, !isChecked)
                        } catch (_: Throwable) {}
                    }
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            ACTION_DATA_CHANGED,
            -> {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                    val thisWidget = ComponentName(context, ListAppWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                    if ((appWidgetIds != null) && appWidgetIds.isNotEmpty()) {
                        for (id in appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, id)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    companion object {
        const val ACTION_ITEM_CLICK = "com.example.widget.ACTION_ITEM_CLICK"
        const val ACTION_TOGGLE_TASK = "com.example.widget.ACTION_TOGGLE_TASK"
        const val ACTION_DATA_CHANGED = "com.example.widget.ACTION_DATA_CHANGED"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_IS_CHECKED = "extra_is_checked"
        const val EXTRA_LIST_ID = "extra_list_id"
        const val EXTRA_OPEN_ITEM_ID = "extra_open_item_id"
        const val EXTRA_PREVIEW_ITEM_ID = "extra_preview_item_id"
        const val EXTRA_QUICK_ADD = "extra_quick_add"
        const val EXTRA_QUICK_ADD_LIST = "extra_quick_add_list"
        const val EXTRA_TOGGLE_CHECK = "extra_toggle_check"
        const val EXTRA_TOGGLE_SUBTASK = "extra_toggle_subtask"
        const val EXTRA_SUBTASK_INDEX = "extra_subtask_index"

        fun sendUpdateBroadcast(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val providers = listOf(
                    AllListsWidgetProvider::class.java,
                    AllItemsWidgetProvider::class.java,
                    SpecificListWidgetProvider::class.java,
                    SpecificItemWidgetProvider::class.java,
                    ListAppWidgetProvider::class.java,
                )

                for (providerClass in providers) {
                    try {
                        val component = ComponentName(context, providerClass)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
                        if ((appWidgetIds != null) && appWidgetIds.isNotEmpty()) {
                            for (id in appWidgetIds) {
                                updateAppWidget(context, appWidgetManager, id)
                            }
                        }
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repo = ListRepository(context)

                    // Find out which provider this widget belongs to if default is needed
                    val providerInfo = try {
                        appWidgetManager.getAppWidgetInfo(appWidgetId)
                    } catch (_: Throwable) { null }

                    val defaultType = when (providerInfo?.provider?.className) {
                        AllListsWidgetProvider::class.java.name -> com.example.data.model.WidgetType.ALL_LISTS
                        AllItemsWidgetProvider::class.java.name -> com.example.data.model.WidgetType.ALL_ITEMS
                        SpecificItemWidgetProvider::class.java.name -> com.example.data.model.WidgetType.SPECIFIC_ITEM
                        SpecificListWidgetProvider::class.java.name -> com.example.data.model.WidgetType.SPECIFIC_LIST
                        else -> null
                    }

                    val widgetConfig = repo.getWidgetConfigForAppWidgetId(appWidgetId, defaultType)
                    val lists = db.listDao().getAllListsSync()

                    // Check size from widget options
                    val options = try {
                        appWidgetManager.getAppWidgetOptions(appWidgetId)
                    } catch (_: Throwable) { null }
                    val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0

                    val isWhiteTheme = widgetConfig.themeStyle.equals("WHITE", ignoreCase = true)
                    val bgResId = when (widgetConfig.themeStyle.uppercase()) {
                        "WHITE" -> R.drawable.widget_bg_white
                        "MIDNIGHT" -> R.drawable.widget_bg_midnight
                        "OCEAN" -> R.drawable.widget_bg_ocean
                        "EMERALD" -> R.drawable.widget_bg_emerald
                        "SUNSET" -> R.drawable.widget_bg_sunset
                        "ROSE" -> R.drawable.widget_bg_rose
                        "SLATE" -> R.drawable.widget_bg_slate
                        "MINT" -> R.drawable.widget_bg_mint
                        "AMBER" -> R.drawable.widget_bg_amber
                        "LAVENDER" -> R.drawable.widget_bg_lavender
                        "CORAL" -> R.drawable.widget_bg_coral
                        else -> R.drawable.widget_bg_purple
                    }

                    // Intent to launch WidgetConfigActivity as an isolated floating pop-up dialog
                    val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val configPendingIntent = PendingIntent.getActivity(
                        context,
                        (appWidgetId * 100) + 9,
                        configIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                    if (widgetConfig.widgetType == com.example.data.model.WidgetType.SPECIFIC_ITEM) {
                        // Render Specific Item Widget Layout with scrollable ListView
                        val allItems = db.listItemDao().getAllItemsSync()
                        val item = allItems.find { it.id == widgetConfig.itemId } ?: allItems.firstOrNull()
                        val views = RemoteViews(context.packageName, R.layout.widget_specific_item_layout)
                        views.setInt(R.id.widget_root, "setBackgroundResource", bgResId)

                        views.setOnClickPendingIntent(R.id.widget_item_btn_config, configPendingIntent)

                        val textScale = when (widgetConfig.textSizeScale.uppercase()) {
                            "SMALL" -> 0.85f
                            "LARGE" -> 1.15f
                            "XLARGE" -> 1.30f
                            else -> 1.0f
                        }

                        val titleText = if (item != null) "📌 ${item.title}" else "📌 Featured Item"
                        views.setTextViewText(R.id.widget_item_header_title, titleText)
                        views.setTextViewTextSize(R.id.widget_item_header_title, TypedValue.COMPLEX_UNIT_SP, 14f * textScale)

                        if (isWhiteTheme) {
                            views.setTextColor(R.id.widget_item_header_title, android.graphics.Color.parseColor("#0F172A"))
                            views.setTextColor(R.id.widget_specific_item_empty, android.graphics.Color.parseColor("#64748B"))
                        } else {
                            views.setTextColor(R.id.widget_item_header_title, android.graphics.Color.parseColor("#FFFFFF"))
                            views.setTextColor(R.id.widget_specific_item_empty, android.graphics.Color.parseColor("#B0FFFFFF"))
                        }

                        // Connect RemoteViewsService for specific item rows (Header, Image, Description, Subtasks, Metadata)
                        val serviceIntent = Intent(context, SpecificItemRemoteViewsService::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME) + "_specific_id_$appWidgetId")
                        }
                        views.setRemoteAdapter(R.id.widget_specific_item_list, serviceIntent)
                        views.setEmptyView(R.id.widget_specific_item_list, R.id.widget_specific_item_empty)

                        // PendingIntent template for item clicks (Broadcast)
                        val clickIntentTemplate = Intent(context, ListAppWidgetProvider::class.java).apply {
                            action = ACTION_ITEM_CLICK
                        }
                        val clickPendingIntent = PendingIntent.getBroadcast(
                            context,
                            (appWidgetId * 100) + 4,
                            clickIntentTemplate,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                        )
                        views.setPendingIntentTemplate(R.id.widget_specific_item_list, clickPendingIntent)

                        appWidgetManager.updateAppWidget(appWidgetId, views)

                        try {
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_specific_item_list)
                        } catch (_: Throwable) {}
                        return@launch
                    }

                    // For ALL_LISTS, ALL_ITEMS, and SPECIFIC_LIST modes
                    val views = RemoteViews(context.packageName, R.layout.widget_layout)
                    views.setInt(R.id.widget_root, "setBackgroundResource", bgResId)

                    val targetList = lists.find { it.id == widgetConfig.listId } ?: lists.firstOrNull()
                    val headerTitle = when (widgetConfig.widgetType) {
                        com.example.data.model.WidgetType.ALL_LISTS -> "All Lists"
                        WidgetType.ALL_ITEMS -> "All Items"
                        com.example.data.model.WidgetType.SPECIFIC_LIST -> "${targetList?.name ?: "All Items"} ▾"
                        else -> "My List"
                    }

                    val textScale = when (widgetConfig.textSizeScale.uppercase()) {
                        "SMALL" -> 0.85f
                        "LARGE" -> 1.15f
                        "XLARGE" -> 1.30f
                        else -> 1.0f
                    }

                    views.setTextViewText(R.id.widget_list_title, headerTitle)
                    views.setTextViewTextSize(R.id.widget_list_title, TypedValue.COMPLEX_UNIT_SP, 15f * textScale)

                    // Quick add button label set to +
                    views.setTextViewText(R.id.widget_btn_quick_add, "+")
                    views.setTextViewTextSize(R.id.widget_btn_quick_add, TypedValue.COMPLEX_UNIT_SP, 16f * textScale)

                    if (isWhiteTheme) {
                        views.setTextColor(R.id.widget_list_title, android.graphics.Color.parseColor("#0F172A"))
                        views.setTextColor(R.id.widget_btn_quick_add, android.graphics.Color.parseColor("#4F46E5"))
                        views.setTextColor(R.id.widget_empty_view, android.graphics.Color.parseColor("#64748B"))
                    } else {
                        views.setTextColor(R.id.widget_list_title, android.graphics.Color.parseColor("#FFFFFF"))
                        views.setTextColor(R.id.widget_btn_quick_add, android.graphics.Color.parseColor("#FFFFFF"))
                        views.setTextColor(R.id.widget_empty_view, android.graphics.Color.parseColor("#B0FFFFFF"))
                    }

                    // Config button intent (opens Widget Configuration pop-up for this widget)
                    views.setOnClickPendingIntent(R.id.widget_btn_config, configPendingIntent)

                    // Quick Add button intent (opens List or Item Editor in an isolated floating pop-up without bringing MainActivity forward)
                    val listId = targetList?.id ?: 0L
                    val quickAddIntent = Intent(context, WidgetQuickAddActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        if (widgetConfig.widgetType == WidgetType.ALL_LISTS) {
                            putExtra(EXTRA_QUICK_ADD_LIST, true)
                        } else {
                            putExtra(EXTRA_QUICK_ADD, true)
                            putExtra(EXTRA_LIST_ID, listId)
                        }
                    }
                    val quickAddPending = PendingIntent.getActivity(
                        context,
                        (appWidgetId * 100) + 1,
                        quickAddIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_quick_add, quickAddPending)

                    // Set up RemoteViewsService Adapter with appWidgetId
                    val serviceIntent = Intent(context, ListWidgetRemoteViewsService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(EXTRA_LIST_ID, listId)
                        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME) + "_id_$appWidgetId")
                    }
                    views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                    views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

                    // PendingIntent template for item clicks (Broadcast to ListAppWidgetProvider.onReceive)
                    val clickIntentTemplate = Intent(context, ListAppWidgetProvider::class.java).apply {
                        action = ACTION_ITEM_CLICK
                    }
                    val clickPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (appWidgetId * 100) + 3,
                        clickIntentTemplate,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                    views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)

                    try {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                    } catch (_: Throwable) {}
                } catch (_: Throwable) {}
            }
        }
    }
}
