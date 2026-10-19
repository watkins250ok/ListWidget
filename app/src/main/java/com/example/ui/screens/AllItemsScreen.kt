package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.CategoryOption
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.PriorityOption
import com.example.ui.components.IconPickerGrid
import com.example.ui.components.ItemRowCard
import com.example.ui.components.ItemViewMode
import com.example.ui.components.getVectorForCategory
import com.example.ui.viewmodel.ItemSortOption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllItemsScreen(
    items: List<ListItemEntity>,
    lists: List<ListEntity>,
    priorities: List<PriorityOption>,
    categories: List<CategoryOption>,
    searchQuery: String,
    selectedTags: Set<String>,
    selectedCategories: Set<String>,
    selectedPriorities: Set<String>,
    sortOption: ItemSortOption,
    groupByList: Boolean,
    groupByCategory: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onClearTags: () -> Unit,
    onToggleCategory: (String) -> Unit,
    onClearCategories: () -> Unit,
    onTogglePriority: (String) -> Unit,
    onClearPriorities: () -> Unit,
    onSortChange: (ItemSortOption) -> Unit,
    onToggleGroupByList: (Boolean) -> Unit,
    onToggleGroupByCategory: (Boolean) -> Unit,
    onToggleCheck: (Long, Boolean) -> Unit,
    onClickItem: (ListItemEntity) -> Unit,
    onNavigateToList: (Long) -> Unit,
    onAddNewItem: () -> Unit,
    onAddTag: (String) -> Unit = {},
    onAddCategory: (String, String, String) -> Unit = { _, _, _ -> },
    onReorderItems: (List<Long>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listsMap = remember(lists) { lists.associateBy { it.id } }

    val allDistinctTags = remember(items) {
        items.flatMap { it.tags.split(",") }
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    // Filter items
    val filteredItems = remember(
        items,
        searchQuery,
        selectedTags,
        selectedCategories,
        selectedPriorities,
        sortOption,
        listsMap
    ) {
        items.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.tags.contains(searchQuery, ignoreCase = true)
            }
            val matchesTags = if (selectedTags.isEmpty()) true else {
                val itemTags = item.tags.split(",").map { it.trim().removePrefix("#") }.toSet()
                selectedTags.any { it.removePrefix("#") in itemTags }
            }
            val matchesCategories = if (selectedCategories.isEmpty()) true else {
                selectedCategories.contains(item.effectiveCategory)
            }
            val matchesPriorities = if (selectedPriorities.isEmpty()) true else {
                selectedPriorities.contains(item.priority.uppercase()) ||
                selectedPriorities.any { it.equals(item.priority, ignoreCase = true) }
            }
            matchesSearch && matchesTags && matchesCategories && matchesPriorities
        }.let { listToOrder ->
            when (sortOption) {
                ItemSortOption.DATE_DESC -> listToOrder.sortedByDescending { it.createdAt }
                ItemSortOption.DATE_ASC -> listToOrder.sortedBy { it.createdAt }
                ItemSortOption.LIST_NAME -> listToOrder.sortedBy { listsMap[it.listId]?.name.orEmpty() }
                ItemSortOption.CATEGORY -> listToOrder.sortedBy { it.effectiveCategory }
                ItemSortOption.PRIORITY -> listToOrder.sortedBy {
                    when (it.priority.uppercase()) {
                        "HIGH" -> 0
                        "MEDIUM" -> 1
                        "LOW" -> 2
                        else -> 3
                    }
                }
                ItemSortOption.TITLE -> listToOrder.sortedBy { it.title.lowercase() }
            }
        }
    }

    var displayItems by remember(filteredItems) { mutableStateOf(filteredItems) }
    LaunchedEffect(filteredItems) {
        displayItems = filteredItems
    }

    // Item drag-and-drop state
    var activeDragId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemBounds = remember { mutableMapOf<Long, Pair<Float, Float>>() }

    // Dropdown visibility & View Mode states
    var showSortMenu by remember { mutableStateOf(false) }
    var showTagsMenu by remember { mutableStateOf(false) }
    var showCategoriesMenu by remember { mutableStateOf(false) }
    var showPrioritiesMenu by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ItemViewMode.CONDENSED) }

    // Dialog states for New Tag / New Category from dropdowns
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagNameInput by remember { mutableStateOf("") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryNameInput by remember { mutableStateOf("") }
    var newCategoryIconInput by remember { mutableStateOf("tag") }
    var newCategoryColorInput by remember { mutableStateOf("#0284C7") }

    // Custom 1-line, 2-line, and 3-line ImageVector icons for View Mode
    val singleLineIcon = remember {
        ImageVector.Builder(
            name = "SingleLine",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(3f, 9f)
            lineTo(17f, 9f)
            lineTo(17f, 11f)
            lineTo(3f, 11f)
            close()
        }.build()
    }

    val doubleLineIcon = remember {
        ImageVector.Builder(
            name = "DoubleLine",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(3f, 6.5f)
            lineTo(17f, 6.5f)
            lineTo(17f, 8.5f)
            lineTo(3f, 8.5f)
            close()

            moveTo(3f, 11.5f)
            lineTo(17f, 11.5f)
            lineTo(17f, 13.5f)
            lineTo(3f, 13.5f)
            close()
        }.build()
    }

    val tripleLineIcon = remember {
        ImageVector.Builder(
            name = "TripleLine",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(3f, 5f)
            lineTo(17f, 5f)
            lineTo(17f, 7f)
            lineTo(3f, 7f)
            close()

            moveTo(3f, 9.5f)
            lineTo(17f, 9.5f)
            lineTo(17f, 11.5f)
            lineTo(3f, 11.5f)
            close()

            moveTo(3f, 14f)
            lineTo(17f, 14f)
            lineTo(17f, 16f)
            lineTo(3f, 16f)
            close()
        }.build()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("all_items_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search items, notes, tags...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_all_items_input"),
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

            // All Dropdowns & View Mode Controls
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: View Mode Toggle + Sort Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View Mode Toggle Icons (Minimum, Condensed, Maximum)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minimum (1 Line)
                                IconButton(
                                    onClick = { viewMode = ItemViewMode.MINIMUM },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = singleLineIcon,
                                        contentDescription = "Minimum View",
                                        tint = if (viewMode == ItemViewMode.MINIMUM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Condensed (2 Lines)
                                IconButton(
                                    onClick = { viewMode = ItemViewMode.CONDENSED },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = doubleLineIcon,
                                        contentDescription = "Condensed View",
                                        tint = if (viewMode == ItemViewMode.CONDENSED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Maximum (3 Lines)
                                IconButton(
                                    onClick = { viewMode = ItemViewMode.MAXIMUM },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = tripleLineIcon,
                                        contentDescription = "Maximum View",
                                        tint = if (viewMode == ItemViewMode.MAXIMUM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 1. Sort Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth().testTag("items_sort_dropdown_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = sortOption.label.substringBefore(" ("),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                Text(
                                    text = "SORT BY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                                ItemSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = option.label,
                                                    fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (sortOption == option) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSortChange(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = groupByList,
                                                onCheckedChange = { onToggleGroupByList(it) }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Group by List")
                                        }
                                    },
                                    onClick = { onToggleGroupByList(!groupByList) }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = groupByCategory,
                                                onCheckedChange = { onToggleGroupByCategory(it) }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Group by Category")
                                        }
                                    },
                                    onClick = { onToggleGroupByCategory(!groupByCategory) }
                                )
                            }
                        }
                    }

                    // Row 2: Tags, Category, Priority Dropdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 2. Tags Multi-Select Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val tagsActive = selectedTags.isNotEmpty()
                            Surface(
                                onClick = { showTagsMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (tagsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (tagsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("items_tags_dropdown_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Tag,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (tagsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (tagsActive) "Tags (${selectedTags.size})" else "Tags",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (tagsActive) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = if (tagsActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showTagsMenu,
                                onDismissRequest = { showTagsMenu = false }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SELECT TAGS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    if (selectedTags.isNotEmpty()) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.clickable { onClearTags() }
                                        )
                                    }
                                }
                                if (allDistinctTags.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No tags found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { showTagsMenu = false }
                                    )
                                } else {
                                    allDistinctTags.forEach { tag ->
                                        val isSelected = tag in selectedTags
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { onToggleTag(tag) }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("#$tag")
                                                }
                                            },
                                            onClick = { onToggleTag(tag) }
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("New Tag", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        showTagsMenu = false
                                        showAddTagDialog = true
                                    }
                                )
                            }
                        }

                        // 3. Category Multi-Select Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val categoriesActive = selectedCategories.isNotEmpty()
                            Surface(
                                onClick = { showCategoriesMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (categoriesActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (categoriesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("items_categories_dropdown_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.FilterList,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (categoriesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (categoriesActive) "Cat (${selectedCategories.size})" else "Category",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (categoriesActive) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = if (categoriesActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showCategoriesMenu,
                                onDismissRequest = { showCategoriesMenu = false }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CATEGORIES",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    if (selectedCategories.isNotEmpty()) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.clickable { onClearCategories() }
                                        )
                                    }
                                }
                                categories.forEach { cat ->
                                    val isSelected = cat.name in selectedCategories
                                    val catIcon = getVectorForCategory(cat.name, cat.iconKey)
                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(cat.colorHex))
                                    } catch (_: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { onToggleCategory(cat.name) }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = catIcon,
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(cat.name)
                                            }
                                        },
                                        onClick = { onToggleCategory(cat.name) }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("New Category", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        showCategoriesMenu = false
                                        showAddCategoryDialog = true
                                    }
                                )
                            }
                        }

                        // 4. Priority Multi-Select Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val prioritiesActive = selectedPriorities.isNotEmpty()
                            Surface(
                                onClick = { showPrioritiesMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (prioritiesActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (prioritiesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("items_priorities_dropdown_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (prioritiesActive) "Pri (${selectedPriorities.size})" else "Priority",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (prioritiesActive) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (prioritiesActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showPrioritiesMenu,
                                onDismissRequest = { showPrioritiesMenu = false }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PRIORITIES",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    if (selectedPriorities.isNotEmpty()) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.clickable { onClearPriorities() }
                                        )
                                    }
                                }
                                priorities.forEach { p ->
                                    val isSelected = p.label.uppercase() in selectedPriorities || p.id.uppercase() in selectedPriorities
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { onTogglePriority(p.label.uppercase()) }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(p.label)
                                            }
                                        },
                                        onClick = { onTogglePriority(p.label.uppercase()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Total Items Count & Active Filter Indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL: ${items.size} ITEMS (${filteredItems.size} MATCHING)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    if (selectedTags.isNotEmpty() || selectedCategories.isNotEmpty() || selectedPriorities.isNotEmpty() || searchQuery.isNotEmpty()) {
                        Surface(
                            onClick = {
                                onClearTags()
                                onClearCategories()
                                onClearPriorities()
                                onSearchChange("")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("clear_all_filters_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear All Filters",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Clear All Filters",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Grouped or flat item list
            if (displayItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No items match your filter criteria",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting tags, categories, or clear the search query",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else if (groupByList) {
                val groupedByList = displayItems.groupBy { it.listId }
                groupedByList.forEach { (listId, listItems) ->
                    val parentList = listsMap[listId]
                    item(key = "header_list_$listId") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = parentList?.name ?: "General List",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${listItems.size})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    items(listItems, key = { it.id }) { item ->
                        val isDragging = activeDragId == item.id
                        ItemRowCard(
                            item = item,
                            listName = null,
                            viewMode = viewMode,
                            onToggleCheck = { onToggleCheck(item.id, it) },
                            onClickItem = { onClickItem(item) },
                            onListBadgeClick = { onNavigateToList(item.listId) },
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    val parent = coordinates.parentLayoutCoordinates
                                    if (parent != null) {
                                        val top = coordinates.positionInParent().y
                                        val height = coordinates.size.height.toFloat()
                                        itemBounds[item.id] = Pair(top, top + height)
                                    }
                                }
                                .zIndex(if (isDragging) 3f else 1f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                                }
                                .pointerInput(item.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            activeDragId = item.id
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            val currentId = activeDragId ?: return@detectDragGesturesAfterLongPress
                                            change.consume()
                                            dragOffsetY += dragAmount.y

                                            val currIndex = displayItems.indexOfFirst { it.id == currentId }
                                            if (currIndex != -1) {
                                                val currBounds = itemBounds[currentId]
                                                if (currBounds != null) {
                                                    val currentCenterY = (currBounds.first + currBounds.second) / 2f + dragOffsetY

                                                    // Swap next (downward)
                                                    if (currIndex < displayItems.lastIndex) {
                                                        val nextId = displayItems[currIndex + 1].id
                                                        val nextBounds = itemBounds[nextId]
                                                        if (nextBounds != null && currentCenterY > (nextBounds.first + nextBounds.second) / 2f) {
                                                            val mutable = displayItems.toMutableList()
                                                            val itemToMove = mutable.removeAt(currIndex)
                                                            mutable.add(currIndex + 1, itemToMove)
                                                            displayItems = mutable
                                                            dragOffsetY -= (nextBounds.second - nextBounds.first)
                                                        }
                                                    }

                                                    // Swap prev (upward)
                                                    if (currIndex > 0) {
                                                        val prevId = displayItems[currIndex - 1].id
                                                        val prevBounds = itemBounds[prevId]
                                                        if (prevBounds != null && currentCenterY < (prevBounds.first + prevBounds.second) / 2f) {
                                                            val mutable = displayItems.toMutableList()
                                                            val itemToMove = mutable.removeAt(currIndex)
                                                            mutable.add(currIndex - 1, itemToMove)
                                                            displayItems = mutable
                                                            dragOffsetY += (prevBounds.second - prevBounds.first)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            activeDragId = null
                                            dragOffsetY = 0f
                                            onReorderItems(displayItems.map { it.id })
                                        },
                                        onDragCancel = {
                                            activeDragId = null
                                            dragOffsetY = 0f
                                            onReorderItems(displayItems.map { it.id })
                                        }
                                    )
                                }
                        )
                    }
                }
            } else if (groupByCategory) {
                val groupedByCategory = displayItems.groupBy { it.effectiveCategory }
                groupedByCategory.forEach { (catName, catItems) ->
                    item(key = "header_cat_$catName") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = catName.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${catItems.size})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    items(catItems, key = { it.id }) { item ->
                        val parentList = listsMap[item.listId]
                        val isDragging = activeDragId == item.id
                        ItemRowCard(
                            item = item,
                            listName = parentList?.name,
                            viewMode = viewMode,
                            onToggleCheck = { onToggleCheck(item.id, it) },
                            onClickItem = { onClickItem(item) },
                            onListBadgeClick = { onNavigateToList(item.listId) },
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    val parent = coordinates.parentLayoutCoordinates
                                    if (parent != null) {
                                        val top = coordinates.positionInParent().y
                                        val height = coordinates.size.height.toFloat()
                                        itemBounds[item.id] = Pair(top, top + height)
                                    }
                                }
                                .zIndex(if (isDragging) 3f else 1f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                                }
                                .pointerInput(item.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            activeDragId = item.id
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            val currentId = activeDragId ?: return@detectDragGesturesAfterLongPress
                                            change.consume()
                                            dragOffsetY += dragAmount.y

                                            val currIndex = displayItems.indexOfFirst { it.id == currentId }
                                            if (currIndex != -1) {
                                                val currBounds = itemBounds[currentId]
                                                if (currBounds != null) {
                                                    val currentCenterY = (currBounds.first + currBounds.second) / 2f + dragOffsetY

                                                    // Swap next (downward)
                                                    if (currIndex < displayItems.lastIndex) {
                                                        val nextId = displayItems[currIndex + 1].id
                                                        val nextBounds = itemBounds[nextId]
                                                        if (nextBounds != null && currentCenterY > (nextBounds.first + nextBounds.second) / 2f) {
                                                            val mutable = displayItems.toMutableList()
                                                            val itemToMove = mutable.removeAt(currIndex)
                                                            mutable.add(currIndex + 1, itemToMove)
                                                            displayItems = mutable
                                                            dragOffsetY -= (nextBounds.second - nextBounds.first)
                                                        }
                                                    }

                                                    // Swap prev (upward)
                                                    if (currIndex > 0) {
                                                        val prevId = displayItems[currIndex - 1].id
                                                        val prevBounds = itemBounds[prevId]
                                                        if (prevBounds != null && currentCenterY < (prevBounds.first + prevBounds.second) / 2f) {
                                                            val mutable = displayItems.toMutableList()
                                                            val itemToMove = mutable.removeAt(currIndex)
                                                            mutable.add(currIndex - 1, itemToMove)
                                                            displayItems = mutable
                                                            dragOffsetY += (prevBounds.second - prevBounds.first)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            activeDragId = null
                                            dragOffsetY = 0f
                                            onReorderItems(displayItems.map { it.id })
                                        },
                                        onDragCancel = {
                                            activeDragId = null
                                            dragOffsetY = 0f
                                            onReorderItems(displayItems.map { it.id })
                                        }
                                    )
                                }
                        )
                    }
                }
            } else {
                items(displayItems, key = { it.id }) { item ->
                    val parentList = listsMap[item.listId]
                    val isDragging = activeDragId == item.id
                    ItemRowCard(
                        item = item,
                        listName = parentList?.name,
                        viewMode = viewMode,
                        onToggleCheck = { onToggleCheck(item.id, it) },
                        onClickItem = { onClickItem(item) },
                        onListBadgeClick = { onNavigateToList(item.listId) },
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                val parent = coordinates.parentLayoutCoordinates
                                if (parent != null) {
                                    val top = coordinates.positionInParent().y
                                    val height = coordinates.size.height.toFloat()
                                    itemBounds[item.id] = Pair(top, top + height)
                                }
                            }
                            .zIndex(if (isDragging) 3f else 1f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                            }
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        activeDragId = item.id
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        val currentId = activeDragId ?: return@detectDragGesturesAfterLongPress
                                        change.consume()
                                        dragOffsetY += dragAmount.y

                                        val currIndex = displayItems.indexOfFirst { it.id == currentId }
                                        if (currIndex != -1) {
                                            val currBounds = itemBounds[currentId]
                                            if (currBounds != null) {
                                                val currentCenterY = (currBounds.first + currBounds.second) / 2f + dragOffsetY

                                                // Swap next (downward)
                                                if (currIndex < displayItems.lastIndex) {
                                                    val nextId = displayItems[currIndex + 1].id
                                                    val nextBounds = itemBounds[nextId]
                                                    if (nextBounds != null && currentCenterY > (nextBounds.first + nextBounds.second) / 2f) {
                                                        val mutable = displayItems.toMutableList()
                                                        val itemToMove = mutable.removeAt(currIndex)
                                                        mutable.add(currIndex + 1, itemToMove)
                                                        displayItems = mutable
                                                        dragOffsetY -= (nextBounds.second - nextBounds.first)
                                                    }
                                                }

                                                // Swap prev (upward)
                                                if (currIndex > 0) {
                                                    val prevId = displayItems[currIndex - 1].id
                                                    val prevBounds = itemBounds[prevId]
                                                    if (prevBounds != null && currentCenterY < (prevBounds.first + prevBounds.second) / 2f) {
                                                        val mutable = displayItems.toMutableList()
                                                        val itemToMove = mutable.removeAt(currIndex)
                                                        mutable.add(currIndex - 1, itemToMove)
                                                        displayItems = mutable
                                                        dragOffsetY += (prevBounds.second - prevBounds.first)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        activeDragId = null
                                        dragOffsetY = 0f
                                        onReorderItems(displayItems.map { it.id })
                                    },
                                    onDragCancel = {
                                        activeDragId = null
                                        dragOffsetY = 0f
                                        onReorderItems(displayItems.map { it.id })
                                    }
                                )
                            }
                    )
                }
            }
        }

        // Floating Action Button for adding new item
        FloatingActionButton(
            onClick = onAddNewItem,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_item_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Item", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Item", fontWeight = FontWeight.Bold)
            }
        }

        // --- DIALOGS FOR NEW TAG AND NEW CATEGORY FROM DROPDOWNS ---
        if (showAddTagDialog) {
            AlertDialog(
                onDismissRequest = { showAddTagDialog = false },
                title = { Text("Add New Tag", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newTagNameInput,
                        onValueChange = { newTagNameInput = it },
                        label = { Text("Tag Name") },
                        placeholder = { Text("e.g. idea, home, work...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTagNameInput.isNotBlank()) {
                                onAddTag(newTagNameInput.trim())
                                showAddTagDialog = false
                                newTagNameInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Tag")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showAddTagDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add New Category", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newCategoryNameInput,
                            onValueChange = { newCategoryNameInput = it },
                            label = { Text("Category Name") },
                            placeholder = { Text("e.g. Work, Gym, Books...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Category Icon", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        IconPickerGrid(
                            selectedIconName = newCategoryIconInput,
                            onSelectIcon = { newCategoryIconInput = it },
                            accentColor = try { Color(android.graphics.Color.parseColor(newCategoryColorInput)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Category Color", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("#6366F1", "#F59E0B", "#EC4899", "#10B981", "#3B82F6", "#8B5CF6", "#0284C7", "#EF4444").forEach { colorHex ->
                                val c = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                                val isSelected = newCategoryColorInput.equals(colorHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { newCategoryColorInput = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCategoryNameInput.isNotBlank()) {
                                onAddCategory(newCategoryNameInput.trim(), newCategoryIconInput, newCategoryColorInput)
                                showAddCategoryDialog = false
                                newCategoryNameInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Category")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showAddCategoryDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
