package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.ui.components.ItemRowCard
import com.example.ui.components.ItemViewMode
import com.example.ui.components.getVectorForIconName
import com.example.ui.theme.PrimaryPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListDetailScreen(
    list: ListEntity,
    items: List<ListItemEntity>,
    onBackClick: () -> Unit,
    onEditListClick: (ListEntity) -> Unit,
    onToggleHideChecked: (ListEntity) -> Unit,
    onClearCompleted: (Long) -> Unit,
    onToggleCheck: (Long, Boolean) -> Unit,
    onClickItem: (ListItemEntity) -> Unit,
    onAddNewItem: (Long) -> Unit,
    onReorderItems: (List<Long>) -> Unit = {},
    onLoadSectionOrder: (Long) -> List<String> = { emptyList() },
    onSaveSectionOrder: (Long, List<String>) -> Unit = { _, _ -> },
    onRenameSection: (Long, String, String) -> Unit = { _, _, _ -> },
    onUpdateItemSection: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ItemViewMode.CONDENSED) }

    // Section Management State
    var customSectionOrder by remember(list.id) { mutableStateOf(onLoadSectionOrder(list.id)) }
    var collapsedSections by remember { mutableStateOf(setOf<String>()) } // Expanded by default
    var showCreateSectionDialog by remember { mutableStateOf(false) }
    var newSectionNameInput by remember { mutableStateOf("") }
    var sectionToEdit by remember { mutableStateOf<String?>(null) }
    var editSectionNameInput by remember { mutableStateOf("") }

    val listColor = remember(list.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(list.colorHex))
        } catch (_: Exception) {
            PrimaryPurple
        }
    }

    var displayItems by remember(items, list.hideCheckedItems, searchQuery) {
        var result = items
        if (list.hideCheckedItems) {
            result = result.filter { !it.isChecked }
        }
        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true)
            }
        }
        mutableStateOf(result)
    }

    // Item drag-and-drop state
    var activeDragId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemBounds = remember { mutableMapOf<Long, Pair<Float, Float>>() }
    val sectionBounds = remember { mutableMapOf<String, Pair<Float, Float>>() }

    // Compute all sections ordered by customSectionOrder
    val allSections = remember(displayItems, customSectionOrder) {
        val sectionsFromItems = displayItems.map { if (it.section.isNotBlank()) it.section.trim() else "General" }.distinct()
        val combined = (customSectionOrder + sectionsFromItems).distinct()
        if (combined.isEmpty()) listOf("General") else combined
    }

    val createdDateStr = remember(list.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(list.createdAt))
    }
    val updatedDateStr = remember(list.updatedAt) {
        SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(list.updatedAt))
    }

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

    fun moveSectionUp(secName: String) {
        val idx = allSections.indexOf(secName)
        if (idx > 0) {
            val mutable = allSections.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(idx - 1, item)
            customSectionOrder = mutable
            onSaveSectionOrder(list.id, mutable)
        }
    }

    fun moveSectionDown(secName: String) {
        val idx = allSections.indexOf(secName)
        if (idx != -1 && idx < allSections.lastIndex) {
            val mutable = allSections.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(idx + 1, item)
            customSectionOrder = mutable
            onSaveSectionOrder(list.id, mutable)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("list_detail_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // More menu (includes Create Section, Hide/Show Checked Items, Edit, Share, Clear)
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Create Section") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        newSectionNameInput = ""
                                        showCreateSectionDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (list.hideCheckedItems) "Show Checked Items" else "Hide Checked Items") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (list.hideCheckedItems) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleHideChecked(list)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit List Details") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEditListClick(list)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share List as Text") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        val textToShare = formatListAsText(list, displayItems)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, list.name)
                                            putExtra(Intent.EXTRA_TEXT, textToShare)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(shareIntent, "Share List"))
                                        } catch (_: Throwable) {
                                            Toast.makeText(context, "Unable to share list", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Completed Items") },
                                    leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onClearCompleted(list.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // List Header Card (with icon, title, description, and dates)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, listColor.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(listColor.copy(alpha = 0.15f))
                                    .border(1.dp, listColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (list.customIconUri != null) {
                                    AsyncImage(
                                        model = list.customIconUri,
                                        contentDescription = list.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = getVectorForIconName(list.iconName),
                                        contentDescription = list.name,
                                        tint = listColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${items.size} total items",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        if (!list.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = list.description,
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Created: $createdDateStr",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            )
                            Text(
                                text = "Updated: $updatedDateStr",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            )
                        }
                    }
                }
            }

            // Items List Header & View Mode Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ITEMS (${displayItems.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (list.hideCheckedItems) {
                            Text(
                                text = "Checked items hidden",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        // View Mode Toggle Icons (Minimum, Condensed, Maximum)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                    }
                }
            }

            // Items grouped by section with collapsible headers, up/down move arrows, and edit options
            if (displayItems.isEmpty()) {
                item {
                    EmptyItemsPlaceholder(list.name) { onAddNewItem(list.id) }
                }
            } else {
                allSections.forEach { secName ->
                    val secItems = displayItems.filter {
                        val sec = if (it.section.isNotBlank()) it.section.trim() else "General"
                        sec.equals(secName, ignoreCase = true)
                    }
                    val isCollapsed = secName in collapsedSections

                    item(key = "section_header_$secName") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 2.dp)
                                .onGloballyPositioned { coords ->
                                    val parent = coords.parentLayoutCoordinates
                                    if (parent != null) {
                                        val top = coords.positionInParent().y
                                        val height = coords.size.height.toFloat()
                                        sectionBounds[secName] = Pair(top, top + height)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Collapse / Expand Chevron & Title
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            collapsedSections = if (isCollapsed) collapsedSections - secName else collapsedSections + secName
                                        }
                                ) {
                                    Icon(
                                        imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Expand/Collapse Section",
                                        tint = listColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = secName.uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${secItems.size})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                }

                                // Section Moving (Up / Down Arrows) & Edit Options
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Move Up Arrow
                                    IconButton(
                                        onClick = { moveSectionUp(secName) },
                                        enabled = allSections.indexOf(secName) > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Section Up",
                                            tint = if (allSections.indexOf(secName) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Move Down Arrow
                                    IconButton(
                                        onClick = { moveSectionDown(secName) },
                                        enabled = allSections.indexOf(secName) < allSections.lastIndex,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Section Down",
                                            tint = if (allSections.indexOf(secName) < allSections.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Edit Section Pencil Button
                                    IconButton(
                                        onClick = {
                                            sectionToEdit = secName
                                            editSectionNameInput = secName
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Section",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Render Section Items (Only when Expanded)
                    if (!isCollapsed) {
                        items(secItems, key = { it.id }) { item ->
                            val isDragging = activeDragId == item.id
                            ItemRowCard(
                                item = item,
                                listName = list.name,
                                viewMode = viewMode,
                                onToggleCheck = { onToggleCheck(item.id, it) },
                                onClickItem = { onClickItem(item) },
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

                                                        // Check if dragged item crossed into a different section
                                                        val targetSection = allSections.find { sec ->
                                                            val sb = sectionBounds[sec]
                                                            sb != null && currentCenterY >= sb.first && currentCenterY <= sb.second
                                                        }
                                                        if (targetSection != null) {
                                                            val activeItem = displayItems.find { it.id == currentId }
                                                            if (activeItem != null) {
                                                                val activeSec = if (activeItem.section.isNotBlank()) activeItem.section.trim() else "General"
                                                                if (!activeSec.equals(targetSection, ignoreCase = true)) {
                                                                    onUpdateItemSection(currentId, targetSection)
                                                                }
                                                            }
                                                        }

                                                        // Check swap with next item (downward)
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

                                                        // Check swap with previous item (upward)
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
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { onAddNewItem(list.id) },
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Item", fontWeight = FontWeight.Bold)
            }
        }

        // --- CREATE SECTION DIALOG ---
        if (showCreateSectionDialog) {
            AlertDialog(
                onDismissRequest = { showCreateSectionDialog = false },
                title = { Text("Create New Section", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newSectionNameInput,
                        onValueChange = { newSectionNameInput = it },
                        label = { Text("Section Name") },
                        placeholder = { Text("e.g. Work, Ideas, Shopping...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSectionNameInput.isNotBlank()) {
                                val secName = newSectionNameInput.trim()
                                val updatedOrder = (customSectionOrder + secName).distinct()
                                customSectionOrder = updatedOrder
                                onSaveSectionOrder(list.id, updatedOrder)
                                showCreateSectionDialog = false
                                newSectionNameInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Create Section")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showCreateSectionDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- EDIT SECTION DIALOG ---
        sectionToEdit?.let { oldSecName ->
            AlertDialog(
                onDismissRequest = { sectionToEdit = null },
                title = { Text("Edit Section Name", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editSectionNameInput,
                        onValueChange = { editSectionNameInput = it },
                        label = { Text("Section Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editSectionNameInput.isNotBlank()) {
                                onRenameSection(list.id, oldSecName, editSectionNameInput.trim())
                                sectionToEdit = null
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { sectionToEdit = null },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyItemsPlaceholder(listName: String, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No items in $listName",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Add tasks with checkboxes or notes with rich bullet points.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

private fun formatListAsText(list: ListEntity, items: List<ListItemEntity>): String {
    val sb = StringBuilder()
    sb.append("📋 ").append(list.name).append("\n")
    if (!list.description.isNullOrBlank()) {
        sb.append(list.description).append("\n")
    }
    sb.append("\n")

    val hasSections = items.any { it.section.isNotBlank() }
    if (hasSections) {
        val grouped = items.groupBy { if (it.section.isNotBlank()) it.section.trim() else "General" }
        grouped.forEach { (sectionName, secItems) ->
            sb.append("📌 ").append(sectionName.uppercase()).append("\n")
            secItems.forEach { item ->
                if (item.type == "TASK") {
                    val checkMark = if (item.isChecked) "[x]" else "[ ]"
                    sb.append("  ").append(checkMark).append(" ").append(item.title).append("\n")
                } else {
                    sb.append("  • ").append(item.title).append("\n")
                }
            }
            sb.append("\n")
        }
    } else {
        items.forEach { item ->
            if (item.type == "TASK") {
                val checkMark = if (item.isChecked) "[x]" else "[ ]"
                sb.append(checkMark).append(" ").append(item.title).append("\n")
            } else {
                sb.append("• ").append(item.title).append("\n")
            }
        }
    }

    return sb.toString().trim()
}
