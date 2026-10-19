package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SampleDataProvider
import com.example.data.model.AppFontFamily
import com.example.data.model.AppThemeMode
import com.example.data.model.CategoryOption
import com.example.data.model.ColorPalettePreset
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.PriorityOption
import com.example.data.model.WidgetStyleConfig
import com.example.data.model.WidgetType
import com.example.data.repository.ListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavTab(val label: String) {
    ALL_LISTS("All Lists"),
    ALL_ITEMS("All Items"),
    GALLERY("Gallery"),
    SETTINGS("Settings")
}

enum class ItemSortOption(val label: String) {
    DATE_DESC("Date Added (Newest)"),
    DATE_ASC("Date Added (Oldest)"),
    LIST_NAME("List Name (A-Z)"),
    CATEGORY("Category"),
    PRIORITY("Priority (High to Low)"),
    TITLE("Title (A-Z)")
}

enum class GallerySortOption(val label: String) {
    DATE_DESC("Date Added (Newest)"),
    DATE_ASC("Date Added (Oldest)"),
    LIST_NAME("List Name (A-Z)"),
    CATEGORY("Category"),
    PRIORITY("Priority")
}

enum class ItemFilterType {
    ALL, TASKS_ONLY, NOTES_ONLY
}

class ListAppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ListRepository(application)

    val allLists: StateFlow<List<ListEntity>> = repository.allLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<ListItemEntity>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itemsWithImages: StateFlow<List<ListItemEntity>> = repository.itemsWithImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<AppThemeMode> = repository.themeMode
    val colorPalette: StateFlow<ColorPalettePreset> = repository.colorPalette
    val fontFamily: StateFlow<AppFontFamily> = repository.fontFamily
    val showCounts: StateFlow<Boolean> = repository.showCounts
    val widgetConfig: StateFlow<WidgetStyleConfig> = repository.widgetConfig
    val allWidgets: StateFlow<List<WidgetStyleConfig>> = repository.allWidgets
    val activeWidgetId: StateFlow<String> = repository.activeWidgetId
    val priorities: StateFlow<List<PriorityOption>> = repository.priorities
    val categories: StateFlow<List<CategoryOption>> = repository.categories
    val itemTags: StateFlow<List<String>> = repository.itemTags

    // Navigation and screen state
    private val _currentTab = MutableStateFlow(AppNavTab.ALL_LISTS)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    // Dialogs state
    private val _activeItemForPreview = MutableStateFlow<ListItemEntity?>(null)
    val activeItemForPreview: StateFlow<ListItemEntity?> = _activeItemForPreview.asStateFlow()

    private val _activeItemForPopup = MutableStateFlow<ListItemEntity?>(null)
    val activeItemForPopup: StateFlow<ListItemEntity?> = _activeItemForPopup.asStateFlow()

    private val _showNewItemPopup = MutableStateFlow(false)
    val showNewItemPopup: StateFlow<Boolean> = _showNewItemPopup.asStateFlow()

    private val _activeListForEdit = MutableStateFlow<ListEntity?>(null)
    val activeListForEdit: StateFlow<ListEntity?> = _activeListForEdit.asStateFlow()

    private val _showCreateListDialog = MutableStateFlow(false)
    val showCreateListDialog: StateFlow<Boolean> = _showCreateListDialog.asStateFlow()

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _itemFilterType = MutableStateFlow(ItemFilterType.ALL)
    val itemFilterType: StateFlow<ItemFilterType> = _itemFilterType.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _selectedPriorities = MutableStateFlow<Set<String>>(emptySet())
    val selectedPriorities: StateFlow<Set<String>> = _selectedPriorities.asStateFlow()

    private val _groupByList = MutableStateFlow(false)
    val groupByList: StateFlow<Boolean> = _groupByList.asStateFlow()

    private val _groupByCategory = MutableStateFlow(false)
    val groupByCategory: StateFlow<Boolean> = _groupByCategory.asStateFlow()

    private val _itemSortOption = MutableStateFlow(ItemSortOption.DATE_DESC)
    val itemSortOption: StateFlow<ItemSortOption> = _itemSortOption.asStateFlow()

    private val _gallerySortOption = MutableStateFlow(GallerySortOption.DATE_DESC)
    val gallerySortOption: StateFlow<GallerySortOption> = _gallerySortOption.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(application)
            SampleDataProvider.populateData(db, application, forceReset = false)
            repository.runAutoDeleteSweep()
        }
    }

    fun selectTab(tab: AppNavTab) {
        _selectedListId.value = null
        _currentTab.value = tab
    }

    fun openListDetail(listId: Long) {
        _selectedListId.value = listId
    }

    fun reorderLists(newOrderIds: List<Long>) {
        repository.reorderLists(newOrderIds)
    }

    fun reorderItems(newOrderIds: List<Long>) {
        repository.reorderItems(newOrderIds)
    }

    fun closeListDetail() {
        _selectedListId.value = null
    }

    fun openItemPreview(item: ListItemEntity) {
        _activeItemForPreview.value = item
    }

    fun closeItemPreview() {
        _activeItemForPreview.value = null
    }

    fun editItemFromPreview(item: ListItemEntity) {
        _activeItemForPreview.value = null
        _activeItemForPopup.value = item
    }

    fun openItemPopup(item: ListItemEntity) {
        _activeItemForPopup.value = item
    }

    fun openItemById(itemId: Long) {
        viewModelScope.launch {
            val item = repository.getItemById(itemId)
            if (item != null) {
                _currentTab.value = AppNavTab.ALL_ITEMS
                _selectedListId.value = null
                _activeItemForPreview.value = item
            }
        }
    }

    fun openNewItemPopup(listId: Long = 0L) {
        _selectedListId.value = if (listId > 0) listId else _selectedListId.value
        _showNewItemPopup.value = true
    }

    fun closeItemPopup() {
        _activeItemForPopup.value = null
        _showNewItemPopup.value = false
    }

    fun openCreateListDialog(listToEdit: ListEntity? = null) {
        _activeListForEdit.value = listToEdit
        _showCreateListDialog.value = true
    }

    fun closeCreateListDialog() {
        _activeListForEdit.value = null
        _showCreateListDialog.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setItemFilterType(type: ItemFilterType) {
        _itemFilterType.value = type
    }

    fun toggleTagFilter(tag: String) {
        val current = _selectedTags.value.toMutableSet()
        if (current.contains(tag)) {
            current.remove(tag)
        } else {
            current.add(tag)
        }
        _selectedTags.value = current
    }

    fun clearTagFilters() {
        _selectedTags.value = emptySet()
    }

    fun toggleCategoryFilter(category: String) {
        val current = _selectedCategories.value.toMutableSet()
        if (current.contains(category)) {
            current.remove(category)
        } else {
            current.add(category)
        }
        _selectedCategories.value = current
    }

    fun clearCategoryFilters() {
        _selectedCategories.value = emptySet()
    }

    fun togglePriorityFilter(priority: String) {
        val current = _selectedPriorities.value.toMutableSet()
        if (current.contains(priority)) {
            current.remove(priority)
        } else {
            current.add(priority)
        }
        _selectedPriorities.value = current
    }

    fun clearPriorityFilters() {
        _selectedPriorities.value = emptySet()
    }

    fun setGroupByList(grouped: Boolean) {
        _groupByList.value = grouped
    }

    fun setGroupByCategory(grouped: Boolean) {
        _groupByCategory.value = grouped
    }

    fun setItemSortOption(sort: ItemSortOption) {
        _itemSortOption.value = sort
    }

    fun setGallerySortOption(sort: GallerySortOption) {
        _gallerySortOption.value = sort
    }

    // Repository operations
    fun saveItem(item: ListItemEntity) {
        viewModelScope.launch {
            if (item.id > 0) {
                repository.updateItem(item)
            } else {
                repository.insertItem(item)
            }
            closeItemPopup()
        }
    }

    fun deleteItem(item: ListItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            closeItemPopup()
        }
    }

    fun toggleTaskChecked(itemId: Long, isChecked: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskChecked(itemId, isChecked)
        }
    }

    fun saveList(list: ListEntity) {
        viewModelScope.launch {
            if (list.id > 0) {
                repository.updateList(list)
            } else {
                repository.insertList(list)
            }
            closeCreateListDialog()
        }
    }

    fun deleteList(list: ListEntity) {
        viewModelScope.launch {
            if (_selectedListId.value == list.id) {
                _selectedListId.value = null
            }
            repository.deleteList(list)
            closeCreateListDialog()
        }
    }

    fun toggleListHideChecked(list: ListEntity) {
        viewModelScope.launch {
            repository.updateList(list.copy(hideCheckedItems = !list.hideCheckedItems))
        }
    }

    fun clearCompletedInList(listId: Long) {
        viewModelScope.launch {
            repository.clearCompletedInList(listId)
        }
    }

    // Section operations
    fun loadListSectionOrder(listId: Long): List<String> = repository.loadListSectionOrder(listId)
    fun saveListSectionOrder(listId: Long, sections: List<String>) = repository.saveListSectionOrder(listId, sections)

    fun renameSectionInList(listId: Long, oldSection: String, newSection: String) {
        viewModelScope.launch {
            repository.renameSectionInList(listId, oldSection, newSection)
        }
    }

    fun updateItemSection(itemId: Long, newSection: String) {
        viewModelScope.launch {
            repository.updateItemSection(itemId, newSection)
        }
    }

    // Theme & Settings operations
    fun setThemeMode(mode: AppThemeMode) = repository.setThemeMode(mode)
    fun setColorPalette(palette: ColorPalettePreset) = repository.setColorPalette(palette)
    fun setFontFamily(font: AppFontFamily) = repository.setFontFamily(font)
    fun setShowCounts(show: Boolean) = repository.setShowCounts(show)
    suspend fun exportBackupJson(): String = repository.exportBackupJson()
    suspend fun restoreBackupJson(jsonStr: String): Boolean = repository.restoreBackupJson(jsonStr)
    fun updateWidgetConfig(config: WidgetStyleConfig) = repository.updateWidgetConfig(config)
    fun selectActiveWidget(widgetId: String) = repository.selectActiveWidget(widgetId)
    fun addNewWidget(name: String? = null, type: WidgetType = WidgetType.SPECIFIC_LIST) = repository.addNewWidget(name, type)
    fun duplicateWidget(widgetId: String) = repository.duplicateWidget(widgetId)
    fun deleteWidget(widgetId: String) = repository.deleteWidget(widgetId)

    // Category operations
    fun addCustomCategory(name: String, iconKey: String = "tag", colorHex: String = "#0284C7") =
        repository.addCustomCategory(name, iconKey, colorHex)
    fun updateCategory(id: String, newName: String, newIconKey: String, newColorHex: String) =
        repository.updateCategory(id, newName, newIconKey, newColorHex)
    fun deleteCategory(id: String) = repository.deleteCategory(id)
    fun resetCategories() = repository.resetCategories()

    // Priority operations
    fun addCustomPriority(label: String, colorHex: String) = repository.addCustomPriority(label, colorHex)
    fun updatePriority(id: String, newLabel: String, newColorHex: String) = repository.updatePriority(id, newLabel, newColorHex)
    fun deletePriority(id: String) = repository.deletePriority(id)
    fun resetPriorities() = repository.resetPriorities()

    // Tag operations
    fun addCustomTag(tag: String) = repository.addCustomTag(tag)
    fun updateTagAppWide(oldTag: String, newTag: String) {
        viewModelScope.launch {
            repository.updateTagAppWide(oldTag, newTag)
        }
    }
    fun deleteCustomTag(tag: String) {
        viewModelScope.launch {
            repository.deleteCustomTagAppWide(tag)
        }
    }
}
