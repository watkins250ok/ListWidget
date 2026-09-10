package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.model.AppFontFamily
import com.example.data.model.AppThemeMode
import com.example.data.model.CategoryOption
import com.example.data.model.ColorPalettePreset
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.PriorityOption
import com.example.data.model.SubTask
import com.example.data.model.WidgetStyleConfig
import com.example.data.model.WidgetType
import com.example.notification.NotificationScheduler
import com.example.widget.ListAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ListRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val listDao = database.listDao()
    private val itemDao = database.listItemDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("list_widget_prefs", Context.MODE_PRIVATE)

    val allLists: Flow<List<ListEntity>> = listDao.getAllLists()
    val allItems: Flow<List<ListItemEntity>> = itemDao.getAllItems()
    val itemsWithImages: Flow<List<ListItemEntity>> = itemDao.getItemsWithImages()

    // Settings state
    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) { AppThemeMode.SYSTEM }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow(
        try {
            ColorPalettePreset.valueOf(prefs.getString("color_palette", ColorPalettePreset.BENTO_PURPLE.name) ?: ColorPalettePreset.BENTO_PURPLE.name)
        } catch (_: Exception) { ColorPalettePreset.BENTO_PURPLE }
    )
    val colorPalette: StateFlow<ColorPalettePreset> = _colorPalette.asStateFlow()

    private val _fontFamily = MutableStateFlow(
        try {
            AppFontFamily.valueOf(prefs.getString("font_family", AppFontFamily.CLEAN_SANS.name) ?: AppFontFamily.CLEAN_SANS.name)
        } catch (_: Exception) { AppFontFamily.CLEAN_SANS }
    )
    val fontFamily: StateFlow<AppFontFamily> = _fontFamily.asStateFlow()

    private val _showCounts = MutableStateFlow(prefs.getBoolean("show_counts", true))
    val showCounts: StateFlow<Boolean> = _showCounts.asStateFlow()

    // Categories state (Default: Task, Idea, Note + user created)
    private val _categories = MutableStateFlow(loadCategories())
    val categories: StateFlow<List<CategoryOption>> = _categories.asStateFlow()

    private fun loadCategories(): List<CategoryOption> {
        val rawJson = prefs.getString("custom_categories_v2_json", null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val jsonArray = org.json.JSONArray(rawJson)
                val list = mutableListOf<CategoryOption>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        CategoryOption(
                            id = obj.optString("id", obj.optString("name", "")),
                            name = obj.getString("name"),
                            iconKey = obj.optString("iconKey", "tag"),
                            colorHex = obj.optString("colorHex", "#0284C7"),
                            isDefault = obj.optBoolean("isDefault", false)
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }

        // Check legacy categories migration
        val savedLegacy = prefs.getStringSet("custom_categories", null)
        val defaultList = CategoryOption.DEFAULT_CATEGORIES.toMutableList()
        if (savedLegacy != null) {
            savedLegacy.forEach { legacyName ->
                if (defaultList.none { it.name.equals(legacyName, ignoreCase = true) }) {
                    defaultList.add(
                        CategoryOption(
                            id = "CAT_" + legacyName.replace(" ", "_"),
                            name = legacyName,
                            iconKey = "tag",
                            colorHex = "#0284C7",
                            isDefault = false
                        )
                    )
                }
            }
        }
        return defaultList
    }

    private fun saveCategories(list: List<CategoryOption>) {
        val jsonArray = org.json.JSONArray()
        list.forEach { cat ->
            val obj = org.json.JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("iconKey", cat.iconKey)
            obj.put("colorHex", cat.colorHex)
            obj.put("isDefault", cat.isDefault)
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_categories_v2_json", jsonArray.toString()).apply()
        _categories.value = list
    }

    // Multi-Widget Management & Persistence
    private val _allWidgets = MutableStateFlow<List<WidgetStyleConfig>>(loadSavedWidgets())
    val allWidgets: StateFlow<List<WidgetStyleConfig>> = _allWidgets.asStateFlow()

    private val _activeWidgetId = MutableStateFlow(
        prefs.getString("active_widget_id", _allWidgets.value.firstOrNull()?.widgetId ?: "widget_1") ?: "widget_1"
    )
    val activeWidgetId: StateFlow<String> = _activeWidgetId.asStateFlow()

    private val _widgetConfig = MutableStateFlow(
        _allWidgets.value.find { it.widgetId == _activeWidgetId.value } ?: _allWidgets.value.firstOrNull() ?: WidgetStyleConfig()
    )
    val widgetConfig: StateFlow<WidgetStyleConfig> = _widgetConfig.asStateFlow()

    private fun loadSavedWidgets(): List<WidgetStyleConfig> {
        val rawJson = prefs.getString("saved_widget_configs_v3_json", null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val jsonArray = org.json.JSONArray(rawJson)
                val list = mutableListOf<WidgetStyleConfig>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        WidgetStyleConfig(
                            widgetId = obj.optString("widgetId", "widget_${i + 1}"),
                            widgetName = obj.optString("widgetName", "Widget ${i + 1}"),
                            widgetType = try {
                                WidgetType.valueOf(obj.optString("widgetType", WidgetType.SPECIFIC_LIST.name))
                            } catch (_: Exception) { WidgetType.SPECIFIC_LIST },
                            listId = obj.optLong("listId", 0L),
                            itemId = obj.optLong("itemId", 0L),
                            categoryFilter = obj.optString("categoryFilter", "ALL"),
                            tagFilter = obj.optString("tagFilter", "ALL"),
                            customImageUri = if (obj.has("customImageUri") && !obj.isNull("customImageUri")) obj.getString("customImageUri") else null,
                            colorPreset = try {
                                ColorPalettePreset.valueOf(obj.optString("colorPreset", ColorPalettePreset.PURPLE_VIOLET.name))
                            } catch (_: Exception) { ColorPalettePreset.PURPLE_VIOLET },
                            themeStyle = obj.optString("themeStyle", "PURPLE"),
                            showDescription = obj.optBoolean("showDescription", true),
                            hideChecked = obj.optBoolean("hideChecked", false),
                            textSizeScale = obj.optString("textSizeScale", "MEDIUM")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }

        // Default set of curated widgets showcasing each of the 4 Widget Types & Colors
        val defaults = listOf(
            WidgetStyleConfig(
                widgetId = "widget_1",
                widgetName = "Widget 1 (Specific List)",
                widgetType = WidgetType.SPECIFIC_LIST,
                themeStyle = "PURPLE",
                showDescription = true
            ),
            WidgetStyleConfig(
                widgetId = "widget_2",
                widgetName = "Widget 2 (All Lists)",
                widgetType = WidgetType.ALL_LISTS,
                themeStyle = "OCEAN",
                showDescription = true
            ),
            WidgetStyleConfig(
                widgetId = "widget_3",
                widgetName = "Widget 3 (All Items)",
                widgetType = WidgetType.ALL_ITEMS,
                themeStyle = "EMERALD",
                categoryFilter = "ALL",
                showDescription = true
            ),
            WidgetStyleConfig(
                widgetId = "widget_4",
                widgetName = "Widget 4 (Specific Item)",
                widgetType = WidgetType.SPECIFIC_ITEM,
                themeStyle = "SUNSET",
                showDescription = true
            )
        )
        return defaults
    }

    private fun saveWidgetsList(list: List<WidgetStyleConfig>) {
        val jsonArray = org.json.JSONArray()
        list.forEach { w ->
            val obj = org.json.JSONObject()
            obj.put("widgetId", w.widgetId)
            obj.put("widgetName", w.widgetName)
            obj.put("widgetType", w.widgetType.name)
            obj.put("listId", w.listId)
            obj.put("itemId", w.itemId)
            obj.put("categoryFilter", w.categoryFilter)
            obj.put("tagFilter", w.tagFilter)
            if (w.customImageUri != null) {
                obj.put("customImageUri", w.customImageUri)
            }
            obj.put("colorPreset", w.colorPreset.name)
            obj.put("themeStyle", w.themeStyle)
            obj.put("showDescription", w.showDescription)
            obj.put("hideChecked", w.hideChecked)
            obj.put("textSizeScale", w.textSizeScale)
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_widget_configs_v3_json", jsonArray.toString()).apply()
        _allWidgets.value = list
        val currentActive = list.find { it.widgetId == _activeWidgetId.value } ?: list.firstOrNull()
        if (currentActive != null) {
            _widgetConfig.value = currentActive
        }
    }

    // Editable priorities (None, Low, Medium, High + Custom)
    private val _priorities = MutableStateFlow(loadPriorities())
    val priorities: StateFlow<List<PriorityOption>> = _priorities.asStateFlow()

    private fun loadPriorities(): List<PriorityOption> {
        val rawJson = prefs.getString("custom_priorities_json", null)
        if (rawJson.isNullOrBlank()) {
            return PriorityOption.DEFAULT_PRIORITIES
        }
        return try {
            val jsonArray = org.json.JSONArray(rawJson)
            val list = mutableListOf<PriorityOption>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PriorityOption(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        colorHex = obj.getString("colorHex")
                    )
                )
            }
            if (list.isEmpty()) PriorityOption.DEFAULT_PRIORITIES else list
        } catch (_: Exception) {
            PriorityOption.DEFAULT_PRIORITIES
        }
    }

    private fun savePriorities(list: List<PriorityOption>) {
        val jsonArray = org.json.JSONArray()
        list.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("label", p.label)
            obj.put("colorHex", p.colorHex)
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_priorities_json", jsonArray.toString()).apply()
        _priorities.value = list
    }

    fun getItemsForList(listId: Long): Flow<List<ListItemEntity>> {
        return itemDao.getItemsForList(listId)
    }

    suspend fun getListById(id: Long): ListEntity? = withContext(Dispatchers.IO) {
        listDao.getListById(id)
    }

    fun observeListById(id: Long): Flow<ListEntity?> = listDao.observeListById(id)

    suspend fun getItemById(id: Long): ListItemEntity? = withContext(Dispatchers.IO) {
        itemDao.getItemById(id)
    }

    suspend fun insertList(list: ListEntity): Long = withContext(Dispatchers.IO) {
        val id = listDao.insertList(list)
        triggerWidgetUpdate()
        id
    }

    suspend fun updateList(list: ListEntity) = withContext(Dispatchers.IO) {
        listDao.updateList(list.copy(updatedAt = System.currentTimeMillis()))
        triggerWidgetUpdate()
    }

    suspend fun deleteList(list: ListEntity) = withContext(Dispatchers.IO) {
        listDao.deleteList(list)
        triggerWidgetUpdate()
    }

    suspend fun insertItem(item: ListItemEntity): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val savedItem = item.copy(updatedAt = now)
        val id = itemDao.insertItem(savedItem)
        val list = listDao.getListById(item.listId)
        if (list != null) {
            listDao.updateList(list.copy(updatedAt = now))
        }
        NotificationScheduler.scheduleItemReminder(context, savedItem.copy(id = id), list?.name ?: "List")
        triggerWidgetUpdate()
        id
    }

    suspend fun updateItem(item: ListItemEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val savedItem = item.copy(updatedAt = now)
        itemDao.updateItem(savedItem)
        val list = listDao.getListById(item.listId)
        if (list != null) {
            listDao.updateList(list.copy(updatedAt = now))
        }
        NotificationScheduler.scheduleItemReminder(context, savedItem, list?.name ?: "List")
        checkAutoDelete(item)
        triggerWidgetUpdate()
    }

    suspend fun toggleTaskChecked(itemId: Long, isChecked: Boolean) = withContext(Dispatchers.IO) {
        val item = itemDao.getItemById(itemId) ?: return@withContext
        val now = System.currentTimeMillis()
        val updated = item.copy(
            isChecked = isChecked,
            checkedAt = if (isChecked) now else null,
            updatedAt = now
        )
        itemDao.updateItem(updated)
        val list = listDao.getListById(item.listId)
        if (list != null) {
            listDao.updateList(list.copy(updatedAt = now))
        }
        if (isChecked) {
            NotificationScheduler.cancelItemReminder(context, itemId)
        } else {
            NotificationScheduler.scheduleItemReminder(context, updated, list?.name ?: "List")
        }
        checkAutoDelete(updated)
        triggerWidgetUpdate()
    }

    suspend fun toggleSubTaskChecked(itemId: Long, subtaskIndex: Int) = withContext(Dispatchers.IO) {
        val item = itemDao.getItemById(itemId) ?: return@withContext
        val subtasks = SubTask.fromJson(item.subtasksJson).toMutableList()
        if (subtaskIndex in subtasks.indices) {
            val st = subtasks[subtaskIndex]
            subtasks[subtaskIndex] = st.copy(isCompleted = !st.isCompleted)
            val now = System.currentTimeMillis()
            val updated = item.copy(
                subtasksJson = SubTask.toJson(subtasks),
                updatedAt = now
            )
            itemDao.updateItem(updated)
            val list = listDao.getListById(item.listId)
            if (list != null) {
                listDao.updateList(list.copy(updatedAt = now))
            }
            triggerWidgetUpdate()
        }
    }

    suspend fun deleteItem(item: ListItemEntity) = withContext(Dispatchers.IO) {
        itemDao.deleteItem(item)
        val list = listDao.getListById(item.listId)
        if (list != null) {
            listDao.updateList(list.copy(updatedAt = System.currentTimeMillis()))
        }
        NotificationScheduler.cancelItemReminder(context, item.id)
        triggerWidgetUpdate()
    }

    suspend fun deleteItemById(itemId: Long) = withContext(Dispatchers.IO) {
        val item = itemDao.getItemById(itemId)
        itemDao.deleteItemById(itemId)
        if (item != null) {
            val list = listDao.getListById(item.listId)
            if (list != null) {
                listDao.updateList(list.copy(updatedAt = System.currentTimeMillis()))
            }
        }
        triggerWidgetUpdate()
    }

    suspend fun clearCompletedInList(listId: Long) = withContext(Dispatchers.IO) {
        itemDao.deleteCheckedItemsInList(listId)
        triggerWidgetUpdate()
    }

    private suspend fun checkAutoDelete(item: ListItemEntity) {
        if (!item.isChecked) return
        val list = listDao.getListById(item.listId) ?: return
        when (list.autoDeleteMode) {
            "IMMEDIATELY" -> {
                itemDao.deleteItemById(item.id)
            }
            "AFTER_24H" -> {
                // Background periodic/startup sweep handles this, or check here
                if (item.checkedAt != null && System.currentTimeMillis() - item.checkedAt >= 86400000L) {
                    itemDao.deleteItemById(item.id)
                }
            }
            "AFTER_1W" -> {
                if (item.checkedAt != null && System.currentTimeMillis() - item.checkedAt >= 604800000L) {
                    itemDao.deleteItemById(item.id)
                }
            }
        }
    }

    suspend fun runAutoDeleteSweep() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lists = listDao.getAllLists().firstOrNull() ?: return@withContext
        for (l in lists) {
            val items = itemDao.getItemsForListSync(l.id)
            for (it in items) {
                if (it.isChecked && it.checkedAt != null) {
                    val age = now - it.checkedAt
                    val shouldDelete = when (l.autoDeleteMode) {
                        "IMMEDIATELY" -> true
                        "AFTER_24H" -> age >= 86400000L
                        "AFTER_1W" -> age >= 604800000L
                        else -> false
                    }
                    if (shouldDelete) {
                        itemDao.deleteItemById(it.id)
                    }
                }
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setColorPalette(palette: ColorPalettePreset) {
        _colorPalette.value = palette
        prefs.edit().putString("color_palette", palette.name).apply()
        triggerWidgetUpdate()
    }

    fun setFontFamily(font: AppFontFamily) {
        _fontFamily.value = font
        prefs.edit().putString("font_family", font.name).apply()
    }

    fun setShowCounts(show: Boolean) {
        _showCounts.value = show
        prefs.edit().putBoolean("show_counts", show).apply()
    }

    fun addCustomCategory(name: String, iconKey: String = "tag", colorHex: String = "#0284C7"): CategoryOption {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return CategoryOption.DEFAULT_CATEGORIES.first()
        val existing = _categories.value.find { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing

        val newCategory = CategoryOption(
            id = "CAT_" + System.currentTimeMillis(),
            name = trimmed,
            iconKey = iconKey,
            colorHex = colorHex,
            isDefault = false
        )
        val updated = _categories.value.toMutableList().apply { add(newCategory) }
        saveCategories(updated)
        return newCategory
    }

    fun updateCategory(id: String, newName: String, newIconKey: String, newColorHex: String) {
        val updated = _categories.value.map { cat ->
            if (cat.id == id || cat.name == id) {
                cat.copy(
                    name = newName.trim().ifBlank { cat.name },
                    iconKey = newIconKey,
                    colorHex = newColorHex
                )
            } else cat
        }
        saveCategories(updated)
    }

    fun deleteCategory(id: String) {
        val current = _categories.value
        val target = current.find { it.id == id || it.name == id }
        if (target != null && target.isDefault) return // Protect default categories from complete deletion
        val updated = current.filterNot { it.id == id || it.name == id }
        saveCategories(updated)
    }

    fun resetCategories() {
        saveCategories(CategoryOption.DEFAULT_CATEGORIES)
    }

    fun selectActiveWidget(widgetId: String) {
        val target = _allWidgets.value.find { it.widgetId == widgetId }
        if (target != null) {
            _activeWidgetId.value = widgetId
            _widgetConfig.value = target
            prefs.edit().putString("active_widget_id", widgetId).apply()
        }
    }

    fun addNewWidget(name: String? = null, type: WidgetType = WidgetType.SPECIFIC_LIST): WidgetStyleConfig {
        val current = _allWidgets.value.toMutableList()
        val count = current.size + 1
        val newId = "widget_" + System.currentTimeMillis()
        val defaultTheme = when (type) {
            WidgetType.ALL_LISTS -> "OCEAN"
            WidgetType.ALL_ITEMS -> "EMERALD"
            WidgetType.SPECIFIC_ITEM -> "SUNSET"
            WidgetType.SPECIFIC_LIST -> "PURPLE"
        }
        val newWidget = WidgetStyleConfig(
            widgetId = newId,
            widgetName = name ?: "Widget $count (${type.label})",
            widgetType = type,
            themeStyle = defaultTheme,
            showDescription = true
        )
        current.add(newWidget)
        saveWidgetsList(current)
        selectActiveWidget(newId)
        triggerWidgetUpdate()
        return newWidget
    }

    fun duplicateWidget(widgetId: String): WidgetStyleConfig? {
        val current = _allWidgets.value.toMutableList()
        val source = current.find { it.widgetId == widgetId } ?: return null
        val newId = "widget_" + System.currentTimeMillis()
        val copy = source.copy(
            widgetId = newId,
            widgetName = "${source.widgetName} (Copy)"
        )
        current.add(copy)
        saveWidgetsList(current)
        selectActiveWidget(newId)
        triggerWidgetUpdate()
        return copy
    }

    fun deleteWidget(widgetId: String) {
        val current = _allWidgets.value.toMutableList()
        if (current.size <= 1) return // Keep at least one widget
        current.removeAll { it.widgetId == widgetId }
        saveWidgetsList(current)
        if (_activeWidgetId.value == widgetId) {
            val next = current.first()
            _activeWidgetId.value = next.widgetId
            _widgetConfig.value = next
            prefs.edit().putString("active_widget_id", next.widgetId).apply()
        }
        triggerWidgetUpdate()
    }

    fun updateWidgetConfig(config: WidgetStyleConfig) {
        val current = _allWidgets.value.toMutableList()
        val index = current.indexOfFirst { it.widgetId == config.widgetId }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(config)
        }
        saveWidgetsList(current)
        _widgetConfig.value = config

        // Also update legacy single-widget keys for compatibility
        prefs.edit()
            .putString("widget_type", config.widgetType.name)
            .putLong("widget_list_id", config.listId)
            .putLong("widget_item_id", config.itemId)
            .putString("widget_color_preset", config.colorPreset.name)
            .putString("widget_theme_style", config.themeStyle)
            .putBoolean("widget_show_description", config.showDescription)
            .putBoolean("widget_hide_checked", config.hideChecked)
            .apply()

        triggerWidgetUpdate()
    }

    fun getWidgetConfigForAppWidgetId(appWidgetId: Int, defaultType: WidgetType? = null): WidgetStyleConfig {
        val directJson = prefs.getString("appwidget_${appWidgetId}_config_json", null)
        if (!directJson.isNullOrBlank()) {
            try {
                val obj = org.json.JSONObject(directJson)
                return WidgetStyleConfig(
                    widgetId = obj.optString("widgetId", "widget_$appWidgetId"),
                    widgetName = obj.optString("widgetName", "Widget #$appWidgetId"),
                    widgetType = WidgetType.valueOf(obj.optString("widgetType", (defaultType ?: WidgetType.SPECIFIC_LIST).name)),
                    listId = obj.optLong("listId", 0L),
                    itemId = obj.optLong("itemId", 0L),
                    categoryFilter = obj.optString("categoryFilter", "ALL"),
                    tagFilter = obj.optString("tagFilter", "ALL"),
                    colorPreset = try {
                        ColorPalettePreset.valueOf(obj.optString("colorPreset", ColorPalettePreset.BENTO_PURPLE.name))
                    } catch (_: Exception) { ColorPalettePreset.BENTO_PURPLE },
                    themeStyle = obj.optString("themeStyle", "PURPLE"),
                    showDescription = obj.optBoolean("showDescription", true),
                    hideChecked = obj.optBoolean("hideChecked", false),
                    textSizeScale = obj.optString("textSizeScale", "MEDIUM"),
                    customImageUri = if (obj.isNull("customImageUri")) null else obj.optString("customImageUri", null)
                )
            } catch (_: Exception) {}
        }

        val boundConfigId = prefs.getString("appwidget_${appWidgetId}_config_id", null)
        if (boundConfigId != null) {
            val bound = _allWidgets.value.find { it.widgetId == boundConfigId }
            if (bound != null) {
                return if (defaultType != null && bound.widgetType != defaultType) {
                    bound.copy(widgetType = defaultType)
                } else {
                    bound
                }
            }
        }

        val widgets = _allWidgets.value
        if (defaultType != null) {
            val match = widgets.find { it.widgetType == defaultType }
            if (match != null) return match
            return WidgetStyleConfig(
                widgetId = "widget_$appWidgetId",
                widgetName = "${defaultType.label} Widget",
                widgetType = defaultType,
                themeStyle = when (defaultType) {
                    WidgetType.ALL_LISTS -> "OCEAN"
                    WidgetType.ALL_ITEMS -> "EMERALD"
                    WidgetType.SPECIFIC_ITEM -> "SUNSET"
                    WidgetType.SPECIFIC_LIST -> "PURPLE"
                }
            )
        }

        if (widgets.isNotEmpty()) {
            val modIndex = kotlin.math.abs(appWidgetId) % widgets.size
            return widgets[modIndex]
        }
        return _widgetConfig.value
    }

    fun saveWidgetConfigForAppWidgetId(appWidgetId: Int, config: WidgetStyleConfig) {
        try {
            val obj = org.json.JSONObject().apply {
                put("widgetId", config.widgetId)
                put("widgetName", config.widgetName)
                put("widgetType", config.widgetType.name)
                put("listId", config.listId)
                put("itemId", config.itemId)
                put("categoryFilter", config.categoryFilter)
                put("tagFilter", config.tagFilter)
                put("colorPreset", config.colorPreset.name)
                put("themeStyle", config.themeStyle)
                put("showDescription", config.showDescription)
                put("hideChecked", config.hideChecked)
                put("textSizeScale", config.textSizeScale)
                put("customImageUri", config.customImageUri)
            }
            prefs.edit()
                .putString("appwidget_${appWidgetId}_config_json", obj.toString())
                .putString("appwidget_${appWidgetId}_config_id", config.widgetId)
                .apply()

            val current = _allWidgets.value.toMutableList()
            val index = current.indexOfFirst { it.widgetId == config.widgetId }
            if (index >= 0) {
                current[index] = config
            } else {
                current.add(config)
            }
            saveWidgetsList(current)
            _widgetConfig.value = config
        } catch (_: Exception) {}
        triggerWidgetUpdate()
    }

    fun bindAppWidgetToConfig(appWidgetId: Int, configId: String) {
        prefs.edit().putString("appwidget_${appWidgetId}_config_id", configId).apply()
        triggerWidgetUpdate()
    }

    fun cycleNextListForWidget(appWidgetId: Int) {
        val currentConfig = getWidgetConfigForAppWidgetId(appWidgetId)
        val allLists = listDao.getAllListsSync()
        if (allLists.isEmpty()) return

        val currentIndex = allLists.indexOfFirst { it.id == currentConfig.listId }
        val nextIndex = (currentIndex + 1) % allLists.size
        val nextList = allLists[nextIndex]

        val updated = currentConfig.copy(listId = nextList.id)
        updateWidgetConfig(updated)
    }

    fun addCustomPriority(label: String, colorHex: String) {
        val current = _priorities.value.toMutableList()
        val newId = "CUSTOM_" + System.currentTimeMillis()
        current.add(PriorityOption(newId, label.trim(), colorHex))
        savePriorities(current)
    }

    fun updatePriority(id: String, newLabel: String, newColorHex: String) {
        val current = _priorities.value.map { p ->
            if (p.id == id) p.copy(label = newLabel.trim(), colorHex = newColorHex)
            else p
        }
        savePriorities(current)
    }

    fun deletePriority(id: String) {
        val current = _priorities.value.filter { it.id != id }
        savePriorities(current)
    }

    fun resetPriorities() {
        savePriorities(PriorityOption.DEFAULT_PRIORITIES)
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val appVersionName = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Throwable) { "1.0.0" }
        root.put("appVersion", appVersionName)

        // Settings
        val settingsObj = JSONObject().apply {
            put("themeMode", _themeMode.value.name)
            put("colorPalette", _colorPalette.value.name)
            put("fontFamily", _fontFamily.value.name)
            put("showCounts", _showCounts.value)
        }
        root.put("settings", settingsObj)

        // Lists
        val lists = listDao.getAllListsSync()
        val listsArray = JSONArray()
        lists.forEach { list ->
            val lObj = JSONObject().apply {
                put("id", list.id)
                put("name", list.name)
                put("iconName", list.iconName)
                put("description", list.description ?: "")
                put("colorHex", list.colorHex)
                put("autoDeleteMode", list.autoDeleteMode)
                put("hideCheckedItems", list.hideCheckedItems)
                put("customIconUri", list.customIconUri)
                put("createdAt", list.createdAt)
                put("updatedAt", list.updatedAt)
            }
            listsArray.put(lObj)
        }
        root.put("lists", listsArray)

        // Items
        val items = itemDao.getAllItemsSync()
        val itemsArray = JSONArray()
        items.forEach { item ->
            val iObj = JSONObject().apply {
                put("id", item.id)
                put("listId", item.listId)
                put("title", item.title)
                put("type", item.type)
                put("category", item.category)
                put("section", item.section)
                put("isChecked", item.isChecked)
                if (item.checkedAt != null) put("checkedAt", item.checkedAt)
                put("description", item.description)
                if (item.imageUri != null) put("imageUri", item.imageUri)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
                if (item.dueDate != null) put("dueDate", item.dueDate)
                put("priority", item.priority)
                put("tags", item.tags)
                put("subtasksJson", item.subtasksJson)
            }
            itemsArray.put(iObj)
        }
        root.put("items", itemsArray)

        // Categories
        val categoriesArray = JSONArray()
        _categories.value.forEach { cat ->
            val cObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("iconKey", cat.iconKey)
                put("colorHex", cat.colorHex)
                put("isDefault", cat.isDefault)
            }
            categoriesArray.put(cObj)
        }
        root.put("categories", categoriesArray)

        // Priorities
        val prioritiesArray = JSONArray()
        _priorities.value.forEach { pri ->
            val pObj = JSONObject().apply {
                put("id", pri.id)
                put("label", pri.label)
                put("colorHex", pri.colorHex)
            }
            prioritiesArray.put(pObj)
        }
        root.put("priorities", prioritiesArray)

        // Widgets
        val widgetsArray = JSONArray()
        _allWidgets.value.forEach { w ->
            val wObj = JSONObject().apply {
                put("widgetId", w.widgetId)
                put("widgetName", w.widgetName)
                put("widgetType", w.widgetType.name)
                put("listId", w.listId)
                put("itemId", w.itemId)
                put("categoryFilter", w.categoryFilter)
                put("tagFilter", w.tagFilter)
                if (w.customImageUri != null) put("customImageUri", w.customImageUri)
                put("colorPreset", w.colorPreset.name)
                put("themeStyle", w.themeStyle)
                put("showDescription", w.showDescription)
                put("hideChecked", w.hideChecked)
                put("textSizeScale", w.textSizeScale)
            }
            widgetsArray.put(wObj)
        }
        root.put("widgets", widgetsArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)

            // Settings
            if (root.has("settings")) {
                val settings = root.getJSONObject("settings")
                val tModeStr = settings.optString("themeMode", null)
                if (tModeStr != null) {
                    try { setThemeMode(AppThemeMode.valueOf(tModeStr)) } catch (_: Exception) {}
                }
                val colorPresetStr = settings.optString("colorPalette", null)
                if (colorPresetStr != null) {
                    try { setColorPalette(ColorPalettePreset.valueOf(colorPresetStr)) } catch (_: Exception) {}
                }
                val fontStr = settings.optString("fontFamily", null)
                if (fontStr != null) {
                    try { setFontFamily(AppFontFamily.valueOf(fontStr)) } catch (_: Exception) {}
                }
                if (settings.has("showCounts")) {
                    setShowCounts(settings.getBoolean("showCounts"))
                }
            }

            // Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                val newCatList = mutableListOf<CategoryOption>()
                for (i in 0 until catArray.length()) {
                    val cObj = catArray.getJSONObject(i)
                    newCatList.add(
                        CategoryOption(
                            id = cObj.optString("id", "cat_$i"),
                            name = cObj.optString("name", "Category $i"),
                            iconKey = cObj.optString("iconKey", "tag"),
                            colorHex = cObj.optString("colorHex", "#0284C7"),
                            isDefault = cObj.optBoolean("isDefault", false)
                        )
                    )
                }
                if (newCatList.isNotEmpty()) {
                    saveCategories(newCatList)
                }
            }

            // Priorities
            if (root.has("priorities")) {
                val priArray = root.getJSONArray("priorities")
                val newPriList = mutableListOf<PriorityOption>()
                for (i in 0 until priArray.length()) {
                    val pObj = priArray.getJSONObject(i)
                    newPriList.add(
                        PriorityOption(
                            id = pObj.optString("id", "pri_$i"),
                            label = pObj.optString("label", "Priority $i"),
                            colorHex = pObj.optString("colorHex", "#64748B")
                        )
                    )
                }
                if (newPriList.isNotEmpty()) {
                    savePriorities(newPriList)
                }
            }

            // Lists
            if (root.has("lists")) {
                val listsArray = root.getJSONArray("lists")
                val listsToInsert = mutableListOf<ListEntity>()
                for (i in 0 until listsArray.length()) {
                    val lObj = listsArray.getJSONObject(i)
                    listsToInsert.add(
                        ListEntity(
                            id = lObj.optLong("id", 0L),
                            name = lObj.optString("name", "List"),
                            iconName = lObj.optString("iconName", "checklist"),
                            description = if (lObj.isNull("description")) null else lObj.optString("description", null),
                            colorHex = lObj.optString("colorHex", "#7C3AED"),
                            autoDeleteMode = lObj.optString("autoDeleteMode", "NEVER"),
                            hideCheckedItems = lObj.optBoolean("hideCheckedItems", false),
                            customIconUri = if (lObj.isNull("customIconUri")) null else lObj.optString("customIconUri", null),
                            createdAt = lObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = lObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (listsToInsert.isNotEmpty()) {
                    listsToInsert.forEach { listDao.insertList(it) }
                }
            }

            // Items
            if (root.has("items")) {
                val itemsArray = root.getJSONArray("items")
                val itemsToInsert = mutableListOf<ListItemEntity>()
                for (i in 0 until itemsArray.length()) {
                    val iObj = itemsArray.getJSONObject(i)
                    itemsToInsert.add(
                        ListItemEntity(
                            id = iObj.optLong("id", 0L),
                            listId = iObj.optLong("listId", 0L),
                            title = iObj.optString("title", ""),
                            type = iObj.optString("type", "TASK"),
                            category = iObj.optString("category", "Task"),
                            section = iObj.optString("section", ""),
                            isChecked = iObj.optBoolean("isChecked", false),
                            checkedAt = if (iObj.isNull("checkedAt")) null else iObj.optLong("checkedAt"),
                            description = iObj.optString("description", ""),
                            imageUri = if (iObj.isNull("imageUri")) null else iObj.optString("imageUri"),
                            createdAt = iObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = iObj.optLong("updatedAt", System.currentTimeMillis()),
                            dueDate = if (iObj.isNull("dueDate")) null else iObj.optLong("dueDate"),
                            priority = iObj.optString("priority", "NONE"),
                            tags = iObj.optString("tags", ""),
                            subtasksJson = iObj.optString("subtasksJson", "[]")
                        )
                    )
                }
                if (itemsToInsert.isNotEmpty()) {
                    itemsToInsert.forEach { itemDao.insertItem(it) }
                }
            }

            // Widgets
            if (root.has("widgets")) {
                val widgetsArray = root.getJSONArray("widgets")
                val widgetList = mutableListOf<WidgetStyleConfig>()
                for (i in 0 until widgetsArray.length()) {
                    val wObj = widgetsArray.getJSONObject(i)
                    val wType = try {
                        WidgetType.valueOf(wObj.optString("widgetType", WidgetType.SPECIFIC_LIST.name))
                    } catch (_: Exception) { WidgetType.SPECIFIC_LIST }
                    val cPreset = try {
                        ColorPalettePreset.valueOf(wObj.optString("colorPreset", ColorPalettePreset.PURPLE_VIOLET.name))
                    } catch (_: Exception) { ColorPalettePreset.PURPLE_VIOLET }

                    widgetList.add(
                        WidgetStyleConfig(
                            widgetId = wObj.optString("widgetId", "widget_${i + 1}"),
                            widgetName = wObj.optString("widgetName", "Widget ${i + 1}"),
                            widgetType = wType,
                            listId = wObj.optLong("listId", 0L),
                            itemId = wObj.optLong("itemId", 0L),
                            categoryFilter = wObj.optString("categoryFilter", "ALL"),
                            tagFilter = wObj.optString("tagFilter", "ALL"),
                            customImageUri = if (wObj.isNull("customImageUri")) null else wObj.optString("customImageUri"),
                            colorPreset = cPreset,
                            themeStyle = wObj.optString("themeStyle", "PURPLE"),
                            showDescription = wObj.optBoolean("showDescription", true),
                            hideChecked = wObj.optBoolean("hideChecked", false),
                            textSizeScale = wObj.optString("textSizeScale", "MEDIUM")
                        )
                    )
                }
                if (widgetList.isNotEmpty()) {
                    saveWidgetsList(widgetList)
                }
            }

            triggerWidgetUpdate()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun triggerWidgetUpdate() {
        try {
            ListAppWidgetProvider.sendUpdateBroadcast(context)
        } catch (_: Exception) {}
    }
}
