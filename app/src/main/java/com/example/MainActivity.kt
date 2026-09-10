package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ListItemEntity
import com.example.ui.components.CreateListDialog
import com.example.ui.components.ItemPopupDialog
import com.example.ui.screens.AllItemsScreen
import com.example.ui.screens.AllListsScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.ListDetailScreen
import com.example.ui.screens.WidgetStudioScreen
import com.example.ui.theme.ListWidgetAppTheme
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.ListAppViewModel
import com.example.widget.ListAppWidgetProvider

class MainActivity : ComponentActivity() {
    private val viewModel: ListAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWidgetIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
            val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()

            ListWidgetAppTheme(
                themeMode = themeMode,
                colorPalette = colorPalette,
                fontFamily = fontFamily,
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return
        val openItemId = intent.getLongExtra(ListAppWidgetProvider.EXTRA_OPEN_ITEM_ID, -1L)
        val listId = intent.getLongExtra(ListAppWidgetProvider.EXTRA_LIST_ID, -1L)
        val quickAdd = intent.getBooleanExtra(ListAppWidgetProvider.EXTRA_QUICK_ADD, false)

        if (openItemId > 0) {
            viewModel.openItemById(openItemId)
        } else if (quickAdd) {
            viewModel.openNewItemPopup(if (listId > 0) listId else 0L)
        } else if (listId > 0) {
            viewModel.openListDetail(listId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ListAppViewModel) {
    val allLists by viewModel.allLists.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val itemsWithImages by viewModel.itemsWithImages.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()

    val activeItemForPopup by viewModel.activeItemForPopup.collectAsStateWithLifecycle()
    val showNewItemPopup by viewModel.showNewItemPopup.collectAsStateWithLifecycle()
    val activeListForEdit by viewModel.activeListForEdit.collectAsStateWithLifecycle()
    val showCreateListDialog by viewModel.showCreateListDialog.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val selectedPriorities by viewModel.selectedPriorities.collectAsStateWithLifecycle()
    val itemSortOption by viewModel.itemSortOption.collectAsStateWithLifecycle()
    val gallerySortOption by viewModel.gallerySortOption.collectAsStateWithLifecycle()
    val groupByList by viewModel.groupByList.collectAsStateWithLifecycle()
    val groupByCategory by viewModel.groupByCategory.collectAsStateWithLifecycle()

    val showCounts by viewModel.showCounts.collectAsStateWithLifecycle()
    val widgetConfig by viewModel.widgetConfig.collectAsStateWithLifecycle()
    val allWidgets by viewModel.allWidgets.collectAsStateWithLifecycle()
    val activeWidgetId by viewModel.activeWidgetId.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val priorities by viewModel.priorities.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val selectedList = remember(allLists, selectedListId) {
        allLists.find { it.id == selectedListId }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            if (selectedList == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = when (currentTab) {
                                AppNavTab.ALL_LISTS -> "Lists"
                                AppNavTab.ALL_ITEMS -> "Items"
                                AppNavTab.GALLERY -> "Gallery"
                                AppNavTab.SETTINGS -> "Settings"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (selectedList == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        .testTag("bottom_nav_bar"),
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.ALL_LISTS,
                        onClick = { viewModel.selectTab(AppNavTab.ALL_LISTS) },
                        icon = { Icon(Icons.Default.Layers, contentDescription = "Lists") },
                        label = { Text("Lists", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.ALL_ITEMS,
                        onClick = { viewModel.selectTab(AppNavTab.ALL_ITEMS) },
                        icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "All Items") },
                        label = { Text("Items", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.GALLERY,
                        onClick = { viewModel.selectTab(AppNavTab.GALLERY) },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery") },
                        label = { Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.SETTINGS,
                        onClick = { viewModel.selectTab(AppNavTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = Pair(selectedList, currentTab),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { (currentSelectedList, tab) ->
                if (currentSelectedList != null) {
                    val listItems = allItems.filter { it.listId == currentSelectedList.id }
                    ListDetailScreen(
                        list = currentSelectedList,
                        items = listItems,
                        onBackClick = { viewModel.closeListDetail() },
                        onEditListClick = { viewModel.openCreateListDialog(it) },
                        onToggleHideChecked = { viewModel.toggleListHideChecked(it) },
                        onClearCompleted = { viewModel.clearCompletedInList(it) },
                        onToggleCheck = { id, checked -> viewModel.toggleTaskChecked(id, checked) },
                        onClickItem = { viewModel.openItemPopup(it) },
                        onAddNewItem = { viewModel.openNewItemPopup(it) }
                    )
                } else {
                    when (tab) {
                        AppNavTab.ALL_LISTS -> {
                            AllListsScreen(
                                lists = allLists,
                                allItems = allItems,
                                showCounts = showCounts,
                                categories = categories,
                                onListClick = { viewModel.openListDetail(it) },
                                onCreateListClick = { viewModel.openCreateListDialog() },
                                onEditListClick = { viewModel.openCreateListDialog(it) }
                            )
                        }
                        AppNavTab.ALL_ITEMS -> {
                            AllItemsScreen(
                                items = allItems,
                                lists = allLists,
                                priorities = priorities,
                                categories = categories,
                                searchQuery = searchQuery,
                                selectedTags = selectedTags,
                                selectedCategories = selectedCategories,
                                selectedPriorities = selectedPriorities,
                                sortOption = itemSortOption,
                                groupByList = groupByList,
                                groupByCategory = groupByCategory,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onToggleTag = { viewModel.toggleTagFilter(it) },
                                onClearTags = { viewModel.clearTagFilters() },
                                onToggleCategory = { viewModel.toggleCategoryFilter(it) },
                                onClearCategories = { viewModel.clearCategoryFilters() },
                                onTogglePriority = { viewModel.togglePriorityFilter(it) },
                                onClearPriorities = { viewModel.clearPriorityFilters() },
                                onSortChange = { viewModel.setItemSortOption(it) },
                                onToggleGroupByList = { viewModel.setGroupByList(it) },
                                onToggleGroupByCategory = { viewModel.setGroupByCategory(it) },
                                onToggleCheck = { id, checked -> viewModel.toggleTaskChecked(id, checked) },
                                onClickItem = { viewModel.openItemPopup(it) },
                                onNavigateToList = { viewModel.openListDetail(it) },
                                onAddNewItem = { viewModel.openNewItemPopup() }
                            )
                        }
                        AppNavTab.GALLERY -> {
                            GalleryScreen(
                                itemsWithImages = itemsWithImages,
                                lists = allLists,
                                categories = categories,
                                sortOption = gallerySortOption,
                                onSortChange = { viewModel.setGallerySortOption(it) },
                                onClickItem = { viewModel.openItemPopup(it) },
                                onNavigateToList = { viewModel.openListDetail(it) }
                            )
                        }
                        AppNavTab.SETTINGS -> {
                            WidgetStudioScreen(
                                lists = allLists,
                                allItems = allItems,
                                widgetConfig = widgetConfig,
                                allWidgets = allWidgets,
                                activeWidgetId = activeWidgetId,
                                onSelectActiveWidget = { viewModel.selectActiveWidget(it) },
                                onAddNewWidget = { name, type -> viewModel.addNewWidget(name, type) },
                                onDuplicateWidget = { viewModel.duplicateWidget(it) },
                                onDeleteWidget = { viewModel.deleteWidget(it) },
                                themeMode = themeMode,
                                colorPalette = colorPalette,
                                fontFamily = fontFamily,
                                showCounts = showCounts,
                                priorities = priorities,
                                categories = categories,
                                onUpdateWidgetConfig = { viewModel.updateWidgetConfig(it) },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetColorPalette = { viewModel.setColorPalette(it) },
                                onSetFontFamily = { viewModel.setFontFamily(it) },
                                onSetShowCounts = { viewModel.setShowCounts(it) },
                                onExportBackup = { viewModel.exportBackupJson() },
                                onRestoreBackup = { viewModel.restoreBackupJson(it) },
                                onAddPriority = { label, hex -> viewModel.addCustomPriority(label, hex) },
                                onUpdatePriority = { id, label, hex -> viewModel.updatePriority(id, label, hex) },
                                onDeletePriority = { id -> viewModel.deletePriority(id) },
                                onResetPriorities = { viewModel.resetPriorities() },
                                onAddCategory = { name, icon, color -> viewModel.addCustomCategory(name, icon, color) },
                                onUpdateCategory = { id, name, icon, color -> viewModel.updateCategory(id, name, icon, color) },
                                onDeleteCategory = { id -> viewModel.deleteCategory(id) },
                                onResetCategories = { viewModel.resetCategories() },
                                onToggleTaskChecked = { id, checked -> viewModel.toggleTaskChecked(id, checked) },
                                onQuickAddClick = { viewModel.openNewItemPopup(it) },
                                onClickItem = { viewModel.openItemPopup(it) }
                            )
                        }
                    }
                }
            }

            // POP-UP ITEM DETAIL / EDIT DIALOG (Matching Screenshot)
            if ((activeItemForPopup != null) || showNewItemPopup) {
                ItemPopupDialog(
                    itemToEdit = activeItemForPopup,
                    defaultListId = selectedListId ?: 0L,
                    lists = allLists,
                    categories = categories,
                    priorities = priorities,
                    onDismiss = viewModel::closeItemPopup,
                    onSave = viewModel::saveItem,
                    onDelete = viewModel::deleteItem,
                    onAddCustomCategory = viewModel::addCustomCategory
                )
            }

            // CREATE / EDIT LIST DIALOG
            if (showCreateListDialog) {
                CreateListDialog(
                    listToEdit = activeListForEdit,
                    onDismiss = viewModel::closeCreateListDialog,
                    onSave = viewModel::saveList,
                    onDelete = viewModel::deleteList
                )
            }
        }
    }
}
