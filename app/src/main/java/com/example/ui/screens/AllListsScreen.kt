package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.model.CategoryOption
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.ui.components.getVectorForCategory
import com.example.ui.components.getVectorForIconName
import com.example.ui.theme.PrimaryPurple

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllListsScreen(
    lists: List<ListEntity>,
    allItems: List<ListItemEntity>,
    showCounts: Boolean,
    categories: List<CategoryOption> = CategoryOption.DEFAULT_CATEGORIES,
    onListClick: (Long) -> Unit,
    onCreateListClick: () -> Unit,
    onEditListClick: (ListEntity) -> Unit,
    onReorderLists: (List<Long>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchKeyword by remember { mutableStateOf("") }
    val filteredLists = remember(lists, searchKeyword) {
        if (searchKeyword.isBlank()) lists
        else lists.filter {
            it.name.contains(searchKeyword, ignoreCase = true) ||
            (it.description?.contains(searchKeyword, ignoreCase = true) == true)
        }
    }

    var displayLists by remember { mutableStateOf(filteredLists) }
    LaunchedEffect(filteredLists) {
        displayLists = filteredLists
    }

    // Outer-container drag-and-drop state
    var activeDragId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val cardBounds = remember { mutableMapOf<Long, Pair<Float, Float>>() }

    // Category counts across all items
    val categoryCounts = remember(allItems, categories) {
        val grouped = allItems.groupBy { it.effectiveCategory }
        categories.map { cat ->
            cat to (grouped[cat.name]?.size ?: 0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val touchY = startOffset.y
                        val draggedList = displayLists.find { list ->
                            val bounds = cardBounds[list.id]
                            bounds != null && touchY >= bounds.first && touchY <= bounds.second
                        }
                        if (draggedList != null) {
                            activeDragId = draggedList.id
                            dragOffsetY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val currentId = activeDragId ?: return@detectDragGestures
                        change.consume()
                        dragOffsetY += dragAmount.y

                        val currIndex = displayLists.indexOfFirst { it.id == currentId }
                        if (currIndex != -1) {
                            val currBounds = cardBounds[currentId]
                            if (currBounds != null) {
                                val currentCenterY = (currBounds.first + currBounds.second) / 2f + dragOffsetY

                                // Check swap with next item (downward)
                                if (currIndex < displayLists.lastIndex) {
                                    val nextId = displayLists[currIndex + 1].id
                                    val nextBounds = cardBounds[nextId]
                                    if (nextBounds != null && currentCenterY > (nextBounds.first + nextBounds.second) / 2f) {
                                        val mutable = displayLists.toMutableList()
                                        val item = mutable.removeAt(currIndex)
                                        mutable.add(currIndex + 1, item)
                                        displayLists = mutable
                                        dragOffsetY -= (nextBounds.second - nextBounds.first)
                                    }
                                }

                                // Check swap with previous item (upward)
                                if (currIndex > 0) {
                                    val prevId = displayLists[currIndex - 1].id
                                    val prevBounds = cardBounds[prevId]
                                    if (prevBounds != null && currentCenterY < (prevBounds.first + prevBounds.second) / 2f) {
                                        val mutable = displayLists.toMutableList()
                                        val item = mutable.removeAt(currIndex)
                                        mutable.add(currIndex - 1, item)
                                        displayLists = mutable
                                        dragOffsetY += (prevBounds.second - prevBounds.first)
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        activeDragId = null
                        dragOffsetY = 0f
                        onReorderLists(displayLists.map { it.id })
                    },
                    onDragCancel = {
                        activeDragId = null
                        dragOffsetY = 0f
                        onReorderLists(displayLists.map { it.id })
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("all_lists_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = { Text("Search lists...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty()) {
                            IconButton(onClick = { searchKeyword = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_lists_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )
            }

            // Total number of items for each category
            if (showCounts) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "TOTAL BY CATEGORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoryCounts.forEach { (cat, count) ->
                                val catIcon = getVectorForCategory(cat.name, cat.iconKey)
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = catColor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // List of lists with Drag and Drop Reordering
            if (displayLists.isEmpty()) {
                item {
                    EmptyListsPlaceholder(onCreateListClick)
                }
            } else {
                itemsIndexed(displayLists, key = { _, listEntity -> listEntity.id }) { index, listEntity ->
                    val itemsForList = allItems.filter { it.listId == listEntity.id }
                    val sectionsCount = remember(itemsForList) {
                        if (itemsForList.isEmpty()) 0
                        else itemsForList.map { if (it.section.isNotBlank()) it.section.trim() else "General" }.distinct().size
                    }
                    val listCategoryCounts = remember(itemsForList, categories) {
                        val grouped = itemsForList.groupBy { it.effectiveCategory }
                        categories.map { cat -> cat to (grouped[cat.name]?.size ?: 0) }
                    }

                    val isDragging = activeDragId == listEntity.id

                    ListOverviewCard(
                        list = listEntity,
                        totalItems = itemsForList.size,
                        sectionsCount = sectionsCount,
                        categoryCounts = listCategoryCounts,
                        showCounts = showCounts,
                        onClick = { onListClick(listEntity.id) },
                        onEditClick = { onEditListClick(listEntity) },
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                val parent = coordinates.parentLayoutCoordinates
                                if (parent != null) {
                                    val top = coordinates.positionInParent().y
                                    val height = coordinates.size.height.toFloat()
                                    cardBounds[listEntity.id] = Pair(top, top + height)
                                }
                            }
                            .zIndex(if (isDragging) 3f else 1f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                            }
                    )
                }
            }
        }

        // New List FAB Button
        FloatingActionButton(
            onClick = onCreateListClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_list_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New List", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New List", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListOverviewCard(
    list: ListEntity,
    totalItems: Int,
    sectionsCount: Int,
    categoryCounts: List<Pair<CategoryOption, Int>>,
    showCounts: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listColor = remember(list.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(list.colorHex))
        } catch (_: Exception) {
            PrimaryPurple
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag("list_card_${list.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Icon + Name + Description + Drag Handle + Edit Pencil Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // List Icon Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(listColor.copy(alpha = 0.15f))
                        .border(1.dp, listColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (list.customIconUri != null) {
                        AsyncImage(
                            model = list.customIconUri,
                            contentDescription = list.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = getVectorForIconName(list.iconName),
                            contentDescription = list.name,
                            tint = listColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    if (!list.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = list.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit List",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Number of items in list & Number of sections in list
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = listColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, listColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "$totalItems ${if (totalItems == 1) "item" else "items"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = listColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "$sectionsCount ${if (sectionsCount == 1) "section" else "sections"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Category counts per list (if showCounts toggled on)
            if (showCounts) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categoryCounts.filter { it.second > 0 }.forEach { (cat, count) ->
                        val catIcon = getVectorForCategory(cat.name, cat.iconKey)
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(cat.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${cat.name}: $count",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyListsPlaceholder(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No lists created yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap + New List below to create your first list",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
