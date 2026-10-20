package com.example.widget

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.model.CategoryOption
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.PriorityOption
import com.example.data.repository.ListRepository
import com.example.ui.components.CreateListDialog
import com.example.ui.components.ItemPopupDialog
import com.example.ui.components.ItemPreviewDialog
import com.example.ui.screens.ListDetailScreen
import com.example.ui.theme.ListWidgetAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetQuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.transparent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        val toggleCheck = intent.getBooleanExtra(ListAppWidgetProvider.EXTRA_TOGGLE_CHECK, false)
        val isListQuickAdd = intent.getBooleanExtra(ListAppWidgetProvider.EXTRA_QUICK_ADD_LIST, false)
        val listId = intent.getLongExtra(ListAppWidgetProvider.EXTRA_LIST_ID, 0L)
        val openListDetailId = intent.getLongExtra(ListAppWidgetProvider.EXTRA_OPEN_LIST_DETAIL_ID, -1L)
        val previewItemId = intent.getLongExtra(ListAppWidgetProvider.EXTRA_PREVIEW_ITEM_ID, -1L)
        val itemId = intent.getLongExtra(ListAppWidgetProvider.EXTRA_ITEM_ID, -1L)
        val isChecked = intent.getBooleanExtra(ListAppWidgetProvider.EXTRA_IS_CHECKED, false)

        val repo = ListRepository(applicationContext)

        // Handle Checkbox Toggling on Widget without showing any UI
        if (toggleCheck && itemId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo.toggleTaskChecked(itemId, !isChecked)
                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                } catch (_: Throwable) {
                } finally {
                    finish()
                }
            }
            return
        }

        setContent {
            ListWidgetAppTheme {
                var lists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
                var allItems by remember { mutableStateOf<List<ListItemEntity>>(emptyList()) }
                var categories by remember { mutableStateOf<List<CategoryOption>>(emptyList()) }
                var priorities by remember { mutableStateOf<List<PriorityOption>>(emptyList()) }
                var itemTags by remember { mutableStateOf<List<String>>(emptyList()) }
                var targetListForDetail by remember { mutableStateOf<ListEntity?>(null) }
                var previewItem by remember { mutableStateOf<ListItemEntity?>(null) }
                var editingItemFromPreview by remember { mutableStateOf<ListItemEntity?>(null) }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(applicationContext)
                        lists = db.listDao().getAllListsSync()
                        allItems = db.listItemDao().getAllItemsSync()
                        categories = repo.categories.value
                        priorities = repo.priorities.value
                        itemTags = repo.itemTags.value

                        if (openListDetailId > 0) {
                            targetListForDetail = db.listDao().getListById(openListDetailId)
                        }
                        if (previewItemId > 0) {
                            previewItem = db.listItemDao().getItemById(previewItemId)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    if (targetListForDetail != null) {
                        val listItems = allItems.filter { it.listId == targetListForDetail!!.id }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(enabled = false) {},
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            ListDetailScreen(
                                list = targetListForDetail!!,
                                items = listItems,
                                onBackClick = { finish() },
                                onEditListClick = { },
                                onToggleHideChecked = { listEntity ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repo.updateList(listEntity.copy(hideCheckedItems = !listEntity.hideCheckedItems))
                                        ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    }
                                },
                                onClearCompleted = { targetListId ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repo.clearCompletedInList(targetListId)
                                        ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    }
                                },
                                onToggleCheck = { itemIdToToggle, checked ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repo.toggleTaskChecked(itemIdToToggle, checked)
                                        ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                        withContext(Dispatchers.IO) {
                                            val db = AppDatabase.getDatabase(applicationContext)
                                            allItems = db.listItemDao().getAllItemsSync()
                                        }
                                    }
                                },
                                onClickItem = { item ->
                                    previewItem = item
                                },
                                onAddNewItem = { },
                                onReorderItems = { newOrderIds ->
                                    repo.reorderItems(newOrderIds)
                                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                },
                                onLoadSectionOrder = { targetListId -> repo.loadListSectionOrder(targetListId) },
                                onSaveSectionOrder = { targetListId, sections -> repo.saveListSectionOrder(targetListId, sections) },
                                onRenameSection = { targetListId, oldSec, newSec ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repo.renameSectionInList(targetListId, oldSec, newSec)
                                        ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    }
                                },
                                onUpdateItemSection = { itemIdToUpdate, newSec ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repo.updateItemSection(itemIdToUpdate, newSec)
                                        ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    }
                                }
                            )
                        }
                    } else if (previewItem != null) {
                        ItemPreviewDialog(
                            item = previewItem!!,
                            lists = lists,
                            categories = categories,
                            priorities = priorities,
                            onDismiss = { finish() },
                            onEdit = { item ->
                                editingItemFromPreview = item
                                previewItem = null
                            },
                            onDelete = { item ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.deleteItem(item)
                                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    finish()
                                }
                            },
                            onToggleCheck = { id, checked ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.toggleTaskChecked(id, checked)
                                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                }
                            }
                        )
                    } else if (editingItemFromPreview != null) {
                        ItemPopupDialog(
                            itemToEdit = editingItemFromPreview,
                            defaultListId = editingItemFromPreview?.listId ?: 0L,
                            lists = lists,
                            categories = categories,
                            priorities = priorities,
                            itemTags = itemTags,
                            onDismiss = { finish() },
                            onSave = { newItem ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.insertItem(newItem)
                                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    finish()
                                }
                            },
                            onDelete = { item ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.deleteItem(item)
                                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    finish()
                                }
                            },
                            onAddCustomCategory = { name, icon, color ->
                                repo.addCustomCategory(name, icon, color)
                            },
                            onAddPriority = { label, hex ->
                                repo.addCustomPriority(label, hex)
                            },
                            onAddTag = {
                                repo.addCustomTag(it)
                            }
                        )
                    } else if (isListQuickAdd) {
                        CreateListDialog(
                            onDismiss = { finish() },
                            onSave = { newList ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.insertList(newList)
                                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    finish()
                                }
                            }
                        )
                    } else {
                        ItemPopupDialog(
                            defaultListId = listId,
                            lists = lists,
                            categories = categories,
                            priorities = priorities,
                            itemTags = itemTags,
                            onDismiss = { finish() },
                            onSave = { newItem ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.insertItem(newItem)
                                    ListAppWidgetProvider.sendUpdateBroadcast(applicationContext)
                                    finish()
                                }
                            },
                            onAddCustomCategory = { name, icon, color ->
                                repo.addCustomCategory(name, icon, color)
                            },
                            onAddPriority = { label, hex ->
                                repo.addCustomPriority(label, hex)
                            },
                            onAddTag = {
                                repo.addCustomTag(it)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
