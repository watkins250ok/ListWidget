package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.PrimaryPurple
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.ui.components.ItemRowCard
import com.example.ui.components.getVectorForIconName
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    val listColor = remember(list.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(list.colorHex))
        } catch (_: Exception) {
            PrimaryPurple
        }
    }

    val displayItems = remember(items, list.hideCheckedItems, searchQuery) {
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
        result
    }

    val groupedSections = remember(displayItems) {
        val hasSections = displayItems.any { it.section.isNotBlank() }
        if (!hasSections) {
            null
        } else {
            displayItems.groupBy {
                if (it.section.isNotBlank()) it.section.trim() else "General"
            }
        }
    }

    val createdDateStr = remember(list.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(list.createdAt))
    }
    val updatedDateStr = remember(list.updatedAt) {
        SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(list.updatedAt))
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
                        // Hide checked toggle icon
                        IconButton(onClick = { onToggleHideChecked(list) }) {
                            Icon(
                                imageVector = if (list.hideCheckedItems) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Hide Checked",
                                tint = if (list.hideCheckedItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // More menu
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, listColor.copy(alpha = 0.25f))
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

            // Items List Header & Search
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

                    if (list.hideCheckedItems) {
                        Text(
                            text = "Checked items hidden",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Items
            if (displayItems.isEmpty()) {
                item {
                    EmptyItemsPlaceholder(list.name) { onAddNewItem(list.id) }
                }
            } else if (groupedSections != null) {
                groupedSections.forEach { (sectionName, sectionItems) ->
                    item(key = "section_header_$sectionName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(listColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sectionName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${sectionItems.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                    items(sectionItems, key = { it.id }) { item ->
                        ItemRowCard(
                            item = item,
                            onToggleCheck = { onToggleCheck(item.id, it) },
                            onClickItem = { onClickItem(item) }
                        )
                    }
                }
            } else {
                items(displayItems, key = { it.id }) { item ->
                    ItemRowCard(
                        item = item,
                        onToggleCheck = { onToggleCheck(item.id, it) },
                        onClickItem = { onClickItem(item) }
                    )
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
